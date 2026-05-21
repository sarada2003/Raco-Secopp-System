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

/**
 * Servicio de autenticación y gestión de usuarios del sistema RACO–SECOPP.
 *
 * <p>Centraliza toda la lógica de negocio relacionada con el ciclo de vida de los usuarios:
 * registro, inicio de sesión manual, inicio de sesión con Google y consulta del perfil
 * a partir de un token JWT.</p>
 *
 * <p>Las contraseñas nunca se almacenan en texto plano; siempre se encriptan con
 * {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder BCrypt}
 * antes de persistir en la base de datos.</p>
 *
 * @author Proyecto de Grado — Universidad El Bosque
 */
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

    /**
     * Autentica a un usuario con email y contraseña.
     *
     * <p>Flujo interno:
     * <ol>
     *   <li>Busca el usuario por email en la base de datos.</li>
     *   <li>Verifica la contraseña contra el hash BCrypt almacenado.</li>
     *   <li>Genera un token JWT firmado con HS512 válido por 24 horas.</li>
     *   <li>Retorna el token junto con los datos públicos del usuario (sin contraseña).</li>
     * </ol>
     *
     * @param request credenciales del usuario ({@code email} y {@code password})
     * @return {@link LoginResponseDTO} con el token JWT y los datos del usuario
     * @throws RuntimeException si el email no existe o la contraseña no coincide
     */
    public LoginResponseDTO login(LoginRequestDTO request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        String token = jwtUtil.generateToken(usuario.getEmail());
        UsuarioDTO dto = usuarioMapper.toDTO(usuario);

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setUsuario(dto);

        return response;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * <p>Flujo interno:
     * <ol>
     *   <li>Valida que el email no esté ya registrado (unicidad).</li>
     *   <li>Convierte el {@link UsuarioDTO} a entidad {@link Usuario}.</li>
     *   <li>Encripta la contraseña con BCrypt y asigna la fecha de registro.</li>
     *   <li>Persiste el usuario en PostgreSQL.</li>
     *   <li>Retorna el usuario creado sin exponer la contraseña hasheada.</li>
     * </ol>
     *
     * @param dto datos del nuevo usuario, incluyendo {@code password} en texto plano
     * @return {@link UsuarioDTO} con los datos del usuario creado (sin contraseña)
     * @throws RuntimeException si el email ya está registrado
     */
    public UsuarioDTO register(UsuarioDTO dto) {
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        Usuario usuario = usuarioMapper.toEntity(dto);
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        usuario.setFechaRegistro(LocalDateTime.now());

        Usuario guardado = usuarioRepository.save(usuario);
        return usuarioMapper.toDTO(guardado);
    }

    /**
     * Obtiene los datos del usuario autenticado a partir del token JWT del header HTTP.
     *
     * <p>Espera el token en formato {@code Bearer <jwt>} (con el prefijo incluido),
     * tal como lo envía el frontend en el header {@code Authorization}.</p>
     *
     * @param token valor completo del header {@code Authorization}, ej: {@code "Bearer eyJhbGci..."}
     * @return {@link UsuarioDTO} con los datos públicos del usuario
     * @throws RuntimeException si el token es nulo, no comienza con "Bearer " o el usuario no existe
     */
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

    /**
     * Inicia sesión para un usuario autenticado previamente con Google (Firebase Auth).
     *
     * <p>Firebase valida las credenciales de Google en el frontend y envía el email
     * verificado a este endpoint. El sistema busca ese email en la base de datos y
     * genera un token JWT propio del sistema RACO si el usuario existe.</p>
     *
     * <p><b>Nota:</b> el usuario debe haberse registrado previamente con ese email
     * mediante {@link #register(UsuarioDTO)}.</p>
     *
     * @param email email verificado por Google/Firebase
     * @return {@link LoginResponseDTO} con el token JWT y los datos del usuario
     * @throws RuntimeException si el email no está registrado en el sistema
     */
    public LoginResponseDTO loginConGoogle(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no registrado con ese correo"));

        String token = jwtUtil.generateToken(usuario.getEmail());
        UsuarioDTO dto = usuarioMapper.toDTO(usuario);

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setUsuario(dto);
        return response;
    }
}
