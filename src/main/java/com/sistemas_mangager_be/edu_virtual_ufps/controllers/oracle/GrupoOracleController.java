package com.sistemas_mangager_be.edu_virtual_ufps.controllers.oracle;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sistemas_mangager_be.edu_virtual_ufps.oracle.entities.GrupoOracle;
import com.sistemas_mangager_be.edu_virtual_ufps.oracle.repositories.GrupoOracleRespository;

@Profile("oracle")
@RestController
@RequestMapping("/api/oracle/grupos")

public class GrupoOracleController {

    private final GrupoOracleRespository grupoOracleRespository;

    public GrupoOracleController(GrupoOracleRespository grupoOracleRespository) {
        this.grupoOracleRespository = grupoOracleRespository;
    }

    @GetMapping
    public List<GrupoOracle> obtenerTodos() {
        return grupoOracleRespository.findAll();
    }

    @GetMapping("/carrera")
    public List<GrupoOracle> obtenerGruposSistemas(@RequestParam String codCarrera) {
        return grupoOracleRespository.findByCodCarrera(codCarrera);
    }

}
