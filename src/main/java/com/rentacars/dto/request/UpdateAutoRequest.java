package com.rentacars.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * HU-11 (Suarez): actualizar disponibilidad de auto.
 *
 *   PATCH /autos/{id}/disponibilidad
 *   { "disponibilidad": false }
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateAutoRequest {

    // HU-11 (Suarez):
    @NotNull(message = "El campo disponibilidad es obligatorio")
    private Boolean disponibilidad;

}

