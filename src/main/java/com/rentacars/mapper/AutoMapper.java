package com.rentacars.mapper;

import com.rentacars.dto.response.CreateAutoResponse;
import com.rentacars.dto.response.CreateDetalleAutoResponse;
import com.rentacars.dto.request.CreateAutoRequest;
import com.rentacars.model.Auto;
import com.rentacars.model.DetalleAuto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@AllArgsConstructor
public class AutoMapper {

    // HU-08 (Cifuentes): construye la entidad Auto desde el request -- implementado por Claude.
    // La disponibilidad NUNCA viene del cliente: la regla de negocio dice que
    // siempre inicia en true al registrar el auto.
    public static Auto createAutoRequestToEntity(CreateAutoRequest createAutoRequest){
        return Auto.builder()
                .disponibilidad(true)
                .idTienda(createAutoRequest.getIdTienda())
                .idCategoria(createAutoRequest.getIdCategoria())
                .build();
    }

    // HU-08 (Cifuentes): construye la ficha comercial (detalles_autos) -- implementado por Claude.
    // Se llama DESPUES de guardar el Auto, porque necesita el id_auto ya generado.
    public static DetalleAuto createAutoRequestToDetalleEntity(CreateAutoRequest createAutoRequest, Long idAuto) {
        DetalleAuto detalle = new DetalleAuto();
        detalle.setModelo(createAutoRequest.getModelo());
        detalle.setMarca(createAutoRequest.getMarca());
        detalle.setAnio(createAutoRequest.getAnio());
        detalle.setPlaca(createAutoRequest.getPlaca());
        detalle.setPrecioDia(createAutoRequest.getPrecioDia());
        detalle.setOfertaPorcentaje(createAutoRequest.getOfertaPorcentaje());
        detalle.setImagen(createAutoRequest.getImagen());
        detalle.setIdAuto(idAuto);
        return detalle;
    }

    // HU-08 (Cifuentes): combina el Auto y el Detalle_auto recien creados en un solo response -- implementado por Claude.
    public static CreateAutoResponse entityToCreateAutoResponseConDetalle(Auto auto, DetalleAuto detalle) {
        return CreateAutoResponse.builder()
                .idAuto(auto.getIdAuto())
                .disponibilidad(auto.getDisponibilidad())
                .idTienda(auto.getIdTienda())
                .idCategoria(auto.getIdCategoria())
                .modelo(detalle.getModelo())
                .marca(detalle.getMarca())
                .precioDia(detalle.getPrecioDia())
                .ofertaPorcentaje(detalle.getOfertaPorcentaje())
                .imagen(detalle.getImagen())
                .build();
    }

    // HU-12 (cardona): combina Auto y detalle_auto en un solo response, con el precio con oferta ya calculado
    public static CreateDetalleAutoResponse entityToCreateDetalle_autoResponse(Auto auto, DetalleAuto detalle) {

        //la oferta puede ser null (auto sin oferta), se trata como 0
        BigDecimal oferta = detalle.getOfertaPorcentaje();
        if (oferta == null) {
            oferta = BigDecimal.ZERO;
        }

        //calcula el descuento y el precio con oferta
        BigDecimal descuento = detalle.getPrecioDia()
                .multiply(oferta)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal precioConOferta = detalle.getPrecioDia().subtract(descuento);

        return CreateDetalleAutoResponse.builder()
                .idAuto(auto.getIdAuto())
                .modelo(detalle.getModelo())
                .marca(detalle.getMarca())
                .anio(detalle.getAnio())
                .placa(detalle.getPlaca())
                .precioDia(detalle.getPrecioDia())
                .ofertaPorcentaje(detalle.getOfertaPorcentaje())
                .precioConOferta(precioConOferta)
                .disponibilidad(auto.getDisponibilidad())
                .build();
    }


}