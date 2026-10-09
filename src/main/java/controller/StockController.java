package controller;

import Entidad.Stock;
import Entidad.HistorialStock;
import repository.StockRepository;
import repository.HistorialStockRepository;
import repository.TabletRepository;
import service.StockService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*")
public class StockController {

        private final StockRepository stockRepository;
        private final HistorialStockRepository historialStockRepository;
        private final TabletRepository tabletRepository;

        @Autowired
        private StockService stockService;

        public StockController(
                        StockRepository stockRepository,
                        HistorialStockRepository historialStockRepository,
                        TabletRepository tabletRepository) {

                this.stockRepository = stockRepository;
                this.historialStockRepository = historialStockRepository;
                this.tabletRepository = tabletRepository;
        }

        // =====================================================
        // LISTAR EQUIPOS ACTUALMENTE EN STOCK
        // =====================================================

        @GetMapping
        public ResponseEntity<List<Map<String, Object>>> listarStock() {

                return ResponseEntity.ok(
                                stockRepository.listarStockCompleto());
        }

        // =====================================================
        // BUSCAR UN ACTIVO EN STOCK
        // =====================================================

        @GetMapping("/{activo}")
        public ResponseEntity<?> buscarPorActivo(
                        @PathVariable String activo) {

                return stockRepository.findByActivo(activo)
                                .<ResponseEntity<?>>map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.status(404).body(
                                                Map.of(
                                                                "success", false,
                                                                "message", "El activo no se encuentra en Stock")));
        }

        // =====================================================
        // ENVIAR / INGRESAR EQUIPO A STOCK
        // =====================================================

        @PostMapping
        public ResponseEntity<?> ingresarStock(
                        @RequestBody Map<String, String> body) {

                String activo = body.get("activo");
                String motivo = body.get("motivo");
                String condicion = body.get("condicion");
                String observacion = body.get("observacion");
                String usuario = body.get("usuario");

                // =====================================================
                // VALIDAR ACTIVO
                // =====================================================

                if (activo == null || activo.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El activo es obligatorio"));
                }

                // =====================================================
                // VALIDAR MOTIVO
                // =====================================================

                if (motivo == null || motivo.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El motivo es obligatorio"));
                }

                // =====================================================
                // VALIDAR USUARIO
                // =====================================================

                if (usuario == null || usuario.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "No se pudo identificar al usuario"));
                }

                activo = activo.trim();
                motivo = motivo.trim();
                usuario = usuario.trim();

                // =====================================================
                // VALIDAR SI YA ESTÁ EN STOCK
                // =====================================================

                if (stockRepository.existsByActivo(activo)) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El activo ya se encuentra en Stock"));
                }

                // =====================================================
                // CREAR STOCK
                // =====================================================

                Stock stock = new Stock();

                stock.setActivo(activo);
                stock.setMotivoIngreso(motivo);

                if (condicion != null && !condicion.trim().isEmpty()) {
                        stock.setCondicion(
                                        condicion.trim().toUpperCase());
                } else {
                        stock.setCondicion("BUENO");
                }

                stock.setObservacion(observacion);

                Stock guardado = stockRepository.save(stock);

                // =====================================================
                // REGISTRAR HISTORIAL
                // =====================================================

                registrarHistorialStock(
                                activo,
                                "INGRESO_STOCK",
                                stock.getCondicion(),
                                motivo,
                                usuario);

                // =====================================================
                // RESPUESTA
                // =====================================================

                return ResponseEntity.ok(
                                Map.of(
                                                "success", true,
                                                "message", "Equipo ingresado a Stock correctamente",
                                                "stock", guardado));
        }
        // =====================================================
        // LIBERAR EQUIPO DE STOCK
        // =====================================================

        @PutMapping("/{activo}/liberar")
        public ResponseEntity<?> liberarStock(
                        @PathVariable String activo,
                        @RequestBody(required = false) Map<String, String> body) {

                Stock stock = stockRepository.findByActivo(activo).orElse(null);

                if (stock == null) {
                        return ResponseEntity.status(404).body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El activo no se encuentra en Stock"));
                }

                // =====================================================
                // VALIDAR QUE EL EQUIPO EXISTA EN EL MDM
                // =====================================================

                boolean existeEnMdm = tabletRepository.findByActivo(activo).isPresent();

                if (!existeEnMdm) {

                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message",
                                                        "El activo " + activo
                                                                        + " nunca ha sido registrado en el MDM. "
                                                                        + "Debe vincular el dispositivo antes de liberarlo de Stock"));
                }

                // =====================================================
                // OBTENER DATOS
                // =====================================================

                String comentario = "Equipo liberado de Stock";

                String usuario = body != null
                                ? body.get("usuario")
                                : null;

                if (body != null
                                && body.get("comentario") != null
                                && !body.get("comentario").trim().isEmpty()) {

                        comentario = body.get("comentario").trim();
                }

                // =====================================================
                // VALIDAR USUARIO
                // =====================================================

                if (usuario == null || usuario.trim().isEmpty()) {

                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "No se pudo identificar al usuario"));
                }

                usuario = usuario.trim();

                // =====================================================
                // REGISTRAR LIBERACIÓN EN HISTORIAL
                // =====================================================

                registrarHistorialStock(
                                activo,
                                "LIBERADO_STOCK",
                                stock.getCondicion(),
                                comentario,
                                usuario);

                // =====================================================
                // ELIMINAR DEL STOCK ACTUAL
                // =====================================================

                stockRepository.delete(stock);

                return ResponseEntity.ok(
                                Map.of(
                                                "success", true,
                                                "message", "Equipo liberado de Stock correctamente",
                                                "activo", activo));
        }

        // =====================================================
        // HISTORIAL DE UN ACTIVO
        // =====================================================

        @GetMapping("/{activo}/historial")
        public ResponseEntity<?> obtenerHistorial(
                        @PathVariable String activo) {

                return historialStockRepository.findByActivo(activo)
                                .<ResponseEntity<?>>map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.status(404).body(
                                                Map.of(
                                                                "success", false,
                                                                "message",
                                                                "No existe historial para el activo " + activo)));
        }
        // =====================================================
        // BUSCAR INFORMACIÓN DE ACTIVO PARA INGRESO A STOCK
        // =====================================================

        @GetMapping("/buscar/{activo}")
        public ResponseEntity<?> buscarInformacionActivo(
                        @PathVariable String activo) {

                activo = activo.trim();

                Map<String, Object> info = stockRepository.buscarInformacionActivo(activo);

                if (info == null || info.isEmpty()) {

                        return ResponseEntity.status(404).body(
                                        Map.of(
                                                        "success", false,
                                                        "message",
                                                        "No se encontró información para el activo " + activo));
                }

                return ResponseEntity.ok(info);
        }

        // =====================================================
        // EDITAR EQUIPO EN STOCK
        // =====================================================

        @PutMapping("/{activo}")
        public ResponseEntity<?> editarStock(
                        @PathVariable String activo,
                        @RequestBody Map<String, String> body) {

                Stock stock = stockRepository.findByActivo(activo).orElse(null);

                if (stock == null) {
                        return ResponseEntity.status(404).body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El activo no se encuentra en Stock"));
                }

                // =====================================================
                // DATOS RECIBIDOS
                // =====================================================

                String condicion = body.get("condicion");
                String motivo = body.get("motivo");
                String usuario = body.get("usuario");

                // =====================================================
                // VALIDAR CONDICIÓN
                // =====================================================

                if (condicion == null || condicion.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "La condición es obligatoria"));
                }
                condicion = condicion.trim().toUpperCase();

                if (!condicion.equals("BUENO")
                                && !condicion.equals("CON FALLA")
                                && !condicion.equals("BATERÍA INFLADA")) {

                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "La condición seleccionada no es válida"));
                }

                // =====================================================
                // VALIDAR MOTIVO
                // =====================================================

                if (motivo == null || motivo.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "Debe indicar el motivo del cambio"));
                }

                // =====================================================
                // VALIDAR USUARIO
                // =====================================================

                if (usuario == null || usuario.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "No se pudo identificar al usuario"));
                }

                motivo = motivo.trim();
                usuario = usuario.trim();

                // =====================================================
                // ACTUALIZAR STOCK
                // =====================================================

                stock.setCondicion(condicion);
                stock.setMotivoIngreso(motivo);

                stockRepository.save(stock);

                // =====================================================
                // REGISTRAR HISTORIAL
                // =====================================================

                registrarHistorialStock(
                                activo,
                                "ACTUALIZACION_STOCK",
                                condicion,
                                motivo,
                                usuario);

                // =====================================================
                // RESPUESTA
                // =====================================================

                return ResponseEntity.ok(
                                Map.of(
                                                "success", true,
                                                "message", "Información de Stock actualizada correctamente"));
        }

        private void registrarHistorialStock(
                        String activo,
                        String accion,
                        String condicion,
                        String motivo,
                        String usuario) {

                LocalDateTime ahora = LocalDateTime.now();

                HistorialStock historial = historialStockRepository.findByActivo(activo)
                                .orElse(null);

                // =====================================================
                // PRIMER MOVIMIENTO DEL ACTIVO
                // =====================================================

                if (historial == null) {

                        historial = new HistorialStock();

                        historial.setActivo(activo);

                        historial.setAccionActual(accion);
                        historial.setCondicionActual(condicion);
                        historial.setMotivoActual(motivo);
                        historial.setUsuarioActual(usuario);
                        historial.setFechaActual(ahora);

                        historialStockRepository.save(historial);

                        return;
                }

                // =====================================================
                // ACTUAL PASA A ANTERIOR
                // =====================================================

                historial.setAccionAnterior(
                                historial.getAccionActual());

                historial.setCondicionAnterior(
                                historial.getCondicionActual());

                historial.setMotivoAnterior(
                                historial.getMotivoActual());

                historial.setUsuarioAnterior(
                                historial.getUsuarioActual());

                historial.setFechaAnterior(
                                historial.getFechaActual());

                // =====================================================
                // NUEVO MOVIMIENTO PASA A ACTUAL
                // =====================================================

                historial.setAccionActual(accion);
                historial.setCondicionActual(condicion);
                historial.setMotivoActual(motivo);
                historial.setUsuarioActual(usuario);
                historial.setFechaActual(ahora);

                // =====================================================
                // ACTUALIZA LA MISMA FILA
                // =====================================================

                historialStockRepository.save(historial);
        }

        // =====================================================
        // ELIMINAR / DAR DE BAJA EQUIPO
        // =====================================================

        @DeleteMapping("/{activo}")
        public ResponseEntity<?> eliminarStock(
                        @PathVariable String activo,
                        @RequestBody Map<String, String> body) {

                // =====================================================
                // BUSCAR EQUIPO EN STOCK
                // =====================================================

                Stock stock = stockRepository.findByActivo(activo).orElse(null);

                if (stock == null) {
                        return ResponseEntity.status(404).body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El activo no se encuentra en Stock"));
                }

                // =====================================================
                // DATOS RECIBIDOS
                // =====================================================

                String motivo = body.get("motivo");
                String usuario = body.get("usuario");

                // =====================================================
                // VALIDAR MOTIVO
                // =====================================================

                if (motivo == null || motivo.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "Debe indicar el motivo de la baja"));
                }

                // =====================================================
                // VALIDAR USUARIO
                // =====================================================

                if (usuario == null || usuario.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "No se pudo identificar al usuario"));
                }

                motivo = motivo.trim();
                usuario = usuario.trim();

                // =====================================================
                // REGISTRAR BAJA EN HISTORIAL
                // IMPORTANTE: SE HACE ANTES DE ELIMINAR
                // =====================================================

                registrarHistorialStock(
                                activo,
                                "BAJA_STOCK",
                                stock.getCondicion(),
                                motivo,
                                usuario);

                // =====================================================
                // ELIMINAR DE DISPOSITIVOS SI EXISTE EN EL MDM
                // =====================================================

                tabletRepository.findByActivo(activo)
                                .ifPresent(tabletRepository::delete);

                // =====================================================
                // ELIMINAR DEL STOCK ACTUAL
                // =====================================================

                stockRepository.delete(stock);

                // =====================================================
                // RESPUESTA
                // =====================================================

                return ResponseEntity.ok(
                                Map.of(
                                                "success", true,
                                                "message", "Equipo dado de baja correctamente",
                                                "activo", activo));
        }

        @GetMapping("/reporte")
        public ResponseEntity<byte[]> generarReporteStock(
                        @RequestParam(required = false) String buscar,
                        @RequestParam(required = false) String filtro,
                        @RequestParam(required = false) String columnas) {

                try {

                        byte[] archivo = stockService.obtenerReporteStock(
                                        buscar,
                                        filtro,
                                        columnas);

                        return ResponseEntity.ok()
                                        .header(
                                                        HttpHeaders.CONTENT_DISPOSITION,
                                                        "attachment; filename=Reporte_Stock.xlsx")
                                        .contentType(
                                                        MediaType.parseMediaType(
                                                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                        .body(archivo);

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity
                                        .internalServerError()
                                        .build();
                }
        }

        // =====================================================
        // CONSULTAR ESTADO DE BATERÍA PARA ENVÍO A STOCK
        // =====================================================

        @GetMapping("/bateria/{activo}")
        public ResponseEntity<?> consultarBateriaStock(
                        @PathVariable String activo) {

                return tabletRepository.findByActivo(activo)
                                .map(tablet -> ResponseEntity.ok(
                                                Map.of(
                                                                "activo", activo,
                                                                "estadoBateria",
                                                                tablet.getEstadoBateria() != null
                                                                                ? tablet.getEstadoBateria()
                                                                                : "NORMAL")))
                                .orElseGet(() -> ResponseEntity.status(404).body(
                                                Map.of(
                                                                "message", "Dispositivo no encontrado")));
        }
}