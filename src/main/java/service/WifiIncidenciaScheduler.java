
package service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class WifiIncidenciaScheduler {

    private final WifiIncidenciaService wifiService;

    private static final ZoneId ZONA_GUATEMALA = ZoneId.of("America/Guatemala");

    public WifiIncidenciaScheduler(
            WifiIncidenciaService wifiService) {

        this.wifiService = wifiService;
    }

    // =====================================================
    // LIMPIEZA AUTOMATICA CADA DOS MESES
    // =====================================================
    // Se ejecuta el dia 1 de febrero, abril, junio,
    // agosto, octubre y diciembre a las 02:00 AM.
    // Conserva solamente los registros del mes actual.
    // =====================================================

    @Scheduled(cron = "0 0 2 1 2,4,6,8,10,12 *", zone = "America/Guatemala")
    public void limpiarHistorialWifi() {

        LocalDate inicioMesActual = LocalDate.now(ZONA_GUATEMALA)
                .withDayOfMonth(1);

        int eliminados = wifiService.eliminarAnterioresA(
                inicioMesActual);

        System.out.println(
                "[WIFI] Limpieza automatica completada. "
                        + "Registros eliminados: " + eliminados);
    }
}
