package co.edu.unbosque.sistemawebraco.services;

import co.edu.unbosque.sistemawebraco.dto.ContractSearchRequest;
import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import co.edu.unbosque.sistemawebraco.entity.Informe;
import co.edu.unbosque.sistemawebraco.entity.Usuario;
import co.edu.unbosque.sistemawebraco.repository.InformeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InformeServiceTest {

    @Mock
    private InformeRepository informeRepository;

    private InformeService informeService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        informeService = new InformeService(informeRepository, new ObjectMapper());
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("dueño@correo.com");
    }

    @Test
    void registrarBusquedaGuardaUnInformeConElOrigenIndicado() {
        ContractSearchRequest criterio = new ContractSearchRequest();
        criterio.setPalabraClave("obras");
        SecopContratoDTO contrato = new SecopContratoDTO();

        informeService.registrarBusquedaManual(usuario, criterio, List.of(contrato));

        ArgumentCaptor<Informe> captor = ArgumentCaptor.forClass(Informe.class);
        verify(informeRepository).save(captor.capture());
        assertThat(captor.getValue().getOrigen()).isEqualTo("MANUAL");
        assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);
        assertThat(captor.getValue().getCantidadResultados()).isEqualTo(1);
        assertThat(captor.getValue().getNombre()).contains("obras");
    }

    @Test
    void registrarBusquedaChatbotUsaOrigenChatbot() {
        informeService.registrarBusquedaChatbot(usuario, new ContractSearchRequest(), List.of());

        ArgumentCaptor<Informe> captor = ArgumentCaptor.forClass(Informe.class);
        verify(informeRepository).save(captor.capture());
        assertThat(captor.getValue().getOrigen()).isEqualTo("CHATBOT");
        assertThat(captor.getValue().getCantidadResultados()).isEqualTo(0);
    }

    @Test
    void registrarBusquedaSinUsuarioNoGuardaNada() {
        informeService.registrarBusquedaManual(null, new ContractSearchRequest(), List.of());

        verify(informeRepository, never()).save(any());
    }

    @Test
    void registrarBusquedaNuncaPropagaExcepcionAunqueFalleElGuardado() {
        when(informeRepository.save(any())).thenThrow(new RuntimeException("DB caída"));

        // No debe lanzar: un fallo al guardar el historial no debe tumbar la búsqueda ya respondida.
        informeService.registrarBusquedaManual(usuario, new ContractSearchRequest(), List.of());
    }

    @Test
    void obtenerDevuelveElDetalleDelDueño() {
        Informe informe = new Informe();
        informe.setId(7L);
        informe.setUsuario(usuario);
        informe.setNombre("Búsqueda: obras");
        informe.setCriterios("{\"palabraClave\":\"obras\"}");
        informe.setResultados("[]");
        informe.setFecha(LocalDateTime.now());
        when(informeRepository.findByIdAndUsuarioId(7L, 1L)).thenReturn(Optional.of(informe));

        var detalle = informeService.obtener(7L, 1L);

        assertThat(detalle.getId()).isEqualTo(7L);
        assertThat(detalle.getCriterio().getPalabraClave()).isEqualTo("obras");
        assertThat(detalle.getResultados()).isEmpty();
    }

    @Test
    void obtenerDeInformeDeOtroUsuarioResponde404() {
        when(informeRepository.findByIdAndUsuarioId(7L, 999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> informeService.obtener(7L, 999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void obtenerConCriterioJsonMalformadoNoLanzaYCaeADefaults() {
        Informe informe = new Informe();
        informe.setId(7L);
        informe.setNombre("x");
        informe.setCriterios("{esto-no-es-json-valido");
        informe.setResultados(null);
        informe.setFecha(LocalDateTime.now());
        when(informeRepository.findByIdAndUsuarioId(7L, 1L)).thenReturn(Optional.of(informe));

        var detalle = informeService.obtener(7L, 1L);

        assertThat(detalle.getCriterio()).isNotNull();
        assertThat(detalle.getResultados()).isEmpty();
    }
}
