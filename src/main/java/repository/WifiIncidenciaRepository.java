
package repository;

import Entidad.WifiIncidencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WifiIncidenciaRepository
        extends JpaRepository<WifiIncidencia, Long> {

    // Evitar registros duplicados (función existente)
    boolean existsByEventoId(String eventoId);

    // =====================================================
    // 1. RESUMEN: UNA SOLA FILA POR ACTIVO
    // =====================================================
    // Devuelve el último evento de cada activo y la cantidad
    // de incidencias dentro del rango seleccionado.
    @Query(value = """
            SELECT
                h.activo AS activo,
                h.ssid AS ssid,
                h.fecha_perdida AS fechaPerdida,
                h.fecha_recuperacion AS fechaRecuperacion,
                h.duracion_segundos AS duracionSegundos,
                h.wifi_habilitado AS wifiHabilitado,
                h.transporte_wifi AS transporteWifi,
                h.internet_validado AS internetValidado,
                conteo.total AS totalRegistros
            FROM (
                SELECT
                    activo,
                    COUNT(*) AS total
                FROM "monitoreo tablet".historial_wifi
                WHERE (:activo IS NULL OR activo = :activo)
                  AND (:desde IS NULL OR fecha_perdida >= :desde)
                  AND (:hasta IS NULL OR fecha_perdida < :hasta)
                GROUP BY activo
            ) conteo
            JOIN LATERAL (
                SELECT *
                FROM "monitoreo tablet".historial_wifi x
                WHERE x.activo = conteo.activo
                  AND (:desde IS NULL OR x.fecha_perdida >= :desde)
                  AND (:hasta IS NULL OR x.fecha_perdida < :hasta)
                ORDER BY x.fecha_perdida DESC, x.id DESC
                LIMIT 1
            ) h ON TRUE
            ORDER BY h.fecha_perdida DESC
            """, countQuery = """
            SELECT COUNT(DISTINCT activo)
            FROM "monitoreo tablet".historial_wifi
            WHERE (:activo IS NULL OR activo = :activo)
              AND (:desde IS NULL OR fecha_perdida >= :desde)
              AND (:hasta IS NULL OR fecha_perdida < :hasta)
            """, nativeQuery = true)
    Page<WifiResumenProjection> consultarResumen(
            @Param("activo") String activo,
            @Param("desde") Long desde,
            @Param("hasta") Long hasta,
            Pageable pageable);

    // =====================================================
    // 2. HISTORIAL INDIVIDUAL DE UN ACTIVO
    // =====================================================
    @Query("""
            SELECT h
            FROM WifiIncidencia h
            WHERE h.activo = :activo
              AND (:desde IS NULL OR h.fechaPerdida >= :desde)
              AND (:hasta IS NULL OR h.fechaPerdida < :hasta)
            ORDER BY h.fechaPerdida DESC, h.id DESC
            """)
    Page<WifiIncidencia> consultarHistorial(
            @Param("activo") String activo,
            @Param("desde") Long desde,
            @Param("hasta") Long hasta,
            Pageable pageable);

    // =====================================================
    // 3. ELIMINAR INCIDENCIAS ANTERIORES A UNA FECHA
    // =====================================================
    @Modifying
    @Query("""
            DELETE FROM WifiIncidencia h
            WHERE h.fechaPerdida < :limite
            """)
    int eliminarAnterioresA(@Param("limite") Long limite);
}
