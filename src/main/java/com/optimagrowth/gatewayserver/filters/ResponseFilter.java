package com.optimagrowth.gatewayserver.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.WebFluxSleuthOperators;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class ResponseFilter {

    final Logger logger = LoggerFactory.getLogger(ResponseFilter.class);

    @Autowired
    private Tracer tracer;

    @Bean
    public GlobalFilter postGlobalFilter() {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                WebFluxSleuthOperators.withSpanInScope(tracer, Objects.requireNonNull(tracer.currentTraceContext()), exchange, () ->
                        {
                            String correlationId = tracer.currentTraceContext().context().traceId();
                            logger.debug("Adding the correlation id to the outbound headers. {}", correlationId);
                            exchange.getResponse().getHeaders().add(FilterUtils.CORRELATION_ID, correlationId);
                        }
                );
                logger.debug("Completing outgoing response for {} request.", exchange.getRequest().getURI());
            }));
        };
    }
}