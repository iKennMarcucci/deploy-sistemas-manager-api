package com.sistemas_mangager_be.edu_virtual_ufps.entities;

import lombok.*;

import java.util.Date;

import jakarta.persistence.*;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "contraprestaciones")
public class Contraprestacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudianteId;

    private String actividades;

    private Boolean aprobada;

    private Date fechaCreacion;

    private String semestre;

    private Date fechaInicio;

    private Date fechaFin;

    private Date fechaCertificado;

    private Boolean certificadoGenerado;

    @ManyToOne
    @JoinColumn(name = "certificado_id")
    private Soporte certificadoId;

    @ManyToOne
    @JoinColumn(name = "tipo_contraprestacion_id")
    private TipoContraprestacion tipoContraprestacionId;


    @ManyToOne
    @JoinColumn(name = "soporte_id")
    private Soporte soporteId;

}
