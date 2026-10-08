
package service;

import Entidad.WifiIncidencia;
import repository.WifiIncidenciaRepository;
import repository.WifiResumenProjection;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class WifiIncidenciaService {

    private final WifiIncidenciaRepository repository;

    public WifiIncidenciaService(
            WifiIncidenciaRepository repository) {

        this.repository = repository;
    }

    // =====================================================
    // CONSULTAR RESUMEN WIFI - UNA FILA POR ACTIVO
    // =====================================================
    @Transactional(readOnly = true)
    public Page<WifiResumenProjection> consultarResumen(
            String activo,
            LocalDate desde,
            LocalDate hasta,
            int pagina,
            int cantidad) {

        validarFechas(desde, hasta);

        Pageable pageable = crearPaginacion(pagina, cantidad);

        return repository.consultarResumen(
                normalizarActivo(activo),
                convertirInicio(desde),
                convertirFinExclusivo(hasta),
                pageable);
    }

    // =====================================================
    // CONSULTAR HISTORIAL INDIVIDUAL DE UN ACTIVO
    // =====================================================
    @Transactional(readOnly = true)
    public Page<WifiIncidencia> consultarHistorial(
            String activo,
            LocalDate desde,
            LocalDate hasta,
            int pagina,
            int cantidad) {

        validarFechas(desde, hasta);

        String activoNormalizado = normalizarActivo(activo);

        if (activoNormalizado == null) {
            throw new IllegalArgumentException(
                    "Debe indicar un activo para consultar su historial");
        }

        Pageable pageable = crearPaginacion(pagina, cantidad);

        return repository.consultarHistorial(
                activoNormalizado,
                convertirInicio(desde),
                convertirFinExclusivo(hasta),
                pageable);
    }

    // =====================================================
    // ELIMINAR REGISTROS WIFI ANTERIORES A UNA FECHA
    // =====================================================
    @Transactional
    public int eliminarAnterioresA(LocalDate fechaLimite) {

        if (fechaLimite == null) {
            throw new IllegalArgumentException(
                    "Debe indicar una fecha límite");
        }

        return repository.eliminarAnterioresA(
                convertirInicio(fechaLimite));
    }

    // =====================================================
    // FUNCIONES AUXILIARES
    // =====================================================

    private static final ZoneId ZONA_GUATEMALA = ZoneId.of("America/Guatemala");

    private String normalizarActivo(String activo) {

        if (activo == null || activo.trim().isEmpty()) {
            return null;
        }

        return activo.trim();
    }

    private Pageable crearPaginacion(int pagina, int cantidad) {

        int paginaSegura = Math.max(0, pagina);
        int cantidadSegura = Math.max(1, Math.min(cantidad, 100));

        return PageRequest.of(paginaSegura, cantidadSegura);
    }

    private void validarFechas(LocalDate desde, LocalDate hasta) {

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final");
        }
    }

    private Long convertirInicio(LocalDate fecha) {

        if (fecha == null) {
            return null;
        }

        return fecha.atStartOfDay(ZONA_GUATEMALA)
                .toInstant()
                .toEpochMilli();
    }

    private Long convertirFinExclusivo(LocalDate fecha) {

        if (fecha == null) {
            return null;
        }

        return fecha.plusDays(1)
                .atStartOfDay(ZONA_GUATEMALA)
                .toInstant()
                .toEpochMilli();
    }

    // =====================================================
    // RECIBIR Y GUARDAR INCIDENCIA WIFI
    // =====================================================
    @Transactional
    public WifiIncidencia guardarIncidencia(
            WifiIncidencia incidencia) {

        if (incidencia.getEventoId() == null ||
                incidencia.getEventoId().isBlank()) {

            throw new IllegalArgumentException(
                    "El eventoId es obligatorio");
        }

        if (incidencia.getActivo() == null ||
                incidencia.getActivo().isBlank()) {

            throw new IllegalArgumentException(
                    "El activo es obligatorio");
        }

        if (incidencia.getFechaPerdida() == null ||
                incidencia.getFechaRecuperacion() == null ||
                incidencia.getFechaConfirmacion() == null) {

            throw new IllegalArgumentException(
                    "Las fechas de la incidencia son obligatorias");
        }

        if (incidencia.getFechaRecuperacion() < incidencia.getFechaPerdida()) {

            throw new IllegalArgumentException(
                    "La fecha de recuperación no puede ser anterior a la pérdida");
        }

        // Evitar registros duplicados cuando Android reintenta.
        if (repository.existsByEventoId(
                incidencia.getEventoId())) {

            return repository.findAll().stream()
                    .filter(i -> incidencia.getEventoId()
                            .equals(i.getEventoId()))
                    .findFirst()
                    .orElseThrow();
        }

        // Guardar en PostgreSQL.
        return repository.saveAndFlush(incidencia);
    }

    // =====================================================
    // CONSULTAR HISTORIAL
    // =====================================================
    @Transactional(readOnly = true)
    public List<WifiIncidencia> listarHistorial() {

        return repository.findAll();
    }
}
