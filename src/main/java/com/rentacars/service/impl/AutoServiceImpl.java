package com.rentacars.service.impl;


import com.rentacars.dto.request.*;
import com.rentacars.dto.response.CreateAutoResponse;
import com.rentacars.dto.response.CreateDetalleAutoResponse;
import com.rentacars.exception.BadRequestException;
import com.rentacars.exception.ConflictException;
import com.rentacars.exception.ResourceNotFoundException;
import com.rentacars.mapper.AutoMapper;
import com.rentacars.model.Auto;
import com.rentacars.model.DetalleAuto;
import com.rentacars.repository.AutoRepository;
import com.rentacars.repository.Detalle_autoRepository;
import com.rentacars.service.AutoService;
import com.rentacars.service.CategoriaService;
import com.rentacars.service.TiendaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// repo para chequear alquileres
import com.rentacars.repository.AlquilerRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AutoServiceImpl implements AutoService {

    private final AutoRepository autoRepository;
    private final Detalle_autoRepository detalleAutoRepository;
    // valida FK antes de borrar
    private final AlquilerRepository alquilerRepository;
    // HU-08 (Cifuentes): valida que tienda y categoria existan -- implementado por Claude
    // Cambio v2: inyeccion directa en vez de FeignClient
    private final TiendaService tiendaService;
    private final CategoriaService categoriaService;



    //HU-09
  @Override
    public List<CreateAutoResponse> buscarAutos(String ciudad, Long idCategoria) {
        List<Auto> autos = autoRepository.buscarDisponibles(ciudad, idCategoria);

        return autos.stream()
                .map(auto -> {
                    DetalleAuto detalle = detalleAutoRepository.findByIdAuto(auto.getIdAuto())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Detalle no encontrado para auto ID: " + auto.getIdAuto()));

                    CreateAutoResponse response = new CreateAutoResponse();
                    response.setIdAuto(auto.getIdAuto());
                    response.setDisponibilidad(auto.getDisponibilidad());
                    response.setModelo(detalle.getModelo());
                    response.setMarca(detalle.getMarca());
                    response.setPrecioDia(detalle.getPrecioDia());
                    response.setOfertaPorcentaje(detalle.getOfertaPorcentaje());
                    return response;
                })
                .toList();
    }

    //HU-10
    @Override
    public CreateAutoResponse actualizarDetalles(Long id, UpdateDetalleAutoRequest request) {
        Auto auto = autoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auto no encontrado con ID: " + id));

        DetalleAuto detalle = detalleAutoRepository.findByIdAuto(id)
                .orElseThrow(() -> new ResourceNotFoundException("Detalle no encontrado para auto ID: " + id));

        if (request.getPrecioDia() != null) {
            detalle.setPrecioDia(request.getPrecioDia());
        }
        if (request.getOfertaPorcentaje() != null) {
            detalle.setOfertaPorcentaje(request.getOfertaPorcentaje());
        }
        if (request.getImagen() != null) {
            detalle.setImagen(request.getImagen());
        }

        DetalleAuto detalleGuardado = detalleAutoRepository.save(detalle);

        CreateAutoResponse response = new CreateAutoResponse();
        response.setIdAuto(auto.getIdAuto());
        response.setDisponibilidad(auto.getDisponibilidad());
        response.setModelo(detalleGuardado.getModelo());
        response.setMarca(detalleGuardado.getMarca());
        response.setPrecioDia(detalleGuardado.getPrecioDia());
        response.setOfertaPorcentaje(detalleGuardado.getOfertaPorcentaje());
        response.setImagen(detalleGuardado.getImagen());
        return response;
    }

    

 
    //HU-11
   @Override
   public CreateAutoResponse actualizarDisponibilidad (Long id, UpdateAutoRequest request){
       Auto auto = autoRepository.findById(id)
               .orElseThrow(() -> new ResourceNotFoundException("Auto no encontrado con ID:"+ id));
       auto.setDisponibilidad(request.getDisponibilidad());
       Auto autoGuardado = autoRepository.save(auto);

       CreateAutoResponse response = new CreateAutoResponse();
       response.setIdAuto(autoGuardado.getIdAuto());
       response.setDisponibilidad(autoGuardado.getDisponibilidad());
       return response;
   }

    // HU-12 (Cardona): obtiene el detalle completo del auto (autos + detalles_autos), con precio calculado
    @Override
    public CreateDetalleAutoResponse getAutoById(Long id) {

        Auto auto = autoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auto no encontrado con id " + id));

        DetalleAuto detalle = detalleAutoRepository.findByIdAuto(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontraron detalles para el auto con id " + id));

        return AutoMapper.entityToCreateDetalle_autoResponse(auto, detalle);
    }

    // HU-08 (Cifuentes): registrar auto con detalles -- implementado por Claude.
    //
    // Corregido: la version anterior solo guardaba la fila de "autos" (disponibilidad,
    // idTienda, idCategoria) y nunca creaba el "detalles_autos" que pide la HU, ademas
    // de no validar que la tienda/categoria existieran. Ahora:
    //   1. Valida tienda y categoria (404 si no existen) inyectando los services.
    //   2. Guarda primero en "autos" (disponibilidad = true siempre) para obtener el id_auto.
    //   3. Guarda "detalles_autos" con ese id_auto.
    // Si falla el detalle, se borra tambien el auto (compensacion, DynamoDB no hace rollback).
    @Override
    public CreateAutoResponse createAuto(CreateAutoRequest createAutoRequest) throws Exception {

        tiendaService.getTiendaById(createAutoRequest.getIdTienda());
        categoriaService.obtenerCategoria(createAutoRequest.getIdCategoria());

        Auto auto = AutoMapper.createAutoRequestToEntity(createAutoRequest);
        auto = autoRepository.save(auto);

        DetalleAuto detalle = AutoMapper.createAutoRequestToDetalleEntity(createAutoRequest, auto.getIdAuto());
        try {
            detalle = detalleAutoRepository.save(detalle);
        } catch (RuntimeException ex) {
            // DynamoDB no hace rollback: si falla el detalle (ej. placa repetida),
            // se borra el auto recien creado para no dejarlo huerfano.
            autoRepository.delete(auto);
            throw ex;
        }

        return AutoMapper.entityToCreateAutoResponseConDetalle(auto, detalle);
    }

    //metodo para eliminar auto
    // HU-13 (Cardona): borra detalle y auto en cascada
    @Override
    public void deleteAuto(Long id) {

        //busca auto por id, 404 si no existe
        Auto auto = autoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auto no encontrado con id " + id));

        //bloquea borrado si esta alquilado
        if (Boolean.FALSE.equals(auto.getDisponibilidad())) {
            throw new BadRequestException("El auto esta alquilado, no se puede eliminar");
        }

        //bloquea si tiene alquileres asociados -> 409
        if (alquilerRepository.existsByIdAuto(id)) {
            throw new ConflictException("No se puede eliminar el auto porque tiene alquileres asociados");
        }

        //borra detalle antes del auto (el detalle es parte del auto, no bloquea el borrado)
        detalleAutoRepository.findByIdAuto(id)
                .ifPresent(detalleAutoRepository::delete);

        //borra el auto al final
        autoRepository.delete(auto);
    }

  

}