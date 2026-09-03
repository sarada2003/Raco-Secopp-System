package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.dto.SuscripcionRequestDTO;
import co.edu.unbosque.sistemawebraco.entity.Notificacion;
import co.edu.unbosque.sistemawebraco.entity.SuscripcionAlerta;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.NotificacionRepository;
import co.edu.unbosque.sistemawebraco.repository.SuscripcionAlertaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private SuscripcionAlertaRepository suscripcionRepository;

    private NotificacionService notificacionService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        notificacionService = new NotificacionService(notificacionRepository, suscripcionRepository, new ObjectMapper());
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("dueño@correo.com");
    }

    // --- marcarLeida: ownership ---

    @Test
    void marcarLeidaMarcaLaNotificacionDelDueño() {
        Notificacion notificacion = new Notificacion();
        notificacion.setId(10L);
        notificacion.setUsuario(usuario);
        notificacion.setLeida(false);
        when(notificacionRepository.findByIdAndUsuarioId(10L, 1L)).thenReturn(Optional.of(notificacion));

        notificacionService.marcarLeida(10L, 1L);

        assertThat(notificacion.isLeida()).isTrue();
        verify(notificacionRepository).save(notificacion);
    }

    @Test
    void marcarLeidaNoGuardaSiYaEstabaLeida() {
        Notificacion notificacion = new Notificacion();
        notificacion.setId(10L);
        notificacion.setLeida(true);
        when(notificacionRepository.findByIdAndUsuarioId(10L, 1L)).thenReturn(Optional.of(notificacion));

        notificacionService.marcarLeida(10L, 1L);

        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void marcarLeidaDeNotificacionDeOtroUsuarioResponde404() {
        // findByIdAndUsuarioId ya filtra por dueño en la query: si el id es de otro usuario,
        // el repositorio devuelve vacío exactamente igual que si el id no existiera.
        when(notificacionRepository.findByIdAndUsuarioId(10L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.marcarLeida(10L, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    // --- eliminarSuscripcion: ownership ---

    @Test
    void eliminarSuscripcionDesactivaLaDelDueño() {
        SuscripcionAlerta suscripcion = new SuscripcionAlerta();
        suscripcion.setId(5L);
        suscripcion.setActivo(true);
        when(suscripcionRepository.findByIdAndUsuarioId(5L, 1L)).thenReturn(Optional.of(suscripcion));

        notificacionService.eliminarSuscripcion(5L, 1L);

        assertThat(suscripcion.isActivo()).isFalse();
        verify(suscripcionRepository).save(suscripcion);
    }

    @Test
    void eliminarSuscripcionDeOtroUsuarioResponde404() {
        when(suscripcionRepository.findByIdAndUsuarioId(5L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.eliminarSuscripcion(5L, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));

        verify(suscripcionRepository, never()).save(any());
    }

    // --- crearSuscripcion: deduplicación ---

    @Test
    void crearSuscripcionInsertaCuandoNoHayUnaIdenticaActiva() {
        SuscripcionRequestDTO request = new SuscripcionRequestDTO();
        request.setFrecuencia("diario");
        when(suscripcionRepository.findByUsuarioIdAndCriterioAndActivoTrue(eq(1L), any())).thenReturn(Optional.empty());
        when(suscripcionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuscripcionAlerta resultado = notificacionService.crearSuscripcion(usuario, request);

        ArgumentCaptor<SuscripcionAlerta> captor = ArgumentCaptor.forClass(SuscripcionAlerta.class);
        verify(suscripcionRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);
        assertThat(captor.getValue().isActivo()).isTrue();
        assertThat(resultado).isEqualTo(captor.getValue());
    }

    @Test
    void crearSuscripcionDuplicadaDevuelveLaExistenteSinInsertarOtra() {
        SuscripcionRequestDTO request = new SuscripcionRequestDTO();
        request.setFrecuencia("diario");
        SuscripcionAlerta existente = new SuscripcionAlerta();
        existente.setId(42L);
        when(suscripcionRepository.findByUsuarioIdAndCriterioAndActivoTrue(eq(1L), any())).thenReturn(Optional.of(existente));

        SuscripcionAlerta resultado = notificacionService.crearSuscripcion(usuario, request);

        assertThat(resultado).isSameAs(existente);
        verify(suscripcionRepository, never()).save(any());
    }

    @Test
    void crearSuscripcionNormalizaFrecuenciaSemanal() {
        SuscripcionRequestDTO request = new SuscripcionRequestDTO();
        request.setFrecuencia("SEMANAL");
        when(suscripcionRepository.findByUsuarioIdAndCriterioAndActivoTrue(anyLong(), any())).thenReturn(Optional.empty());
        when(suscripcionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuscripcionAlerta resultado = notificacionService.crearSuscripcion(usuario, request);

        assertThat(resultado.getFrecuencia()).isEqualTo("semanal");
    }

    @Test
    void crearSuscripcionConFrecuenciaDesconocidaCaeADiario() {
        SuscripcionRequestDTO request = new SuscripcionRequestDTO();
        request.setFrecuencia("mensual");
        when(suscripcionRepository.findByUsuarioIdAndCriterioAndActivoTrue(anyLong(), any())).thenReturn(Optional.empty());
        when(suscripcionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuscripcionAlerta resultado = notificacionService.crearSuscripcion(usuario, request);

        assertThat(resultado.getFrecuencia()).isEqualTo("diario");
    }
}
