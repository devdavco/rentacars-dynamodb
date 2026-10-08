package com.rentacars.service.impl;

import com.rentacars.exception.BadRequestException;
import com.rentacars.exception.ConflictException;
import com.rentacars.exception.ErrorResponse;
import com.rentacars.exception.GlobalExceptionHandler;
import com.rentacars.mapper.TiendaMapper;
import com.rentacars.model.Auto;
import com.rentacars.model.Tienda;
import com.rentacars.repository.AlquilerRepository;
import com.rentacars.repository.AutoRepository;
import com.rentacars.repository.Detalle_autoRepository;
import com.rentacars.repository.TiendaRepository;
import com.rentacars.service.CategoriaService;
import com.rentacars.service.TiendaService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EliminarConDependenciasTest {

    private final TiendaRepository tiendaRepository = mock(TiendaRepository.class);
    private final AutoRepository autoRepository = mock(AutoRepository.class);
    private final AlquilerRepository alquilerRepository = mock(AlquilerRepository.class);
    private final Detalle_autoRepository detalleAutoRepository = mock(Detalle_autoRepository.class);

    private final TiendaServiceImpl tiendaService =
            new TiendaServiceImpl(tiendaRepository, mock(TiendaMapper.class), autoRepository);
    private final AutoServiceImpl autoService = new AutoServiceImpl(
            autoRepository, detalleAutoRepository, alquilerRepository,
            mock(TiendaService.class), mock(CategoriaService.class));

    @Test
    void tiendaConAutosLanzaConflictoYNoBorra() {
        Tienda tienda = new Tienda();
        when(tiendaRepository.findById(1L)).thenReturn(Optional.of(tienda));
        when(autoRepository.existsByIdTienda(1L)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> tiendaService.eliminarTienda(1L));
        assertEquals("No se puede eliminar la tienda porque tiene autos asociados", ex.getMessage());
        verify(tiendaRepository, never()).delete(any());
    }

    @Test
    void tiendaSinAutosSeBorra() {
        Tienda tienda = new Tienda();
        when(tiendaRepository.findById(1L)).thenReturn(Optional.of(tienda));
        when(autoRepository.existsByIdTienda(1L)).thenReturn(false);

        tiendaService.eliminarTienda(1L);
        verify(tiendaRepository).delete(tienda);
    }

    @Test
    void autoConAlquileresLanzaConflictoYNoBorra() {
        Auto auto = new Auto();
        auto.setDisponibilidad(true);
        when(autoRepository.findById(5L)).thenReturn(Optional.of(auto));
        when(alquilerRepository.existsByIdAuto(5L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> autoService.deleteAuto(5L));
        verify(autoRepository, never()).delete(any());
        verify(detalleAutoRepository, never()).delete(any());
    }

    @Test
    void autoAlquiladoSigueSiendo400() {
        Auto auto = new Auto();
        auto.setDisponibilidad(false);
        when(autoRepository.findById(5L)).thenReturn(Optional.of(auto));

        assertThrows(BadRequestException.class, () -> autoService.deleteAuto(5L));
    }

    @Test
    void handlerTraduceA409SinDetallesTecnicos() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ErrorResponse> r1 = handler.manejarConflicto(new ConflictException("msg"));
        assertEquals(409, r1.getStatusCode().value());
        assertEquals("msg", r1.getBody().getMensaje());

        ResponseEntity<ErrorResponse> r2 = handler.manejarIntegridadDatos(
                new DataIntegrityViolationException("violates foreign key constraint \"autos_id_tienda_fkey\""));
        assertEquals(409, r2.getStatusCode().value());
        assertFalse(r2.getBody().getMensaje().contains("fkey"));
    }
}
