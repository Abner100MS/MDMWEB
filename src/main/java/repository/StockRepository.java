package repository;

import Entidad.Stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;
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

}