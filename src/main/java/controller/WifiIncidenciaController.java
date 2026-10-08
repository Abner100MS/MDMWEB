
package controller;

import Entidad.WifiIncidencia;
import service.WifiIncidenciaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import repository.WifiResumenProjection;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/wifi-incidencias")
public class WifiIncidenciaController {

    private final WifiIncidenciaService wifiService;

    public WifiIncidenciaController(
            WifiIncidenciaService wifiService) {

        this.wifiService = wifiService;
    }

    // =====================================================
    // RECIBIR INCIDENCIA DESDE ANDROID
    // =====================================================
    @PostMapping
    public ResponseEntity<?> recibirIncidencia(
            @RequestBody WifiIncidencia incidencia) {

        try {

            WifiIncidencia guardada = wifiService.guardarIncidencia(incidencia);

            // Confirmación para Android.
            // Android eliminará el registro local
            // únicamente si recibe este ACK.

            return ResponseEntity.ok(
                    Map.of(
                            "ack", true,
                            "eventoId", guardada.getEventoId(),
                            "mensaje", "Incidencia WiFi recibida y guardada"));

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "ack", false,
                            "mensaje", e.getMessage()));

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "ack", false,
                            "mensaje", "Error al guardar incidencia WiFi"));
        }
    }

    // =====================================================
    // CONSULTAR HISTORIAL DE INCIDENCIAS WIFI
    // =====================================================
    @GetMapping
    public ResponseEntity<List<WifiIncidencia>> listarHistorial() {

        return ResponseEntity.ok(
                wifiService.listarHistorial());
    }

    // =====================================================
    // CONSULTAR RESUMEN WIFI - UNA FILA POR ACTIVO
    // =====================================================
    @GetMapping("/resumen")
    public ResponseEntity<Page<WifiResumenProjection>> consultarResumen(

            @RequestParam(required = false) String activo,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,

            @RequestParam(defaultValue = "0") int pagina) {

        return ResponseEntity.ok(
                wifiService.consultarResumen(
                        activo,
                        desde,
                        hasta,
                        pagina,
                        10));
    }

    // =====================================================
    // CONSULTAR HISTORIAL INDIVIDUAL DE UN ACTIVO
    // =====================================================
    @GetMapping("/historial")
    public ResponseEntity<?> consultarHistorial(

            @RequestParam String activo,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,

            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,

            @RequestParam(defaultValue = "0") int pagina) {

        try {

            Page<WifiIncidencia> historial = wifiService.consultarHistorial(
                    activo,
                    desde,
                    hasta,
                    pagina,
                    10);

            return ResponseEntity.ok(historial);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "mensaje", e.getMessage()));
        }
    }

}
