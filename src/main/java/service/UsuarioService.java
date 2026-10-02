package service;

import Dto.LoginResponseDTO;
import Dto.UsuarioDTO;
import Entidad.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioDTO crear(UsuarioDTO dto) {

        if (usuarioRepository.existsByUsuario(dto.getUsuario())) {
            throw new RuntimeException("El usuario ya existe.");
        }

        Usuario usuario = new Usuario();

        usuario.setUsuario(dto.getUsuario());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setNombre(dto.getNombre());
        usuario.setRol(dto.getRol());
        usuario.setActivo(dto.getActivo() == null ? true : dto.getActivo());

        usuario = usuarioRepository.save(usuario);

        return convertirDTO(usuario);
    }

    public UsuarioDTO actualizar(Long id, UsuarioDTO dto) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        usuario.setNombre(dto.getNombre());
        usuario.setRol(dto.getRol());
        usuario.setActivo(dto.getActivo());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        usuario.setFechaModificacion(LocalDateTime.now());

        usuario = usuarioRepository.save(usuario);

        return convertirDTO(usuario);
    }

    public void eliminar(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        usuarioRepository.delete(usuario);

    }

    @Transactional(readOnly = true)
    public UsuarioDTO obtenerPorId(Long id) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        return convertirDTO(usuario);

    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listar() {

        return usuarioRepository.findAll()
                .stream()
                .map(this::convertirDTO)
                .collect(Collectors.toList());

    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(String usuario, String password) {

        Usuario user = usuarioRepository.findByUsuarioAndActivoTrue(usuario)
                .orElseThrow(() -> new RuntimeException("Usuario o contraseña incorrectos."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Usuario o contraseña incorrectos.");
        }

        LoginResponseDTO response = new LoginResponseDTO();

        response.setId(user.getId());
        response.setUsuario(user.getUsuario());
        response.setNombre(user.getNombre());
        response.setRol(user.getRol());
        response.setActivo(user.getActivo());

        return response;

    }

    @Transactional(readOnly = true)
    public Usuario obtenerUsuarioActivo(String usuario) {

        return usuarioRepository.findByUsuarioAndActivoTrue(usuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado o inactivo."));
    }

    private UsuarioDTO convertirDTO(Usuario usuario) {

        UsuarioDTO dto = new UsuarioDTO();

        dto.setId(usuario.getId());
        dto.setUsuario(usuario.getUsuario());
        dto.setNombre(usuario.getNombre());
        dto.setRol(usuario.getRol());
        dto.setActivo(usuario.getActivo());
        dto.setFechaCreacion(usuario.getFechaCreacion());
        dto.setFechaModificacion(usuario.getFechaModificacion());

        return dto;

    }

}