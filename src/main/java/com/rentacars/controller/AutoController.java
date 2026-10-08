package com.rentacars.controller;

import com.rentacars.dto.request.CreateAutoRequest;
import com.rentacars.dto.request.UpdateAutoRequest;
import com.rentacars.dto.request.UpdateDetalleAutoRequest;
import com.rentacars.dto.response.CreateAutoResponse;
import com.rentacars.dto.response.CreateDetalleAutoResponse;
import com.rentacars.service.AutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/autos")
@Tag(name = "autos", description = "operaciones de autos")
public class AutoController {

    private final AutoService autoService;

    // HU-09 (Suarez):
    @GetMapping
    public ResponseEntity<List<CreateAutoResponse>> buscarAutos(
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false, name = "id_categoria") Long idCategoria) {
        return ResponseEntity.ok(autoService.buscarAutos(ciudad, idCategoria));
    }

    // HU-10 (Suarez):
    @PutMapping("/{id}")
    @Operation(summary = "actualizar detalles comerciales de un auto")
    public ResponseEntity<CreateAutoResponse> actualizarDetalles(
            @PathVariable Long id,
            @RequestBody UpdateDetalleAutoRequest request) {
        return ResponseEntity.ok(autoService.actualizarDetalles(id, request));
    }

    //HU-11 (SUAREZ):
    @PatchMapping ("/{id}/disponibilidad")
    public ResponseEntity<CreateAutoResponse> actualizarDisponibilidad(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAutoRequest request) {
        return ResponseEntity.ok(autoService.actualizarDisponibilidad(id, request));
    }

    //obtiene por id
    // HU-12 (Cardona): detalle completo del auto con precio calculado
    @GetMapping("/{id}")
    @Operation(summary = "ver detalle completo de un auto")
    public ResponseEntity<CreateDetalleAutoResponse> getAutoById(@PathVariable Long id){

        CreateDetalleAutoResponse autoResponse = autoService.getAutoById(id);

        return new ResponseEntity<>(
                autoResponse,
                HttpStatus.OK
        );

    }

    //hace post
    // HU-08 (Cifuentes): ruta que pide el backlog es POST /autos
    @PostMapping
    @Operation(summary = "crear auto")
    public ResponseEntity<CreateAutoResponse> createAuto(
            @Valid @RequestBody CreateAutoRequest createAutoRequest
    ) throws Exception {

        CreateAutoResponse autoCreated = autoService.createAuto(createAutoRequest);

        return new ResponseEntity<>(
                autoCreated,
                HttpStatus.CREATED
        );
    }

    //elimina auto
    // HU-13 (Cardona): el backlog pide 204 No Content, sin cuerpo
    @DeleteMapping("/{id}")
    @Operation(summary = "eliminar auto")
    public ResponseEntity<Void> deleteAuto(@PathVariable Long id) {

        //llama service delete
        autoService.deleteAuto(id);

        //devuelve 204 sin contenido
        return ResponseEntity.noContent().build();
    }

}
