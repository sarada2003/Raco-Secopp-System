package co.edu.unbosque.sistemawebraco.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Deserializador para el campo {@code urlproceso} de la API SECOP II.
 *
 * <p>La API de datos.gov.co retorna este campo como un objeto JSON
 * {@code {"url": "https://...", "description": "..."}} en lugar de un String plano.
 * Este deserializador extrae únicamente el valor del campo {@code url}.</p>
 */
public class UrlProcesoDeserializer extends StdDeserializer<String> {

    public UrlProcesoDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        // Si ya es un String plano, devolverlo directamente
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return p.getText();
        }

        // Si es un objeto { "url": "...", "description": "..." }
        if (p.currentToken() == JsonToken.START_OBJECT) {
            String url = null;
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = p.currentName();
                p.nextToken();
                if ("url".equals(fieldName)) {
                    url = p.getText();
                } else {
                    p.skipChildren();
                }
            }
            return url;
        }

        // Para cualquier otro token, saltar y retornar null
        p.skipChildren();
        return null;
    }
}
