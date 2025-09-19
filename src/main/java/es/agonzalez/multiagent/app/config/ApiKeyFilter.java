package es.agonzalez.multiagent.app.config;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyFilter extends OncePerRequestFilter{
    @Autowired
    private SecurityProperties securityProperties;
    private final ObjectMapper om;
    public ApiKeyFilter(ObjectMapper om) { this.om = om; }
    
    @Override
    @SuppressWarnings("null")
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        // Excepciones públicas: documentación OpenAPI y recursos de Swagger UI
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.equals("/swagger-ui.html")) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader("X-API-Key");
        if(provided == null || !isValidApiKey(provided, securityProperties.getApikey())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            // Indicamos esquema genérico de API key (custom) para clientes automatizados
            response.setHeader("WWW-Authenticate", "ApiKey realm=multi-agents-service");
            Map<String,Object> body = new LinkedHashMap<>();
            body.put("status", "error");
            body.put("error", "unauthorized");
            body.put("message", "Missing or invalid X-API-Key header");
            body.put("path", request.getRequestURI());
            body.put("timestamp", Instant.now().toString());
            response.getWriter().write(om.writeValueAsString(body));
            return; // Corte crítico: evita que la petición avance sin credenciales válidas
        }

        filterChain.doFilter(request, response); // Sólo continúa si autenticado
    }

    /**
     * Comparación constant-time de API keys para prevenir timing attacks.
     * Usa MessageDigest.isEqual() que implementa comparación segura.
     */
    private boolean isValidApiKey(String provided, String expected) {
        if (provided == null || expected == null) {
            return false;
        }
        // Convertir a bytes UTF-8 para comparación constant-time
        byte[] providedBytes = provided.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] expectedBytes = expected.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        return MessageDigest.isEqual(providedBytes, expectedBytes);
    }

}
