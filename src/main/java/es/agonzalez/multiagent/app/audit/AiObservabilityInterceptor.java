package es.agonzalez.multiagent.app.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiObservabilityInterceptor {

    private final JdbcTemplate jdbcTemplate;

    public AiObservabilityInterceptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void logInteraction(String query, String response, String sql, long duration) {
        String sqlInsert = "INSERT INTO ai_audit_logs (user_query, ai_response, sql_generated, execution_time_ms) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sqlInsert, query, response, sql, duration);
    }
}
