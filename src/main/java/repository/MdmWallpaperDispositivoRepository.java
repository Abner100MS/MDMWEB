package repository;

import Entidad.MdmWallpaperDispositivo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MdmWallpaperDispositivoRepository
        extends JpaRepository<MdmWallpaperDispositivo, Long> {

    // Buscar el registro de un wallpaper para una tablet
    Optional<MdmWallpaperDispositivo> findByWallpaperIdAndTabletId(
            Long wallpaperId,
            Long tabletId);

    // Obtener todos los dispositivos asignados a un wallpaper
    List<MdmWallpaperDispositivo> findByWallpaperId(Long wallpaperId);

    // Obtener dispositivos de un wallpaper según estado
    List<MdmWallpaperDispositivo> findByWallpaperIdAndEstado(
            Long wallpaperId,
            String estado);

    // Contadores para el dashboard
    long countByWallpaperId(Long wallpaperId);

    long countByWallpaperIdAndEstado(
            Long wallpaperId,
            String estado);
}