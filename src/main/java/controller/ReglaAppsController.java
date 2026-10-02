package controller;

import Entidad.Tablet;
import repository.TabletRepository;
import service.ReglaAppsService;
import com.example.monitoreo.MdmSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;

@RestController
@RequestMapping("/reglas-apps")
public class ReglaAppsController {

        private final ReglaAppsService reglaAppsService;
        private final TabletRepository tabletRepository;

        public ReglaAppsController(
                        ReglaAppsService reglaAppsService,
                        TabletRepository tabletRepository) {
                this.reglaAppsService = reglaAppsService;
                this.tabletRepository = tabletRepository;
        }

        @GetMapping("/efectiva/{activo}")
        public ResponseEntity<?> obtenerReglaEfectiva(
                        @PathVariable String activo) {

                Tablet tablet = tabletRepository.findByActivo(activo)
                                .orElse(null);

                if (tablet == null) {
                        return ResponseEntity.notFound().build();
                }

                Set<String> packages = reglaAppsService.obtenerReglaEfectiva(tablet);

                Map<String, Object> respuesta = new LinkedHashMap<>();

                respuesta.put("activo", tablet.getActivo());
                respuesta.put("categoria", tablet.getCategoria());
                respuesta.put("packages", packages);

                return ResponseEntity.ok(respuesta);
        }

        @PostMapping("/{reglaId}/aplicar")
        public ResponseEntity<?> aplicarRegla(
                        @PathVariable Long reglaId) {

                try {

                        List<Tablet> dispositivos = reglaAppsService.obtenerDispositivosAfectados(reglaId);

                        ObjectMapper mapper = new ObjectMapper();

                        int enviados = 0;
                        int desconectados = 0;

                        for (Tablet tablet : dispositivos) {

                                Set<String> paquetes = reglaAppsService.obtenerReglaEfectiva(tablet);

                                Map<String, Object> comando = new java.util.LinkedHashMap<>();

                                comando.put("command", "apps_rule");
                                comando.put("packages", paquetes);

                                String json = mapper.writeValueAsString(comando);

                                String socketId = String.valueOf(tablet.getId());

                                System.out.println(
                                                "DEBUG SOCKET | ACTIVO=" + tablet.getActivo() +
                                                                " | ID_BD=" + tablet.getId() +
                                                                " | BUSCANDO_SOCKET=" + socketId +
                                                                " | CONECTADAS="
                                                                + MdmSocketHandler.obtenerTabletsConectadas());

                                if (MdmSocketHandler
                                                .obtenerTabletsConectadas()
                                                .contains(socketId)) {

                                        MdmSocketHandler.enviarOrden(
                                                        socketId,
                                                        json);

                                        enviados++;

                                        System.out.println(
                                                        "REGLA APPS ENVIADA | ACTIVO=" +
                                                                        tablet.getActivo() +
                                                                        " | SOCKET_ID=" +
                                                                        socketId +
                                                                        " | PAQUETES=" +
                                                                        paquetes.size());

                                } else {

                                        desconectados++;

                                        System.out.println(
                                                        "REGLA APPS PENDIENTE | ACTIVO=" +
                                                                        tablet.getActivo() +
                                                                        " | TABLET DESCONECTADA");
                                }
                        }

                        return ResponseEntity.ok(
                                        Map.of(
                                                        "ok", true,
                                                        "reglaId", reglaId,
                                                        "dispositivos", dispositivos.size(),
                                                        "enviados", enviados,
                                                        "desconectados", desconectados));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body(
                                                        Map.of(
                                                                        "ok", false,
                                                                        "error", e.getMessage() != null
                                                                                        ? e.getMessage()
                                                                                        : "Error aplicando regla"));
                }
        }

        @PostMapping("/aplicar/{activo}")
        public ResponseEntity<?> aplicarReglaActivo(
                        @PathVariable String activo) {

                try {

                        Tablet tablet = tabletRepository.findByActivo(activo)
                                        .orElse(null);

                        if (tablet == null) {
                                return ResponseEntity.notFound().build();
                        }

                        Set<String> paquetes = reglaAppsService.obtenerReglaEfectiva(tablet);

                        Map<String, Object> comando = new LinkedHashMap<>();

                        comando.put("command", "apps_rule");
                        comando.put("packages", paquetes);

                        ObjectMapper mapper = new ObjectMapper();
                        String json = mapper.writeValueAsString(comando);

                        String socketId = String.valueOf(tablet.getId());

                        System.out.println("========== DEBUG REGLA APPS ==========");
                        System.out.println("ACTIVO SOLICITADO: " + activo);
                        System.out.println("ID BD TABLET: " + tablet.getId());
                        System.out.println("SOCKET ID BUSCADO: [" + socketId + "]");
                        System.out.println("SOCKETS CONECTADOS: " + MdmSocketHandler.obtenerTabletsConectadas());
                        System.out.println("ESTA CONECTADA: " + MdmSocketHandler.estaTabletConectada(socketId));
                        System.out.println("======================================");

                        boolean conectado = MdmSocketHandler.estaTabletConectada(socketId);

                        if (!conectado) {

                                return ResponseEntity.ok(
                                                Map.of(
                                                                "ok", false,
                                                                "activo", activo,
                                                                "conectado", false,
                                                                "mensaje", "Tablet desconectada"));
                        }

                        MdmSocketHandler.enviarOrden(
                                        socketId,
                                        json);

                        System.out.println(
                                        "REGLA APPS INDIVIDUAL ENVIADA | ACTIVO=" +
                                                        tablet.getActivo() +
                                                        " | SOCKET_ID=" +
                                                        socketId +
                                                        " | PAQUETES=" +
                                                        paquetes.size());

                        return ResponseEntity.ok(
                                        Map.of(
                                                        "ok", true,
                                                        "activo", activo,
                                                        "conectado", true,
                                                        "packages", paquetes.size()));

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError()
                                        .body(
                                                        Map.of(
                                                                        "ok", false,
                                                                        "error",
                                                                        e.getMessage() != null
                                                                                        ? e.getMessage()
                                                                                        : "Error aplicando regla"));
                }
        }
}