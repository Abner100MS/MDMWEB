package repository;

import Entidad.MdmWallpaperAsignacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MdmWallpaperAsignacionRepository
        extends JpaRepository<MdmWallpaperAsignacion, Long> {

    // =========================================
    // ASIGNACIONES DE UN WALLPAPER
    // =========================================

    List<MdmWallpaperAsignacion> findByWallpaperIdOrderByFechaAsignacionDesc(
            Long wallpaperId);

    // =========================================
    // ASIGNACIONES ACTIVAS DE UN WALLPAPER
    // =========================================

    List<MdmWallpaperAsignacion> findByWallpaperIdAndActivoTrue(
            Long wallpaperId);

    // =========================================
    // BUSCAR POR TIPO DE DESTINO
    // TODAS / PLANTA / CATEGORIA / DISPOSITIVOS
    // =========================================

    List<MdmWallpaperAsignacion> findByWallpaperIdAndTipoDestinoAndActivoTrue(
            Long wallpaperId,
            String tipoDestino);

    // =========================================
    // BUSCAR DESTINO ESPECÍFICO
    // Ejemplo: PLANTA = PC
    // =========================================

    List<MdmWallpaperAsignacion> findByWallpaperIdAndTipoDestinoAndValorDestinoAndActivoTrue(
            Long wallpaperId,
            String tipoDestino,
            String valorDestino);
}