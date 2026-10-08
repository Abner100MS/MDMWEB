
package Entidad;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "historial_wifi", schema = "monitoreo tablet")
@Data
public class WifiIncidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evento_id", nullable = false, unique = true)
    private String eventoId;

    @Column(name = "activo", nullable = false)
    private String activo;

    @Column(name = "ssid")
    private String ssid;

    @Column(name = "fecha_perdida", nullable = false)
    private Long fechaPerdida;

    @Column(name = "fecha_confirmacion", nullable = false)
    private Long fechaConfirmacion;

    @Column(name = "fecha_recuperacion", nullable = false)
    private Long fechaRecuperacion;

    @Column(name = "duracion_segundos", nullable = false)
    private Long duracionSegundos;

    @Column(name = "wifi_habilitado")
    private Boolean wifiHabilitado;

    @Column(name = "transporte_wifi")
    private Boolean transporteWifi;

    @Column(name = "internet_validado")
    private Boolean internetValidado;

}
