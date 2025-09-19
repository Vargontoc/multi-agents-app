package es.agonzalez.multiagent.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuración de OpenTelemetry para distributed tracing.
 * Proporciona instrumentación automática para requests HTTP y llamadas LLM.
 */
@Configuration
@EnableConfigurationProperties(TracingConfig.TracingProperties.class)
public class TracingConfig {

    @Bean
    public OpenTelemetry openTelemetry(TracingProperties tracingProperties) {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(
                Attributes.of(
                    ResourceAttributes.SERVICE_NAME, tracingProperties.getServiceName(),
                    ResourceAttributes.SERVICE_VERSION, tracingProperties.getServiceVersion()
                )
            ));

        SdkTracerProviderBuilder tracerProvider = SdkTracerProvider.builder()
            .setResource(resource);

        // Configurar exportador OTLP si está habilitado
        if (tracingProperties.isExportEnabled()) {
            OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(tracingProperties.getOtlp().getEndpoint())
                .build();

            tracerProvider.addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build());
        }

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider.build())
            .setPropagators(ContextPropagators.create(
                TextMapPropagator.composite(
                    W3CTraceContextPropagator.getInstance(),
                    W3CBaggagePropagator.getInstance()
                )
            ))
            .build();
    }

    @Bean
    public io.micrometer.tracing.Tracer micrometerTracer(OpenTelemetry openTelemetry) {
        return new OtelTracer(
            openTelemetry.getTracer("multiagent-service"),
            new OtelCurrentTraceContext(),
            event -> {
                // Event listener para logging personalizado si es necesario
            }
        );
    }

    /**
     * Propiedades de configuración para tracing.
     */
    @ConfigurationProperties(prefix = "management.tracing")
    @Validated
    public static class TracingProperties {

        @NotBlank(message = "{validation.tracing.serviceName.notBlank}")
        private String serviceName = "multi-agents-service";

        private String serviceVersion = "1.0.0";

        private boolean exportEnabled = false;

        private Otlp otlp = new Otlp();

        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }

        public String getServiceVersion() { return serviceVersion; }
        public void setServiceVersion(String serviceVersion) { this.serviceVersion = serviceVersion; }

        public boolean isExportEnabled() { return exportEnabled; }
        public void setExportEnabled(boolean exportEnabled) { this.exportEnabled = exportEnabled; }

        public Otlp getOtlp() { return otlp; }
        public void setOtlp(Otlp otlp) { this.otlp = otlp; }

        @Validated
        public static class Otlp {
            private String endpoint = "http://localhost:4317";

            public String getEndpoint() { return endpoint; }
            public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        }
    }
}