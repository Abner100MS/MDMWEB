package Entidad;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mdm_wallpaper_dispositivos", uniqueConstraints = {
        @UniqueConstraint(name = "uq_wallpaper_tablet", columnNames = { "wallpaper_id", "tablet_id" })
})
public class MdmWallpaperDispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================
    // WALLPAPER
    // =========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallpaper_id", nullable = false)
    private MdmWallpaper wallpaper;

    // =========================================
    // TABLET
    // =========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tablet_id", nullable = false)
    private Tablet tablet;

    // =========================================
    // ESTADO
    // =========================================

    @Column(name = "estado", nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    // =========================================
    // PRE PERSIST
    // =========================================

    @PrePersist
    protected void prePersist() {

        if (estado == null) {
            estado = "PENDIENTE";
        }

        if (intentos == null) {
            intentos = 0;
        }
    }

    // =========================================
    // GETTERS Y SETTERS
    // =========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public MdmWallpaper getWallpaper() {
        return wallpaper;
    }

    public void setWallpaper(MdmWallpaper wallpaper) {
        this.wallpaper = wallpaper;
    }

    public Tablet getTablet() {
        return tablet;
    }

    public void setTablet(Tablet tablet) {
        this.tablet = tablet;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public LocalDateTime getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public void setFechaConfirmacion(
            LocalDateTime fechaConfirmacion) {

        this.fechaConfirmacion = fechaConfirmacion;
    }

    public Integer getIntentos() {
        return intentos;
    }

    public void setIntentos(Integer intentos) {
        this.intentos = intentos;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public void setUltimoError(String ultimoError) {
        this.ultimoError = ultimoError;
    }
}