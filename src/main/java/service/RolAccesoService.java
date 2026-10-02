package service;

import Entidad.RolAcceso;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import repository.RolAccesoRepository;

import java.util.List;

@Service
public class RolAccesoService {

    @Autowired
    private RolAccesoRepository rolAccesoRepository;

    public List<RolAcceso> obtenerTodos() {
        return rolAccesoRepository.findAll();
    }

    public RolAcceso obtenerPorId(Long id) {
        return rolAccesoRepository.findById(id).orElse(null);
    }

    public RolAcceso guardar(RolAcceso rol) {
        return rolAccesoRepository.save(rol);
    }

    public void eliminar(Long id) {
        rolAccesoRepository.deleteById(id);
    }

   public boolean existePassword(String password) {
    return rolAccesoRepository.existsByPassword(password);
}

    public RolAcceso validarPassword(String password) {
    return rolAccesoRepository
            .findByPasswordAndActivoTrue(password)
            .orElse(null);
}

public RolAcceso obtenerActivoPorRol(String rol) {
    return rolAccesoRepository
            .findFirstByRolIgnoreCaseAndActivoTrue(rol)
            .orElse(null);
}

}