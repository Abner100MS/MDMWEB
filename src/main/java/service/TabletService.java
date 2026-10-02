package service;

import Entidad.Tablet;
import repository.TabletRepository;
import repository.TareaProgramadaDispositivoRepository;
import repository.TareaProgramadaDispositivoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import repository.GpsTrackingRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import Dto.LocationDTO;
import Dto.TabletDashboardProjection;
import Entidad.GpsTracking;
import exception.GpsTrackingAlreadyActiveException;
import java.util.Optional;
import Entidad.ActivoInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import Dto.DashboardDTO;
import Dto.DashboardGraficaDTO;
import Dto.TabletDashboardProjection;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import Dto.DashboardGraficaDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class TabletService {

    @Autowired
    private TabletRepository tabletRepository;

    @Autowired
    private TareaProgramadaDispositivoRepository tareaProgramadaDispositivoRepository;

    @Autowired
    private GpsTrackingRepository gpsTrackingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional // Esto asegura que la operación sea atómica
    public Tablet procesarHeartbeat(Tablet datosRecibidos) {

        // No procesar reportes sin activo
        if (datosRecibidos.getActivo() == null ||
                datosRecibidos.getActivo().trim().isEmpty()) {

            System.out.println("Heartbeat ignorado: activo vacío.");
            return null;
        }

        Optional<Tablet> tabletExistente = tabletRepository.findByActivo(datosRecibidos.getActivo());

        if (tabletExistente.isPresent()) {

            Tablet tablet = tabletExistente.get();

            // ACTUALIZAMOS SOLO TELEMETRÍA (No tocamos apps_reportadas)
            tablet.setBatteryLevel(datosRecibidos.getBatteryLevel());
            tablet.setIpAddress(datosRecibidos.getIpAddress());
            tablet.setRamUsage(datosRecibidos.getRamUsage());
            tablet.setStorageUsage(datosRecibidos.getStorageUsage());
            tablet.setUptime(datosRecibidos.getUptime());
            tablet.setTemperatura(datosRecibidos.getTemperatura());
            tablet.setEstado(datosRecibidos.getEstado());
            tablet.setEstadoWifi(datosRecibidos.getEstadoWifi());
            tablet.setDeviceName(datosRecibidos.getDeviceName());
            tablet.setAndroidId(datosRecibidos.getAndroidId());
            tablet.setOsVersion(datosRecibidos.getOsVersion());
            tablet.setAppVersion(datosRecibidos.getAppVersion());
            tablet.setImei(datosRecibidos.getImei());
            tablet.setSecurityPatch(datosRecibidos.getSecurityPatch());
            tablet.setSystemUpdatePending(
                    Boolean.TRUE.equals(
                            datosRecibidos.getSystemUpdatePending()));

            tablet.setSystemUpdateReceivedTime(
                    datosRecibidos.getSystemUpdateReceivedTime());

            // ==========================================
            // ÚLTIMA UBICACIÓN RECIBIDA EN HEARTBEAT
            // ==========================================

            if (datosRecibidos.getLatitude() != null
                    && datosRecibidos.getLongitude() != null) {

                tablet.setLatitude(
                        datosRecibidos.getLatitude());

                tablet.setLongitude(
                        datosRecibidos.getLongitude());

                if (datosRecibidos.getGpsAccuracy() != null) {
                    tablet.setGpsAccuracy(
                            datosRecibidos.getGpsAccuracy());
                }

                if (datosRecibidos.getGpsSource() != null) {
                    tablet.setGpsSource(
                            datosRecibidos.getGpsSource());
                }

                if (datosRecibidos.getGpsTimestampMillis() != null) {

                    tablet.setGpsTimestamp(
                            java.time.Instant
                                    .ofEpochMilli(
                                            datosRecibidos.getGpsTimestampMillis())
                                    .atZone(
                                            java.time.ZoneId.systemDefault())
                                    .toLocalDateTime());
                }
            }

            if (datosRecibidos.getCategoria() != null
                    && !datosRecibidos.getCategoria().isBlank()) {
                tablet.setCategoria(datosRecibidos.getCategoria());
            }

            if (datosRecibidos.getEstadoCargador() != null) {

                String estadoAnterior = tablet.getEstadoCargador();
                String estadoNuevo = datosRecibidos.getEstadoCargador();

                if (estadoAnterior != null && !estadoAnterior.equals(estadoNuevo)) {

                    System.out.println("CAMBIO DE CARGADOR: "
                            + estadoAnterior + " -> " + estadoNuevo);

                    if (estadoAnterior.startsWith("Desconectado desde ")) {

                        String[] partes = estadoAnterior.split(" desde ", 2);

                        String estadoHistorial = partes[0].trim();

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

                        LocalDateTime fechaEvento = LocalDateTime.parse(partes[1].trim(), formatter);

                        jdbcTemplate.update(
                                """
                                        INSERT INTO "monitoreo tablet".historial_cargador
                                        (tablet_id, estado_cargador, porcentaje_bateria, fecha_evento)
                                        VALUES (?, ?, ?, ?)
                                        """,
                                tablet.getId(),
                                estadoHistorial,
                                tablet.getBatteryLevel(),
                                Timestamp.valueOf(fechaEvento));
                    }
                }

                tablet.setEstadoCargador(estadoNuevo);
            }

            // Lógica de Comandos
            String comandoAEnviar = tablet.getPendingCommand();

            // Limpiamos el comando en la base de datos para que no se repita
            tablet.setPendingCommand(null);

            // Si recibimos heartbeat, sabemos que la tablet está respondiendo
            tablet.setSinRespuesta(false);

            tablet.setLastConnection(LocalDateTime.now());

            // Guardamos
            Tablet tabletGuardada = tabletRepository.save(tablet);

            // Re-adjuntamos el comando solo para la respuesta a la Tablet
            tabletGuardada.setPendingCommand(comandoAEnviar);

            return tabletGuardada;

        } else {

            datosRecibidos.setSinRespuesta(false);

            datosRecibidos.setLastConnection(LocalDateTime.now());

            if (datosRecibidos.getModoKiosco() == null) {
                datosRecibidos.setModoKiosco(false);
            }

            return tabletRepository.save(datosRecibidos);

        }
    }

    public List<Object[]> obtenerHistorialCargador(
            Long tabletId,
            String fechaDesde,
            String fechaHasta) {

        if ((fechaDesde == null || fechaDesde.isBlank()) &&
                (fechaHasta == null || fechaHasta.isBlank())) {

            return tabletRepository.obtenerHistorialCargadorSinFechas(tabletId);
        }

        return tabletRepository.obtenerHistorialCargador(
                tabletId,
                fechaDesde,
                fechaHasta);
    }

    public Page<TabletDashboardProjection> obtenerDashboard(
            String buscar,
            String planta,
            String categoria,
            String version,
            String estadoCargador,
            String estado,
            String estadoBateria,
            Pageable pageable) {

        return tabletRepository.obtenerDashboard(
                buscar,
                planta,
                categoria,
                version,
                estadoCargador,
                estado,
                estadoBateria,
                pageable);
    }

    @Transactional
    public void eliminarTablet(Long id) {

        if (!tabletRepository.existsById(id)) {
            throw new RuntimeException("Tablet no encontrada");
        }

        System.out.println("SERVICE ELIMINAR " + id);

        tareaProgramadaDispositivoRepository.deleteByTabletId(id);

        tabletRepository.deleteById(id);
    }

    // --- NUEVO MÉTODO: Para guardar lo que tú edites en el Modal de la Web ---
    @Transactional
    public Tablet actualizarConfiguracionRemota(Long id, Tablet nuevaConfig) {
        Tablet tablet = tabletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tablet no encontrada con ID: " + id));
        System.out.println("COMANDO RECIBIDO = " + nuevaConfig.getPendingCommand());

        System.out.println("================================");
        System.out.println("APPS = " + nuevaConfig.getAppsBloqueadas());
        System.out.println("REINICIO = " + nuevaConfig.getConfigReinicio());
        System.out.println("RESTRICCIONES = " + nuevaConfig.getRestricciones());
        System.out.println("URLS = " + nuevaConfig.getUrlsPermitidas());
        System.out.println("================================");

        if (nuevaConfig.getPendingCommand() != null) {

            if ("location_track_start".equals(nuevaConfig.getPendingCommand())) {

                GpsTracking trackingExistente = gpsTrackingRepository
                        .findByTabletId(id)
                        .orElse(null);

                if (trackingExistente != null &&
                        Boolean.TRUE.equals(trackingExistente.getActivo())) {

                    throw new GpsTrackingAlreadyActiveException();
                }

                GpsTracking tracking = gpsTrackingRepository
                        .findByTabletId(id)
                        .orElseGet(GpsTracking::new);

                tracking.setTabletId(id);
                tracking.setActivo(true);
                tracking.setStartedAt(LocalDateTime.now());
                tracking.setLastActivity(LocalDateTime.now());
                tracking.setStoppedAt(null);

                gpsTrackingRepository.save(tracking);

                tablet.setPendingCommand("location_track_start");
            }

            else if ("location_track_stop".equals(nuevaConfig.getPendingCommand())) {

                gpsTrackingRepository
                        .findByTabletId(id)
                        .ifPresent(t -> {

                            t.setActivo(false);
                            t.setStoppedAt(LocalDateTime.now());

                            gpsTrackingRepository.save(t);
                        });

                tablet.setPendingCommand("location_track_stop");
            }

            else {

                if (nuevaConfig.getPendingCommand() != null
                        && !nuevaConfig.getPendingCommand().isBlank()) {

                    tablet.setPendingCommand(nuevaConfig.getPendingCommand());
                }
            }
        }
        // Solo actualizamos los campos que se controlan desde el Modal Web
        if (nuevaConfig.getAppsBloqueadas() != null) {
            tablet.setAppsBloqueadas(nuevaConfig.getAppsBloqueadas());
        }
        if (nuevaConfig.getConfigReinicio() != null) {
            tablet.setConfigReinicio(nuevaConfig.getConfigReinicio());
        }
        if (nuevaConfig.getRestricciones() != null) {
            tablet.setRestricciones(nuevaConfig.getRestricciones());
        }
        if (nuevaConfig.getModoKiosco() != null) {
            tablet.setModoKiosco(nuevaConfig.getModoKiosco());
        }
        if (nuevaConfig.getUrlsPermitidas() != null) {
            tablet.setUrlsPermitidas(nuevaConfig.getUrlsPermitidas());
        }
        if (nuevaConfig.getActivo() != null) {
            tablet.setActivo(nuevaConfig.getActivo());
        }

        return tabletRepository.save(tablet);
    }

    // TEMPORAL SIRVE PARA DESACTIVAR BLOQUEO APPS, RESERT Y MODO DESARROYADOR
    @Transactional
    public Tablet cambiarRestriccionesInstalacionMasiva(
            String activo,
            boolean bloquear) {

        Tablet tablet = tabletRepository.findByActivo(activo)
                .orElseThrow(() -> new RuntimeException("Tablet no encontrada con activo: " + activo));

        try {

            ObjectMapper mapper = new ObjectMapper();

            String restriccionesActuales = tablet.getRestricciones();

            ObjectNode restricciones;

            if (restriccionesActuales == null || restriccionesActuales.isBlank()) {
                restricciones = mapper.createObjectNode();
            } else {
                restricciones = (ObjectNode) mapper.readTree(restriccionesActuales);
            }

            // ============================================
            // INSTALACIÓN DE APPS
            // true = bloquear instalación
            // false = permitir instalación
            // ============================================
            restricciones.put("installApps", bloquear);

            // ============================================
            // ESTAS DOS SIEMPRE DEBEN QUEDAR DESACTIVADAS
            // ============================================
            restricciones.put("developer", false);
            restricciones.put("factoryReset", false);

            tablet.setRestricciones(
                    mapper.writeValueAsString(restricciones));

            return tabletRepository.save(tablet);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error modificando restricciones del activo " + activo,
                    e);
        }
    }

    @Transactional
    public void actualizarUbicacion(Long id, LocationDTO location) {

        Tablet tablet = tabletRepository.findById(id)
                .orElseThrow();

        tablet.setLatitude(location.getLatitude());
        tablet.setLongitude(location.getLongitude());
        tablet.setGpsTimestamp(
                LocalDateTime.now());

        tabletRepository.save(tablet);
    }

    public DashboardDTO obtenerDashboard() {

        Object[] r = (Object[]) tabletRepository.obtenerDashboardResumen();

        DashboardDTO dto = new DashboardDTO();

        dto.setTotal(r[0] == null ? 0 : ((Number) r[0]).longValue());
        dto.setOffline(r[1] == null ? 0 : ((Number) r[1]).longValue());
        dto.setAutorizados(r[2] == null ? 0 : ((Number) r[2]).longValue());

        dto.setNopowerCel(r[3] == null ? 0 : ((Number) r[3]).longValue());
        dto.setNopowerHand(r[4] == null ? 0 : ((Number) r[4]).longValue());
        dto.setNopowerTab(r[5] == null ? 0 : ((Number) r[5]).longValue());
        dto.setNopowerGen(r[6] == null ? 0 : ((Number) r[6]).longValue());

        dto.setNoAutorizados(r[7] == null ? 0 : ((Number) r[7]).longValue());
        dto.setBateriaBaja(
                r[8] == null ? 0 : ((Number) r[8]).longValue());

        dto.setBateria80a100(
                r[9] == null ? 0 : ((Number) r[9]).longValue());

        dto.setBateria50a79(
                r[10] == null ? 0 : ((Number) r[10]).longValue());

        dto.setBateria20a49(
                r[11] == null ? 0 : ((Number) r[11]).longValue());

        dto.setBateria0a19(
                r[12] == null ? 0 : ((Number) r[12]).longValue());

        dto.setBateriaSinDatos(
                r[13] == null ? 0 : ((Number) r[13]).longValue());

        dto.setBateriasInfladas(
                r[14] == null ? 0 : ((Number) r[14]).longValue());

        dto.setPlantaGeneral(
                convertirGrafica(
                        tabletRepository.obtenerGraficaPlantas()));

        dto.setPlantaPc(
                convertirGrafica(
                        tabletRepository.obtenerGraficaCategorias("PC")));

        dto.setPlantaPf(
                convertirGrafica(
                        tabletRepository.obtenerGraficaCategorias("PF")));

        dto.setCategorias(
                convertirGrafica(
                        tabletRepository.obtenerGraficaCategorias(null)));

        dto.setEstadoRed(
                convertirGrafica(
                        tabletRepository.obtenerGraficaEstadoRed()));

        return dto;
    }

    private List<DashboardGraficaDTO> convertirGrafica(List<Object[]> datos) {

        return datos.stream()
                .map(r -> {

                    DashboardGraficaDTO dto = new DashboardGraficaDTO();

                    dto.setNombre((String) r[0]);
                    dto.setCantidad(((Number) r[1]).longValue());

                    return dto;

                })
                .toList();
    }

    public byte[] obtenerReporte(
            String buscar,
            String planta,
            String categoria,
            String estadoCargador,
            String estado,
            String estadoBateria,
            String columnas) throws IOException {

        List<TabletDashboardProjection> datos = tabletRepository.obtenerDashboardReporte(
                buscar == null ? "" : buscar,
                planta == null ? "" : planta,
                categoria == null ? "" : categoria,
                estadoCargador == null ? "" : estadoCargador,
                estado == null ? "" : estado,
                estadoBateria == null ? "" : estadoBateria);

        Set<String> cols = new HashSet<>();

        if (columnas != null && !columnas.isBlank()) {
            cols.addAll(Arrays.asList(columnas.split(",")));
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reporte");

        int fila = 0;

        int col = 0;

        Row encabezado = sheet.createRow(fila++);

        if (cols.contains("activo"))
            encabezado.createCell(col++).setCellValue("Activo");

        if (cols.contains("equipo"))
            encabezado.createCell(col++).setCellValue("Equipo / Modelo");

        if (cols.contains("empleado"))
            encabezado.createCell(col++).setCellValue("Empleado");

        if (cols.contains("codigo"))
            encabezado.createCell(col++).setCellValue("Código");

        if (cols.contains("area"))
            encabezado.createCell(col++).setCellValue("Área");

        if (cols.contains("departamento"))
            encabezado.createCell(col++).setCellValue("Departamento");

        if (cols.contains("estadoDispositivo"))
            encabezado.createCell(col++).setCellValue("Estado Dispositivo");

        if (cols.contains("alerta"))
            encabezado.createCell(col++).setCellValue("Alerta de Conexión");

        if (cols.contains("ip"))
            encabezado.createCell(col++).setCellValue("IP");

        if (cols.contains("estadoRed"))
            encabezado.createCell(col++).setCellValue("Estado Red");

        if (cols.contains("ultimoReporte"))
            encabezado.createCell(col++).setCellValue("Último Reporte");

        if (cols.contains("bateria"))
            encabezado.createCell(col++).setCellValue("Batería");
        if (cols.contains("estadoBateria"))
            encabezado.createCell(col++).setCellValue("Estado batería");

        if (cols.contains("porcentajeInflado"))
            encabezado.createCell(col++).setCellValue("Porcentaje inflado");

        if (cols.contains("temperatura"))
            encabezado.createCell(col++).setCellValue("Temperatura");

        if (cols.contains("ram"))
            encabezado.createCell(col++).setCellValue("RAM");

        if (cols.contains("almacenamiento"))
            encabezado.createCell(col++).setCellValue("Almacenamiento");

        if (cols.contains("uptime"))
            encabezado.createCell(col++).setCellValue("Tiempo Activo");

        if (cols.contains("estadoWifi"))
            encabezado.createCell(col++).setCellValue("Estado WiFi");

        if (cols.contains("android"))
            encabezado.createCell(col++).setCellValue("Versión Android");

        if (cols.contains("androidId"))
            encabezado.createCell(col++).setCellValue("Android ID");

        for (TabletDashboardProjection t : datos) {

            Row row = sheet.createRow(fila++);
            col = 0;

            String estadoDispositivo = t.getLastConnection() != null
                    && t.getLastConnection().isAfter(LocalDateTime.now().minusMinutes(35))
                            ? "EN LÍNEA"
                            : "OFFLINE";

            if (cols.contains("activo"))
                row.createCell(col++).setCellValue(
                        t.getActivo() == null ? "" : t.getActivo());

            if (cols.contains("equipo"))
                row.createCell(col++).setCellValue(
                        (t.getDeviceName() == null ? "" : t.getDeviceName())
                                + " / "
                                + (t.getModel() == null ? "" : t.getModel()));

            if (cols.contains("empleado"))
                row.createCell(col++).setCellValue(
                        t.getEmpleadoAsig() == null ? "" : t.getEmpleadoAsig());

            if (cols.contains("codigo"))
                row.createCell(col++).setCellValue(
                        t.getCodigoEmpInfo() == null ? "" : t.getCodigoEmpInfo());

            if (cols.contains("area"))
                row.createCell(col++).setCellValue(
                        t.getArea() == null ? "" : t.getArea());

            if (cols.contains("departamento"))
                row.createCell(col++).setCellValue(
                        t.getDepartamento() == null ? "" : t.getDepartamento());

            if (cols.contains("estadoDispositivo"))
                row.createCell(col++).setCellValue(estadoDispositivo);

            if (cols.contains("alerta"))
                row.createCell(col++).setCellValue(
                        t.getEstadoCargador() == null ? "" : t.getEstadoCargador());

            if (cols.contains("ip"))
                row.createCell(col++).setCellValue(
                        t.getIpAddress() == null ? "" : t.getIpAddress());

            if (cols.contains("estadoRed"))
                row.createCell(col++).setCellValue(
                        t.getEstado() == null ? "" : t.getEstado());

            if (cols.contains("ultimoReporte"))
                row.createCell(col++).setCellValue(
                        t.getLastConnection() == null ? "" : t.getLastConnection().toString());

            if (cols.contains("bateria"))
                row.createCell(col++).setCellValue(
                        t.getBatteryLevel() == null ? "" : String.valueOf(t.getBatteryLevel()));

            if (cols.contains("estadoBateria"))
                row.createCell(col++).setCellValue(
                        t.getEstadoBateria() == null
                                ? "NORMAL"
                                : t.getEstadoBateria());

            if (cols.contains("porcentajeInflado"))
                row.createCell(col++).setCellValue(
                        t.getPorcentajeInflado() == null
                                ? ""
                                : t.getPorcentajeInflado() + "%");

            if (cols.contains("temperatura"))
                row.createCell(col++).setCellValue(
                        t.getTemperatura() == null ? "" : t.getTemperatura());

            if (cols.contains("ram"))
                row.createCell(col++).setCellValue(
                        t.getRamUsage() == null ? "" : t.getRamUsage());

            if (cols.contains("almacenamiento"))
                row.createCell(col++).setCellValue(
                        t.getStorageUsage() == null ? "" : t.getStorageUsage());

            if (cols.contains("uptime"))
                row.createCell(col++).setCellValue(
                        t.getUptime() == null ? "" : t.getUptime());

            if (cols.contains("estadoWifi"))
                row.createCell(col++).setCellValue(
                        t.getEstadoWifi() == null ? "" : t.getEstadoWifi());

            if (cols.contains("android"))
                row.createCell(col++).setCellValue(
                        t.getOsVersion() == null ? "" : t.getOsVersion());

            if (cols.contains("androidId"))
                row.createCell(col++).setCellValue(
                        t.getAndroidId() == null ? "" : t.getAndroidId());
        }

        for (int i = 0; i < col; i++) {
            sheet.autoSizeColumn(i);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        workbook.write(out);
        workbook.close();

        return out.toByteArray();
    }

    public ActivoInfo obtenerActivoInfo(String activo) {
        return tabletRepository.obtenerActivoInfo(activo);
    }

    public List<String> obtenerPlantas() {

        return tabletRepository.obtenerPlantas();
    }

    public List<String> obtenerCategorias() {

        return tabletRepository.obtenerCategorias();
    }

    public List<String> obtenerDepartamentos() {
        return tabletRepository.obtenerDepartamentos();
    }

    public List<String> obtenerVersionesApp() {
        return tabletRepository.obtenerVersionesApp();
    }

    public List<Tablet> obtenerTablets() {

        return tabletRepository.findAll();
    }

    public Page<TabletDashboardProjection> obtenerTabletsSelector(
            String buscar,
            String planta,
            String departamento,
            String categoria,
            Pageable pageable) {

        return tabletRepository.obtenerTabletsSelector(
                buscar == null ? "" : buscar,
                planta == null ? "" : planta,
                departamento == null ? "" : departamento,
                categoria == null ? "" : categoria,
                pageable);
    }

    @Transactional
    public Tablet actualizarEstadoBateria(
            Long id,
            String estadoBateria,
            Integer porcentajeInflado) {

        Tablet tablet = tabletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tablet no encontrada con ID: " + id));

        if (estadoBateria == null || estadoBateria.isBlank()) {
            throw new IllegalArgumentException("Estado de batería obligatorio");
        }

        String estado = estadoBateria.trim().toUpperCase();

        if (!estado.equals("NORMAL") && !estado.equals("INFLADA")) {
            throw new IllegalArgumentException("Estado de batería no válido");
        }

        if (estado.equals("NORMAL")) {

            tablet.setEstadoBateria("NORMAL");
            tablet.setPorcentajeInflado(null);

        } else {

            if (porcentajeInflado == null ||
                    porcentajeInflado < 1 ||
                    porcentajeInflado > 100) {

                throw new IllegalArgumentException(
                        "El porcentaje debe estar entre 1 y 100");
            }

            tablet.setEstadoBateria("INFLADA");
            tablet.setPorcentajeInflado(porcentajeInflado);
        }

        return tabletRepository.save(tablet);
    }

    public Tablet obtenerPorId(Long id) {

        return tabletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Tablet no encontrada con ID: " + id));
    }
}