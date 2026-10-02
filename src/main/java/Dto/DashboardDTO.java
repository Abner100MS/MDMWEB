package Dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardDTO {

    private long total;
    private long offline;
    private long autorizados;
    private long noAutorizados;
    private long bateriaBaja;

    private long bateria80a100;

    private long bateria50a79;

    private long bateria20a49;

    private long bateria0a19;

    private long bateriaSinDatos;

    private long bateriasInfladas;

    private long nopowerCel;
    private long nopowerHand;
    private long nopowerTab;
    private long nopowerGen;

    private List<DashboardGraficaDTO> plantaGeneral;

    private List<DashboardGraficaDTO> plantaPc;

    private List<DashboardGraficaDTO> plantaPf;

    private List<DashboardGraficaDTO> categorias;

    private List<DashboardGraficaDTO> estadoRed;

}
