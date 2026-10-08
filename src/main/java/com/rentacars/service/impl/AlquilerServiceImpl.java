package com.rentacars.service.impl;

import com.rentacars.dto.request.CreateAlquilerRequest;
import com.rentacars.dto.request.UpdateAutoRequest;
import com.rentacars.dto.response.CreateAlquilerResponse;
import com.rentacars.dto.response.CreateDetalleAutoResponse;
import com.rentacars.exception.BadRequestException;
import com.rentacars.exception.ResourceNotFoundException;
import com.rentacars.mapper.AlquilerMapper;
import com.rentacars.model.Alquiler;
import com.rentacars.repository.AlquilerRepository;
import com.rentacars.service.AlquilerService;
import com.rentacars.service.AutoService;
import com.rentacars.service.ClienteService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@AllArgsConstructor
public class AlquilerServiceImpl implements AlquilerService {

    private final AlquilerRepository alquilerRepository;

    // llama actualizarDisponibilidad al cancelar, y al crear/devolver un alquiler
    private final AutoService autoService;

    // HU-18 (Pedroza): valida que el cliente exista -- implementado por Claude
    private final ClienteService clienteService;

    //obtiene alquiler segun id
    @Override
    public CreateAlquilerResponse getAlquilerById(Long id) {

        Alquiler alquiler = alquilerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alquiler no encontrado con id " + id));
        CreateAlquilerResponse createAlquilerResponse = AlquilerMapper.entityToCreateAlquilerResponse(alquiler);
        return createAlquilerResponse;
    }

    // HU-18 (Pedroza): crear alquiler -- implementado por Claude.
    //
    // Corregido: la version anterior no validaba que el cliente existiera ni que el auto
    // estuviera disponible, dejaba que el cliente inventara precio_total y estado, y nunca
    // marcaba el auto como no disponible. Ahora:
    //   1. Valida que el cliente exista (404) -- clienteService.obtenerCliente(id).
    //   2. Obtiene el detalle del auto (404 si no existe) y valida que este disponible (400).
    //   3. Valida que fecha_inicio sea posterior a hoy (400).
    //   4. Calcula precio_total = dias * precio_dia * (1 - oferta/100).
    //   5. Guarda el alquiler y marca el auto como no disponible.
    @Override
    public CreateAlquilerResponse createAlquiler(CreateAlquilerRequest createAlquilerRequest) throws Exception {

        clienteService.obtenerCliente(createAlquilerRequest.getIdCliente());

        CreateDetalleAutoResponse auto = autoService.getAutoById(createAlquilerRequest.getIdAuto());

        if (!Boolean.TRUE.equals(auto.getDisponibilidad())) {
            throw new BadRequestException("El auto no esta disponible");
        }

        if (!createAlquilerRequest.getFechaInicio().isAfter(LocalDate.now())) {
            throw new BadRequestException("La fecha de inicio debe ser posterior a hoy");
        }

        if (createAlquilerRequest.getFechaFin().isBefore(createAlquilerRequest.getFechaInicio())) {
            throw new BadRequestException("La fechaFin no puede ser anterior a la fechaInicio");
        }

        long dias = ChronoUnit.DAYS.between(createAlquilerRequest.getFechaInicio(), createAlquilerRequest.getFechaFin());

        BigDecimal oferta = auto.getOfertaPorcentaje();
        if (oferta == null) {
            oferta = BigDecimal.ZERO;
        }
        BigDecimal descuento = auto.getPrecioDia()
                .multiply(oferta)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal precioConOferta = auto.getPrecioDia().subtract(descuento);
        BigDecimal precioTotal = precioConOferta.multiply(BigDecimal.valueOf(dias));

        Alquiler alquiler = AlquilerMapper.createAlquilerRequestToEntity(createAlquilerRequest, precioTotal);
        alquiler = alquilerRepository.save(alquiler);

        // marca el auto como no disponible, ahora que quedo reservado
        autoService.actualizarDisponibilidad(createAlquilerRequest.getIdAuto(), new UpdateAutoRequest(false));

        return AlquilerMapper.entityToCreateAlquilerResponse(alquiler);
    }

    // HU-20 (Pedroza): historial de alquileres de un cliente -- implementado por Claude
    @Override
    public List<CreateAlquilerResponse> historialPorCliente(Long idCliente) {
        List<Alquiler> alquileres = alquilerRepository.findByIdCliente(idCliente);
        return AlquilerMapper.entityToListCreateAlquilerResponse(alquileres);
    }

    // HU-21 (Pedroza): activo = estado ACTIVO y fecha_fin >= hoy (regla del backlog)
    @Override
    public List<CreateAlquilerResponse> listarActivos() {
        List<Alquiler> activos = alquilerRepository.findByEstadoAndFechaFinGreaterThanEqual("ACTIVO", LocalDate.now());
        return AlquilerMapper.entityToListCreateAlquilerResponse(activos);
    }

    // HU-24 (Corrales): registrar devolucion de auto -- implementado por Claude.
    // Cierra el alquiler (estado = CERRADO) y libera el auto inyectando AutoService
    // (Cambio v2: antes hubiera sido CatalogoFeignClient).
    @Override
    public CreateAlquilerResponse registrarDevolucion(Long id) {
        Alquiler alquiler = alquilerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alquiler no encontrado con id " + id));

        alquiler.setEstado("CERRADO");
        alquiler = alquilerRepository.save(alquiler);

        autoService.actualizarDisponibilidad(alquiler.getIdAuto(), new UpdateAutoRequest(true));

        return AlquilerMapper.entityToCreateAlquilerResponse(alquiler);
    }

    //metodo para eliminar alquiler
    // HU-22 (Cardona): cancela y libera el auto
    @Override
    public void deleteAlquiler(Long id) {

        //busca alquiler por id, 404 si no existe
        Alquiler alquiler = alquilerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alquiler no encontrado con id " + id));

        //bloquea cancelar si ya inicio
        if (!alquiler.getFechaInicio().isAfter(LocalDate.now())) {
            throw new BadRequestException("El alquiler ya inició, no se puede cancelar");
        }

        //borra el alquiler cancelado
        alquilerRepository.delete(alquiler);

        //arma datos para liberar auto
        UpdateAutoRequest liberarAuto = new UpdateAutoRequest(true);

        //libera el auto tras cancelar
        autoService.actualizarDisponibilidad(alquiler.getIdAuto(), liberarAuto);
    }

}
