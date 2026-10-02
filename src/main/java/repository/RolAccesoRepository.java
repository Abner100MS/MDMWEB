package repository;

import Entidad.RolAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolAccesoRepository extends JpaRepository<RolAcceso, Long> {

    Optional<RolAcceso> findByPasswordAndActivoTrue(String password);

    boolean existsByPassword(String password);

    Optional<RolAcceso> findFirstByRolIgnoreCaseAndActivoTrue(String rol);

}