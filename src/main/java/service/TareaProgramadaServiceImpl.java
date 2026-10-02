package service;

import Dto.CrearTareaProgramadaDTO;
import Dto.DetalleTareaDTO;
import Dto.TareaProgramadaDTO;
import Entidad.Tablet;
import Entidad.TareaProgramada;
import Entidad.TareaProgramadaDispositivo;
import Enums.DestinoTarea;
import Enums.EstadoTarea;
import Enums.EstadoTareaDispositivo;
import Enums.TipoProgramacion;
import Enums.TipoTarea;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.monitoreo.MdmSocketHandler;

import repository.TabletRepository;
import repository.TareaProgramadaDispositivoRepository;
import repository.TareaProgramadaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.concurrent.ExecutorService;

import java.util.concurrent.Future;

@Service
public class TareaProgramadaServiceImpl implements TareaProgramadaService {

    @Autowired
    private TareaProgramadaRepository tareaProgramadaRepository;

    @Autowired
    private TareaProgramadaDispositivoRepository tareaProgramadaDispositivoRepository;

    @Autowired
    private TabletRepository tabletRepository;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final Map<Long, ScheduledFuture<?>> tareasProgramadas = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public TareaProgramadaDTO crear(CrearTareaProgramadaDTO dto) {

        TareaProgramada tarea = new TareaProgramada();

        tarea.setNombre(dto.getNombre());
        tarea.setDescripcion(dto.getDescripcion());
        tarea.setTipoTarea(dto.getTipoTarea());
        tarea.setDestinoTarea(dto.getDestinoTarea());
        tarea.setValorDestino(dto.getValorDestino());
        tarea.setFechaProgramada(dto.getFechaProgramada());
        tarea.setHoraProgramada(dto.getHoraProgramada());
        tarea.setTipoProgramacion(dto.getTipoProgramacion());

        tarea.setDiasSemana(dto.getDiasSemana());
        tarea.setDiaMes(dto.getDiaMes());
        tarea.setParametros(dto.getParametros());
        tarea.setEstado(EstadoTarea.PENDIENTE);
        tarea.setActivo(true);

        tarea.setProximaEjecucion(
                LocalDateTime.of(
                        dto.getFechaProgramada(),
                        dto.getHoraProgramada()));

        tarea = tareaProgramadaRepository.save(tarea);

        int total = crearDispositivosDeTarea(tarea, dto);

        programar(tarea.getId());

        TareaProgramadaDTO respuesta = convertirDTOBasico(tarea);
        respuesta.setTotalDispositivos((long) total);
        respuesta.setCompletados(0L);
        respuesta.setPendientes((long) total);
        respuesta.setErrores(0L);

        return respuesta;
    }

    @Override
    public List<TareaProgramadaDTO> listar() {

        return tareaProgramadaRepository.findAll()
                .stream()
                .map(this::convertirDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TareaProgramadaDTO obtener(Long id) {

        TareaProgramada tarea = tareaProgramadaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        return convertirDTO(tarea);
    }

    @Override
    @Transactional
    public TareaProgramadaDTO actualizar(Long id, CrearTareaProgramadaDTO dto) {

        TareaProgramada tarea = tareaProgramadaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        tarea.setNombre(dto.getNombre());
        tarea.setDescripcion(dto.getDescripcion());
        tarea.setTipoTarea(dto.getTipoTarea());
        tarea.setDestinoTarea(dto.getDestinoTarea());
        tarea.setValorDestino(dto.getValorDestino());
        tarea.setFechaProgramada(dto.getFechaProgramada());
        tarea.setHoraProgramada(dto.getHoraProgramada());
        tarea.setTipoProgramacion(dto.getTipoProgramacion());
        tarea.setDiasSemana(dto.getDiasSemana());
        tarea.setDiaMes(dto.getDiaMes());
        tarea.setParametros(dto.getParametros());
        tarea.setEstado(EstadoTarea.PENDIENTE);
        tarea.setActivo(true);
        tarea.setFechaEjecucion(null);

        tarea.setProximaEjecucion(
                LocalDateTime.of(
                        dto.getFechaProgramada(),
                        dto.getHoraProgramada()));

        tarea = tareaProgramadaRepository.save(tarea);

        List<TareaProgramadaDispositivo> anteriores = tareaProgramadaDispositivoRepository.findByTareaProgramadaId(id);

        tareaProgramadaDispositivoRepository.deleteAll(anteriores);

        int total = crearDispositivosDeTarea(tarea, dto);

        reprogramar(tarea.getId());

        TareaProgramadaDTO respuesta = convertirDTOBasico(tarea);
        respuesta.setTotalDispositivos((long) total);
        respuesta.setCompletados(0L);
        respuesta.setPendientes((long) total);
        respuesta.setErrores(0L);

        return respuesta;
    }

    @Override
    @Transactional
    public void eliminar(Long id) {

        if (!tareaProgramadaRepository.existsById(id)) {
            throw new RuntimeException("Tarea no encontrada");
        }

        ScheduledFuture<?> future = tareasProgramadas.remove(id);

        if (future != null) {
            future.cancel(false);
        }

        tareaProgramadaDispositivoRepository.deleteByTareaProgramadaId(id);

        tareaProgramadaRepository.deleteById(id);
    }

    @Override
    public void programar(Long tareaId) {

        TareaProgramada tarea = tareaProgramadaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        LocalDateTime fechaHora = tarea.getProximaEjecucion();

        if (fechaHora == null) {

            fechaHora = LocalDateTime.of(
                    tarea.getFechaProgramada(),
                    tarea.getHoraProgramada());

            tarea.setProximaEjecucion(fechaHora);

            tareaProgramadaRepository.save(tarea);
        }

        LocalDateTime ahora = LocalDateTime.now();

        if (!fechaHora.isAfter(ahora)) {

            System.out.println(
                    "TAREA NO PROGRAMADA: la fecha ya pasó. Tarea ID: "
                            + tareaId
                            + " Fecha: "
                            + fechaHora);

            return;
        }

        long delay = Duration.between(ahora, fechaHora).toMillis();

        ScheduledFuture<?> future = scheduler.schedule(
                () -> ejecutar(tareaId),
                delay,
                TimeUnit.MILLISECONDS);

        tareasProgramadas.put(tareaId, future);

        System.out.println(
                "TAREA PROGRAMADA ID "
                        + tareaId
                        + " PARA "
                        + fechaHora);
    }

    @Override
    public void reprogramar(Long tareaId) {

        ScheduledFuture<?> future = tareasProgramadas.remove(tareaId);

        if (future != null) {
            future.cancel(false);
        }

        programar(tareaId);
    }

    @Override
    @Transactional
    public void cancelar(Long tareaId) {

        ScheduledFuture<?> future = tareasProgramadas.remove(tareaId);

        if (future != null) {
            future.cancel(false);
        }

        TareaProgramada tarea = tareaProgramadaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        tarea.setEstado(EstadoTarea.CANCELADA);
        tarea.setActivo(false);

        tareaProgramadaRepository.save(tarea);
    }

    @Override
    @Transactional
    public void ejecutar(Long tareaId) {

        TareaProgramada tarea = tareaProgramadaRepository.findById(tareaId)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        if (tareaProgramadaDispositivoRepository
                .findByTareaProgramadaId(tareaId)
                .isEmpty()) {

            CrearTareaProgramadaDTO dto = new CrearTareaProgramadaDTO();

            dto.setDestinoTarea(tarea.getDestinoTarea());
            dto.setValorDestino(tarea.getValorDestino());

            crearDispositivosDeTarea(tarea, dto);
        }

        tarea.setEstado(EstadoTarea.EN_PROCESO);
        tareaProgramadaRepository.save(tarea);

        List<TareaProgramadaDispositivo> dispositivos = tareaProgramadaDispositivoRepository
                .findByTareaProgramadaId(tareaId);

        boolean huboErrores = false;

        ExecutorService executor = Executors.newFixedThreadPool(20);

        List<Future<Boolean>> resultados = new ArrayList<>();

        for (TareaProgramadaDispositivo dispositivo : dispositivos) {

            Future<Boolean> future = executor.submit(() -> {

                try {

                    dispositivo.setEstado(
                            EstadoTareaDispositivo.ENVIADA);

                    tareaProgramadaDispositivoRepository.save(
                            dispositivo);

                    boolean ack;

                    switch (tarea.getTipoTarea()) {

                        case REINICIO -> {

                            ack = MdmSocketHandler.enviarOrdenConAck(
                                    dispositivo.getTablet().getId().toString(),
                                    "{\"pending_command\":\"reboot\"}",
                                    "reboot");
                        }

                        case ACTUALIZAR_APP -> {

                            String apkUrl = tarea.getParametros();

                            if (apkUrl == null ||
                                    apkUrl.isBlank()) {

                                System.out.println(
                                        "Tarea " + tarea.getId()
                                                + " no tiene URL del APK");

                                ack = false;

                            } else {

                                System.out.println(
                                        "Actualizando tablet "
                                                + dispositivo.getTablet().getId()
                                                + " desde "
                                                + apkUrl);

                                ack = MdmSocketHandler
                                        .enviarActualizacionConAck(
                                                dispositivo
                                                        .getTablet()
                                                        .getId()
                                                        .toString(),
                                                apkUrl);
                            }
                        }

                        default -> {

                            System.out.println(
                                    "Tipo de tarea todavía no implementado: "
                                            + tarea.getTipoTarea());

                            ack = false;
                        }
                    }

                    if (ack) {

                        dispositivo.setEstado(
                                EstadoTareaDispositivo.CONFIRMADA);

                        dispositivo.setConfirmado(true);

                        dispositivo.setFechaEjecucion(
                                LocalDateTime.now());

                    } else {

                        String deviceId = dispositivo.getTablet()
                                .getId()
                                .toString();

                        if (tarea.getTipoTarea() == TipoTarea.ACTUALIZAR_APP) {

                            boolean fueEnviada = MdmSocketHandler.fueActualizacionEnviada(deviceId);

                            if (fueEnviada) {

                                dispositivo.setEstado(
                                        EstadoTareaDispositivo.ENVIADA);

                                dispositivo.setConfirmado(false);

                                System.out.println(
                                        "TAREA ACTUALIZACION | Tablet "
                                                + deviceId
                                                + " pendiente de confirmacion final");

                            } else {

                                dispositivo.setEstado(
                                        EstadoTareaDispositivo.SIN_CONEXION);

                                dispositivo.setConfirmado(false);

                                System.out.println(
                                        "TAREA ACTUALIZACION | Tablet "
                                                + deviceId
                                                + " no recibio el comando");
                            }

                        } else {

                            boolean conectado = MdmSocketHandler.estaTabletConectada(deviceId);

                            if (!conectado) {

                                dispositivo.setEstado(
                                        EstadoTareaDispositivo.SIN_CONEXION);

                                System.out.println(
                                        "TAREA | Tablet "
                                                + deviceId
                                                + " SIN CONEXION");

                            } else {

                                dispositivo.setEstado(
                                        EstadoTareaDispositivo.ERROR);

                                System.out.println(
                                        "TAREA | Tablet "
                                                + deviceId
                                                + " conectada pero SIN CONFIRMACION");
                            }

                            dispositivo.setConfirmado(false);
                        }
                    }

                    tareaProgramadaDispositivoRepository.save(
                            dispositivo);

                    return ack;

                } catch (Exception e) {

                    e.printStackTrace();

                    dispositivo.setEstado(
                            EstadoTareaDispositivo.ERROR);

                    dispositivo.setConfirmado(false);

                    tareaProgramadaDispositivoRepository.save(
                            dispositivo);

                    return false;
                }
            });

            resultados.add(future);
        }

        for (Future<Boolean> resultado : resultados) {

            try {

                boolean resultadoOk = resultado.get();

                if (!resultadoOk &&
                        tarea.getTipoTarea() != TipoTarea.ACTUALIZAR_APP) {

                    huboErrores = true;
                }

            } catch (Exception e) {

                huboErrores = true;
                e.printStackTrace();
            }
        }

        executor.shutdown();
        tarea.setFechaEjecucion(LocalDateTime.now());

        if (tarea.getTipoProgramacion() == TipoProgramacion.UNA_VEZ) {

            if (huboErrores) {
                tarea.setEstado(EstadoTarea.COMPLETADA_CON_ERRORES);
            } else {
                tarea.setEstado(EstadoTarea.COMPLETADA);
            }

        } else {

            tarea.setEstado(EstadoTarea.PENDIENTE);

            LocalDateTime siguiente = calcularProximaEjecucion(tarea);

            tarea.setProximaEjecucion(siguiente);
        }

        tareaProgramadaRepository.save(tarea);

        if (tarea.getTipoProgramacion() != TipoProgramacion.UNA_VEZ) {

            programar(tarea.getId());

        }
    }

    @Override
    @Transactional
    public void confirmarActualizacion(Long tabletId) {

        List<TareaProgramadaDispositivo> pendientes = tareaProgramadaDispositivoRepository
                .findByTabletIdAndEstadoOrderByIdDesc(
                        tabletId,
                        EstadoTareaDispositivo.ENVIADA);

        for (TareaProgramadaDispositivo dispositivo : pendientes) {

            if (dispositivo.getTareaProgramada() == null) {
                continue;
            }

            TareaProgramada tarea = dispositivo.getTareaProgramada();

            if (tarea.getTipoTarea() != TipoTarea.ACTUALIZAR_APP) {
                continue;
            }

            dispositivo.setEstado(
                    EstadoTareaDispositivo.CONFIRMADA);

            dispositivo.setConfirmado(true);

            dispositivo.setFechaEjecucion(
                    LocalDateTime.now());

            tareaProgramadaDispositivoRepository.save(
                    dispositivo);

            System.out.println(
                    "TAREA ACTUALIZACION CONFIRMADA | Tablet "
                            + tabletId
                            + " | Tarea "
                            + tarea.getId());

            // Revisar el estado completo de la tarea
            List<TareaProgramadaDispositivo> todos = tareaProgramadaDispositivoRepository
                    .findByTareaProgramadaId(
                            tarea.getId());

            boolean quedanPendientes = false;
            boolean hayErrores = false;

            for (TareaProgramadaDispositivo item : todos) {

                if (item.getEstado() == EstadoTareaDispositivo.ENVIADA) {

                    quedanPendientes = true;
                }

                if (item.getEstado() == EstadoTareaDispositivo.ERROR ||
                        item.getEstado() == EstadoTareaDispositivo.SIN_CONEXION) {

                    hayErrores = true;
                }
            }

            if (!quedanPendientes) {

                if (hayErrores) {

                    tarea.setEstado(
                            EstadoTarea.COMPLETADA_CON_ERRORES);

                } else {

                    tarea.setEstado(
                            EstadoTarea.COMPLETADA);
                }

                tarea.setFechaEjecucion(
                        LocalDateTime.now());

                tareaProgramadaRepository.save(tarea);

                System.out.println(
                        "TAREA ACTUALIZACION FINALIZADA | Tarea "
                                + tarea.getId()
                                + " | Estado "
                                + tarea.getEstado());

            } else {

                System.out.println(
                        "TAREA ACTUALIZACION | Tarea "
                                + tarea.getId()
                                + " todavía tiene dispositivos pendientes");
            }

            return;
        }

        System.out.println(
                "ACK update_app success recibido de tablet "
                        + tabletId
                        + " pero no existe tarea ACTUALIZAR_APP ENVIADA");
    }

    @Override
    @Transactional
    public void procesarActualizacionPendiente(Long tabletId) {

        List<TareaProgramadaDispositivo> pendientes = tareaProgramadaDispositivoRepository
                .findByTabletIdAndEstadoOrderByIdDesc(
                        tabletId,
                        EstadoTareaDispositivo.SIN_CONEXION);

        for (TareaProgramadaDispositivo dispositivo : pendientes) {

            TareaProgramada tarea = dispositivo.getTareaProgramada();

            if (tarea == null) {
                continue;
            }

            if (tarea.getTipoTarea() != TipoTarea.ACTUALIZAR_APP) {
                continue;
            }

            String apkUrl = tarea.getParametros();

            if (apkUrl == null || apkUrl.isBlank()) {

                System.out.println(
                        "ACTUALIZACION PENDIENTE SIN URL | Tablet "
                                + tabletId
                                + " | Tarea "
                                + tarea.getId());

                continue;
            }

            dispositivo.setEstado(
                    EstadoTareaDispositivo.ENVIADA);

            dispositivo.setConfirmado(false);

            tareaProgramadaDispositivoRepository.save(
                    dispositivo);

            System.out.println(
                    "TABLET RECONECTADA | Reintentando actualización | Tablet "
                            + tabletId
                            + " | Tarea "
                            + tarea.getId());

            CompletableFuture.runAsync(() -> {

                MdmSocketHandler.enviarActualizacionConAck(
                        tabletId.toString(),
                        apkUrl);
            });

            return;
        }
    }

    @PostConstruct
    @Override
    public void iniciar() {

        tareaProgramadaRepository.findAll()
                .stream()
                .filter(TareaProgramada::getActivo)
                .filter(t -> t.getEstado() == EstadoTarea.PENDIENTE)
                .forEach(t -> programar(t.getId()));
    }

    private LocalDateTime calcularProximaEjecucion(TareaProgramada tarea) {

        LocalDateTime ahora = LocalDateTime.now();

        switch (tarea.getTipoProgramacion()) {

            case DIARIA:

                LocalDateTime diaria = LocalDateTime.of(
                        ahora.toLocalDate(),
                        tarea.getHoraProgramada());

                if (!diaria.isAfter(ahora)) {
                    diaria = diaria.plusDays(1);
                }

                return diaria;

            case SEMANAL:

                String[] dias = tarea.getDiasSemana().split(",");

                for (int i = 0; i < 7; i++) {

                    LocalDateTime candidato = LocalDateTime.of(
                            ahora.toLocalDate().plusDays(i),
                            tarea.getHoraProgramada());

                    String nombre = switch (candidato.getDayOfWeek()) {
                        case MONDAY -> "LUN";
                        case TUESDAY -> "MAR";
                        case WEDNESDAY -> "MIE";
                        case THURSDAY -> "JUE";
                        case FRIDAY -> "VIE";
                        case SATURDAY -> "SAB";
                        case SUNDAY -> "DOM";
                    };

                    for (String d : dias) {

                        if (d.equalsIgnoreCase(nombre)
                                && candidato.isAfter(ahora)) {

                            return candidato;
                        }
                    }
                }

                return ahora.plusWeeks(1);

            case MENSUAL:

                LocalDateTime mensual = LocalDateTime.of(
                        ahora.withDayOfMonth(tarea.getDiaMes()).toLocalDate(),
                        tarea.getHoraProgramada());

                if (!mensual.isAfter(ahora)) {

                    mensual = LocalDateTime.of(
                            ahora.plusMonths(1)
                                    .withDayOfMonth(tarea.getDiaMes())
                                    .toLocalDate(),
                            tarea.getHoraProgramada());
                }

                return mensual;

            default:

                return null;
        }
    }

    private int crearDispositivosDeTarea(TareaProgramada tarea, CrearTareaProgramadaDTO dto) {

        List<Tablet> tablets;
        List<TareaProgramadaDispositivo> detalles = new ArrayList<>();

        if (dto.getDestinoTarea() == DestinoTarea.TODAS) {

            tablets = tabletRepository.findAll();

        } else if (dto.getDestinoTarea() == DestinoTarea.DISPOSITIVOS) {

            tablets = tabletRepository.findAllById(dto.getDispositivos());

        } else if (dto.getDestinoTarea() == DestinoTarea.CATEGORIA) {

            tablets = tabletRepository.findAll()
                    .stream()
                    .filter(t -> t.getCategoria() != null)
                    .filter(t -> t.getCategoria().equalsIgnoreCase(dto.getValorDestino()))
                    .collect(Collectors.toList());

        } else if (dto.getDestinoTarea() == DestinoTarea.PLANTA) {

            tablets = tabletRepository.obtenerPorPlanta(
                    dto.getValorDestino());

        } else if (dto.getDestinoTarea() == DestinoTarea.DEPARTAMENTO) {

            tablets = tabletRepository.obtenerPorDepartamento(
                    dto.getValorDestino());

        } else if (dto.getDestinoTarea() == DestinoTarea.EMPLEADO) {

            tablets = tabletRepository.obtenerPorEmpleado(
                    dto.getValorDestino());

        } else {

            tablets = List.of();
        }

        for (Tablet tablet : tablets) {

            TareaProgramadaDispositivo detalle = new TareaProgramadaDispositivo();

            detalle.setTareaProgramada(tarea);
            detalle.setTablet(tablet);
            detalle.setEstado(EstadoTareaDispositivo.PENDIENTE);
            detalle.setConfirmado(false);

            detalles.add(detalle);
        }

        tareaProgramadaDispositivoRepository.saveAll(detalles);

        return detalles.size();
    }

    private TareaProgramadaDTO convertirDTO(TareaProgramada tarea) {

        TareaProgramadaDTO dto = new TareaProgramadaDTO();

        dto.setId(tarea.getId());
        dto.setNombre(tarea.getNombre());
        dto.setDescripcion(tarea.getDescripcion());
        dto.setTipoTarea(tarea.getTipoTarea());
        dto.setDestinoTarea(tarea.getDestinoTarea());
        dto.setValorDestino(tarea.getValorDestino());
        dto.setFechaProgramada(tarea.getFechaProgramada());
        dto.setHoraProgramada(tarea.getHoraProgramada());
        dto.setTipoProgramacion(tarea.getTipoProgramacion());
        dto.setDiasSemana(tarea.getDiasSemana());
        dto.setDiaMes(tarea.getDiaMes());
        dto.setProximaEjecucion(tarea.getProximaEjecucion());
        dto.setParametros(tarea.getParametros());
        dto.setEstado(tarea.getEstado());
        dto.setActivo(tarea.getActivo());
        dto.setFechaCreacion(tarea.getFechaCreacion());
        dto.setFechaEjecucion(tarea.getFechaEjecucion());

        List<TareaProgramadaDispositivo> dispositivos = tareaProgramadaDispositivoRepository
                .findByTareaProgramadaId(tarea.getId());

        dto.setTotalDispositivos((long) dispositivos.size());

        dto.setCompletados(
                dispositivos.stream()
                        .filter(d -> d.getEstado() == EstadoTareaDispositivo.CONFIRMADA)
                        .count());

        dto.setPendientes(
                dispositivos.stream()
                        .filter(d -> d.getEstado() == EstadoTareaDispositivo.PENDIENTE
                                || d.getEstado() == EstadoTareaDispositivo.ENVIADA)
                        .count());

        dto.setErrores(
                dispositivos.stream()
                        .filter(d -> d.getEstado() == EstadoTareaDispositivo.SIN_CONEXION)
                        .count());

        return dto;
    }

    private TareaProgramadaDTO convertirDTOBasico(TareaProgramada tarea) {

        TareaProgramadaDTO dto = new TareaProgramadaDTO();

        dto.setId(tarea.getId());
        dto.setNombre(tarea.getNombre());
        dto.setDescripcion(tarea.getDescripcion());
        dto.setTipoTarea(tarea.getTipoTarea());
        dto.setDestinoTarea(tarea.getDestinoTarea());
        dto.setValorDestino(tarea.getValorDestino());
        dto.setFechaProgramada(tarea.getFechaProgramada());
        dto.setHoraProgramada(tarea.getHoraProgramada());
        dto.setTipoProgramacion(tarea.getTipoProgramacion());
        dto.setDiasSemana(tarea.getDiasSemana());
        dto.setDiaMes(tarea.getDiaMes());
        dto.setProximaEjecucion(tarea.getProximaEjecucion());
        dto.setParametros(tarea.getParametros());
        dto.setEstado(tarea.getEstado());
        dto.setActivo(tarea.getActivo());
        dto.setFechaCreacion(tarea.getFechaCreacion());
        dto.setFechaEjecucion(tarea.getFechaEjecucion());

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public DetalleTareaDTO obtenerDetalle(Long tareaId) {

        List<TareaProgramadaDispositivo> lista = tareaProgramadaDispositivoRepository.findByTareaProgramadaId(tareaId);

        DetalleTareaDTO dto = new DetalleTareaDTO();

        dto.setTotalDispositivos((long) lista.size());

        dto.setCompletados(
                lista.stream()
                        .filter(d -> d.getEstado() == EstadoTareaDispositivo.CONFIRMADA)
                        .count());

        dto.setPendientes(
                lista.stream()
                        .filter(d -> d.getEstado() == EstadoTareaDispositivo.PENDIENTE
                                || d.getEstado() == EstadoTareaDispositivo.ENVIADA)
                        .count());

        dto.setErrores(
                lista.stream()
                        .filter(d -> d.getEstado() == EstadoTareaDispositivo.SIN_CONEXION)
                        .count());

        List<DetalleTareaDTO.Dispositivo> dispositivos = lista.stream().map(d -> {

            DetalleTareaDTO.Dispositivo item = new DetalleTareaDTO.Dispositivo();

            item.setActivo(d.getTablet().getActivo());
            item.setEquipo(d.getTablet().getDeviceName());
            item.setEstado(d.getEstado());
            item.setConfirmado(d.getConfirmado());
            item.setFechaEjecucion(d.getFechaEjecucion());

            return item;

        }).collect(Collectors.toList());

        dto.setDispositivos(dispositivos);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DetalleTareaDTO.Dispositivo> obtenerDispositivosDetalle(
            Long tareaId,
            String buscar,
            String estado,
            Pageable pageable) {

        Page<TareaProgramadaDispositivo> pagina;

        if ((buscar == null || buscar.isBlank())
                && (estado == null || estado.isBlank())) {

            pagina = tareaProgramadaDispositivoRepository
                    .findByTareaProgramadaId(tareaId, pageable);

        } else if ((buscar != null && !buscar.isBlank())
                && (estado == null || estado.isBlank())) {

            pagina = tareaProgramadaDispositivoRepository
                    .findByTareaProgramadaIdAndTabletActivoContainingIgnoreCase(
                            tareaId,
                            buscar,
                            pageable);

        } else if ((buscar == null || buscar.isBlank())
                && (estado != null && !estado.isBlank())) {

            pagina = tareaProgramadaDispositivoRepository
                    .findByTareaProgramadaIdAndEstado(
                            tareaId,
                            EstadoTareaDispositivo.valueOf(estado),
                            pageable);

        } else {

            pagina = tareaProgramadaDispositivoRepository
                    .findByTareaProgramadaIdAndEstadoAndTabletActivoContainingIgnoreCase(
                            tareaId,
                            EstadoTareaDispositivo.valueOf(estado),
                            buscar,
                            pageable);
        }

        List<DetalleTareaDTO.Dispositivo> lista = pagina
                .getContent()
                .stream()
                .map(d -> {

                    DetalleTareaDTO.Dispositivo item = new DetalleTareaDTO.Dispositivo();

                    item.setActivo(d.getTablet().getActivo());
                    item.setEquipo(d.getTablet().getDeviceName());
                    item.setEstado(d.getEstado());
                    item.setConfirmado(d.getConfirmado());
                    item.setFechaEjecucion(d.getFechaEjecucion());

                    return item;

                }).toList();

        return new PageImpl<>(
                lista,
                pageable,
                pagina.getTotalElements());
    }

    @Override
    public List<String> obtenerPlantas() {
        return tabletRepository.obtenerPlantas();
    }

    @Override
    public List<String> obtenerCategorias() {
        return tabletRepository.obtenerCategorias();
    }

    @Override
    public List<String> obtenerDepartamentos() {
        return tabletRepository.obtenerDepartamentos();
    }

    @Override
    public List<String> obtenerEmpleados() {
        return tabletRepository.obtenerEmpleados();
    }

}