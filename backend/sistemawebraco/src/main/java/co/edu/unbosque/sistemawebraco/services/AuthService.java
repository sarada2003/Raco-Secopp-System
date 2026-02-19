package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.dto.UsuarioDTO;
import co.edu.unbosque.sistemawebraco.entity.LoginRequestDTO;
import co.edu.unbosque.sistemawebraco.entity.LoginResponseDTO;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.mapper.UsuarioMapper;
import co.edu.unbosque.sistemawebraco.repository.UsuarioRepository;
import co.edu.unbosque.sistemawebraco.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioMapper usuarioMapper;



    public LoginResponseDTO login(LoginRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtUtil.generateToken(usuario.getEmail());
        UsuarioDTO dto = usuarioMapper.toDTO(usuario);

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setUsuario(dto);

        return response;
    }



    public UsuarioDTO register(UsuarioDTO dto) {

        // Validar email único
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Convertir DTO → Entity
        Usuario usuario = usuarioMapper.toEntity(dto);

        // Encriptar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        // Setear fecha de registro
        usuario.setFechaRegistro(LocalDateTime.now());

        // Guardar
        Usuario guardado = usuarioRepository.save(usuario);

        // Devolver usuario sin contraseña
        return usuarioMapper.toDTO(guardado);
    }



    // =========================================================
    // OBTENER DATOS DEL USUARIO DESDE TOKEN
    // =========================================================
    public UsuarioDTO obtenerUsuarioDesdeToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token no válido");
        }

        String jwt = token.substring(7);
        String email = jwtUtil.extractUsername(jwt);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return usuarioMapper.toDTO(usuario);
    }



    // =========================================================
    // LOGIN CON GOOGLE
    // =========================================================
    public LoginResponseDTO loginConGoogle(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no registrado con ese correo"));

        String token = jwtUtil.generateToken(usuario.getEmail());

        return new LoginResponseDTO();
    }
}
