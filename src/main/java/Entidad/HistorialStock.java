package Entidad;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_stock", schema = "monitoreo tablet", uniqueConstraints = {
        @UniqueConstraint(columnNames = "activo")
})
public class HistorialStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String activo;

    // =====================================================
    // MOVIMIENTO ACTUAL
    // =====================================================

    @Column(name = "accion_actual", nullable = false, length = 50)
    private String accionActual;

    @Column(name = "condicion_actual", length = 30)
    private String condicionActual;

    @Column(name = "motivo_actual", nullable = false, columnDefinition = "TEXT")
    private String motivoActual;

    @Column(name = "usuario_actual", nullable = false, length = 150)
    private String usuarioActual;

    @Column(name = "fecha_actual", nullable = false)
    private LocalDateTime fechaActual;

    // =====================================================
    // MOVIMIENTO ANTERIOR
    // =====================================================

    @Column(name = "accion_anterior", length = 50)
    private String accionAnterior;

    @Column(name = "condicion_anterior", length = 30)
    private String condicionAnterior;

    @Column(name = "motivo_anterior", columnDefinition = "TEXT")
    private String motivoAnterior;

    @Column(name = "usuario_anterior", length = 150)
    private String usuarioAnterior;

    @Column(name = "fecha_anterior")
    private LocalDateTime fechaAnterior;

    // =====================================================
    // FECHA AUTOMÁTICA
    // =====================================================

    @PrePersist
    protected void onCreate() {
        if (fechaActual == null) {
            fechaActual = LocalDateTime.now();
        }
    }

    // =====================================================
    // GETTERS Y SETTERS
    // =====================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActivo() {
        return activo;
    }

    public void setActivo(String activo) {
        this.activo = activo;
    }

    public String getAccionActual() {
        return accionActual;
    }

    public void setAccionActual(String accionActual) {
        this.accionActual = accionActual;
    }

    public String getCondicionActual() {
        return condicionActual;
    }

    public void setCondicionActual(String condicionActual) {
        this.condicionActual = condicionActual;
    }

    public String getMotivoActual() {
        return motivoActual;
    }

    public void setMotivoActual(String motivoActual) {
        this.motivoActual = motivoActual;
    }

    public String getUsuarioActual() {
        return usuarioActual;
    }

    public void setUsuarioActual(String usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    public LocalDateTime getFechaActual() {
        return fechaActual;
    }

    public void setFechaActual(LocalDateTime fechaActual) {
        this.fechaActual = fechaActual;
    }

    public String getAccionAnterior() {
        return accionAnterior;
    }

    public void setAccionAnterior(String accionAnterior) {
        this.accionAnterior = accionAnterior;
    }

    public String getCondicionAnterior() {
        return condicionAnterior;
    }

    public void setCondicionAnterior(String condicionAnterior) {
        this.condicionAnterior = condicionAnterior;
    }

    public String getMotivoAnterior() {
        return motivoAnterior;
    }

    public void setMotivoAnterior(String motivoAnterior) {
        this.motivoAnterior = motivoAnterior;
    }

    public String getUsuarioAnterior() {
        return usuarioAnterior;
    }

    public void setUsuarioAnterior(String usuarioAnterior) {
        this.usuarioAnterior = usuarioAnterior;
    }

    public LocalDateTime getFechaAnterior() {
        return fechaAnterior;
    }

    public void setFechaAnterior(LocalDateTime fechaAnterior) {
        this.fechaAnterior = fechaAnterior;
    }
}