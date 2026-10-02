package service;

import Entidad.AuditoriaDispositivo;
import repository.AuditoriaDispositivoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditoriaDispositivoService {

    private final AuditoriaDispositivoRepository auditoriaRepository;

    public AuditoriaDispositivoService(
            AuditoriaDispositivoRepository auditoriaRepository) {

        this.auditoriaRepository = auditoriaRepository;
    }


    @Transactional
    public AuditoriaDispositivo registrar(
            String activo,
            String accion,
            String usuario,
            String detalle) {

        AuditoriaDispositivo auditoria =
                auditoriaRepository
                        .findByActivoAndAccion(activo, accion)
                        .orElse(null);


        // ==========================================
        // PRIMERA MODIFICACIÓN
        // ==========================================

        if (auditoria == null) {

            auditoria = new AuditoriaDispositivo();

            auditoria.setActivo(activo);
            auditoria.setAccion(accion);

            auditoria.setUltimoUsuario(usuario);
            auditoria.setUltimaFecha(LocalDateTime.now());
            auditoria.setUltimoDetalle(detalle);

            return auditoriaRepository.save(auditoria);
        }


        // ==========================================
        // MOVER ÚLTIMA -> ANTERIOR
        // ==========================================

        auditoria.setAnteriorUsuario(
                auditoria.getUltimoUsuario()
        );

        auditoria.setAnteriorFecha(
                auditoria.getUltimaFecha()
        );

        auditoria.setAnteriorDetalle(
                auditoria.getUltimoDetalle()
        );


        // ==========================================
        // GUARDAR NUEVA ÚLTIMA
        // ==========================================

        auditoria.setUltimoUsuario(usuario);
        auditoria.setUltimaFecha(LocalDateTime.now());
        auditoria.setUltimoDetalle(detalle);


        return auditoriaRepository.save(auditoria);
    }


    public AuditoriaDispositivo obtener(
            String activo,
            String accion) {

        return auditoriaRepository
                .findByActivoAndAccion(activo, accion)
                .orElse(null);
    }
}