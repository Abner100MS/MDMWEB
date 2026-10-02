package Dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface TabletDashboardProjection {

    Long getId();

    String getActivo();

    @JsonProperty("android_id")
    String getAndroidId();

    @JsonProperty("device_name")
    String getDeviceName();

    String getModel();

    @JsonProperty("categoria")
    String getCategoria();

    @JsonProperty("codigo_emp")
    String getCodigoEmpInfo();

    @JsonProperty("empleado_asig")
    String getEmpleadoAsig();

    @JsonProperty("planta")
    String getPlanta();

    @JsonProperty("area")
    String getArea();

    @JsonProperty("departamento")
    String getDepartamento();

    @JsonProperty("estado_bateria")
    String getEstadoBateria();

    @JsonProperty("porcentaje_inflado")
    Integer getPorcentajeInflado();

    @JsonProperty("battery_level")
    Integer getBatteryLevel();

    String getTemperatura();

    @JsonProperty("estado_cargador")
    String getEstadoCargador();

    @JsonProperty("estado_wifi")
    String getEstadoWifi();

    String getEstado();

    @JsonProperty("ip_address")
    String getIpAddress();

    @JsonProperty("ram_usage")
    String getRamUsage();

    @JsonProperty("storage_usage")
    String getStorageUsage();

    String getUptime();

    @JsonProperty("last_connection")
    LocalDateTime getLastConnection();

    @JsonProperty("os_version")
    String getOsVersion();

    @JsonProperty("app_version")
    String getAppVersion();

    @JsonProperty("en_stock")
    Boolean getEnStock();
}