package controller;

import Entidad.AuditoriaDispositivo;
import Entidad.RolAcceso;
import Entidad.Tablet;
import Entidad.ActivoInfo;
import service.TabletService;
import service.UsuarioService;
import service.WebTitleService;
import repository.TabletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import Dto.TabletDashboardProjection;
import Dto.WebUrlDTO;
import Dto.DashboardDTO;
import Dto.LocationDTO;
import java.util.stream.Collectors;
import com.example.monitoreo.MdmSocketHandler;

import service.AuditoriaDispositivoService;
import service.GpsHistoryService;
import service.ReglaAppsService;
import service.RolAccesoService;
import Dto.PolicyDTO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import exception.GpsTrackingAlreadyActiveException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import Entidad.Usuario;
import Dto.RestriccionInstalacionMasivaRequest;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/devices") // Cambiamos esto para que coincida con la app
public class TabletController {

        @Autowired
        private TabletService tabletService;

        @Autowired
        private TabletRepository tabletRepository;

        @Autowired
        private GpsHistoryService gpsHistoryService;

        @Autowired
        private ReglaAppsService reglaAppsService;

        @Autowired
        private RolAccesoService rolAccesoService;

        @Autowired
        private AuditoriaDispositivoService auditoriaDispositivoService;

        @Autowired
        private UsuarioService usuarioService;

        // La app llama a /devices/register
        @PostMapping("/register")
        public ResponseEntity<?> registrar(@RequestBody Tablet tablet) {

                boolean dispositivoNuevo = tabletRepository.findByActivo(tablet.getActivo()).isEmpty();

                Tablet t = tabletService.procesarHeartbeat(tablet);

                if (t == null) {
                        return ResponseEntity.badRequest().body("Activo obligatorio");
                }

                Map<String, Object> respuesta = new java.util.LinkedHashMap<>();

                respuesta.put("id", t.getId());
                respuesta.put("activo", t.getActivo());

                if (dispositivoNuevo) {

                        // ==============================
                        // REGLA INICIAL DE APLICACIONES
                        // ==============================

                        var packages = reglaAppsService.obtenerReglaEfectiva(t);

                        respuesta.put("apps_rule", packages);

                        // ==============================
                        // CREDENCIALES INICIALES
                        // ==============================

                        RolAcceso admin = rolAccesoService.obtenerActivoPorRol("ADMIN");

                        RolAcceso tecnico = rolAccesoService.obtenerActivoPorRol("TECNICO");

                        Map<String, String> credenciales = new java.util.LinkedHashMap<>();

                        if (admin != null) {
                                credenciales.put("ADMIN", admin.getPassword());
                        }

                        if (tecnico != null) {
                                credenciales.put("TECNICO", tecnico.getPassword());
                        }

                        respuesta.put("credentials", credenciales);

                        System.out.println(
                                        "VINCULACION NUEVA | ACTIVO=" +
                                                        t.getActivo() +
                                                        " | REGLA ENVIADA=" +
                                                        packages.size() +
                                                        " | CREDENCIALES ENVIADAS=" +
                                                        credenciales.size());

                } else {

                        System.out.println(
                                        "REVINCULACION | ACTIVO=" +
                                                        t.getActivo() +
                                                        " | NO SE MODIFICA REGLA DE APPS");
                }

                return ResponseEntity.ok(respuesta);
        }

        @PostMapping("/restrictions/install-apps/bulk")
        public ResponseEntity<?> cambiarRestriccionInstalacionMasiva(
                        @RequestBody RestriccionInstalacionMasivaRequest request) {

                if (request.getActivos() == null || request.getActivos().isEmpty()) {
                        return ResponseEntity.badRequest()
                                        .body("Debe enviar al menos un activo.");
                }

                List<String> exitosos = new ArrayList<>();
                List<String> errores = new ArrayList<>();

                for (String activo : request.getActivos()) {

                        try {

                                // Modifica únicamente las restricciones necesarias
                                Tablet tabletActualizada = tabletService.cambiarRestriccionesInstalacionMasiva(
                                                activo,
                                                request.isBloquear());

                                // Enviar configuración actualizada a la tablet
                                String jsonResponse = objectMapper.writeValueAsString(tabletActualizada);

                                MdmSocketHandler.enviarOrden(
                                                tabletActualizada.getId().toString(),
                                                jsonResponse);

                                exitosos.add(activo);

                                System.out.println(
                                                "RESTRICCIONES MASIVAS | ACTIVO=" + activo +
                                                                " | INSTALL_APPS=" + request.isBloquear() +
                                                                " | DEVELOPER=false" +
                                                                " | FACTORY_RESET=false");

                        } catch (Exception e) {

                                errores.add(activo);

                                System.err.println(
                                                "ERROR RESTRICCIONES MASIVAS | ACTIVO=" +
                                                                activo + " | " + e.getMessage());
                        }
                }

                Map<String, Object> respuesta = new HashMap<>();

                respuesta.put("bloquearInstalacion", request.isBloquear());
                respuesta.put("developer", false);
                respuesta.put("factoryReset", false);
                respuesta.put("totalSolicitados", request.getActivos().size());
                respuesta.put("exitosos", exitosos);
                respuesta.put("errores", errores);

                return ResponseEntity.ok(respuesta);
        }

        @GetMapping("/{id}/apps-regla")
        public ResponseEntity<?> obtenerAppsRegla(
                        @PathVariable Long id) {

                Tablet tablet = tabletRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));

                var packages = reglaAppsService.obtenerReglaEfectiva(tablet);

                return ResponseEntity.ok(
                                Map.of(
                                                "activo", tablet.getActivo(),
                                                "packages", packages));
        }

        // La app llama a /devices/{id}/heartbeat
        @PostMapping("/{id}/heartbeat")
        public ResponseEntity<?> heartbeat(@PathVariable Long id, @RequestBody Tablet datos) {

                datos.setId(id);

                Tablet tablet = tabletService.procesarHeartbeat(datos);

                if (tablet == null) {
                        return ResponseEntity.badRequest().body("Activo obligatorio");
                }

                return ResponseEntity.ok(tablet);
        }

        @GetMapping("/{id}/online")
        public ResponseEntity<?> estadoOnline(@PathVariable Long id) {

                boolean online = MdmSocketHandler.estaTabletConectada(
                                String.valueOf(id));

                return ResponseEntity.ok(
                                Map.of("online", online));
        }

        @GetMapping("/{id}/diagnostico")
        public ResponseEntity<?> diagnosticarTablet(
                        @PathVariable Long id) {

                // =========================================
                // BUSCAR TABLET
                // =========================================

                Tablet tablet = tabletRepository.findById(id)
                                .orElse(null);

                if (tablet == null) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(
                                                        Map.of(
                                                                        "success", false,
                                                                        "message", "Tablet no encontrada",
                                                                        "id", id));
                }

                String deviceId = tablet.getId().toString();

                // =========================================
                // EJECUTAR DIAGNÓSTICO WEBSOCKET
                // =========================================

                Map<String, Object> diagnostico = MdmSocketHandler.diagnosticarConexion(
                                deviceId);

                // =========================================
                // ARMAR RESPUESTA
                // =========================================

                Map<String, Object> respuesta = new java.util.LinkedHashMap<>();

                respuesta.put(
                                "success",
                                Boolean.TRUE.equals(
                                                diagnostico.get("ackReceived")));

                respuesta.put(
                                "id",
                                tablet.getId());

                respuesta.put(
                                "activo",
                                tablet.getActivo());

                respuesta.put(
                                "deviceName",
                                tablet.getDeviceName());

                respuesta.put(
                                "webSocket",
                                diagnostico.get("webSocket"));

                respuesta.put(
                                "commandSent",
                                diagnostico.get("commandSent"));

                respuesta.put(
                                "ackReceived",
                                diagnostico.get("ackReceived"));

                respuesta.put(
                                "latencyMs",
                                diagnostico.get("latencyMs"));

                respuesta.put(
                                "appVersion",
                                tablet.getAppVersion());

                respuesta.put(
                                "androidVersion",
                                tablet.getOsVersion());

                respuesta.put(
                                "lastConnection",
                                tablet.getLastConnection());

                respuesta.put(
                                "message",
                                diagnostico.get("message"));

                return ResponseEntity.ok(respuesta);
        }

        @GetMapping("/{id}/historial-cargador")
        public List<Object[]> obtenerHistorialCargador(
                        @PathVariable Long id,
                        @RequestParam(required = false) String fechaDesde,
                        @RequestParam(required = false) String fechaHasta) {

                return tabletService.obtenerHistorialCargador(
                                id,
                                fechaDesde,
                                fechaHasta);

        }

        @GetMapping("/all")
        public Page<TabletDashboardProjection> obtenerTodas(

                        @RequestParam(defaultValue = "0") int page,

                        @RequestParam(defaultValue = "25") int size,

                        @RequestParam(defaultValue = "") String buscar,

                        @RequestParam(defaultValue = "") String planta,

                        @RequestParam(defaultValue = "") String categoria,

                        @RequestParam(defaultValue = "") String version,

                        @RequestParam(defaultValue = "") String estado,

                        @RequestParam(defaultValue = "") String estadoCargador,

                        @RequestParam(defaultValue = "") String estadoBateria) {

                Pageable pageable = PageRequest.of(page, size);

                System.out.println("PLANTA = [" + planta + "]");
                System.out.println("CATEGORIA = [" + categoria + "]");
                System.out.println("VERSION = [" + version + "]");
                System.out.println("BUSCAR = [" + buscar + "]");
                System.out.println("ESTADO = [" + estadoCargador + "]");

                return tabletService.obtenerDashboard(
                                buscar,
                                planta,
                                categoria,
                                version,
                                estadoCargador,
                                estado,
                                estadoBateria,
                                pageable);
        }

        @PostMapping("/refresh")
        public ResponseEntity<?> actualizarDispositivos() {

                List<Tablet> tablets = tabletRepository.findAll();

                int conectadas = 0;
                int desconectadas = 0;

                for (Tablet tablet : tablets) {

                        String deviceId = tablet.getId().toString();

                        boolean conectada = MdmSocketHandler.estaTabletConectada(deviceId);

                        if (conectada) {

                                tablet.setSinRespuesta(false);

                                // Pedimos actualización de la información
                                MdmSocketHandler.solicitarActualizacion(deviceId);

                                conectadas++;

                                System.out.println(
                                                "REFRESH | ACTIVO=" + tablet.getActivo()
                                                                + " | CONECTADA");

                        } else {

                                tablet.setSinRespuesta(true);

                                desconectadas++;

                                System.out.println(
                                                "REFRESH | ACTIVO=" + tablet.getActivo()
                                                                + " | DESCONECTADA");
                        }

                        tabletRepository.save(tablet);
                }

                return ResponseEntity.ok(
                                Map.of(
                                                "success", true,
                                                "total", tablets.size(),
                                                "conectadas", conectadas,
                                                "desconectadas", desconectadas));
        }

        @PostMapping("/sync-credentials")
        public ResponseEntity<?> sincronizarCredenciales() {

                RolAcceso admin = rolAccesoService.obtenerActivoPorRol("ADMIN");

                RolAcceso tecnico = rolAccesoService.obtenerActivoPorRol("TECNICO");

                if (admin == null || tecnico == null) {

                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message",
                                                        "No se encontraron credenciales activas de ADMIN y TECNICO"));
                }

                int enviados = 0;

                for (String deviceId : MdmSocketHandler.obtenerTabletsConectadas()) {

                        try {

                                String comando = objectMapper.writeValueAsString(
                                                Map.of(
                                                                "pending_command",
                                                                "sync_credentials",

                                                                "ADMIN",
                                                                admin.getPassword(),

                                                                "TECNICO",
                                                                tecnico.getPassword()));

                                MdmSocketHandler.enviarOrden(
                                                deviceId,
                                                comando);

                                enviados++;

                        } catch (Exception e) {

                                System.err.println(
                                                "ERROR ENVIANDO CREDENCIALES A TABLET "
                                                                + deviceId);

                                e.printStackTrace();
                        }
                }

                System.out.println(
                                "SINCRONIZACION CREDENCIALES | TABLETS ENVIADAS="
                                                + enviados);

                return ResponseEntity.ok(
                                Map.of(
                                                "success", true,
                                                "message",
                                                "Credenciales enviadas a dispositivos conectados",
                                                "enviados",
                                                enviados));
        }

        @PostMapping("/sync-credentials/{activo}")
        public ResponseEntity<?> sincronizarCredencialesIndividual(
                        @PathVariable String activo) {

                Tablet tablet = tabletRepository.findByActivo(activo)
                                .orElse(null);

                if (tablet == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "Tablet no encontrada",
                                                        "activo", activo));
                }

                RolAcceso admin = rolAccesoService.obtenerActivoPorRol("ADMIN");
                RolAcceso tecnico = rolAccesoService.obtenerActivoPorRol("TECNICO");

                if (admin == null || tecnico == null) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message",
                                                        "No se encontraron credenciales activas de ADMIN y TECNICO"));
                }

                String deviceId = tablet.getId().toString();

                if (!MdmSocketHandler.estaTabletConectada(deviceId)) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                                        Map.of(
                                                        "success", false,
                                                        "activo", activo,
                                                        "message",
                                                        "La tablet no está conectada"));
                }

                try {

                        String comando = objectMapper.writeValueAsString(
                                        Map.of(
                                                        "pending_command",
                                                        "sync_credentials",

                                                        "ADMIN",
                                                        admin.getPassword(),

                                                        "TECNICO",
                                                        tecnico.getPassword()));

                        MdmSocketHandler.enviarOrden(
                                        deviceId,
                                        comando);

                        System.out.println(
                                        "SINCRONIZACION CREDENCIALES INDIVIDUAL"
                                                        + " | ACTIVO=" + activo
                                                        + " | ID=" + deviceId);

                        return ResponseEntity.ok(
                                        Map.of(
                                                        "success", true,
                                                        "activo", activo,
                                                        "message",
                                                        "Credenciales enviadas a la tablet"));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                        Map.of(
                                                        "success", false,
                                                        "activo", activo,
                                                        "message",
                                                        "Error enviando las credenciales"));
                }
        }

        @GetMapping("/dashboard")
        public DashboardDTO obtenerDashboard() {

                return tabletService.obtenerDashboard();

        }

        @GetMapping("/reporte")
        public ResponseEntity<byte[]> generarReporte(
                        @RequestParam(defaultValue = "") String buscar,
                        @RequestParam(defaultValue = "") String planta,
                        @RequestParam(defaultValue = "") String categoria,
                        @RequestParam(defaultValue = "") String estadoCargador,
                        @RequestParam(defaultValue = "") String estado,
                        @RequestParam(defaultValue = "") String estadoBateria,
                        @RequestParam(defaultValue = "") String columnas) throws IOException {

                byte[] excel = tabletService.obtenerReporte(
                                buscar,
                                planta,
                                categoria,
                                estadoCargador,
                                estado,
                                estadoBateria,
                                columnas);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

                headers.set(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=Reporte_MDM.xlsx");

                return new ResponseEntity<>(excel, headers, HttpStatus.OK);
        }

        @GetMapping("/plantas")
        public List<String> obtenerPlantas() {

                return tabletService.obtenerPlantas();
        }

        @GetMapping("/categorias")
        public List<String> obtenerCategorias() {

                return tabletService.obtenerCategorias();
        }

        @GetMapping("/departamentos")
        public List<String> obtenerDepartamentos() {

                return tabletService.obtenerDepartamentos();
        }

        @GetMapping("/versions")
        public List<String> obtenerVersionesApp() {
                return tabletService.obtenerVersionesApp();
        }

        @GetMapping("/lista")
        public List<Tablet> obtenerTablets() {

                return tabletService.obtenerTablets();
        }

        @GetMapping("/lista-paginada")
        public Page<TabletDashboardProjection> obtenerTabletsPaginadas(

                        @RequestParam(defaultValue = "0") int page,

                        @RequestParam(defaultValue = "25") int size,

                        @RequestParam(defaultValue = "") String buscar,

                        @RequestParam(defaultValue = "") String planta,

                        @RequestParam(defaultValue = "") String departamento,

                        @RequestParam(defaultValue = "") String categoria) {

                Pageable pageable = PageRequest.of(page, size);

                return tabletService.obtenerTabletsSelector(
                                buscar,
                                planta,
                                departamento,
                                categoria,
                                pageable);
        }

        @PostMapping("/{id}/location")
        public void recibirUbicacion(
                        @PathVariable Long id,
                        @RequestBody LocationDTO location) {

                tabletRepository.findById(id).ifPresent(tablet -> {

                        tablet.setLatitude(location.getLatitude());
                        tablet.setLongitude(location.getLongitude());
                        tablet.setGpsAccuracy(location.getAccuracy());
                        tablet.setGpsSource(location.getSource());

                        if (location.getTimestamp() != null) {
                                tablet.setGpsTimestamp(
                                                java.time.Instant.ofEpochMilli(location.getTimestamp())
                                                                .atZone(java.time.ZoneId.systemDefault())
                                                                .toLocalDateTime());
                        }

                        tabletRepository.save(tablet);

                        gpsHistoryService.guardarPunto(
                                        id,
                                        location.getLatitude(),
                                        location.getLongitude(),
                                        location.getAccuracy());

                        System.out.println("GPS RECIBIDO TABLET " + id);
                        System.out.println("LAT = " + location.getLatitude());
                        System.out.println("LON = " + location.getLongitude());
                        System.out.println("ACC = " + location.getAccuracy());
                        System.out.println("SRC = " + location.getSource());

                });
        }

        @GetMapping("/{id}/location")
        public LocationDTO obtenerUbicacion(
                        @PathVariable Long id) {

                Tablet tablet = tabletRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));

                LocationDTO dto = new LocationDTO();

                dto.setLatitude(tablet.getLatitude());
                dto.setLongitude(tablet.getLongitude());
                dto.setAccuracy(tablet.getGpsAccuracy());
                dto.setSource(tablet.getGpsSource());

                if (tablet.getGpsTimestamp() != null) {
                        dto.setTimestamp(
                                        tablet.getGpsTimestamp()
                                                        .atZone(java.time.ZoneId.systemDefault())
                                                        .toInstant()
                                                        .toEpochMilli());
                }

                return dto;
        }

        @PostMapping("/{id}/apps")
        public void recibirApps(@PathVariable Long id, @RequestBody String body) {

                System.out.println("================================");
                System.out.println("RECIBI APPS");
                System.out.println("ID = " + id);
                System.out.println("TAMAÑO = " + body.length());
                System.out.println(body.substring(0, Math.min(300, body.length())));
                System.out.println("================================");

                tabletRepository.findById(id).ifPresent(t -> {
                        t.setAppsReportadas(body);
                        tabletRepository.save(t);
                });
        }

        @Autowired
        private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        @Autowired
        private WebTitleService webTitleService;

        @PutMapping("/{id}/config")
        public Tablet actualizarConfig(@PathVariable Long id, @RequestBody Tablet config) {

                try {

                        // COMANDO TEMPORAL (NO SE GUARDA EN BD)
                        if ("sync_apps".equals(config.getPendingCommand())) {

                                MdmSocketHandler.enviarOrden(
                                                id.toString(),
                                                "{\"pending_command\":\"sync_apps\"}");

                                System.out.println("SYNC_APPS ENVIADO POR WEBSOCKET A TABLET " + id);

                                return tabletRepository.findById(id)
                                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));
                        }

                        if ("reboot".equals(config.getPendingCommand())) {

                                boolean ack = MdmSocketHandler.enviarOrdenConAck(
                                                id.toString(),
                                                "{\"pending_command\":\"reboot\"}",
                                                "reboot");

                                if (ack) {
                                        System.out.println("REBOOT CONFIRMADO POR TABLET " + id);
                                } else {
                                        System.out.println("REBOOT NO CONFIRMADO POR TABLET " + id);
                                }

                                Tablet tablet = tabletRepository.findById(id)
                                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));

                                tablet.setPendingCommand(ack ? "ACK_REBOOT_OK" : "ACK_REBOOT_FAIL");

                                return tablet;
                        }

                        if ("lock_device".equals(config.getPendingCommand())) {

                                MdmSocketHandler.enviarOrden(
                                                id.toString(),
                                                "{\"pending_command\":\"lock_device\"}");

                                return tabletRepository.findById(id)
                                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));
                        }

                        if ("unlock_device".equals(config.getPendingCommand())) {

                                MdmSocketHandler.enviarOrden(
                                                id.toString(),
                                                "{\"pending_command\":\"unlock_device\"}");

                                return tabletRepository.findById(id)
                                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));
                        }

                        if ("alarm_on".equals(config.getPendingCommand())) {

                                MdmSocketHandler.enviarOrden(
                                                id.toString(),
                                                "{\"pending_command\":\"alarm_on\"}");

                                return tabletRepository.findById(id)
                                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));
                        }

                        if ("alarm_off".equals(config.getPendingCommand())) {

                                MdmSocketHandler.enviarOrden(
                                                id.toString(),
                                                "{\"pending_command\":\"alarm_off\"}");

                                return tabletRepository.findById(id)
                                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));
                        }

                        if ("location_request".equals(config.getPendingCommand())) {

                                MdmSocketHandler.enviarOrden(
                                                id.toString(),
                                                "{\"pending_command\":\"location_request\"}");

                                return tabletRepository.findById(id)
                                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));
                        }

                        if ("location_track_start".equals(config.getPendingCommand())) {

                                Tablet tabletActualizada = tabletService.actualizarConfiguracionRemota(id, config);

                                MdmSocketHandler.enviarOrden(
                                                id.toString(),
                                                "{\"pending_command\":\"location_track_start\"}");

                                return tabletActualizada;
                        }

                        if ("location_track_stop".equals(config.getPendingCommand())) {

                                Tablet tabletActualizada = tabletService.actualizarConfiguracionRemota(id, config);

                                MdmSocketHandler.enviarOrden(
                                                id.toString(),
                                                "{\"pending_command\":\"location_track_stop\"}");

                                return tabletActualizada;
                        }

                        if ("factory_reset".equals(config.getPendingCommand())) {

                                boolean ack = MdmSocketHandler.enviarOrdenConAck(
                                                id.toString(),
                                                "{\"command\":\"factory_reset\"}",
                                                "factory_reset");

                                if (ack) {
                                        System.out.println("FACTORY RESET CONFIRMADO POR TABLET " + id);
                                } else {
                                        System.out.println("FACTORY RESET NO CONFIRMADO POR TABLET " + id);
                                }

                                Tablet tablet = tabletRepository.findById(id)
                                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));

                                tablet.setPendingCommand(
                                                ack ? "ACK_FACTORY_RESET_OK"
                                                                : "ACK_FACTORY_RESET_FAIL");

                                return tablet;
                        }

                        System.out.println("============== CONTROLLER ==============");
                        System.out.println("APPS = " + config.getAppsBloqueadas());
                        System.out.println("REINICIO = " + config.getConfigReinicio());
                        System.out.println("RESTRICCIONES = " + config.getRestricciones());
                        System.out.println("URLS = " + config.getUrlsPermitidas());
                        System.out.println("MODO KIOSCO = " + config.getModoKiosco());
                        System.out.println("========================================");

                        // =============================================
                        // CONFIGURACIÓN NORMAL / ACCIONES MASIVAS APPS
                        // =============================================

                        String accionMasivaApps = config.getAccionMasivaApps();
                        String appsBloqueadasOriginal = config.getAppsBloqueadas();
                        var appsModificadas = config.getAppsModificadas();

                        boolean esAccionMasiva = "PERMITIR_TODAS".equals(accionMasivaApps) ||
                                        "BLOQUEAR_TODAS".equals(accionMasivaApps);

                        // Si es PERMITIR TODAS o BLOQUEAR TODAS,
                        // NO guardamos ese estado temporal como política permanente.
                        if (esAccionMasiva) {
                                config.setAppsBloqueadas(null);
                        }

                        // Guardar las demás configuraciones normalmente
                        Tablet tabletActualizada = tabletService.actualizarConfiguracionRemota(id, config);

                        tabletActualizada.setPendingCommand(null);

                        // Para el WebSocket sí mandamos la acción masiva
                        if (esAccionMasiva) {

                                tabletActualizada.setAppsBloqueadas(
                                                appsBloqueadasOriginal);

                                tabletActualizada.setAccionMasivaApps(
                                                accionMasivaApps);

                                // Una acción masiva NO es un cambio individual
                                tabletActualizada.setAppsModificadas(null);

                        } else {

                                tabletActualizada.setAccionMasivaApps(null);

                                // Cambios individuales realizados desde la WEB
                                tabletActualizada.setAppsModificadas(
                                                appsModificadas);
                        }

                        System.out.println(
                                        "APPS MODIFICADAS WEB = " +
                                                        appsModificadas);

                        String jsonResponse = objectMapper.writeValueAsString(tabletActualizada);

                        MdmSocketHandler.enviarOrden(
                                        id.toString(),
                                        jsonResponse);

                        System.out.println(
                                        "WEBSOCKET ENVIADO A TABLET " + id +
                                                        " | ACCION MASIVA = " + accionMasivaApps);

                        return tabletActualizada;

                } catch (GpsTrackingAlreadyActiveException e) {

                        throw e;

                } catch (Exception e) {

                        e.printStackTrace();
                        throw new RuntimeException(e);
                }
        }

        @GetMapping("/web-title")
        public WebUrlDTO obtenerTitulo(@RequestParam String url) {

                WebUrlDTO dto = new WebUrlDTO();

                dto.setUrl(url);
                dto.setNombre(webTitleService.obtenerTitulo(url));

                return dto;
        }

        // Añade esto a tu TabletController.java
        @GetMapping("/{id}/config")
        public PolicyDTO obtenerConfiguracionLigera(@PathVariable Long id) {

                Tablet t = tabletRepository.findById(id)
                                .orElseThrow();

                PolicyDTO dto = new PolicyDTO();

                dto.setAppsBloqueadas(t.getAppsBloqueadas());
                dto.setRestricciones(t.getRestricciones());
                dto.setConfigReinicio(t.getConfigReinicio());
                dto.setUrlsPermitidas(t.getUrlsPermitidas());

                return dto;
        }

        @GetMapping("/{id}")
        public Tablet obtenerPorId(@PathVariable Long id) {

                Tablet t = tabletRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));

                return t;
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> eliminarTablet(@PathVariable Long id) {

                System.out.println("ELIMINANDO TABLET: " + id);
                tabletService.eliminarTablet(id);

                return ResponseEntity.noContent().build();
        }

        @GetMapping("/activo/{activo}")
        public Tablet obtenerPorActivo(@PathVariable String activo) {

                return tabletRepository.findByActivo(activo)
                                .orElseThrow(() -> new RuntimeException("Tablet no encontrada"));
        }

        @GetMapping("/activo-info/{activo}")
        public ResponseEntity<?> obtenerInformacionActivo(
                        @PathVariable String activo) {

                ActivoInfo info = tabletService.obtenerActivoInfo(activo);

                if (info == null) {
                        return ResponseEntity.noContent().build();
                }

                return ResponseEntity.ok(info);
        }

        @PutMapping("/{id}/activo")
        public ResponseEntity<?> actualizarActivo(
                        @PathVariable Long id,
                        @RequestBody Map<String, String> body) {

                String nuevoActivo = body.get("activo");

                if (nuevoActivo == null || nuevoActivo.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "error", "ACTIVO_VACIO",
                                                        "message", "El código de activo es obligatorio"));
                }

                nuevoActivo = nuevoActivo.trim();

                Tablet tablet = tabletRepository.findById(id)
                                .orElse(null);

                if (tablet == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                        Map.of(
                                                        "error", "TABLET_NO_ENCONTRADA",
                                                        "message", "La tablet no existe"));
                }

                String activoAnterior = tablet.getActivo();

                // Si realmente no cambió, no hacemos nada.
                if (nuevoActivo.equals(activoAnterior)) {
                        return ResponseEntity.ok(
                                        Map.of(
                                                        "message", "El activo no cambió",
                                                        "id", tablet.getId(),
                                                        "activo", tablet.getActivo()));
                }

                // Verificar que el nuevo activo no pertenezca a otra tablet.
                var existente = tabletRepository.findByActivo(nuevoActivo);

                if (existente.isPresent() &&
                                !existente.get().getId().equals(id)) {

                        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                                        Map.of(
                                                        "error", "ACTIVO_DUPLICADO",
                                                        "message", "El activo " + nuevoActivo +
                                                                        " ya está asignado a otro dispositivo"));
                }

                boolean confirmado = false;

                try {

                        String comando = objectMapper.writeValueAsString(
                                        Map.of(
                                                        "pending_command", "change_asset",
                                                        "activo", nuevoActivo));

                        confirmado = MdmSocketHandler.enviarOrdenConAck(
                                        id.toString(),
                                        comando,
                                        "change_asset");

                } catch (Exception e) {

                        e.printStackTrace();
                }

                if (!confirmado) {

                        System.out.println(
                                        "CAMBIO DE ACTIVO SIN CONFIRMACION | ID=" + id);

                        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                                        Map.of(
                                                        "success", false,
                                                        "confirmed", false,
                                                        "message",
                                                        "No se recibió confirmación de la tablet. El dispositivo puede estar desconectado."));
                }

                // La tablet confirmó el cambio.
                // Ahora sí actualizamos la base de datos.
                tablet.setActivo(nuevoActivo);
                tabletRepository.save(tablet);

                System.out.println(
                                "ACTIVO MODIFICADO Y CONFIRMADO | ID=" + id +
                                                " | ANTERIOR=" + activoAnterior +
                                                " | NUEVO=" + nuevoActivo);

                return ResponseEntity.ok(
                                Map.of(
                                                "success", true,
                                                "confirmed", true,
                                                "message",
                                                "Activo actualizado y confirmado por la tablet",
                                                "id", tablet.getId(),
                                                "activo", tablet.getActivo()));
        }

        @PutMapping("/{id}/nombre")
        public ResponseEntity<?> actualizarNombre(
                        @PathVariable Long id,
                        @RequestBody Map<String, String> body) {

                String nuevoNombre = body.get("device_name");

                if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "error", "NOMBRE_VACIO",
                                                        "message", "El nombre del dispositivo es obligatorio"));
                }

                nuevoNombre = nuevoNombre.trim();

                Tablet tablet = tabletRepository.findById(id)
                                .orElse(null);

                if (tablet == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                        Map.of(
                                                        "error", "TABLET_NO_ENCONTRADA",
                                                        "message", "La tablet no existe"));
                }

                String nombreAnterior = tablet.getDeviceName();

                if (nuevoNombre.equals(nombreAnterior)) {
                        return ResponseEntity.ok(
                                        Map.of(
                                                        "success", true,
                                                        "confirmed", true,
                                                        "message", "El nombre no cambió",
                                                        "id", tablet.getId(),
                                                        "device_name", tablet.getDeviceName()));
                }

                boolean confirmado = false;

                try {

                        String comando = objectMapper.writeValueAsString(
                                        Map.of(
                                                        "pending_command", "change_device_name",
                                                        "device_name", nuevoNombre));

                        confirmado = MdmSocketHandler.enviarOrdenConAck(
                                        id.toString(),
                                        comando,
                                        "change_device_name");

                } catch (Exception e) {

                        System.err.println(
                                        "ERROR ENVIANDO CAMBIO DE NOMBRE A TABLET " + id);

                        e.printStackTrace();
                }

                if (!confirmado) {

                        System.out.println(
                                        "CAMBIO DE NOMBRE SIN CONFIRMACION | ID=" + id);

                        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                                        Map.of(
                                                        "success", false,
                                                        "confirmed", false,
                                                        "message",
                                                        "No se recibió confirmación de la tablet. El dispositivo puede estar desconectado."));
                }

                // La tablet confirmó el cambio.
                // Ahora sí actualizamos la base de datos.
                tablet.setDeviceName(nuevoNombre);
                tabletRepository.save(tablet);

                System.out.println(
                                "NOMBRE MODIFICADO Y CONFIRMADO | ID=" + id +
                                                " | ANTERIOR=" + nombreAnterior +
                                                " | NUEVO=" + nuevoNombre);

                return ResponseEntity.ok(
                                Map.of(
                                                "success", true,
                                                "confirmed", true,
                                                "message",
                                                "Nombre actualizado y confirmado por la tablet",
                                                "id", tablet.getId(),
                                                "device_name", tablet.getDeviceName()));
        }

        @ExceptionHandler(GpsTrackingAlreadyActiveException.class)
        public ResponseEntity<?> gpsTrackingActivo(
                        GpsTrackingAlreadyActiveException e) {

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(Map.of(
                                                "error", "GPS_TRACKING_ALREADY_ACTIVE",
                                                "message", e.getMessage()));
        }

        @PostMapping("/kiosk/{activo}")
        public ResponseEntity<?> cambiarModoKiosco(
                        @PathVariable String activo,
                        @RequestBody Map<String, Boolean> body) {

                Boolean enabled = body.get("enabled");

                if (enabled == null) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El campo enabled es obligatorio"));
                }

                Tablet tablet = tabletRepository.findByActivo(activo)
                                .orElse(null);

                if (tablet == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "Tablet no encontrada",
                                                        "activo", activo));
                }

                String comando = enabled ? "kiosk_on" : "kiosk_off";

                boolean confirmado = false;

                try {

                        String json = objectMapper.writeValueAsString(
                                        Map.of(
                                                        "pending_command", comando));

                        confirmado = MdmSocketHandler.enviarOrdenConAck(
                                        tablet.getId().toString(),
                                        json,
                                        comando);

                } catch (Exception e) {
                        e.printStackTrace();
                }

                if (!confirmado) {

                        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                                        Map.of(
                                                        "success", false,
                                                        "confirmed", false,
                                                        "enabled", enabled,
                                                        "activo", activo,
                                                        "message",
                                                        "La tablet no confirmó el cambio de modo kiosco"));
                }

                return ResponseEntity.ok(
                                Map.of(
                                                "success", true,
                                                "confirmed", true,
                                                "enabled", enabled,
                                                "activo", activo,
                                                "message",
                                                enabled
                                                                ? "Modo kiosco activado"
                                                                : "Modo kiosco desactivado"));
        }

        @PostMapping("/kiosk/all")
        public ResponseEntity<?> cambiarModoKioscoMasivo(
                        @RequestBody Map<String, Boolean> body) {

                Boolean enabled = body.get("enabled");

                if (enabled == null) {
                        return ResponseEntity.badRequest().body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "El campo enabled es obligatorio"));
                }

                String comando = enabled
                                ? "kiosk_on"
                                : "kiosk_off";

                List<Tablet> tablets = tabletRepository.findAll();

                int total = tablets.size();

                java.util.concurrent.atomic.AtomicInteger enviadas = new java.util.concurrent.atomic.AtomicInteger(0);

                java.util.concurrent.atomic.AtomicInteger confirmadas = new java.util.concurrent.atomic.AtomicInteger(
                                0);

                java.util.concurrent.atomic.AtomicInteger noConfirmadas = new java.util.concurrent.atomic.AtomicInteger(
                                0);

                java.util.concurrent.atomic.AtomicInteger sinConexion = new java.util.concurrent.atomic.AtomicInteger(
                                0);

                java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(20);

                List<java.util.concurrent.Future<?>> resultados = new java.util.ArrayList<>();

                for (Tablet tablet : tablets) {

                        String deviceId = tablet.getId().toString();

                        if (!MdmSocketHandler.estaTabletConectada(deviceId)) {
                                sinConexion.incrementAndGet();
                                continue;
                        }

                        java.util.concurrent.Future<?> future = executor.submit(() -> {

                                try {

                                        enviadas.incrementAndGet();

                                        String json = objectMapper.writeValueAsString(
                                                        Map.of(
                                                                        "pending_command",
                                                                        comando));

                                        boolean confirmado = MdmSocketHandler.enviarOrdenConAck(
                                                        deviceId,
                                                        json,
                                                        comando);

                                        if (confirmado) {
                                                confirmadas.incrementAndGet();
                                        } else {
                                                noConfirmadas.incrementAndGet();
                                        }

                                } catch (Exception e) {

                                        noConfirmadas.incrementAndGet();

                                        System.err.println(
                                                        "ERROR KIOSCO MASIVO | Tablet "
                                                                        + deviceId
                                                                        + " | "
                                                                        + e.getMessage());
                                }
                        });

                        resultados.add(future);
                }

                for (java.util.concurrent.Future<?> resultado : resultados) {

                        try {
                                resultado.get();
                        } catch (Exception e) {
                                System.err.println(
                                                "ERROR esperando resultado de kiosco masivo: "
                                                                + e.getMessage());
                        }
                }

                executor.shutdown();

                System.out.println(
                                "KIOSCO MASIVO FINALIZADO"
                                                + " | enabled=" + enabled
                                                + " | total=" + total
                                                + " | enviadas=" + enviadas.get()
                                                + " | confirmadas=" + confirmadas.get()
                                                + " | noConfirmadas=" + noConfirmadas.get()
                                                + " | sinConexion=" + sinConexion.get());

                return ResponseEntity.ok(
                                Map.of(
                                                "success", true,
                                                "enabled", enabled,
                                                "command", comando,
                                                "total", total,
                                                "enviadas", enviadas.get(),
                                                "confirmadas", confirmadas.get(),
                                                "noConfirmadas", noConfirmadas.get(),
                                                "sinConexion", sinConexion.get(),
                                                "message",
                                                enabled
                                                                ? "Activación masiva de modo kiosco finalizada"
                                                                : "Desactivación masiva de modo kiosco finalizada"));
        }

        @PutMapping("/bateria/{id}")
        public ResponseEntity<?> actualizarEstadoBateria(
                        @PathVariable Long id,
                        @RequestBody Map<String, Object> datos) {

                try {

                        String estado = (String) datos.get("estado_bateria");

                        Integer porcentaje = null;

                        if (datos.get("porcentaje_inflado") != null) {
                                porcentaje = Integer.valueOf(
                                                datos.get("porcentaje_inflado").toString());
                        }

                        // ==============================
                        // USUARIO
                        // ==============================

                        String usuarioLogin = (String) datos.get("usuario");

                        if (usuarioLogin == null || usuarioLogin.isBlank()) {
                                return ResponseEntity.badRequest()
                                                .body("No se recibió el usuario.");
                        }

                        Usuario usuario = usuarioService.obtenerUsuarioActivo(usuarioLogin);

                        // ==============================
                        // GUARDAR VALORES ANTERIORES
                        // ==============================

                        Tablet tabletAnterior = tabletService.obtenerPorId(id);

                        String estadoAnterior = tabletAnterior.getEstadoBateria() != null
                                        ? tabletAnterior.getEstadoBateria()
                                        : "NORMAL";

                        Integer porcentajeAnterior = tabletAnterior.getPorcentajeInflado();

                        // IMPORTANTE:
                        // construir el texto ANTES de actualizar

                        String valorAnterior = estadoAnterior;

                        if ("INFLADA".equals(estadoAnterior)
                                        && porcentajeAnterior != null) {

                                valorAnterior = estadoAnterior + " " +
                                                porcentajeAnterior + "%";
                        }

                        // ==============================
                        // ACTUALIZAR BATERÍA
                        // ==============================

                        Tablet tablet = tabletService.actualizarEstadoBateria(
                                        id,
                                        estado,
                                        porcentaje);

                        // ==============================
                        // NUEVO VALOR
                        // ==============================

                        String valorNuevo = tablet.getEstadoBateria();

                        if ("INFLADA".equals(tablet.getEstadoBateria())
                                        && tablet.getPorcentajeInflado() != null) {

                                valorNuevo = tablet.getEstadoBateria() + " " +
                                                tablet.getPorcentajeInflado() + "%";
                        }

                        // ==============================
                        // AUDITORÍA
                        // ==============================

                        String detalle = valorAnterior + " → " + valorNuevo;

                        auditoriaDispositivoService.registrar(
                                        tablet.getActivo(),
                                        "BATERIA",
                                        usuario.getNombre(),
                                        detalle);

                        return ResponseEntity.ok(tablet);

                } catch (IllegalArgumentException e) {

                        return ResponseEntity.badRequest()
                                        .body(e.getMessage());

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body("Error actualizando estado de batería");
                }
        }

        @GetMapping("/auditoria/{accion}/{activo}")
        public ResponseEntity<?> obtenerAuditoria(
                        @PathVariable String activo,
                        @PathVariable String accion) {

                AuditoriaDispositivo auditoria = auditoriaDispositivoService.obtener(
                                activo,
                                accion.toUpperCase());

                if (auditoria == null) {
                        return ResponseEntity.noContent().build();
                }

                return ResponseEntity.ok(auditoria);
        }

}