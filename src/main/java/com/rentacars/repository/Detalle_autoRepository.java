package com.rentacars.repository;

import com.rentacars.model.DetalleAuto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Detalle_autoRepository extends JpaRepository<DetalleAuto, Long> {

    // HU-12 (Cardona): busca la ficha comercial de un auto a partir de su id_auto
    Optional<DetalleAuto> findByIdAuto(Long idAuto);

}