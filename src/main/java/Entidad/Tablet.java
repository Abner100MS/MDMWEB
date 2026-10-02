package Entidad;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "dispositivos", schema = "monitoreo tablet")
@Data
@DynamicUpdate
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tablet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("activo")
    private String activo;

    @Column(name = "device_name")
    @JsonProperty("device_name")
    private String deviceName;

    @JsonProperty("model")
    private String model;

    @Column(name = "estado_bateria")
    @JsonProperty("estado_bateria")
    private String estadoBateria = "NORMAL";

    @Column(name = "porcentaje_inflado")
    @JsonProperty("porcentaje_inflado")
    private Integer porcentajeInflado;

    @Column(name = "battery_level")
    @JsonProperty("battery_level")
    private Integer batteryLevel;

    @JsonProperty("temperatura")
    private String temperatura;

    @Column(name = "estado_cargador")
    @JsonProperty("estado_cargador")
    private String estadoCargador;

    @Column(name = "estado_wifi")
    @JsonProperty("estado_wifi")
    private String estadoWifi;

    @Column(name = "estado_red")
    @JsonProperty("estado") // <--- ESTO ARREGLA EL "DESCONOCIDO"
    private String estado;

    @Column(name = "ip_address")
    @JsonProperty("ip_address")
    private String ipAddress;

    @Column(name = "ram_usage")
    @JsonProperty("ram_usage")
    private String ramUsage;

    @Column(name = "storage_usage")
    @JsonProperty("storage_usage")
    private String storageUsage;

    @JsonProperty("uptime")
    private String uptime;

    // Agrega la anotación @JsonProperty para que coincida con lo que espera el HTML
    @Column(name = "last_connection")
    @JsonProperty("last_connection")
    private LocalDateTime lastConnection;

    @Column(name = "pending_command")
    @JsonProperty("pending_command")
    private String pendingCommand;

    @Column(name = "android_id")
    @JsonProperty("android_id")
    private String androidId;

    @Column(name = "os_version")
    @JsonProperty("os_version")
    private String osVersion;

    @Column(name = "app_version")
    @JsonProperty("app_version")
    private String appVersion;

    @Column(name = "imei")
    @JsonProperty("imei")
    private String imei;

    @Column(name = "security_patch")
    @JsonProperty("security_patch")
    private String securityPatch;

    @Column(name = "system_update_pending")
    @JsonProperty("system_update_pending")
    private Boolean systemUpdatePending = false;

    @Column(name = "system_update_received_time")
    @JsonProperty("system_update_received_time")
    private Long systemUpdateReceivedTime;

    @Column(name = "sin_respuesta")
    private Boolean sinRespuesta = false;

    // --- CAMPOS PARA GESTIÓN MDM ---

    @Column(name = "apps_reportadas", columnDefinition = "TEXT")
    @JsonProperty("apps_reportadas")
    private String appsReportadas;

    @Transient
    @JsonProperty("accion_masiva_apps")
    private String accionMasivaApps;

    @Transient
    @JsonProperty("apps_modificadas")
    private List<Map<String, String>> appsModificadas;

    @Column(name = "apps_bloqueadas", columnDefinition = "TEXT")
    @JsonProperty("apps_bloqueadas")
    private String appsBloqueadas;

    @Column(name = "config_reinicio", length = 500) // Ej: "0,2,4|03:00"
    @JsonProperty("config_reinicio")
    private String configReinicio;

    @Column(name = "restricciones", length = 1000) // Ej: "camera:false,usb:true"
    @JsonProperty("restricciones")
    private String restricciones;

    // ESTADO DEL MODO KIOSCO
    @Column(name = "modo_kiosco")
    @JsonProperty("modo_kiosco")
    private Boolean modoKiosco;

    @Column(name = "urls_permitidas", length = 2000)
    @JsonProperty("urls_permitidas")
    private String urlsPermitidas;

    @Column(name = "latitude")
    @JsonProperty("latitude")
    private Double latitude;

    @Column(name = "longitude")
    @JsonProperty("longitude")
    private Double longitude;

    @Column(name = "gps_timestamp")
    private LocalDateTime gpsTimestamp;

    @Column(name = "gps_accuracy")
    @JsonProperty("gps_accuracy")
    private Double gpsAccuracy;

    @Column(name = "gps_source")
    @JsonProperty("gps_source")
    private String gpsSource;

    @Transient
    @JsonProperty(value = "gps_timestamp", access = JsonProperty.Access.WRITE_ONLY)
    private Long gpsTimestampMillis;

    @Column(name = "categoria")
    @JsonProperty("categoria")
    private String categoria;

}