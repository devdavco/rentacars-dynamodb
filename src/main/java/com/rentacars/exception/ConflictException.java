package com.rentacars.exception;

/**
 * Lanzar esta excepcion cuando la operacion choca con el ESTADO ACTUAL de los datos,
 * por ejemplo borrar un registro que todavia tiene otros asociados (FK).
 * El GlobalExceptionHandler la convierte automaticamente en 409 Conflict.
 *
 * Ejemplo de uso:
 *
 *     if (autoRepository.existsByIdTienda(id)) {
 *         throw new ConflictException("No se puede eliminar la tienda porque tiene autos asociados");
 *     }
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String mensaje) {
        super(mensaje);
    }
}
