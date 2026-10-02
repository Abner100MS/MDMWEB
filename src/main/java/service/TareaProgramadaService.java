package service;

import Dto.CrearTareaProgramadaDTO;
import Dto.TareaProgramadaDTO;
import Dto.DetalleTareaDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TareaProgramadaService {

    TareaProgramadaDTO crear(CrearTareaProgramadaDTO dto);

    List<TareaProgramadaDTO> listar();

    TareaProgramadaDTO obtener(Long id);

    TareaProgramadaDTO actualizar(Long id, CrearTareaProgramadaDTO dto);

    DetalleTareaDTO obtenerDetalle(Long tareaId);

    void eliminar(Long id);

    void programar(Long tareaId);

    void reprogramar(Long tareaId);

    void cancelar(Long tareaId);

    void ejecutar(Long tareaId);

    void iniciar();

    // <-- AGREGA ESTE MÉTODO AL FINAL
    Page<DetalleTareaDTO.Dispositivo> obtenerDispositivosDetalle(
            Long tareaId,
            String buscar,
            String estado,
            Pageable pageable);

    void confirmarActualizacion(Long tabletId);

    void procesarActualizacionPendiente(Long tabletId);

    List<String> obtenerPlantas();

    List<String> obtenerCategorias();

    List<String> obtenerDepartamentos();

    List<String> obtenerEmpleados();
}