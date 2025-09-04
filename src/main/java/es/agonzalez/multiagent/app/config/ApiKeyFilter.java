package es.agonzalez.multiagent.app.config;

import java.io.IOException;
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
        if(provided == null || !provided.equals(key)) 
        {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write(om.writeValueAsString(Map.of(
                "status", "error",
                "error", "unauthorized"
            )));
        }

        filterChain.doFilter(request, response);
    }

}
