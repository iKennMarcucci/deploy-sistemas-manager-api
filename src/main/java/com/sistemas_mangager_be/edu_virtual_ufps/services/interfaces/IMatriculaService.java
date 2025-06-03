package com.sistemas_mangager_be.edu_virtual_ufps.services.interfaces;

import java.util.List;

import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.EstudianteNotFoundException;
import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.GrupoNotFoundException;
import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.MateriaNotFoundException;
import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.MatriculaException;
import com.sistemas_mangager_be.edu_virtual_ufps.shared.DTOs.MateriaDTO;
import com.sistemas_mangager_be.edu_virtual_ufps.shared.DTOs.MatriculaDTO;
import com.sistemas_mangager_be.edu_virtual_ufps.shared.responses.*;

public interface IMatriculaService {

    public MatriculaDTO crearMatricula(MatriculaDTO matriculaDTO, String usuario)
            throws EstudianteNotFoundException, GrupoNotFoundException, MatriculaException;

    public void anularMatricula(Long idMatricula, String usuario) throws MatriculaException;

    public CorreoResponse enviarCorreo(Integer estudianteId, String usuario) throws EstudianteNotFoundException, MatriculaException;

    public boolean verificarCorreoEnviado(Integer estudianteId) throws EstudianteNotFoundException;

    public List<MatriculaResponse> listarMatriculasEnCursoPorEstudiante(Integer estudianteId) throws EstudianteNotFoundException;

    public List<MateriaDTO> listarMateriasNoMatriculadasPorEstudiante(Integer estudianteId) throws EstudianteNotFoundException;

    public List<PensumResponse> listarPensumPorEstudiante(Integer estudianteId) throws EstudianteNotFoundException;

    public List<GrupoCohorteDocenteResponse> listarGrupoCohorteDocentePorMateria(String codigoMateria)
                        throws MateriaNotFoundException;

    public List<CambioEstadoMatriculaResponse> listarCambiosdeEstadoMatriculaPorEstudiante(Integer estudianteId) throws EstudianteNotFoundException, MatriculaException;

}
