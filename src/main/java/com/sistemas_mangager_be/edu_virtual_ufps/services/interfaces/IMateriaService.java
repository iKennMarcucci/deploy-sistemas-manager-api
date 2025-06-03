package com.sistemas_mangager_be.edu_virtual_ufps.services.interfaces;

import java.util.List;

import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.MateriaExistsException;
import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.MateriaNotFoundException;
import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.PensumNotFoundException;
import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.SemestrePensumNotFoundException;
import com.sistemas_mangager_be.edu_virtual_ufps.shared.DTOs.MateriaDTO;
import com.sistemas_mangager_be.edu_virtual_ufps.shared.requests.MateriaSemestreRequest;
import com.sistemas_mangager_be.edu_virtual_ufps.shared.requests.MoodleRequest;

public interface IMateriaService {

    public MateriaDTO crearMateria(MateriaDTO materiaDTO)
            throws PensumNotFoundException, MateriaExistsException, SemestrePensumNotFoundException;

    public void vincularMoodleId(MoodleRequest moodleRequest) throws MateriaNotFoundException, MateriaExistsException;

    MateriaDTO actualizarMateria(Integer id, MateriaDTO materiaDTO) throws PensumNotFoundException, MateriaNotFoundException, MateriaExistsException, SemestrePensumNotFoundException;

    MateriaDTO listarMateria( Integer materiaId) throws MateriaNotFoundException;

    List<MateriaDTO> listarMaterias();

    List<MateriaDTO> listarMateriasPorPensum(Integer pensumId) throws PensumNotFoundException;

    List<MateriaDTO> listarMateriasPorPensumPorSemestre(MateriaSemestreRequest materiaSemestreRequest) throws PensumNotFoundException;

}
