package es.agonzalez.multiagent.app.config;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyFilter extends OncePerRequestFilter{
    @Value("${security.apikey}")
    private String key;
    private final ObjectMapper om = new ObjectMapper();
    
    @Override
    @SuppressWarnings("null")
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String provided = request.getHeader("X-API-Key");
        if(provided == null || !provided.equals(key)) {
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

}
