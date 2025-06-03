package com.sistemas_mangager_be.edu_virtual_ufps.shared.DTOs;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PensumDTO {

    private Integer id;
    private String nombre;
    private Integer programaId;
    private Integer cantidadSemestres;
    private String moodleId;

}
