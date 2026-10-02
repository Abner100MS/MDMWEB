package controller;

import Dto.CrearTareaProgramadaDTO;
import Dto.TareaProgramadaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import service.TareaProgramadaService;
import Dto.DetalleTareaDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/tareas")
public class TareaProgramadaController {

    @Autowired
    private TareaProgramadaService tareaProgramadaService;

    @PostMapping
    public TareaProgramadaDTO crear(@RequestBody CrearTareaProgramadaDTO dto) {

        return tareaProgramadaService.crear(dto);
    }

    @GetMapping
    public List<TareaProgramadaDTO> listar() {

        return tareaProgramadaService.listar();
    }

    @GetMapping("/{id}")
    public TareaProgramadaDTO obtener(@PathVariable Long id) {

        return tareaProgramadaService.obtener(id);
    }

    @PutMapping("/{id}")
    public TareaProgramadaDTO actualizar(
            @PathVariable Long id,
            @RequestBody CrearTareaProgramadaDTO dto) {

        return tareaProgramadaService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {

        tareaProgramadaService.eliminar(id);
    }

    @GetMapping("/{id}/detalle")
    public DetalleTareaDTO obtenerDetalle(@PathVariable Long id) {

        return tareaProgramadaService.obtenerDetalle(id);

    }

    @GetMapping("/{id}/dispositivos")
    public Page<DetalleTareaDTO.Dispositivo> obtenerDispositivos(

            @PathVariable Long id,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "25") int size,

            @RequestParam(defaultValue = "") String buscar,

            @RequestParam(defaultValue = "") String estado) {

        Pageable pageable = PageRequest.of(page, size);

        return tareaProgramadaService.obtenerDispositivosDetalle(
                id,
                buscar,
                estado,
                pageable);

    }

    // ==========================================
    // OPCIONES DE DESTINO PARA TAREAS
    // ==========================================

    @GetMapping("/opciones/plantas")
    public List<String> obtenerPlantas() {

        return tareaProgramadaService.obtenerPlantas();
    }

    @GetMapping("/opciones/categorias")
    public List<String> obtenerCategorias() {

        return tareaProgramadaService.obtenerCategorias();
    }

    @GetMapping("/opciones/departamentos")
    public List<String> obtenerDepartamentos() {

        return tareaProgramadaService.obtenerDepartamentos();
    }

    @GetMapping("/opciones/empleados")
    public List<String> obtenerEmpleados() {

        return tareaProgramadaService.obtenerEmpleados();
    }

}