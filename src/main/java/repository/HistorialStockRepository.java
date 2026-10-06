package repository;

import Entidad.HistorialStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HistorialStockRepository
        extends JpaRepository<HistorialStock, Long> {

    Optional<HistorialStock> findByActivo(String activo);
}