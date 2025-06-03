package com.sistemas_mangager_be.edu_virtual_ufps.services.implementations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.sistemas_mangager_be.edu_virtual_ufps.entities.CambioEstadoMatricula;
import com.sistemas_mangager_be.edu_virtual_ufps.entities.EstadoMatricula;
import com.sistemas_mangager_be.edu_virtual_ufps.entities.GrupoCohorte;
import com.sistemas_mangager_be.edu_virtual_ufps.entities.HistorialCierreNotas;
import com.sistemas_mangager_be.edu_virtual_ufps.entities.Matricula;
import com.sistemas_mangager_be.edu_virtual_ufps.entities.NotasPosgrado;
import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.GrupoNotFoundException;
import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.MatriculaException;
import com.sistemas_mangager_be.edu_virtual_ufps.exceptions.NotasException;
import com.sistemas_mangager_be.edu_virtual_ufps.repositories.CambioEstadoMatriculaRepository;
import com.sistemas_mangager_be.edu_virtual_ufps.repositories.GrupoCohorteRepository;
import com.sistemas_mangager_be.edu_virtual_ufps.repositories.HistorialCierreNotasRepository;
import com.sistemas_mangager_be.edu_virtual_ufps.repositories.MatriculaRepository;
import com.sistemas_mangager_be.edu_virtual_ufps.repositories.NotasPosgradoRepository;
import com.sistemas_mangager_be.edu_virtual_ufps.services.interfaces.INotaService;
import com.sistemas_mangager_be.edu_virtual_ufps.shared.requests.NotasPosgradoRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotasServiceImplementation implements INotaService {

    public static final String IS_ALREADY_USE = "%s ya esta en uso";
    public static final String IS_NOT_FOUND = "%s no fue encontrado";
    public static final String IS_NOT_FOUND_F = "%s no fue encontrada";
    public static final String IS_NOT_ALLOWED = "no esta permitido %s ";
    public static final String IS_NOT_VALID = "%s no es valido";
    public static final String ARE_NOT_EQUALS = "%s no son iguales";
    public static final String IS_NOT_CORRECT = "%s no es correcta";

    @Autowired
    private HistorialCierreNotasRepository historialCierreNotasRepository;
    @Autowired
    private GrupoCohorteRepository grupoCohorteRepository;

    @Autowired
    private NotasPosgradoRepository notasPosgradoRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private CambioEstadoMatriculaRepository cambioEstadoMatriculaRepository;

    /**
     * Guarda o actualiza la nota de un estudiante de posgrado.
     * - Verifica si la matrícula existe y está activa
     * - Valida que la nota esté dentro del rango permitido (0 a 5)
     * - Si la nota es válida, se guarda en la base de datos
     * 
     * @param notasPosgradoRequest Objeto que contiene los datos de la nota
     * @throws MatriculaException Si la matrícula no existe o no está activa
     * @throws NotasException     Si la nota no es válida
     */
    public void guardaroActualizarNotaPosgrado(NotasPosgradoRequest notasPosgradoRequest)
            throws MatriculaException, NotasException {
        Matricula matricula = matriculaRepository.findById(notasPosgradoRequest.getMatriculaId()).orElse(null);
        if (matricula == null) {
            throw new MatriculaException(
                    String.format(IS_NOT_FOUND_F, "La Matricula con id " + notasPosgradoRequest.getMatriculaId())
                            .toLowerCase());
        }

        if (matricula.getNotaAbierta() == null || !matricula.getNotaAbierta()) {
            throw new NotasException(String.format(IS_NOT_ALLOWED, "No se puede guardar la nota").toLowerCase());
        }

        validarNotaPosgrado(notasPosgradoRequest);

        matricula.setNota(notasPosgradoRequest.getNota());
        matricula.setFechaNota(new Date());

        crearCambioDeNotas(notasPosgradoRequest, matricula);

        matriculaRepository.save(matricula);
    }

    /**
     * Cierra el periodo de notas para un grupo de posgrado.
     * - Establece notaAbierta=false para todas las matrículas activas
     * - Cambia el estado de las matrículas según las notas registradas
     * - Si la nota es menor a 3.0, cambia el estado a "Reprobado"
     * - Si la nota es mayor o igual a 3.0, cambia el estado a "Aprobado"
     * - Guarda registro del cierre para posterior reapertura
     * 
     * @param grupoCohorteId ID del grupo-cohorte
     * @param usuario        Usuario que realiza la operación
     * @throws GrupoNotFoundException Si el grupo no existe o no tiene estudiantes
     *                                matriculados
     */
    @Transactional
    public void cerrarNotasGrupoPosgrado(Long grupoCohorteId, String usuario)
            throws GrupoNotFoundException, NotasException {
        // 1. Buscar el grupo-cohorte primero para validar que existe
        GrupoCohorte grupoCohorte = grupoCohorteRepository.findById(grupoCohorteId)
                .orElseThrow(() -> new GrupoNotFoundException(
                        String.format(IS_NOT_FOUND, "El Grupo Cohorte con id " + grupoCohorteId).toLowerCase()));

        // 2. Verificar si ya existe un historial de cierre para este grupo
        List<HistorialCierreNotas> historialExistente = historialCierreNotasRepository
                .findByGrupoCohorteId(grupoCohorteId);

        // 3. Si ya existe historial pero no se han cerrado las notas, eliminar el
        // historial
        // para permitir cerrarlas nuevamente
        if (!historialExistente.isEmpty()) {
            // Verificar si hay matrículas con notasAbiertas = true
            List<Matricula> matriculasActivas = matriculaRepository
                    .findByGrupoCohorteIdAndEstadoMatriculaId_Id(grupoCohorte, 2);

            boolean hayMatriculasAbiertas = matriculasActivas.stream()
                    .anyMatch(m -> m.getNotaAbierta() != null && m.getNotaAbierta());

            if (hayMatriculasAbiertas) {
                // Si hay matrículas abiertas pero existe historial, limpiar el historial
                log.info("Eliminando historial de cierre previo para el grupo {} porque hay matrículas abiertas",
                        grupoCohorteId);
                historialCierreNotasRepository.deleteByGrupoCohorteId(grupoCohorteId);
            } else {
                // Si todas las matrículas ya están cerradas, no hacemos nada
                log.info("Las notas del grupo {} ya están cerradas", grupoCohorteId);
                return;
            }
        }

        // 4. Obtener matrículas activas
        List<Matricula> matriculasActivasPorGrupo = matriculaRepository
                .findByGrupoCohorteIdAndEstadoMatriculaId_Id(grupoCohorte, 2);

        if (matriculasActivasPorGrupo.isEmpty()) {
            log.warn("No hay estudiantes matriculados activamente en el grupo {}", grupoCohorteId);
            return; // No lanzamos excepción para permitir continuar con los demás grupos
        }

        // 5. Fecha actual para el registro
        Date fechaCierre = new Date();
        int matriculasProcesadas = 0;

        // 6. Procesar matrículas
        for (Matricula matricula : matriculasActivasPorGrupo) {
            try {
                // Solo procesar si la nota está abierta o es null
                if (matricula.getNotaAbierta() == null || matricula.getNotaAbierta()) {
                    matricula.setNotaAbierta(false);
                    cambiarEstadoMatriculaPorNotas(matricula, usuario);
                    matriculaRepository.save(matricula); // Guardamos explícitamente cada matrícula
                    guardarHistorialCierreNotas(grupoCohorte.getId(), matricula.getId(), fechaCierre, usuario);
                    matriculasProcesadas++;
                }
            } catch (Exception e) {
                log.error("Error procesando matrícula {}: {}", matricula.getId(), e.getMessage());
                // Continuar con la siguiente matrícula
            }
        }

        log.info("Proceso de cierre de notas para grupo {} completado. Matrículas procesadas: {}/{}",
                grupoCohorteId, matriculasProcesadas, matriculasActivasPorGrupo.size());
    }

    /**
     * Reabre el periodo de notas para un grupo de posgrado.
     * - Recupera las matrículas que fueron cerradas anteriormente
     * - Establece notaAbierta=true y restaura su estado a "En curso"
     * 
     * @param grupoCohorteId ID del grupo-cohorte
     * @throws GrupoNotFoundException Si el grupo no existe o no tiene historial de
     *                                cierre
     */
    @Transactional
    public void abrirNotasGrupoPosgrado(Long grupoCohorteId, String usuario) throws GrupoNotFoundException {
        // 1. Buscar el grupo-cohorte
        GrupoCohorte grupoCohorte = grupoCohorteRepository.findById(grupoCohorteId)
                .orElseThrow(() -> new GrupoNotFoundException(
                        String.format(IS_NOT_FOUND, "El Grupo Cohorte con id " + grupoCohorteId).toLowerCase()));

        // 2. Buscar el historial de cierre para este grupo
        List<HistorialCierreNotas> historialCierreList = historialCierreNotasRepository
                .findByGrupoCohorteId(grupoCohorteId);

        if (historialCierreList.isEmpty()) {
            throw new GrupoNotFoundException(
                    String.format("El grupo %s no tiene historial de cierre de notas", grupoCohorteId).toLowerCase());
        }

        // 3. Crear estado "En curso" para restaurar matrículas
        EstadoMatricula estadoEnCurso = new EstadoMatricula();
        estadoEnCurso.setId(2);
        estadoEnCurso.setNombre("En curso");

        // 4. Procesar cada matrícula registrada en el historial usando un bucle
        // tradicional
        for (HistorialCierreNotas historial : historialCierreList) {
            // Buscar la matrícula
            Optional<Matricula> optionalMatricula = matriculaRepository.findById(historial.getMatriculaId());

            if (optionalMatricula.isPresent()) {
                Matricula matricula = optionalMatricula.get();

                // Reabrir para edición
                matricula.setNotaAbierta(true);

                // Restaurar estado a "En curso"
                matricula.setEstadoMatriculaId(estadoEnCurso);

                // Registrar el cambio de estado
                crearCambioEstadoMatricula(matricula, estadoEnCurso,
                        " (reabierto por " + usuario + ")");

                // Guardar cambios
                matriculaRepository.save(matricula);
            }
        }

        // 5. Eliminar el historial de cierre una vez procesado (ahora como último paso)
        historialCierreNotasRepository.deleteByGrupoCohorteId(grupoCohorteId);
    }

    private void validarNotaPosgrado(NotasPosgradoRequest notasPosgradoRequest) throws NotasException {
        if (notasPosgradoRequest.getNota() == null) {
            throw new NotasException(String.format(IS_NOT_VALID, "La nota").toLowerCase());
        }
        if (notasPosgradoRequest.getNota() < 0 || notasPosgradoRequest.getNota() > 5) {
            throw new NotasException(String.format(IS_NOT_VALID, "La nota").toLowerCase());
        }
    }

    /**
     * Método para que de acuerdo a la nota se cambie el estado de la matricula
     * 1. Si la nota es menor a 3.0, cambiar el estado de la matricula a "Reprobado"
     * 2. Si la nota es mayor o igual a 3.0 y menor o igual a 5.0, cambiar el estado
     * a "Aprobado"
     * 
     * @param matricula La matrícula a actualizar
     * @param usuario   Usuario que realiza el cambio
     */
    private void cambiarEstadoMatriculaPorNotas(Matricula matricula, String usuario) {
        if (matricula.getNota() == null) {
            // Si la nota es null, asignar una nota por defecto de 0.0 para considerar como
            // reprobada
            matricula.setNota(0.0);
            matricula.setFechaNota(new Date());
        }

        EstadoMatricula nuevoEstado = new EstadoMatricula();

        if (matricula.getNota() >= 3.0) {
            nuevoEstado.setId(1);
            nuevoEstado.setNombre("Aprobada");
        } else {
            nuevoEstado.setId(4);
            nuevoEstado.setNombre("Reprobada");
        }

        // Guardar el estado anterior para saber si realmente cambia
        Integer estadoAnteriorId = matricula.getEstadoMatriculaId() != null ? matricula.getEstadoMatriculaId().getId()
                : null;

        matricula.setEstadoMatriculaId(nuevoEstado);

        // Solo crear registro de cambio de estado si realmente cambia
        if (estadoAnteriorId == null || !estadoAnteriorId.equals(nuevoEstado.getId())) {
            crearCambioEstadoMatricula(matricula, nuevoEstado, " (cerrado por " + usuario + ")");
        }
    }

    private void crearCambioEstadoMatricula(Matricula matricula, EstadoMatricula estadoMatricula, String usuario) {
        CambioEstadoMatricula cambioEstado = new CambioEstadoMatricula();
        cambioEstado.setMatriculaId(matricula);
        cambioEstado.setEstadoMatriculaId(estadoMatricula);
        cambioEstado.setFechaCambioEstado(new Date());
        cambioEstado.setUsuarioCambioEstado(usuario);
        cambioEstado.setSemestre(matricula.getSemestre());

        cambioEstadoMatriculaRepository.save(cambioEstado);

    }

    private void crearCambioDeNotas(NotasPosgradoRequest notasRequest, Matricula matricula) {
        NotasPosgrado notasPosgrado = new NotasPosgrado();
        notasPosgrado.setMatriculaId(matricula);
        notasPosgrado.setNota(notasRequest.getNota());
        notasPosgrado.setFechaNota(new Date());
        notasPosgrado.setRealizadoPor(notasRequest.getRealizadoPor());
        notasPosgradoRepository.save(notasPosgrado);
    }

    private String calcularSemestre(Date fechaMatriculacion) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(fechaMatriculacion);

        int mes = cal.get(Calendar.MONTH) + 1; // Enero = 0
        int anio = cal.get(Calendar.YEAR);

        return anio + "-" + (mes <= 6 ? "I" : "II");
    }

    private void guardarHistorialCierreNotas(Long grupoCohorteId, Long matriculaId, Date fechaCierre, String usuario) {
        HistorialCierreNotas historial = new HistorialCierreNotas();
        historial.setGrupoCohorteId(grupoCohorteId);
        historial.setMatriculaId(matriculaId);
        historial.setFechaCierre(fechaCierre);
        historial.setRealizadoPor(usuario);

        historialCierreNotasRepository.save(historial);
    }
}
