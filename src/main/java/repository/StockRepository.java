package repository;

import Entidad.Stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByActivo(String activo);

    boolean existsByActivo(String activo);

    void deleteByActivo(String activo);

    // =====================================================
    // BUSCAR INFORMACIÓN DEL ACTIVO PARA INGRESO A STOCK
    // =====================================================

    @Query(value = """
            SELECT
                a.activo,
                a.descripcion AS equipo,
                a.modelo AS modelo,
                a.codigo_emp AS codigo,
                a.empleado_asig AS empleado,
                a.planta,
                a.area,
                a.departamento
            FROM public.activo_info a
            WHERE TRIM(a.activo) = TRIM(:activo)
            LIMIT 1
            """, nativeQuery = true)
    Map<String, Object> buscarInformacionActivo(
            @Param("activo") String activo);

    @Query(value = """
            SELECT
                s.id,
                s.activo,
                s.fecha_ingreso AS "fechaIngreso",
                s.motivo_ingreso AS "motivoIngreso",
                s.condicion,
                s.observacion,

                a.descripcion AS equipo,
                a.modelo AS modelo,
                a.codigo_emp AS codigo,
                a.empleado_asig AS empleado,
                a.planta AS planta,
                a.area AS area,
                a.departamento AS departamento,

                CASE
                    WHEN d.id IS NOT NULL
                         AND d.last_connection >= NOW() - INTERVAL '17 minutes'
                         AND COALESCE(d.sin_respuesta, false) = false
                    THEN true
                    ELSE false
                END AS reportando

            FROM "monitoreo tablet".stock s

            LEFT JOIN public.activo_info a
                ON TRIM(a.activo) = TRIM(s.activo)

            LEFT JOIN "monitoreo tablet".dispositivos d
                ON TRIM(d.activo) = TRIM(s.activo)

            ORDER BY s.fecha_ingreso DESC
            """, nativeQuery = true)
    List<Map<String, Object>> listarStockCompleto();

}