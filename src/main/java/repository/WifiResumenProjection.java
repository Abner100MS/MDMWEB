package repository;

public interface WifiResumenProjection {

    String getActivo();

    String getSsid();

    Long getFechaPerdida();

    Long getFechaRecuperacion();

    Long getDuracionSegundos();

    Boolean getWifiHabilitado();

    Boolean getTransporteWifi();

    Boolean getInternetValidado();

    Long getTotalRegistros();
}