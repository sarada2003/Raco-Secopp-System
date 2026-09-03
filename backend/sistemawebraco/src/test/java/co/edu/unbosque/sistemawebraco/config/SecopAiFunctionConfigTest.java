package co.edu.unbosque.sistemawebraco.config;

import co.edu.unbosque.sistemawebraco.dto.SecopContratoDTO;
import co.edu.unbosque.sistemawebraco.dto.SecopSearchInput;
import co.edu.unbosque.sistemawebraco.services.InformeService;
import co.edu.unbosque.sistemawebraco.services.SecopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cubre la herramienta ('tool') que el modelo invoca vía function-calling — el corazón
 * del "agente" del chatbot. Se invoca la función directamente (sin levantar Spring AI ni
 * el ChatClient), igual que Spring AI la ejecuta internamente una vez el modelo decide
 * llamarla.
 */
@ExtendWith(MockitoExtension.class)
class SecopAiFunctionConfigTest {

    @Mock
    private SecopService secopService;
    @Mock
    private InformeService informeService;

    private Function<SecopSearchInput, String> buscarContratosSecop;

    @BeforeEach
    void setUp() {
        buscarContratosSecop = new SecopAiFunctionConfig().buscarContratosSecop(secopService, informeService);
    }

    private SecopContratoDTO contrato(String nombre, String entidad, BigDecimal valor) {
        SecopContratoDTO c = new SecopContratoDTO();
        c.setNombreProcedimiento(nombre);
        c.setEntidad(entidad);
        c.setValor(valor);
        return c;
    }

    @Test
    void conResultadosDevuelveUnResumenLegibleConLaCantidad() {
        when(secopService.buscarContratos(any())).thenReturn(List.of(
                contrato("Consultoría técnica", "Alcaldía de Bogotá", new BigDecimal("100000000"))
        ));

        String respuesta = buscarContratosSecop.apply(new SecopSearchInput("consultoría", null, null));

        assertThat(respuesta).contains("Contratos encontrados en SECOP II: 1");
        assertThat(respuesta).contains("Consultoría técnica");
        assertThat(respuesta).contains("Alcaldía de Bogotá");
    }

    @Test
    void sinResultadosDevuelveMensajeDeNoEncontrado() {
        when(secopService.buscarContratos(any())).thenReturn(List.of());

        String respuesta = buscarContratosSecop.apply(new SecopSearchInput(null, null, null));

        assertThat(respuesta).isEqualTo("No se encontraron contratos con los filtros indicados en SECOP II.");
    }

    @Test
    void timeoutDeSecopDevuelveMensajeAmigableSinPropagarLaExcepcion() {
        when(secopService.buscarContratos(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "timeout"));

        String respuesta = buscarContratosSecop.apply(new SecopSearchInput("obras", null, null));

        assertThat(respuesta).contains("No pude consultar SECOP II en este momento");
    }

    @Test
    void errorInesperadoDevuelveMensajeDeErrorSinPropagarLaExcepcion() {
        when(secopService.buscarContratos(any())).thenThrow(new RuntimeException("boom"));

        String respuesta = buscarContratosSecop.apply(new SecopSearchInput("obras", null, null));

        assertThat(respuesta).contains("Error al consultar SECOP II");
        assertThat(respuesta).contains("boom");
    }

    @Test
    void registraLaBusquedaEnElHistorialDeInformes() {
        when(secopService.buscarContratos(any())).thenReturn(List.of(contrato("X", "Y", BigDecimal.TEN)));

        buscarContratosSecop.apply(new SecopSearchInput("x", null, null));

        org.mockito.Mockito.verify(informeService).registrarBusquedaChatbot(any(), any(), any());
    }
}
