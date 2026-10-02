package repository;

import Entidad.MdmWallpaper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MdmWallpaperRepository
        extends JpaRepository<MdmWallpaper, Long> {

    List<MdmWallpaper> findByActivoTrueOrderByFechaCreacionDesc();
}
