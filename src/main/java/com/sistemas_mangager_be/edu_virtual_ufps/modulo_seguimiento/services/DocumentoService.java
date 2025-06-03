package com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.services;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.sistemas_mangager_be.edu_virtual_ufps.entities.Usuario;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.dtos.DocumentoDto;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.dtos.RetroalimentacionDto;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.entities.Documento;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.entities.Proyecto;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.entities.Retroalimentacion;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.entities.enums.TipoDocumento;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.entities.intermedias.ColoquioEstudiante;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.entities.intermedias.SustentacionDocumento;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.mappers.DocumentoMapper;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.mappers.RetroalimentacionMapper;
import com.sistemas_mangager_be.edu_virtual_ufps.modulo_seguimiento.repositories.*;
import com.sistemas_mangager_be.edu_virtual_ufps.repositories.UsuarioRepository;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final DocumentoMapper documentoMapper;
    private final ProyectoRepository proyectoRepository;
    private final SustentacionDocumentoRepository sustentacionDocumentoRepository;
    private final ColoquioEstudianteRepository coloquioEstudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioProyectoRepository usuarioProyectoRepository;
    private final AmazonS3 amazonS3Client;
    private final RetroalimentacionRepository retroalimentacionRepository;
    private final RetroalimentacionMapper retroalimentacionMapper;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Autowired
    public DocumentoService(DocumentoRepository documentoRepository, DocumentoMapper documentoMapper,
                            ProyectoRepository proyectoRepository, SustentacionDocumentoRepository sustentacionDocumentoRepository,
                            ColoquioEstudianteRepository coloquioEstudianteRepository,
                            UsuarioRepository usuarioRepository, UsuarioProyectoRepository usuarioProyectoRepository, AmazonS3 amazonS3Client,
                            RetroalimentacionRepository retroalimentacionRepository, RetroalimentacionMapper retroalimentacionMapper) {
        this.documentoRepository = documentoRepository;
        this.documentoMapper = documentoMapper;
        this.proyectoRepository = proyectoRepository;
        this.sustentacionDocumentoRepository = sustentacionDocumentoRepository;
        this.coloquioEstudianteRepository = coloquioEstudianteRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioProyectoRepository = usuarioProyectoRepository;
        this.amazonS3Client = amazonS3Client;
        this.retroalimentacionRepository = retroalimentacionRepository;
        this.retroalimentacionMapper = retroalimentacionMapper;
    }

    private String generarPresignedUrl(String fileName, int minutosValidez) {
        Date expiration = new Date(System.currentTimeMillis() + (minutosValidez * 60 * 1000));

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, fileName)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration);

        URL url = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE')")
    public DocumentoDto guardarDocumento(Integer idProyecto, MultipartFile archivo, TipoDocumento tipoDocumento, String tag) {
        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        try {
            String fileName = UUID.randomUUID() + "_" + archivo.getOriginalFilename();

            amazonS3Client.putObject(bucketName, fileName, archivo.getInputStream(), null);

            Documento documento = new Documento();
            documento.setNombre(archivo.getOriginalFilename());
            documento.setPath(fileName);
            documento.setTipoArchivo(archivo.getContentType());
            documento.setPeso(archivo.getSize() / 1024 + " KB");
            documento.setTipoDocumento(tipoDocumento);
            documento.setProyecto(proyecto);
            documento.setTag(tag);

            documentoRepository.save(documento);

            DocumentoDto documentoDto = documentoMapper.toDto(documento);
            documentoDto.setUrl(generarPresignedUrl(documentoDto.getPath(), 60));
            return documentoDto;

        } catch (IOException e) {
            throw new RuntimeException("Error al subir el archivo a S3", e);
        }
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE') or hasAuthority('ROLE_SUPERADMIN') or hasAuthority('ROLE_ADMIN')")
    public void eliminarDocumento(Integer id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        amazonS3Client.deleteObject(new DeleteObjectRequest(bucketName, documento.getPath()));

        documentoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE') or hasAuthority('ROLE_DOCENTE') or hasAuthority('ROLE_SUPERADMIN') or hasAuthority('ROLE_ADMIN')")
    public List<DocumentoDto> listarPorProyecto(Integer idProyecto, @Nullable TipoDocumento tipoDocumento) {
        return documentoRepository.findByProyectoIdAndOptionalTipoDocumento(idProyecto, tipoDocumento)
                .stream()
                .map(documento -> {
                    DocumentoDto dto = documentoMapper.toDto(documento);
                    dto.setUrl(generarPresignedUrl(documento.getPath(), 60));
                    dto.setRetroalimentacion(documento.getRetroalimentacion()
                            .stream()
                            .map(retro -> retroalimentacionMapper.toDto(retro))
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE')")
    public DocumentoDto agregarDocumentoaSustentacion(MultipartFile archivo, TipoDocumento tipoDocumento, Integer idSustentacion, String tag) {
        Integer idProyecto = sustentacionDocumentoRepository.findProyectoIdBySustentacionId(idSustentacion)
                .orElseThrow(() -> new RuntimeException("Sustentacion no encontrada"));

        DocumentoDto documentoDto = guardarDocumento(idProyecto, archivo, tipoDocumento, tag);

        SustentacionDocumento sustentacionDocumento = new SustentacionDocumento();
        sustentacionDocumento.setIdSustentacion(idSustentacion);
        sustentacionDocumento.setIdDocumento(documentoDto.getId());
        sustentacionDocumentoRepository.save(sustentacionDocumento);

        return documentoDto;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE') or hasAuthority('ROLE_DOCENTE') or hasAuthority('ROLE_SUPERADMIN') or hasAuthority('ROLE_ADMIN')")
    public List<DocumentoDto> listarDocumentosPorSustentacion(Integer idSustentacion) {
        List<SustentacionDocumento> relaciones = sustentacionDocumentoRepository.findByIdSustentacion(idSustentacion);

        List<Integer> idsDocumento = relaciones.stream()
                .map(SustentacionDocumento::getIdDocumento)
                .collect(Collectors.toList());

        List<Documento> documentos = documentoRepository.findAllById(idsDocumento);

        return documentos.stream()
                .map(documento -> {
                    DocumentoDto dto = documentoMapper.toDto(documento);
                    dto.setUrl(generarPresignedUrl(documento.getPath(), 60));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE')")
    public DocumentoDto agregarDocumentoaColoquio(MultipartFile archivo, TipoDocumento tipoDocumento, Integer idColoquio, String tag) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario activo = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Proyecto proyecto = usuarioProyectoRepository.findProyectoByEstudianteId(activo.getId())
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        DocumentoDto documentoDto = guardarDocumento(proyecto.getId(), archivo, tipoDocumento, tag);

        ColoquioEstudiante coloquioEstudiante = new ColoquioEstudiante();
        coloquioEstudiante.setIdColoquio(idColoquio);
        coloquioEstudiante.setIdEstudiante(activo.getId());
        coloquioEstudiante.setIdDocumento(documentoDto.getId());
        coloquioEstudianteRepository.save(coloquioEstudiante);

        return documentoDto;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ROLE_ESTUDIANTE') or hasAuthority('ROLE_DOCENTE') or hasAuthority('ROLE_SUPERADMIN') or hasAuthority('ROLE_ADMIN')")
    public List<DocumentoDto> listarDocumentosPorColoquioEstudiante(Integer idColoquio, @Nullable Integer idEstudiante) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean isEstudiante = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ESTUDIANTE"));

        if (isEstudiante){
            Usuario activo = usuarioRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            idEstudiante = activo.getId();
        } else {
            if (idEstudiante == null) {
                throw new RuntimeException("El id del estudiante es requerido");
            }
        }

        List<ColoquioEstudiante> relaciones = coloquioEstudianteRepository.findByIdColoquioAndIdEstudiante(idColoquio, idEstudiante);

        List<Integer> idsDocumento = relaciones.stream()
                .map(ColoquioEstudiante::getIdDocumento)
                .collect(Collectors.toList());

        List<Documento> documentos = documentoRepository.findAllById(idsDocumento);

        return documentos.stream()
                .map(documento -> {
                    DocumentoDto dto = documentoMapper.toDto(documento);
                    dto.setUrl(generarPresignedUrl(documento.getPath(), 60));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_DOCENTE')")
    public RetroalimentacionDto agregarRetroalimentacionADocumentos(RetroalimentacionDto retroalimentacionDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario activo = usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Documento documento = documentoRepository.findById(retroalimentacionDto.getDocumentoId())
                .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

        Retroalimentacion retroalimentacion = retroalimentacionMapper.toEntity(retroalimentacionDto);
        retroalimentacion.setUsuario(activo);
        retroalimentacionRepository.save(retroalimentacion);

        return retroalimentacionMapper.toDto(retroalimentacion);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_DOCENTE')")
    public RetroalimentacionDto editarRetroalimentacion(RetroalimentacionDto retroalimentacionDto) {
        Retroalimentacion retroalimentacion = retroalimentacionRepository.findById(retroalimentacionDto.getId())
                .orElseThrow(() -> new RuntimeException("Retroalimentacion no encontrada"));

        retroalimentacionMapper.partialUpdate(retroalimentacionDto, retroalimentacion);
        retroalimentacionRepository.save(retroalimentacion);

        return retroalimentacionMapper.toDto(retroalimentacion);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_DOCENTE')")
    public void eliminarRetroalimentacion(Integer id) {
        Retroalimentacion retroalimentacion = retroalimentacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Retroalimentacion no encontrada"));

        retroalimentacionRepository.delete(retroalimentacion);
    }

    @Transactional
    public DocumentoDto guardarDocumentoGenerado(Integer idProyecto, String fileName, String contentType, byte[] contenido, TipoDocumento tipoDocumento, String tag) {
        Proyecto proyecto = proyectoRepository.findById(idProyecto)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        String s3FileName = UUID.randomUUID() + "_" + fileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contenido.length);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(contenido);
        amazonS3Client.putObject(bucketName, s3FileName, inputStream, metadata);

        Documento documento = new Documento();
        documento.setNombre(fileName);
        documento.setPath(s3FileName);
        documento.setTipoArchivo(contentType);
        documento.setPeso((contenido.length / 1024) + " KB");
        documento.setTipoDocumento(tipoDocumento);
        documento.setProyecto(proyecto);
        documento.setTag(tag);

        documentoRepository.save(documento);

        DocumentoDto documentoDto = documentoMapper.toDto(documento);
        documentoDto.setUrl(generarPresignedUrl(documentoDto.getPath(), 60));
        return documentoDto;
    }
}
