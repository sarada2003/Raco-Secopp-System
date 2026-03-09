package co.edu.unbosque.sistemawebraco.query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SoqlQueryBuilder {

    private final List<String> conditions = new ArrayList<>();
    private String orderBy;
    private Integer limit;
    private Integer offset;
    private List<String> selectFields = new ArrayList<>();


    public SoqlQueryBuilder like(String field, String value) {
        if (value != null && !value.isBlank()) {
            conditions.add("lower(" + field + ") like lower('%" + sanitize(value) + "%')");
        }
        return this;
    }

    public SoqlQueryBuilder equals(String field, String value) {
        if (value != null && !value.isBlank()) {
            conditions.add(field + " = '" + sanitize(value) + "'");
        }
        return this;
    }

    public SoqlQueryBuilder min(String field, BigDecimal value) {
        if (value != null) {
            conditions.add(field + " >= " + value);
        }
        return this;
    }

    public SoqlQueryBuilder max(String field, BigDecimal value) {
        if (value != null) {
            conditions.add(field + " <= " + value);
        }
        return this;
    }

    public SoqlQueryBuilder dateBetween(String field, LocalDate start, LocalDate end) {
        if (start != null) {
            conditions.add(field + " >= '" + start + "'");
        }
        if (end != null) {
            conditions.add(field + " <= '" + end + "'");
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

    public String build() {

        StringBuilder query = new StringBuilder();

        if (!selectFields.isEmpty()) {
            query.append("$select=")
                    .append(String.join(",", selectFields));
        }

        if (!conditions.isEmpty()) {
            if (query.length() > 0) query.append("&");
            query.append("$where=")
                    .append(conditions.stream().collect(Collectors.joining(" AND ")));
        }

        if (orderBy != null) {
            query.append("&$order=").append(orderBy);
        }

        if (limit != null) {
            query.append("&$limit=").append(limit);
        }

        if (offset != null) {
            query.append("&$offset=").append(offset);
        }

        return query.toString();
    }

    private String sanitize(String input) {
        return input.replace("'", "''");
    }
}