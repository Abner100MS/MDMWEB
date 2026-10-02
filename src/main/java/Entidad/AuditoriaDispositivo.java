package Entidad;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "auditoria_dispositivos",
        schema = "monitoreo tablet",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_auditoria_activo_accion",
                        columnNames = {"activo", "accion"}
                )
        }
)
@Data
public class AuditoriaDispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activo", nullable = false)
    private String activo;

    @Column(name = "accion", nullable = false)
    private String accion;

    // ==========================
    // ÚLTIMA MODIFICACIÓN
    // ==========================

    @Column(name = "ultimo_usuario")
    private String ultimoUsuario;

    @Column(name = "ultima_fecha")
    private LocalDateTime ultimaFecha;

    @Column(name = "ultimo_detalle")
    private String ultimoDetalle;


    // ==========================
    // MODIFICACIÓN ANTERIOR
    // ==========================

    @Column(name = "anterior_usuario")
    private String anteriorUsuario;

    @Column(name = "anterior_fecha")
    private LocalDateTime anteriorFecha;

    @Column(name = "anterior_detalle")
    private String anteriorDetalle;
}