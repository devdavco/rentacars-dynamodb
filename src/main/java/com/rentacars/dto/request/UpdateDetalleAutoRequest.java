package com.rentacars.dto.request;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateDetalleAutoRequest {

    private BigDecimal precioDia;
    private BigDecimal ofertaPorcentaje;
    private String imagen;


}
