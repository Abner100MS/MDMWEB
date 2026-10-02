package Entidad;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mdm_wallpaper_asignaciones")
public class MdmWallpaperAsignacion {

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
    // TIPO DE DESTINO
    // TODAS / PLANTA / CATEGORIA / DISPOSITIVOS
    // =========================================

    @Column(name = "tipo_destino", nullable = false, length = 30)
    private String tipoDestino;

    // =========================================
    // VALOR DEL DESTINO
    //
    // TODAS -> null
    // PLANTA -> PC / PF
    // CATEGORIA -> GENERAL / CELULAR / etc.
    // DISPOSITIVOS -> puede quedar null porque
    // los equipos concretos estarán
    // en mdm_wallpaper_dispositivos
    // =========================================

    @Column(name = "valor_destino", length = 150)
    private String valorDestino;

    // =========================================
    // FECHA
    // =========================================

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    // =========================================
    // USUARIO QUE REALIZÓ LA ASIGNACIÓN
    // =========================================

    @Column(name = "asignado_por", length = 100)
    private String asignadoPor;

    // =========================================
    // ACTIVO
    // =========================================

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // =========================================
    // PRE PERSIST
    // =========================================

    @PrePersist
    protected void prePersist() {

        if (fechaAsignacion == null) {
            fechaAsignacion = LocalDateTime.now();
        }

        if (activo == null) {
            activo = true;
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

    public String getTipoDestino() {
        return tipoDestino;
    }

    public void setTipoDestino(String tipoDestino) {
        this.tipoDestino = tipoDestino;
    }

    public String getValorDestino() {
        return valorDestino;
    }

    public void setValorDestino(String valorDestino) {
        this.valorDestino = valorDestino;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(
            LocalDateTime fechaAsignacion) {

        this.fechaAsignacion = fechaAsignacion;
    }

    public String getAsignadoPor() {
        return asignadoPor;
    }

    public void setAsignadoPor(String asignadoPor) {
        this.asignadoPor = asignadoPor;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}