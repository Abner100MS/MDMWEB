package repository;

import Entidad.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByActivo(String activo);

    boolean existsByActivo(String activo);

    void deleteByActivo(String activo);
}