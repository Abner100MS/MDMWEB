package Dto;

import java.util.List;

public class RestriccionInstalacionMasivaRequest {

    private List<String> activos;
    private boolean bloquear;

    public List<String> getActivos() {
        return activos;
    }

    public void setActivos(List<String> activos) {
        this.activos = activos;
    }

    public boolean isBloquear() {
        return bloquear;
    }

    public void setBloquear(boolean bloquear) {
        this.bloquear = bloquear;
    }
}