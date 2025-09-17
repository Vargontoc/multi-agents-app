package es.agonzalez.multiagent.app.config;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de rate limiting simple basado en Bucket4j.
 * Estrategia: bucket por API key (cabecera X-API-Key). Si no hay API key (pero debería por filtro anterior)
 * se fallback a IP remota para robustez.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10) // Después del ApiKeyFilter (que usa default precedence)
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimitProperties props;
    private final ObjectMapper om;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private volatile Bandwidth currentBandwidth;
    private Set<String> excluded;

    public RateLimitingFilter(RateLimitProperties props, ObjectMapper om) {
        this.props = props;
        this.om = om;
        rebuildBandwidth();
        rebuildExclusions();
    }

    private void rebuildExclusions() {
        excluded = Set.of(props.getExcludePaths().split(","));
    }

    private void rebuildBandwidth() {
        Duration period = parseDuration(props.getRefillPeriod());
        currentBandwidth = Bandwidth.classic(props.getCapacity(), Refill.greedy(props.getRefillTokens(), period));
    }

    private Duration parseDuration(String val) {
        if (!StringUtils.hasText(val)) return Duration.ofSeconds(60);
        // soportar sufijos s,m,h simplificados
        try {
            if (val.endsWith("ms")) return Duration.ofMillis(Long.parseLong(val.substring(0, val.length()-2)));
            if (val.endsWith("s")) return Duration.ofSeconds(Long.parseLong(val.substring(0, val.length()-1)));
            if (val.endsWith("m")) return Duration.ofMinutes(Long.parseLong(val.substring(0, val.length()-1)));
            if (val.endsWith("h")) return Duration.ofHours(Long.parseLong(val.substring(0, val.length()-1)));
            return Duration.parse(val); // ISO-8601 fallback
        } catch (Exception e) {
            log.warn("Formato de duración inválido '{}' usando 60s por defecto", val);
            return Duration.ofSeconds(60);
        }
    }

    private Bucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> Bucket.builder().addLimit(currentBandwidth).build());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if (!props.isEnabled()) return true;
        String path = request.getRequestURI();
        return excluded.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");
        if (!StringUtils.hasText(apiKey)) {
            apiKey = request.getRemoteAddr();
        }
        Bucket bucket = resolveBucket(apiKey);
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }
        // limite excedido
        response.setStatus(429);
        response.setContentType("application/json");
        Map<String,Object> body = Map.of(
            "status", "error",
            "error", "rate_limited",
            "message", "Too many requests",
            "path", request.getRequestURI(),
            "timestamp", Instant.now().toString()
        );
        response.getWriter().write(om.writeValueAsString(body));
    }
}
