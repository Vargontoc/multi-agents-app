package es.agonzalez.multiagent.app.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro que añade al MDC información contextual para logging estructurado.
 * Incluye: requestId, apiKeyHash (si cabecera presente), path y método.
 * Campos adicionales (userId, intent, model) se añaden progresivamente en capas superiores.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50) // después del filtro de seguridad pero antes de controladores
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    @Autowired
    private Tracer tracer;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        MDC.put("httpMethod", request.getMethod());
        MDC.put("path", request.getRequestURI());

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey != null && !apiKey.isBlank()) {
            MDC.put("apiKeyHash", shortHash(apiKey));
        }

        // Añadir traceId y spanId al MDC para logging correlacionado
        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            String traceId = currentSpan.context().traceId();
            String spanId = currentSpan.context().spanId();
            if (traceId != null && !traceId.isEmpty()) {
                MDC.put("traceId", traceId);
            }
            if (spanId != null && !spanId.isEmpty()) {
                MDC.put("spanId", spanId);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Limpieza para liberar el hilo del pool
            MDC.remove("apiKeyHash");
            MDC.remove("userId");
            MDC.remove("intent");
            MDC.remove("model");
            MDC.remove("httpMethod");
            MDC.remove("path");
            MDC.remove("requestId");
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }

    private String shortHash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(value.getBytes(StandardCharsets.UTF_8));
            // 6 bytes -> 8 chars Base64 url-safe sin padding
            byte[] first = new byte[6];
            System.arraycopy(dig, 0, first, 0, 6);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(first);
        } catch (NoSuchAlgorithmException e) {
            return "na"; // improbable
        }
    }
}