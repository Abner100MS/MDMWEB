package com.example.monitoreo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import Entidad.Tablet;
import repository.TabletRepository;
import service.TareaProgramadaService;
import java.util.Map;
import java.util.concurrent.*;
import java.util.Set;

public class MdmSocketHandler extends TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Boolean>> ackPendientes = new ConcurrentHashMap<>();

    private static final Map<String, CompletableFuture<Boolean>> updatePendientes = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Boolean> actualizacionesEnviadas = new ConcurrentHashMap<>();

    private static final Map<String, String> updateMensajes = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TabletRepository tabletRepository;

    private final TareaProgramadaService tareaProgramadaService;

    public MdmSocketHandler(
            TabletRepository tabletRepository,
            TareaProgramadaService tareaProgramadaService) {
        this.tabletRepository = tabletRepository;
        this.tareaProgramadaService = tareaProgramadaService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        String uri = session.getUri().toString();
        String id = uri.substring(uri.lastIndexOf('/') + 1);

        sessions.put(id, session);

        System.out.println("Tablet conectada al túnel: " + id);

        try {

            Long tabletId = Long.valueOf(id);

            CompletableFuture.runAsync(() -> {

                try {

                    tareaProgramadaService
                            .procesarActualizacionPendiente(tabletId);

                } catch (Exception e) {

                    System.out.println(
                            "Error procesando actualización pendiente | Tablet "
                                    + tabletId
                                    + " | "
                                    + e.getMessage());
                }
            });

        } catch (Exception e) {

            System.out.println(
                    "No se pudo verificar actualización pendiente | Tablet "
                            + id);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        sessions.values().remove(session);

        System.out.println("Tablet desconectada del túnel");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        try {

            String payload = message.getPayload();
            System.out.println("MENSAJE DESDE TABLET: " + payload);

            JsonNode json = objectMapper.readTree(payload);

            if ("ack".equals(json.path("type").asText())) {

                String command = json.path("command").asText();
                String status = json.path("status").asText();
                String mensaje = json.path("message").asText("");

                String deviceId = json.path("deviceId").asText("");

                if (deviceId.isBlank()) {
                    deviceId = obtenerDeviceIdPorSession(session);
                }

                System.out.println(
                        "ACK RECIBIDO TABLET " + deviceId
                                + " COMANDO " + command
                                + " ESTADO " + status
                                + " MENSAJE " + mensaje);

                String key = deviceId + ":" + command;

                // ACTUALIZACIÓN DEL MDM
                if ("update_app".equals(command)) {

                    if ("downloading".equals(status)
                            || "received".equals(status)) {

                        System.out.println(
                                "Tablet " + deviceId
                                        + " está procesando la actualización");

                        // NO completar todavía
                        return;
                    }

                    CompletableFuture<Boolean> future = updatePendientes.remove(key);

                    if ("success".equals(status)) {

                        updateMensajes.put(key, mensaje);

                        if (future != null) {
                            future.complete(true);
                        }

                        // Confirmar también la tarea en la base de datos
                        try {

                            Long tabletId = Long.valueOf(deviceId);

                            tareaProgramadaService.confirmarActualizacion(
                                    tabletId);

                        } catch (Exception e) {

                            System.out.println(
                                    "ERROR confirmando tarea de actualización | Tablet "
                                            + deviceId
                                            + " | "
                                            + e.getMessage());
                        }

                    } else if ("error".equals(status)
                            || status.startsWith("error_")) {

                        updateMensajes.put(key, mensaje);

                        if (future != null) {
                            future.complete(false);
                        }
                    }

                    return;
                }

                // RESTO DE COMANDOS
                CompletableFuture<Boolean> future = ackPendientes.remove(key);

                if (future != null) {

                    if ("success".equalsIgnoreCase(status)) {

                        future.complete(true);

                    } else {

                        System.out.println(
                                "COMANDO NO CONFIRMADO CORRECTAMENTE | TABLET="
                                        + deviceId
                                        + " | COMANDO="
                                        + command
                                        + " | STATUS="
                                        + status);

                        future.complete(false);
                    }
                }
            }

            if ("gps_update".equals(json.path("type").asText())) {

                String deviceId = obtenerDeviceIdPorSession(session);

                String gpsJson = objectMapper.createObjectNode()
                        .put("type", "gps_update")
                        .put("tabletId", deviceId)
                        .put("latitude", json.path("latitude").asDouble())
                        .put("longitude", json.path("longitude").asDouble())
                        .put("accuracy", json.path("accuracy").asDouble())
                        .toString();

                broadcastToWeb(gpsJson);

                System.out.println("GPS TIEMPO REAL TABLET " + deviceId);
            }

            if ("policy_sync".equals(json.path("type").asText())) {

                String deviceId = obtenerDeviceIdPorSession(session);

                System.out.println("POLICY_SYNC RECIBIDO DE TABLET " + deviceId);
                System.out.println(payload);

                Tablet tablet = tabletRepository.findById(
                        Long.valueOf(deviceId)).orElse(null);

                if (tablet != null) {

                    tablet.setRestricciones(
                            json.path("restricciones").toString());

                    tablet.setAppsBloqueadas(
                            json.path("apps_bloqueadas").asText(""));

                    tablet.setConfigReinicio(
                            json.path("config_reinicio").asText(""));

                    tablet.setUrlsPermitidas(
                            json.path("urls_permitidas").toString());

                    tabletRepository.save(tablet);

                    System.out.println("POLITICAS ACTUALIZADAS DESDE TABLET " + deviceId);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean enviarOrdenConAck(
            String deviceId,
            String json,
            String command) {

        String key = deviceId + ":" + command;

        try {

            WebSocketSession session = sessions.get(deviceId);

            if (session == null || !session.isOpen()) {

                System.out.println(
                        "Tablet " + deviceId +
                                " no está conectada al túnel");

                return false;
            }

            CompletableFuture<Boolean> future = new CompletableFuture<>();

            ackPendientes.put(key, future);

            session.sendMessage(
                    new TextMessage(json));

            System.out.println(
                    "Orden enviada a tablet " +
                            deviceId +
                            " | comando=" + command);

            return future.get(
                    10,
                    TimeUnit.SECONDS);

        } catch (TimeoutException e) {

            System.out.println(
                    "Tablet " + deviceId +
                            " no confirmó " +
                            command +
                            " dentro de 10 segundos");

            return false;

        } catch (Exception e) {

            System.err.println(
                    "Error enviando " +
                            command +
                            " a tablet " +
                            deviceId);

            e.printStackTrace();

            return false;

        } finally {

            ackPendientes.remove(key);
        }
    }

    public static boolean enviarActualizacionConAck(
            String deviceId,
            String apkUrl) {

        String command = "update_app";
        String key = deviceId + ":" + command;

        try {

            WebSocketSession session = sessions.get(deviceId);

            if (session == null || !session.isOpen()) {

                System.out.println(
                        "Tablet " + deviceId +
                                " no está conectada para actualizar");

                return false;
            }

            CompletableFuture<Boolean> future = new CompletableFuture<>();

            updatePendientes.put(key, future);

            String json = objectMapper
                    .createObjectNode()
                    .put("command", "update_app")
                    .put("url", apkUrl)
                    .toString();

            session.sendMessage(
                    new TextMessage(json));

            actualizacionesEnviadas.put(deviceId, true);

            System.out.println(
                    "ACTUALIZACIÓN ENVIADA A TABLET "
                            + deviceId
                            + " URL: "
                            + apkUrl);

            return future.get(
                    5,
                    TimeUnit.MINUTES);

        } catch (TimeoutException e) {

            System.out.println(
                    "Tablet " + deviceId +
                            " sigue pendiente de confirmación después de 5 minutos");

            return false;

        } catch (Exception e) {

            updatePendientes.remove(key);

            System.err.println(
                    "Error enviando actualización a tablet "
                            + deviceId);

            e.printStackTrace();

            return false;
        }
    }

    public static void enviarOrden(String deviceId, String json) {

        try {

            WebSocketSession session = sessions.get(deviceId);

            if (session != null && session.isOpen()) {

                session.sendMessage(new TextMessage(json));

                System.out.println("Orden enviada a tablet " + deviceId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String obtenerDeviceIdPorSession(WebSocketSession session) {

        try {

            if (session != null && session.getUri() != null) {

                String uri = session.getUri().toString();

                return uri.substring(
                        uri.lastIndexOf('/') + 1);
            }

        } catch (Exception e) {

            System.out.println(
                    "No se pudo obtener ID desde sesión WebSocket");
        }

        return "desconocido";
    }

    private static final Map<String, WebSocketSession> webSessions = new ConcurrentHashMap<>();

    public static void registrarWeb(String id, WebSocketSession session) {

        webSessions.put(id, session);

        System.out.println("WEB GPS CONECTADA " + id);
    }

    public static void eliminarWeb(String id) {

        webSessions.remove(id);
    }

    public static void broadcastToWeb(String json) {

        webSessions.values().forEach(session -> {

            try {

                if (session.isOpen()) {

                    session.sendMessage(
                            new TextMessage(json));
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        });
    }

    public static void solicitarActualizacion(String deviceId) {

        String json = """
                {
                    "pending_command":"sync_status"
                }
                """;

        enviarOrden(deviceId, json);
    }

    public static boolean verificarConexion(String deviceId) {

        String json = """
                {
                    "command":"ping_mdm"
                }
                """;

        return enviarOrdenConAck(
                deviceId,
                json,
                "ping_mdm");
    }

    public static Set<String> obtenerTabletsConectadas() {
        return sessions.keySet();
    }

    public static boolean estaTabletConectada(String deviceId) {

        WebSocketSession session = sessions.get(deviceId);

        return session != null && session.isOpen();
    }

    public static boolean fueActualizacionEnviada(String deviceId) {
        return actualizacionesEnviadas.remove(deviceId) != null;
    }

    public static Map<String, Object> diagnosticarConexion(
            String deviceId) {

        Map<String, Object> resultado = new ConcurrentHashMap<>();

        // ==========================================
        // 1. VERIFICAR WEBSOCKET
        // ==========================================

        WebSocketSession session = sessions.get(deviceId);

        boolean websocketConectado = session != null &&
                session.isOpen();

        resultado.put(
                "webSocket",
                websocketConectado);

        if (!websocketConectado) {

            resultado.put(
                    "commandSent",
                    false);

            resultado.put(
                    "ackReceived",
                    false);

            resultado.put(
                    "latencyMs",
                    -1);

            resultado.put(
                    "message",
                    "WebSocket no conectado");

            return resultado;
        }

        // ==========================================
        // 2. CREAR PING
        // ==========================================

        String json = """
                {
                    "command":"ping_mdm"
                }
                """;

        // ==========================================
        // 3. MEDIR TIEMPO
        // ==========================================

        long inicio = System.currentTimeMillis();

        resultado.put(
                "commandSent",
                true);

        // ==========================================
        // 4. ENVIAR PING Y ESPERAR ACK
        // ==========================================

        boolean ack = enviarOrdenConAck(
                deviceId,
                json,
                "ping_mdm");

        long fin = System.currentTimeMillis();

        long latencia = fin - inicio;

        // ==========================================
        // 5. RESULTADO
        // ==========================================

        resultado.put(
                "ackReceived",
                ack);

        resultado.put(
                "latencyMs",
                ack ? latencia : -1);

        if (ack) {

            resultado.put(
                    "message",
                    "Comunicación remota operativa");

        } else {

            resultado.put(
                    "message",
                    "WebSocket conectado pero la tablet no respondió al diagnóstico");
        }

        return resultado;
    }

}