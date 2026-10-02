package repository;

import Entidad.AuditoriaDispositivo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditoriaDispositivoRepository
        extends JpaRepository<AuditoriaDispositivo, Long> {

    Optional<AuditoriaDispositivo>
    findByActivoAndAccion(String activo, String accion);
}