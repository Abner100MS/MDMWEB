package repository;

import Entidad.Tablet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import Dto.TabletDashboardProjection;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import Dto.TabletDashboardProjection;
import Entidad.ActivoInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface TabletRepository extends JpaRepository<Tablet, Long> {
    // CAMBIO: Debe coincidir con el nombre del campo 'activo'
    Optional<Tablet> findByActivo(String activo);

    @Query(value = """
                          SELECT
                              d.id,
                              d.activo,
                              d.android_id,
                              d.device_name,
                              d.model,
                              d.categoria,
                              a.codigo_emp      AS codigoEmpInfo,
                              a.empleado_asig   AS empleadoAsig,
                              a.planta          AS planta,
                              a.area            AS area,
                              a.departamento    AS departamento,
                              d.battery_level,
                              d.temperatura,
                              d.estado_cargador,
                              d.estado_wifi,
                              d.estado_red      AS estado,
                              d.ip_address,
                              d.ram_usage,
                              d.storage_usage,
                              d.uptime,
                              d.last_connection,
                              d.os_version,
                              d.app_version AS appVersion,
                              CASE
                            WHEN EXISTS (
                                SELECT 1
                                FROM "monitoreo tablet".stock s
                                WHERE TRIM(s.activo) = TRIM(d.activo)
                            )
                            THEN TRUE
                            ELSE FALSE
                        END AS enStock
                          FROM "monitoreo tablet".dispositivos d
                          LEFT JOIN public.activo_info a
                                 ON d.activo = a.activo
                        WHERE
                            (
                                NOT EXISTS (
                                    SELECT 1
                                    FROM "monitoreo tablet".stock s
                                    WHERE TRIM(s.activo) = TRIM(d.activo)
                                )
                                OR EXISTS (
                                    SELECT 1
                                    FROM "monitoreo tablet".stock s
                                    WHERE TRIM(s.activo) = TRIM(d.activo)
                                    AND d.last_connection > s.fecha_ingreso
                                    AND d.last_connection >= NOW() - INTERVAL '17 minutes'
                                    AND COALESCE(d.sin_respuesta, FALSE) = FALSE
                                )
                            )

                            AND
                            (

                              :buscar = ''
                              OR LOWER(d.activo) LIKE LOWER(CONCAT('%', :buscar, '%'))
                              OR LOWER(d.device_name) LIKE LOWER(CONCAT('%', :buscar, '%'))
                              OR LOWER(d.model) LIKE LOWER(CONCAT('%', :buscar, '%'))
                              OR LOWER(d.ip_address) LIKE LOWER(CONCAT('%', :buscar, '%'))
                              OR LOWER(a.empleado_asig) LIKE LOWER(CONCAT('%', :buscar, '%'))
                              OR LOWER(a.codigo_emp) LIKE LOWER(CONCAT('%', :buscar, '%'))
                          )
                          AND
                          (
                              :planta = ''
                              OR a.planta = :planta
                          )
                          AND
                                  (
                                      :categoria = ''
                                      OR (
                                          UPPER(:categoria) = 'SIN DATOS'
                                          AND a.activo IS NULL
                                      )
                                      OR (
                                          UPPER(:categoria) <> 'SIN DATOS'
                                          AND UPPER(COALESCE(d.categoria, '')) = UPPER(:categoria)
                                      )
                                  )
                                      AND
                                  (
                                      :version = ''
                                      OR (
                                          :version = 'SIN_VERSION'
                                          AND (
                                              d.app_version IS NULL
                                              OR TRIM(d.app_version) = ''
                                          )
                                      )
                                      OR (
                                          :version <> 'SIN_VERSION'
                                          AND TRIM(COALESCE(d.app_version, '')) = TRIM(:version)
                                      )
                                  )

                                          AND
                              (
                                  :estadoCargador = ''
                                  OR LOWER(d.estado_cargador) LIKE LOWER(CONCAT('%', :estadoCargador, '%'))
                              )
                                  AND
                              (
                                  :estado = ''
                                  OR
                                  (
                                      :estado = 'OFFLINE'
                                       COALESCE(d.sin_respuesta, FALSE) = TRUE
                                      AND d.last_connection < NOW() - INTERVAL '17 minutes'
                                  )
                                  OR
                                  (
                                      :estado = 'CARGADOR_DESCONECTADO'
                                      AND LOWER(d.estado_cargador) LIKE '%desconectado%'
                                  )
                                      OR
                                  (
                          :estado = 'CARGADOR_CONECTADO'
                          AND d.estado_cargador = 'Conectado'
                      )
                                          OR
                                          (
                                              :estado = 'AUTORIZADO'
                                              AND d.estado_red = 'Autorizado'
                                          )
                                          OR
                                          (
                                              :estado = 'NO_AUTORIZADO'
                                              AND (d.estado_red IS NULL OR d.estado_red <> 'Autorizado')
                                          )
                                      )
                                          AND
                                      (
                                          :estadoBateria = ''
                                          OR UPPER(COALESCE(d.estado_bateria, 'NORMAL')) = UPPER(:estadoBateria)
                                      )
                                      ORDER BY
              CASE
                  WHEN TRIM(d.activo) ~ '^[0-9]+$'
                  THEN CAST(TRIM(d.activo) AS BIGINT)
                  ELSE 999999999999
              END ASC,
              d.activo ASC
            """, countQuery = """
                        SELECT COUNT(*)
                        FROM "monitoreo tablet".dispositivos d
                        LEFT JOIN public.activo_info a
                            ON d.activo = a.activo
                       WHERE
                            (
                                NOT EXISTS (
                                    SELECT 1
                                    FROM "monitoreo tablet".stock s
                                    WHERE TRIM(s.activo) = TRIM(d.activo)
                                )
                                OR EXISTS (
                                    SELECT 1
                                    FROM "monitoreo tablet".stock s
                                    WHERE TRIM(s.activo) = TRIM(d.activo)
                                    AND d.last_connection > s.fecha_ingreso
                                    AND d.last_connection >= NOW() - INTERVAL '17 minutes'
                                    AND COALESCE(d.sin_respuesta, FALSE) = FALSE
                                )
                            )

                            AND
                            (

                            :buscar = ''
                            OR LOWER(d.activo) LIKE LOWER(CONCAT('%', :buscar, '%'))
                            OR LOWER(d.device_name) LIKE LOWER(CONCAT('%', :buscar, '%'))
                            OR LOWER(d.model) LIKE LOWER(CONCAT('%', :buscar, '%'))
                            OR LOWER(d.ip_address) LIKE LOWER(CONCAT('%', :buscar, '%'))
                            OR LOWER(a.empleado_asig) LIKE LOWER(CONCAT('%', :buscar, '%'))
                            OR LOWER(a.codigo_emp) LIKE LOWER(CONCAT('%', :buscar, '%'))
                        )
                        AND
                        (
                            :planta = ''
                            OR a.planta = :planta
                        )
                AND
                (
                    :categoria = ''
                    OR (
                        UPPER(:categoria) = 'SIN DATOS'
                        AND a.activo IS NULL
                    )
                    OR (
                        UPPER(:categoria) <> 'SIN DATOS'
                        AND UPPER(COALESCE(d.categoria, '')) = UPPER(:categoria)
                    )
                )
                    AND
                (
                    :version = ''
                    OR (
                        :version = 'SIN_VERSION'
                        AND (
                            d.app_version IS NULL
                            OR TRIM(d.app_version) = ''
                        )
                    )
                    OR (
                        :version <> 'SIN_VERSION'
                        AND TRIM(COALESCE(d.app_version, '')) = TRIM(:version)
                    )
                )
                            AND
            (
                :estadoCargador = ''
                OR LOWER(d.estado_cargador) LIKE LOWER(CONCAT('%', :estadoCargador, '%'))
            )

            AND
            (
                :estado = ''
                OR
                (
                    :estado = 'OFFLINE'
                     COALESCE(d.sin_respuesta, FALSE) = TRUE
                    AND d.last_connection < NOW() - INTERVAL '17 minutes'
                )
                OR
                (
                    :estado = 'CARGADOR_DESCONECTADO'
                    AND LOWER(d.estado_cargador) LIKE '%desconectado%'
                )
                    OR
                    (
                :estado = 'CARGADOR_CONECTADO'
                AND d.estado_cargador = 'Conectado'
                )
                OR
                (
                    :estado = 'AUTORIZADO'
                    AND d.estado_red = 'Autorizado'
                )
                OR
                (
                    :estado = 'NO_AUTORIZADO'
                    AND (d.estado_red IS NULL OR d.estado_red <> 'Autorizado')
                )
            )
                AND
                (
                    :estadoBateria = ''
                    OR UPPER(COALESCE(d.estado_bateria, 'NORMAL')) = UPPER(:estadoBateria)
                )


            """, nativeQuery = true)
    Page<TabletDashboardProjection> obtenerDashboard(
            @Param("buscar") String buscar,
            @Param("planta") String planta,
            @Param("categoria") String categoria,
            @Param("version") String version,
            @Param("estadoCargador") String estadoCargador,
            @Param("estado") String estado,
            @Param("estadoBateria") String estadoBateria,
            Pageable pageable);

    @Query(value = """
                                                            SELECT
                                                                COUNT(*) AS total,

                                                                SUM(
                                                                    CASE
                                                                        WHEN d.last_connection < NOW() - INTERVAL '17 minutes'
                                                                        THEN 1 ELSE 0
                                                                    END
                                                                ) AS offline,

                                                                SUM(
                                                                    CASE
                                                                        WHEN d.estado_red = 'Autorizado'
                                                                        THEN 1 ELSE 0
                                                                    END
                                                                ) AS autorizados,

                                                                SUM(
                                                                    CASE
                                                                        WHEN LOWER(d.estado_cargador) LIKE '%desconectado%'
                                                                             AND UPPER(d.categoria) = 'CELULAR'
                                                                        THEN 1 ELSE 0
                                                                    END
                                                                ) AS nopowerCel,

                                                                SUM(
                                                                    CASE
                                                                        WHEN LOWER(d.estado_cargador) LIKE '%desconectado%'
                                                                             AND UPPER(d.categoria) = 'HANDHELD'
                                                                        THEN 1 ELSE 0
                                                                    END
                                                                ) AS nopowerHand,

                                                               SUM(
                                                    CASE
                                                        WHEN LOWER(d.estado_cargador) LIKE '%desconectado%'
                                                             AND UPPER(d.categoria) = 'CALIDAD'
                                                        THEN 1 ELSE 0
                                                    END
                                                ) AS nopowerTab,

                                                                SUM(
                                                                    CASE
                                                                        WHEN LOWER(d.estado_cargador) LIKE '%desconectado%'
                                                                             AND (d.categoria IS NULL
                                                                                  OR d.categoria = ''
                                                                                  OR UPPER(d.categoria) = 'GENERAL')
                                                                        THEN 1 ELSE 0
                                                                    END
                                                                ) AS nopowerGen,

                                                                SUM(
                                                                    CASE
                                                                        WHEN d.estado_red <> 'Autorizado'
                                                                             OR d.estado_red IS NULL
                                                                        THEN 1 ELSE 0
                                                                    END
                                                                ) AS noAutorizados
                                                                 ,
                                    SUM(
                                        CASE
                                            WHEN d.battery_level IS NOT NULL
                                                 AND d.battery_level < 20
                                            THEN 1 ELSE 0
                                        END
                                    ) AS bateriaBaja
                                     ,
                        SUM(
                            CASE
                                WHEN d.battery_level BETWEEN 80 AND 100
                                THEN 1 ELSE 0
                            END
                        ) AS bateria80a100,

                        SUM(
                            CASE
                                WHEN d.battery_level BETWEEN 50 AND 79
                                THEN 1 ELSE 0
                            END
                        ) AS bateria50a79,

                        SUM(
                            CASE
                                WHEN d.battery_level BETWEEN 20 AND 49
                                THEN 1 ELSE 0
                            END
                        ) AS bateria20a49,

                        SUM(
                            CASE
                                WHEN d.battery_level BETWEEN 0 AND 19
                                THEN 1 ELSE 0
                            END
                        ) AS bateria0a19,

                        SUM(
                            CASE
                                WHEN d.battery_level IS NULL
                                THEN 1 ELSE 0
                            END
                        ) AS bateriaSinDatos,

            SUM(
                CASE
                    WHEN UPPER(COALESCE(d.estado_bateria, 'NORMAL')) = 'INFLADA'
                    THEN 1 ELSE 0
                END
            ) AS bateriasInfladas

            FROM "monitoreo tablet".dispositivos d
                                                            """, nativeQuery = true)
    Object obtenerDashboardResumen();

    @Query(value = """
            SELECT *
            FROM public.activo_info
            WHERE activo = :activo
            """, nativeQuery = true)
    ActivoInfo obtenerActivoInfo(String activo);

    @Query(value = """
            SELECT
                COUNT(*) AS total,
                SUM(
                    CASE
                        WHEN last_connection < NOW() - INTERVAL '17 minutes'
                        THEN 1
                        ELSE 0
                    END
                ) AS offline,
                SUM(
                    CASE
                        WHEN estado_red = 'Autorizado'
                        THEN 1
                        ELSE 0
                    END
                ) AS autorizados
            FROM "monitoreo tablet".dispositivos
            """, nativeQuery = true)
    Object[] obtenerDashboard();

    @Query(value = """
            SELECT
                d.id,
                d.activo,
                d.android_id,
                d.device_name,
                d.model,
                d.categoria,
                a.codigo_emp      AS codigoEmpInfo,
                a.empleado_asig   AS empleadoAsig,
                a.planta          AS planta,
                a.area            AS area,
                a.departamento    AS departamento,
                d.battery_level,
                d.temperatura,
                d.estado_cargador,
                d.estado_wifi,
                d.estado_red      AS estado,
                d.ip_address,
                d.ram_usage,
                d.storage_usage,
                d.uptime,
                d.last_connection,
                d.os_version,
                d.estado_bateria AS estadoBateria,
                d.porcentaje_inflado AS porcentajeInflado
            FROM "monitoreo tablet".dispositivos d
            LEFT JOIN public.activo_info a
                   ON d.activo = a.activo
            WHERE
            (
                :buscar = ''
                OR LOWER(d.activo) LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(d.device_name) LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(d.model) LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(d.ip_address) LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(a.empleado_asig) LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(a.codigo_emp) LIKE LOWER(CONCAT('%', :buscar, '%'))
            )
            AND
            (
                :planta = ''
                OR a.planta = :planta
            )
            AND
            (
                :categoria = ''
                OR (
                    UPPER(:categoria) = 'SIN DATOS'
                    AND a.activo IS NULL
                )
                OR (
                    UPPER(:categoria) <> 'SIN DATOS'
                    AND UPPER(COALESCE(d.categoria, '')) = UPPER(:categoria)
                )
            )
            AND
            (
                :estadoCargador = ''
                OR LOWER(d.estado_cargador) LIKE LOWER(CONCAT('%', :estadoCargador, '%'))
            )
            AND
            (
                :estado = ''
                OR (
                    :estado = 'OFFLINE'
                    AND d.last_connection < NOW() - INTERVAL '17 minutes'
                )
                OR (
                    :estado = 'CARGADOR_DESCONECTADO'
                    AND LOWER(d.estado_cargador) LIKE '%desconectado%'
                )
                OR (
                    :estado = 'CARGADOR_CONECTADO'
                    AND d.estado_cargador = 'Conectado'
                )
                OR (
                    :estado = 'AUTORIZADO'
                    AND d.estado_red = 'Autorizado'
                )
                OR (
                    :estado = 'NO_AUTORIZADO'
                    AND (d.estado_red IS NULL OR d.estado_red <> 'Autorizado')
                )
                )
                AND
                (
                    :estadoBateria = ''
                    OR UPPER(COALESCE(d.estado_bateria, 'NORMAL')) = UPPER(:estadoBateria)
                )
            ORDER BY d.id ASC
            """, nativeQuery = true)
    List<TabletDashboardProjection> obtenerDashboardReporte(
            @Param("buscar") String buscar,
            @Param("planta") String planta,
            @Param("categoria") String categoria,
            @Param("estadoCargador") String estadoCargador,
            @Param("estado") String estado,
            @Param("estadoBateria") String estadoBateria);

    @Query(value = """
            SELECT d.*
            FROM "monitoreo tablet".dispositivos d
            INNER JOIN public.activo_info a
                    ON a.activo = d.activo
            WHERE UPPER(a.planta) = UPPER(:planta)
            """, nativeQuery = true)
    List<Tablet> obtenerPorPlanta(@Param("planta") String planta);

    @Query(value = """
            SELECT DISTINCT planta
            FROM public.activo_info
            WHERE planta IS NOT NULL
              AND planta <> ''
            ORDER BY planta
            """, nativeQuery = true)
    List<String> obtenerPlantas();

    @Query(value = """
            SELECT DISTINCT categoria
            FROM "monitoreo tablet".dispositivos
            WHERE categoria IS NOT NULL
              AND categoria <> ''
            ORDER BY categoria
            """, nativeQuery = true)
    List<String> obtenerCategorias();

    // ==========================================
    // TAREAS PROGRAMADAS - DEPARTAMENTO
    // ==========================================

    @Query(value = """
            SELECT d.*
            FROM "monitoreo tablet".dispositivos d
            INNER JOIN public.activo_info a
                    ON a.activo = d.activo
            WHERE UPPER(TRIM(a.departamento)) = UPPER(TRIM(:departamento))
            """, nativeQuery = true)
    List<Tablet> obtenerPorDepartamento(
            @Param("departamento") String departamento);

    @Query(value = """
            SELECT DISTINCT departamento
            FROM public.activo_info
            WHERE departamento IS NOT NULL
              AND TRIM(departamento) <> ''
            ORDER BY departamento
            """, nativeQuery = true)
    List<String> obtenerDepartamentos();

    // ==========================================
    // TAREAS PROGRAMADAS - EMPLEADO
    // ==========================================

    @Query(value = """
            SELECT d.*
            FROM "monitoreo tablet".dispositivos d
            INNER JOIN public.activo_info a
                    ON a.activo = d.activo
            WHERE UPPER(TRIM(a.empleado_asig)) = UPPER(TRIM(:empleado))
            """, nativeQuery = true)
    List<Tablet> obtenerPorEmpleado(
            @Param("empleado") String empleado);

    @Query(value = """
            SELECT DISTINCT empleado_asig
            FROM public.activo_info
            WHERE empleado_asig IS NOT NULL
              AND TRIM(empleado_asig) <> ''
            ORDER BY empleado_asig
            """, nativeQuery = true)
    List<String> obtenerEmpleados();

    @Query(value = """
            SELECT d.*
            FROM "monitoreo tablet".dispositivos d
            WHERE UPPER(TRIM(d.categoria)) = UPPER(TRIM(:categoria))
            """, nativeQuery = true)
    List<Tablet> obtenerPorCategoria(
            @Param("categoria") String categoria);

    @Query(value = """
            SELECT
                CASE
                    WHEN UPPER(TRIM(a.planta)) = 'PC' THEN 'Planta Cinturones'
                    WHEN UPPER(TRIM(a.planta)) = 'PF' THEN 'Planta Fajas'
                    ELSE 'SIN DATOS'
                END AS nombre,
                COUNT(*) AS cantidad
            FROM "monitoreo tablet".dispositivos d
            LEFT JOIN public.activo_info a
                ON TRIM(d.activo) = TRIM(a.activo)
            WHERE
                a.planta IS NULL
                OR UPPER(TRIM(a.planta)) IN ('PC','PF')
            GROUP BY
                CASE
                    WHEN UPPER(TRIM(a.planta)) = 'PC' THEN 'Planta Cinturones'
                    WHEN UPPER(TRIM(a.planta)) = 'PF' THEN 'Planta Fajas'
                    ELSE 'SIN DATOS'
                END
            ORDER BY
                CASE
                    WHEN
                        CASE
                            WHEN UPPER(TRIM(a.planta))='PC' THEN 'Planta Cinturones'
                            WHEN UPPER(TRIM(a.planta))='PF' THEN 'Planta Fajas'
                            ELSE 'SIN DATOS'
                        END='Planta Cinturones' THEN 1
                    WHEN
                        CASE
                            WHEN UPPER(TRIM(a.planta))='PC' THEN 'Planta Cinturones'
                            WHEN UPPER(TRIM(a.planta))='PF' THEN 'Planta Fajas'
                            ELSE 'SIN DATOS'
                        END='Planta Fajas' THEN 2
                    ELSE 3
                END
            """, nativeQuery = true)
    List<Object[]> obtenerGraficaPlantas();

    @Query(value = """
            SELECT
                CASE
                    WHEN d.categoria IS NULL
                         OR TRIM(d.categoria) = ''
                    THEN 'GENERAL'
                    ELSE UPPER(TRIM(d.categoria))
                END AS nombre,
                COUNT(*) AS cantidad
            FROM "monitoreo tablet".dispositivos d
            LEFT JOIN public.activo_info a
                   ON d.activo = a.activo
            WHERE
                (
                    :planta IS NULL
                    OR :planta = ''
                    OR UPPER(a.planta) = UPPER(:planta)
                )
            GROUP BY
                CASE
                    WHEN d.categoria IS NULL
                         OR TRIM(d.categoria) = ''
                    THEN 'GENERAL'
                    ELSE UPPER(TRIM(d.categoria))
                END
            ORDER BY
                CASE
                    WHEN
                        CASE
                            WHEN d.categoria IS NULL
                                 OR TRIM(d.categoria) = ''
                            THEN 'GENERAL'
                            ELSE UPPER(TRIM(d.categoria))
                        END='GENERAL'
                    THEN 1
                    WHEN
                        CASE
                            WHEN d.categoria IS NULL
                                 OR TRIM(d.categoria) = ''
                            THEN 'GENERAL'
                            ELSE UPPER(TRIM(d.categoria))
                        END='CELULAR'
                    THEN 2
                    WHEN
                        CASE
                            WHEN d.categoria IS NULL
                                 OR TRIM(d.categoria) = ''
                            THEN 'GENERAL'
                            ELSE UPPER(TRIM(d.categoria))
                        END='CALIDAD'
                    THEN 3
                    WHEN
                        CASE
                            WHEN d.categoria IS NULL
                                 OR TRIM(d.categoria) = ''
                            THEN 'GENERAL'
                            ELSE UPPER(TRIM(d.categoria))
                        END='HANDHELD'
                    THEN 4
                    ELSE 5
                END
            """, nativeQuery = true)
    List<Object[]> obtenerGraficaCategorias(
            @Param("planta") String planta);

    @Query(value = """
            SELECT
                CASE
                    WHEN d.estado_red='Autorizado'
                    THEN 'Autorizado'
                    ELSE 'No autorizado'
                END AS nombre,
                COUNT(*) AS cantidad
            FROM "monitoreo tablet".dispositivos d
            GROUP BY
                CASE
                    WHEN d.estado_red='Autorizado'
                    THEN 'Autorizado'
                    ELSE 'No autorizado'
                END
            ORDER BY nombre
            """, nativeQuery = true)
    List<Object[]> obtenerGraficaEstadoRed();

    @Query(value = """
            SELECT
                TO_CHAR(fecha_evento, 'DD/MM/YYYY HH24:MI:SS') AS fecha_evento,
                estado_cargador,
                porcentaje_bateria
            FROM "monitoreo tablet".historial_cargador
            WHERE tablet_id = :tabletId
              AND (COALESCE(CAST(:fechaDesde AS DATE), fecha_evento::date) <= fecha_evento::date)
              AND (COALESCE(CAST(:fechaHasta AS DATE), fecha_evento::date) >= fecha_evento::date)
            ORDER BY fecha_evento DESC, id DESC
            """, nativeQuery = true)
    List<Object[]> obtenerHistorialCargador(
            @Param("tabletId") Long tabletId,
            @Param("fechaDesde") String fechaDesde,
            @Param("fechaHasta") String fechaHasta);

    @Query(value = """
            SELECT
                TO_CHAR(fecha_evento, 'DD/MM/YYYY HH24:MI:SS') AS fecha_evento,
                estado_cargador,
                porcentaje_bateria
            FROM "monitoreo tablet".historial_cargador
            WHERE tablet_id = :tabletId
            ORDER BY fecha_evento DESC, id DESC
            """, nativeQuery = true)
    List<Object[]> obtenerHistorialCargadorSinFechas(
            @Param("tabletId") Long tabletId);

    @Query(value = """
            SELECT
                d.id,
                d.activo,
                d.android_id,
                d.device_name,
                d.model,
                d.categoria,

                a.codigo_emp AS codigoEmpInfo,
                a.empleado_asig AS empleadoAsig,
                a.planta AS planta,
                a.area AS area,
                a.departamento AS departamento,

                d.estado_bateria AS estadoBateria,
                d.porcentaje_inflado AS porcentajeInflado,
                d.battery_level,
                d.temperatura,
                d.estado_cargador,
                d.estado_wifi,
                d.estado_red AS estado,
                d.ip_address,
                d.ram_usage,
                d.storage_usage,
                d.uptime,
                d.last_connection,
                d.os_version,
                d.app_version AS appVersion

            FROM "monitoreo tablet".dispositivos d

            LEFT JOIN public.activo_info a
                   ON TRIM(d.activo) = TRIM(a.activo)

            WHERE
            (
                :buscar = ''
                OR LOWER(COALESCE(d.activo, ''))
                    LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(COALESCE(d.device_name, ''))
                    LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(COALESCE(d.model, ''))
                    LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(COALESCE(d.categoria, ''))
                    LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(COALESCE(a.empleado_asig, ''))
                    LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(COALESCE(a.codigo_emp, ''))
                    LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(COALESCE(a.departamento, ''))
                    LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(COALESCE(a.area, ''))
                    LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(COALESCE(a.planta, ''))
                    LIKE LOWER(CONCAT('%', :buscar, '%'))
            )

            AND
            (
                :planta = ''
                OR UPPER(TRIM(COALESCE(a.planta, '')))
                    = UPPER(TRIM(:planta))
            )

            AND
            (
                :departamento = ''
                OR UPPER(TRIM(COALESCE(a.departamento, '')))
                    = UPPER(TRIM(:departamento))
            )

            AND
            (
                :categoria = ''
                OR UPPER(TRIM(COALESCE(d.categoria, '')))
                    = UPPER(TRIM(:categoria))
            )

            ORDER BY d.id ASC
            """,

            countQuery = """
                    SELECT COUNT(*)

                    FROM "monitoreo tablet".dispositivos d

                    LEFT JOIN public.activo_info a
                           ON TRIM(d.activo) = TRIM(a.activo)

                    WHERE
                    (
                        :buscar = ''
                        OR LOWER(COALESCE(d.activo, ''))
                            LIKE LOWER(CONCAT('%', :buscar, '%'))
                        OR LOWER(COALESCE(d.device_name, ''))
                            LIKE LOWER(CONCAT('%', :buscar, '%'))
                        OR LOWER(COALESCE(d.model, ''))
                            LIKE LOWER(CONCAT('%', :buscar, '%'))
                        OR LOWER(COALESCE(d.categoria, ''))
                            LIKE LOWER(CONCAT('%', :buscar, '%'))
                        OR LOWER(COALESCE(a.empleado_asig, ''))
                            LIKE LOWER(CONCAT('%', :buscar, '%'))
                        OR LOWER(COALESCE(a.codigo_emp, ''))
                            LIKE LOWER(CONCAT('%', :buscar, '%'))
                        OR LOWER(COALESCE(a.departamento, ''))
                            LIKE LOWER(CONCAT('%', :buscar, '%'))
                        OR LOWER(COALESCE(a.area, ''))
                            LIKE LOWER(CONCAT('%', :buscar, '%'))
                        OR LOWER(COALESCE(a.planta, ''))
                            LIKE LOWER(CONCAT('%', :buscar, '%'))
                    )

                    AND
                    (
                        :planta = ''
                        OR UPPER(TRIM(COALESCE(a.planta, '')))
                            = UPPER(TRIM(:planta))
                    )

                    AND
                    (
                        :departamento = ''
                        OR UPPER(TRIM(COALESCE(a.departamento, '')))
                            = UPPER(TRIM(:departamento))
                    )

                    AND
                    (
                        :categoria = ''
                        OR UPPER(TRIM(COALESCE(d.categoria, '')))
                            = UPPER(TRIM(:categoria))
                    )
                    """,

            nativeQuery = true)

    Page<TabletDashboardProjection> obtenerTabletsSelector(
            @Param("buscar") String buscar,
            @Param("planta") String planta,
            @Param("departamento") String departamento,
            @Param("categoria") String categoria,
            Pageable pageable);

    @Query(value = """
            SELECT DISTINCT TRIM(app_version)
            FROM "monitoreo tablet".dispositivos
            WHERE app_version IS NOT NULL
              AND TRIM(app_version) <> ''
            ORDER BY TRIM(app_version)
            """, nativeQuery = true)
    List<String> obtenerVersionesApp();
}
