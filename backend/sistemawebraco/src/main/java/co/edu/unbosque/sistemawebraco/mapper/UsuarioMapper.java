package co.edu.unbosque.sistemawebraco.mapper;

import co.edu.unbosque.sistemawebraco.dto.UsuarioDTO;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UsuarioMapper {

    public static Usuario toEntity(UsuarioDTO dto) {
        if (dto == null) return null;

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.getNombre());
        usuario.setEmail(dto.getEmail());
        usuario.setActivo(true);
        usuario.setFechaRegistro(LocalDateTime.now());
        return usuario;
    }

    public static UsuarioDTO toDTO(Usuario usuario) {
        if (usuario == null) return null;

        return UsuarioDTO.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .activo(usuario.isActivo())
                .fechaRegistro(usuario.getFechaRegistro())
                .build();
    }








}
