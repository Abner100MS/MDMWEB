package repository;

import Entidad.HistorialStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialStockRepository
        extends JpaRepository<HistorialStock, Long> {

    List<HistorialStock> findByActivoOrderByFechaDesc(String activo);
}