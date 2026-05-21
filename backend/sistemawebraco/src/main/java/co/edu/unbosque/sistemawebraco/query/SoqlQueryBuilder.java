package co.edu.unbosque.sistemawebraco.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SoqlQueryBuilder {

    private final List<String> conditions = new ArrayList<>();
    private final List<String> selectFields = new ArrayList<>();
    private String fullTextSearch;
    private String orderBy;
    private Integer limit;
    private Integer offset;

    /** $q — full-text search en todos los campos indexados del dataset. */
    public SoqlQueryBuilder fullText(String value) {
        if (value != null && !value.isBlank()) {
            fullTextSearch = sanitize(value.trim());
        }
        return this;
    }

    /** UPPER(field) LIKE '%VALUE%' — búsqueda parcial insensible a mayúsculas. */
    public SoqlQueryBuilder upperLike(String field, String value) {
        if (value != null && !value.isBlank()) {
            conditions.add("UPPER(" + field + ") LIKE '%" + sanitize(value.trim().toUpperCase()) + "%'");
        }
        return this;
    }

    /** field = 'value' — igualdad exacta para selects. */
    public SoqlQueryBuilder equals(String field, String value) {
        if (value != null && !value.isBlank()) {
            conditions.add(field + " = '" + sanitize(value) + "'");
        }
        return this;
    }

    /** field >= value — valor mínimo numérico. */
    public SoqlQueryBuilder min(String field, BigDecimal value) {
        if (value != null) {
            conditions.add(field + " >= " + value.toPlainString());
        }
        return this;
    }

    /** field <= value — valor máximo numérico. */
    public SoqlQueryBuilder max(String field, BigDecimal value) {
        if (value != null) {
            conditions.add(field + " <= " + value.toPlainString());
        }
        return this;
    }

    /** Rango de fechas en formato ISO 8601 requerido por Floating Timestamp de Socrata. */
    public SoqlQueryBuilder dateBetween(String field, LocalDate start, LocalDate end) {
        if (start != null) {
            conditions.add(field + " >= '" + start + "T00:00:00'");
        }
        if (end != null) {
            conditions.add(field + " <= '" + end + "T23:59:59'");
        }
        return this;
    }

    public SoqlQueryBuilder select(String... fields) {
        selectFields.addAll(List.of(fields));
        return this;
    }

    public SoqlQueryBuilder orderBy(String field, String direction) {
        if (field != null && direction != null) {
            this.orderBy = field + " " + direction;
        }
        return this;
    }

    public SoqlQueryBuilder limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public SoqlQueryBuilder offset(Integer offset) {
        this.offset = offset;
        return this;
    }

    // --- Getters para uso con UriBuilder de WebClient ---

    public String getWhereClause() {
        if (conditions.isEmpty()) return null;
        return conditions.stream().collect(Collectors.joining(" AND "));
    }

    public String getFullTextSearch() { return fullTextSearch; }

    public String getOrderBy() { return orderBy; }

    public List<String> getSelectFields() { return selectFields; }

    public boolean hasConditions() { return !conditions.isEmpty(); }

    private String sanitize(String input) {
        return input.replace("'", "''");
    }
}
