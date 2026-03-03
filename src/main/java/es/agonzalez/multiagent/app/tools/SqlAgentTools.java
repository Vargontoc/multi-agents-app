package es.agonzalez.multiagent.app.tools;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.Tool;

@Component
public class SqlAgentTools {

    private final JdbcTemplate jdbcTemplate;

    public SqlAgentTools(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Tool("Ejecuta consultas SQL de solo lectura para obtener estadísticas de ventas")
    public String executeQuery(String sql) {
        if (!sql.trim().toLowerCase().startsWith("select")) {
            return "Error: Solo se permiten consultas de lectura (SELECT).";
        }

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            return results.toString();
        } catch (Exception e) {
            return "Error al ejecutar SQL: " + e.getMessage();
        }
    }

    @Tool("Obtiene el esquema de las tablas de la base de datos para saber el DDL de estas")
    public String getDatabaseSchema() {
        return "Tablas disponibles:";
    }

}
