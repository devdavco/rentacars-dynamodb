package com.rentacars.service;

import com.rentacars.dto.request.CreateAutoRequest;
import com.rentacars.dto.response.CreateAutoResponse;
import com.rentacars.dto.response.CreateDetalleAutoResponse;
import com.rentacars.dto.request.UpdateAutoRequest;
import com.rentacars.dto.request.UpdateDetalleAutoRequest;

/**
 * Interfaz Service del dominio Auto.
 * Cada HU agrega UNA linea aqui. No borren ni reescriban las de los demas.
 *   HU-09 (Suarez) -> buscarAutos
 *   HU-10 (Suarez) -> actualizarDetalles
 *   HU-11 (Suarez) -> actualizarDisponibilidad
 *   HU-12 (Cardona) -> getAutoById ahora retorna el detalle completo
 *   HU-13 (Cardona) -> deleteAuto valida disponibilidad y borra en cascada
 */


public interface AutoService {
  
    // HU-09 (Suarez)
    java.util.List<CreateAutoResponse> buscarAutos (String ciudad, Long idCategoria);

    // HU-11 (Suarez)
    CreateAutoResponse actualizarDisponibilidad (Long id, UpdateAutoRequest updateAutoRequest);

    // HU-10 (Suarez)
    CreateAutoResponse actualizarDetalles (Long id, UpdateDetalleAutoRequest updateDetalle_autoRequest);


    CreateAutoResponse createAuto(CreateAutoRequest createAutoRequest) throws Exception;

    // HU-12 (Cardona): detalle completo del auto (autos + detalles_autos) con precio calculado
    CreateDetalleAutoResponse getAutoById(Long id);

    //delete
    // HU-13 (Cardona): borra detalle y auto, valida disponibilidad
    void deleteAuto(Long id);

}
