package controller;

import Entidad.Stock;
import Entidad.HistorialStock;
import repository.StockRepository;
import repository.HistorialStockRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
@CrossOrigin(origins = "*")
public class StockController {

        private final StockRepository stockRepository;
        private final HistorialStockRepository historialStockRepository;

        public StockController(
                        StockRepository stockRepository,
                        HistorialStockRepository historialStockRepository) {

                this.stockRepository = stockRepository;
                this.historialStockRepository = historialStockRepository;
        }

        // =====================================================
        // LISTAR EQUIPOS ACTUALMENTE EN STOCK
        // =====================================================

        @GetMapping
        public ResponseEntity<List<Stock>> listarStock() {

                return ResponseEntity.ok(stockRepository.findAll());
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

                if (activo == null || activo.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El activo es obligatorio"));
                }

                if (motivo == null || motivo.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El motivo es obligatorio"));
                }

                activo = activo.trim();

                if (stockRepository.existsByActivo(activo)) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El activo ya se encuentra en Stock"));
                }

                Stock stock = new Stock();

                stock.setActivo(activo);
                stock.setMotivoIngreso(motivo.trim());

                if (condicion != null && !condicion.trim().isEmpty()) {
                        stock.setCondicion(condicion.trim().toUpperCase());
                } else {
                        stock.setCondicion("BUENO");
                }

                stock.setObservacion(observacion);

                Stock guardado = stockRepository.save(stock);

                // Guardar historial
                HistorialStock historial = new HistorialStock();

                historial.setActivo(activo);
                historial.setAccion("INGRESO_STOCK");
                historial.setComentario(motivo.trim());

                historialStockRepository.save(historial);

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

                String comentario = "Equipo liberado de Stock";

                if (body != null
                                && body.get("comentario") != null
                                && !body.get("comentario").trim().isEmpty()) {

                        comentario = body.get("comentario").trim();
                }

                // Primero guardamos historial
                HistorialStock historial = new HistorialStock();

                historial.setActivo(activo);
                historial.setAccion("LIBERADO_STOCK");
                historial.setComentario(comentario);

                historialStockRepository.save(historial);

                // Después sale del Stock actual
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
        public ResponseEntity<List<HistorialStock>> obtenerHistorial(
                        @PathVariable String activo) {

                return ResponseEntity.ok(
                                historialStockRepository
                                                .findByActivoOrderByFechaDesc(activo));
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
}