package com.sistemas_mangager_be.edu_virtual_ufps.shared.responses;
import java.util.Date;
import java.util.List;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CorreoResponse {
    
    private String nombreEstudiante;

    private String correo;

    private String semestre;

    private Date fecha;

    public List<MatriculaResponse> matriculas;
    
}
