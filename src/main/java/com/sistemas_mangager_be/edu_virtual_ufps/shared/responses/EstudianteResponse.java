package com.sistemas_mangager_be.edu_virtual_ufps.shared.responses;

import java.util.Date;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EstudianteResponse {

    private Integer id;

    private String codigo;

    private String nombre;

    private String nombre2;

    private String apellido;

    private String apellido2;

    private String cedula;

    private String email;

    private String telefono;

    private Date fechaNacimiento;

    private Date fechaIngreso;

    private Boolean esPosgrado;

    private Integer pensumId;

    private String pensumNombre;

    private Integer programaId;

    private String programaNombre;

    private Integer cohorteId;

    private String cohorteNombre;

    private Integer estadoEstudianteId;
    private String estadoEstudianteNombre;

    private Integer usuarioId;

    private String moodleId;
}
