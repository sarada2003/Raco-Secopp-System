package co.edu.unbosque.sistemawebraco.query;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SoqlQueryBuilderTest {

    @Test
    void sinCondicionesNoHayWhereClause() {
        SoqlQueryBuilder builder = new SoqlQueryBuilder();

        assertThat(builder.hasConditions()).isFalse();
        assertThat(builder.getWhereClause()).isNull();
        assertThat(builder.getFullTextSearch()).isNull();
    }

    @Test
    void fullTextIgnoraValoresNulosOBlancos() {
        assertThat(new SoqlQueryBuilder().fullText(null).getFullTextSearch()).isNull();
        assertThat(new SoqlQueryBuilder().fullText("   ").getFullTextSearch()).isNull();
        assertThat(new SoqlQueryBuilder().fullText("  obras  ").getFullTextSearch()).isEqualTo("obras");
    }

    @Test
    void equalsArmaCondicionDeIgualdadExacta() {
        SoqlQueryBuilder builder = new SoqlQueryBuilder().equals("departamento_entidad", "Cundinamarca");

        assertThat(builder.hasConditions()).isTrue();
        assertThat(builder.getWhereClause()).isEqualTo("departamento_entidad = 'Cundinamarca'");
    }

    @Test
    void equalsIgnoraValoresNulosOBlancos() {
        SoqlQueryBuilder builder = new SoqlQueryBuilder().equals("estado_del_procedimiento", null).equals("modalidad_de_contratacion", "");

        assertThat(builder.hasConditions()).isFalse();
    }

    @Test
    void minYMaxArmanRangoNumerico() {
        SoqlQueryBuilder builder = new SoqlQueryBuilder()
                .min("valor_total_adjudicacion", new BigDecimal("50000000"))
                .max("valor_total_adjudicacion", new BigDecimal("500000000"));

        assertThat(builder.getWhereClause())
                .isEqualTo("valor_total_adjudicacion >= 50000000 AND valor_total_adjudicacion <= 500000000");
    }

    @Test
    void minYMaxIgnoranNull() {
        SoqlQueryBuilder builder = new SoqlQueryBuilder().min("valor_total_adjudicacion", null).max("valor_total_adjudicacion", null);

        assertThat(builder.hasConditions()).isFalse();
    }

    @Test
    void dateBetweenArmaRangoConHoraFijaDeInicioYFin() {
        SoqlQueryBuilder builder = new SoqlQueryBuilder()
                .dateBetween("fecha_de_publicacion_del", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        assertThat(builder.getWhereClause())
                .isEqualTo("fecha_de_publicacion_del >= '2024-01-01T00:00:00' AND fecha_de_publicacion_del <= '2024-12-31T23:59:59'");
    }

    @Test
    void dateBetweenAceptaSoloUnExtremo() {
        assertThat(new SoqlQueryBuilder().dateBetween("f", LocalDate.of(2024, 1, 1), null).getWhereClause())
                .isEqualTo("f >= '2024-01-01T00:00:00'");
        assertThat(new SoqlQueryBuilder().dateBetween("f", null, LocalDate.of(2024, 12, 31)).getWhereClause())
                .isEqualTo("f <= '2024-12-31T23:59:59'");
    }

    @Test
    void multiplesCondicionesSeUnenConAnd() {
        SoqlQueryBuilder builder = new SoqlQueryBuilder()
                .equals("departamento_entidad", "Cundinamarca")
                .equals("estado_del_procedimiento", "activo")
                .min("valor_total_adjudicacion", new BigDecimal("1000"));

        assertThat(builder.getWhereClause())
                .isEqualTo("departamento_entidad = 'Cundinamarca' AND estado_del_procedimiento = 'activo' AND valor_total_adjudicacion >= 1000");
    }

    @Test
    void sanitizeEscapaComillasSimplesParaEvitarInyeccionSoql() {
        // O'Brien -> O''Brien: una comilla simple sin escapar cerraría la condición SoQL
        // antes de tiempo y permitiría inyectar una cláusula adicional.
        SoqlQueryBuilder builder = new SoqlQueryBuilder().equals("entidad", "O'Brien");

        assertThat(builder.getWhereClause()).isEqualTo("entidad = 'O''Brien'");
    }

    @Test
    void sanitizeEscapaComillasEnFullText() {
        SoqlQueryBuilder builder = new SoqlQueryBuilder().fullText("100' OR '1'='1");

        assertThat(builder.getFullTextSearch()).isEqualTo("100'' OR ''1''=''1");
    }

    @Test
    void upperLikeArmaBusquedaParcialInsensibleAMayusculas() {
        SoqlQueryBuilder builder = new SoqlQueryBuilder().upperLike("entidad", " ministerio ");

        assertThat(builder.getWhereClause()).isEqualTo("UPPER(entidad) LIKE '%MINISTERIO%'");
    }
}
