package controller;

import Entidad.MdmWallpaper;
import Entidad.MdmWallpaperDispositivo;
import Entidad.Tablet;
import repository.MdmWallpaperRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import repository.TabletRepository;
import repository.MdmWallpaperDispositivoRepository;
import com.example.monitoreo.MdmSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/wallpapers")
public class MdmWallpaperController {

        @Autowired
        private MdmWallpaperRepository wallpaperRepository;

        @Autowired
        private TabletRepository tabletRepository;

        @Autowired
        private MdmWallpaperDispositivoRepository wallpaperDispositivoRepository;

        @Autowired
        private ObjectMapper objectMapper;

        // =====================================================
        // SUBIR IMAGEN Y GUARDARLA EN POSTGRESQL
        // =====================================================

        @PostMapping("/upload")
        public ResponseEntity<?> subirWallpaper(
                        @RequestParam("file") MultipartFile file,
                        @RequestParam("nombre") String nombre,
                        @RequestParam(value = "creadoPor", required = false) String creadoPor) {

                try {

                        // =========================================
                        // VALIDAR ARCHIVO
                        // =========================================

                        if (file == null || file.isEmpty()) {

                                return ResponseEntity.badRequest().body(
                                                Map.of(
                                                                "success", false,
                                                                "message", "Debe seleccionar una imagen"));
                        }

                        // =========================================
                        // VALIDAR NOMBRE
                        // =========================================

                        if (nombre == null || nombre.trim().isEmpty()) {

                                return ResponseEntity.badRequest().body(
                                                Map.of(
                                                                "success", false,
                                                                "message", "El nombre del fondo es obligatorio"));
                        }

                        // =========================================
                        // VALIDAR TIPO DE ARCHIVO
                        // =========================================

                        String tipoMime = file.getContentType();

                        if (tipoMime == null ||
                                        (!tipoMime.equalsIgnoreCase("image/jpeg")
                                                        && !tipoMime.equalsIgnoreCase("image/png")
                                                        && !tipoMime.equalsIgnoreCase("image/webp"))) {

                                return ResponseEntity.badRequest().body(
                                                Map.of(
                                                                "success", false,
                                                                "message",
                                                                "Solo se permiten imágenes JPG, PNG o WEBP"));
                        }

                        // =========================================
                        // VALIDAR TAMAÑO
                        // Máximo 10 MB
                        // =========================================

                        long maximo = 10L * 1024L * 1024L;

                        if (file.getSize() > maximo) {

                                return ResponseEntity.badRequest().body(
                                                Map.of(
                                                                "success", false,
                                                                "message",
                                                                "La imagen no puede superar los 10 MB"));
                        }

                        // =========================================
                        // CREAR REGISTRO
                        // =========================================

                        MdmWallpaper wallpaper = new MdmWallpaper();

                        wallpaper.setNombre(nombre.trim());

                        wallpaper.setNombreArchivo(
                                        file.getOriginalFilename() != null
                                                        ? file.getOriginalFilename()
                                                        : "wallpaper");

                        wallpaper.setTipoMime(tipoMime);

                        wallpaper.setTamanoBytes(file.getSize());

                        // AQUÍ se guarda realmente la imagen en BYTEA
                        wallpaper.setImagen(file.getBytes());

                        wallpaper.setActivo(true);

                        if (creadoPor != null && !creadoPor.trim().isEmpty()) {
                                wallpaper.setCreadoPor(creadoPor.trim());
                        }

                        /*
                         * Primero guardamos para obtener el ID.
                         * La URL se asignará después.
                         */
                        wallpaper.setUrl("PENDIENTE");

                        wallpaper = wallpaperRepository.save(wallpaper);

                        // =========================================
                        // URL INTERNA DEL WALLPAPER
                        // =========================================

                        String url = "/wallpapers/imagen/" + wallpaper.getId();

                        wallpaper.setUrl(url);

                        wallpaper = wallpaperRepository.save(wallpaper);

                        // =========================================
                        // RESPUESTA
                        // =========================================

                        return ResponseEntity.ok(
                                        Map.of(
                                                        "success", true,
                                                        "message",
                                                        "Fondo de pantalla guardado correctamente",
                                                        "id", wallpaper.getId(),
                                                        "nombre", wallpaper.getNombre(),
                                                        "nombreArchivo", wallpaper.getNombreArchivo(),
                                                        "tipoMime", wallpaper.getTipoMime(),
                                                        "tamanoBytes", wallpaper.getTamanoBytes(),
                                                        "url", wallpaper.getUrl()));

                } catch (Exception e) {

                        System.err.println(
                                        "ERROR GUARDANDO WALLPAPER: "
                                                        + e.getMessage());

                        e.printStackTrace();

                        return ResponseEntity.internalServerError().body(
                                        Map.of(
                                                        "success", false,
                                                        "message",
                                                        "Error guardando el fondo de pantalla"));
                }
        }

        // =====================================================
        // OBTENER IMAGEN DESDE POSTGRESQL
        // =====================================================

        @GetMapping("/imagen/{id}")
        public ResponseEntity<?> obtenerImagen(
                        @PathVariable Long id) {

                MdmWallpaper wallpaper = wallpaperRepository.findById(id)
                                .orElse(null);

                if (wallpaper == null) {

                        return ResponseEntity.notFound().build();
                }

                if (wallpaper.getImagen() == null ||
                                wallpaper.getImagen().length == 0) {

                        return ResponseEntity.notFound().build();
                }

                try {

                        MediaType mediaType = MediaType.parseMediaType(
                                        wallpaper.getTipoMime());

                        return ResponseEntity.ok()
                                        .contentType(mediaType)
                                        .contentLength(
                                                        wallpaper.getImagen().length)
                                        .cacheControl(
                                                        CacheControl.maxAge(
                                                                        1,
                                                                        TimeUnit.HOURS))
                                        .header(
                                                        HttpHeaders.CONTENT_DISPOSITION,
                                                        "inline; filename=\""
                                                                        + wallpaper.getNombreArchivo()
                                                                        + "\"")
                                        .body(wallpaper.getImagen());

                } catch (Exception e) {

                        e.printStackTrace();

                        return ResponseEntity.internalServerError().build();
                }
        }

        // =====================================================
        // APLICAR WALLPAPER A UN DISPOSITIVO
        // =====================================================

        @PostMapping("/{wallpaperId}/dispositivo/{activo}")
        public ResponseEntity<?> aplicarWallpaperDispositivo(
                        @PathVariable Long wallpaperId,
                        @PathVariable String activo) {

                // =========================================
                // BUSCAR WALLPAPER
                // =========================================

                MdmWallpaper wallpaper = wallpaperRepository.findById(wallpaperId)
                                .orElse(null);

                if (wallpaper == null) {

                        return ResponseEntity.status(404).body(
                                        Map.of(
                                                        "success", false,
                                                        "message", "Wallpaper no encontrado"));
                }

                // =========================================
                // BUSCAR TABLET POR ACTIVO
                // =========================================

                Tablet tablet = tabletRepository.findByActivo(activo)
                                .orElse(null);

                if (tablet == null) {

                        return ResponseEntity.status(404).body(
                                        Map.of(
                                                        "success", false,
                                                        "activo", activo,
                                                        "message", "Tablet no encontrada"));
                }

                // =========================================
                // CREAR O RECUPERAR REGISTRO
                // =========================================

                MdmWallpaperDispositivo registro = wallpaperDispositivoRepository
                                .findByWallpaperIdAndTabletId(
                                                wallpaperId,
                                                tablet.getId())
                                .orElseGet(() -> {

                                        MdmWallpaperDispositivo nuevo = new MdmWallpaperDispositivo();

                                        nuevo.setWallpaper(wallpaper);
                                        nuevo.setTablet(tablet);
                                        nuevo.setEstado("PENDIENTE");
                                        nuevo.setIntentos(0);

                                        return nuevo;
                                });

                // =========================================
                // VERIFICAR CONEXIÓN
                // =========================================

                String deviceId = tablet.getId().toString();

                if (!MdmSocketHandler.estaTabletConectada(deviceId)) {

                        registro.setEstado("PENDIENTE");
                        registro.setUltimoError(
                                        "Tablet sin conexión");

                        wallpaperDispositivoRepository.save(registro);

                        return ResponseEntity.status(409).body(
                                        Map.of(
                                                        "success", false,
                                                        "activo", activo,
                                                        "estado", "PENDIENTE",
                                                        "message",
                                                        "La tablet no está conectada. El wallpaper quedó pendiente."));
                }

                try {

                        // =========================================
                        // AUMENTAR INTENTO
                        // =========================================

                        registro.setIntentos(
                                        registro.getIntentos() + 1);

                        registro.setFechaEnvio(
                                        LocalDateTime.now());

                        registro.setUltimoError(null);

                        wallpaperDispositivoRepository.save(registro);

                        // =========================================
                        // URL QUE DESCARGARÁ LA TABLET
                        // =========================================

                        /*
                         * IMPORTANTE:
                         *
                         * wallpaper.getUrl() contiene:
                         *
                         * /wallpapers/imagen/1
                         *
                         * Android necesita una URL completa.
                         *
                         * Por ahora utilizamos la IP del servidor
                         * que ya comprobaste desde la red.
                         */

                        String url = "http://172.16.1.35:9297"
                                        + wallpaper.getUrl();

                        // =========================================
                        // CREAR COMANDO WEBSOCKET
                        // =========================================

                        String json = objectMapper.writeValueAsString(
                                        Map.of(
                                                        "command",
                                                        "set_wallpaper",

                                                        "url",
                                                        url));

                        // =========================================
                        // ENVIAR Y ESPERAR ACK
                        // =========================================

                        boolean confirmado = MdmSocketHandler.enviarOrdenConAck(
                                        deviceId,
                                        json,
                                        "set_wallpaper");

                        // =========================================
                        // TABLET CONFIRMÓ
                        // =========================================

                        if (confirmado) {

                                registro.setEstado("APLICADO");

                                registro.setFechaConfirmacion(
                                                LocalDateTime.now());

                                registro.setUltimoError(null);

                                wallpaperDispositivoRepository.save(
                                                registro);

                                return ResponseEntity.ok(
                                                Map.of(
                                                                "success", true,
                                                                "confirmed", true,
                                                                "activo", activo,
                                                                "wallpaperId", wallpaperId,
                                                                "estado", "APLICADO",
                                                                "message",
                                                                "Fondo de pantalla aplicado correctamente"));
                        }

                        // =========================================
                        // NO HUBO ACK
                        // =========================================

                        registro.setEstado("ERROR");

                        registro.setUltimoError(
                                        "La tablet no confirmó el cambio de fondo");

                        wallpaperDispositivoRepository.save(
                                        registro);

                        return ResponseEntity.status(409).body(
                                        Map.of(
                                                        "success", false,
                                                        "confirmed", false,
                                                        "activo", activo,
                                                        "wallpaperId", wallpaperId,
                                                        "estado", "ERROR",
                                                        "message",
                                                        "La tablet no confirmó el cambio de fondo"));

                } catch (Exception e) {

                        // =========================================
                        // ERROR
                        // =========================================

                        registro.setEstado("ERROR");

                        registro.setUltimoError(
                                        e.getMessage() != null
                                                        ? e.getMessage()
                                                        : "Error desconocido");

                        wallpaperDispositivoRepository.save(
                                        registro);

                        e.printStackTrace();

                        return ResponseEntity.internalServerError().body(
                                        Map.of(
                                                        "success", false,
                                                        "activo", activo,
                                                        "wallpaperId", wallpaperId,
                                                        "estado", "ERROR",
                                                        "message",
                                                        "Error enviando el fondo de pantalla"));
                }
        }

}