package com.anisboulila.customerservice.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";
    private static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".id";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Cet identifiant ne sert qu'à la corrélation; une valeur libre ne devient jamais
        // une identité de sécurité.
        String correlationId = resolveCorrelationId(request);
        String previousMdcValue = MDC.get(MDC_KEY);

        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        MDC.put(MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (previousMdcValue == null) {
                MDC.remove(MDC_KEY);
            } else {
                MDC.put(MDC_KEY, previousMdcValue);
            }
        }
    }

    public static String getOrCreateCorrelationId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ATTRIBUTE);
        if (value instanceof String correlationId) {
            return correlationId;
        }

        String correlationId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        return correlationId;
    }

    private static String resolveCorrelationId(HttpServletRequest request) {
        Enumeration<String> values = request.getHeaders(HEADER_NAME);
        if (values == null || !values.hasMoreElements()) {
            // Sans valeur entrante, le serveur crée l'identifiant qui sera partagé entre réponse et logs.
            return UUID.randomUUID().toString();
        }

        String candidate = values.nextElement();
        // Une seule valeur de forme UUID est réutilisée et normalisée; toute valeur invalide
        // ou répétée est remplacée.
        if (values.hasMoreElements() || candidate == null || !UUID_PATTERN.matcher(candidate).matches()) {
            // Ne jamais refléter une chaîne arbitraire dans les réponses ou les journaux.
            return UUID.randomUUID().toString();
        }

        return UUID.fromString(candidate).toString();
    }
}
