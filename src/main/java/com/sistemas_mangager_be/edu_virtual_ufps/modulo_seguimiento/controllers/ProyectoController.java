package com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.controllers;

import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.dtos.*;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.entities.enums.TipoSustentacion;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.services.ProyectoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private final ProyectoService proyectoService;

    public ProyectoController(ProyectoService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @PostMapping
    public ResponseEntity<ProyectoDto> crearProyecto(@RequestBody ProyectoDto dto) {
        return ResponseEntity.ok(proyectoService.crearProyecto(dto));
    }

    @GetMapping("/proyecto-estudiante")
    public ResponseEntity<ProyectoDto> obtenerProyectoEstudiante() {
        return ResponseEntity.ok(proyectoService.obtenerProyectoEstudiante());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProyectoDto> obtenerProyecto(@PathVariable Integer id) {
        return ResponseEntity.ok(proyectoService.obtenerProyecto(id));
    }

    @GetMapping
    public ResponseEntity<List<ProyectoDto>> listarProyectos(@RequestParam(required = false) Integer lineaId,
                                                             @RequestParam(required = false) Integer grupoId,
                                                             @RequestParam(required = false) Integer programaId) {
        return ResponseEntity.ok(proyectoService.listarProyectos(lineaId, grupoId, programaId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProyectoDto> actualizarProyecto(@PathVariable Integer id, @RequestBody ProyectoDto dto) {
        return ResponseEntity.ok(proyectoService.actualizarProyecto(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarProyecto(@PathVariable Integer id) {
        proyectoService.eliminarProyecto(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/asignar-usuario")
    public ResponseEntity<Void> asignarUsuario(@RequestBody UsuarioProyectoDto dto) {
        proyectoService.asignarUsuarioAProyecto(dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{idProyecto}/usuario/{idUsuario}")
    public ResponseEntity<Void> desasignarUsuario(@PathVariable Integer idProyecto, @PathVariable Integer idUsuario) {
        proyectoService.desasignarUsuarioDeProyecto(idUsuario, idProyecto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/definitiva")
    public ResponseEntity<DefinitivaDto> obtenerDefinitiva(@RequestParam Integer idProyecto,
                                                           @RequestParam TipoSustentacion tipoSustentacion) {
        return ResponseEntity.ok(proyectoService.calcularYAsignarDefinitiva(idProyecto, tipoSustentacion));
    }

    @GetMapping("/lineas-investigacion")
    public ResponseEntity<List<LineaInvestigacionDto>> obtenerLineasInvestigacion() {
        return ResponseEntity.ok(proyectoService.listarLineasInvestigacion());
    }

    @GetMapping("/grupos/lineas-investigacion")
    public ResponseEntity<List<GruposYLineasInvestigacionDto>> obtenerLineasInvestigacionPorGrupo() {
        return ResponseEntity.ok(proyectoService.listarGruposConLineas());
    }
}

