package com.hyperativa.cardapi.filter;

import com.hyperativa.cardapi.entity.RequestLog;
import com.hyperativa.cardapi.repository.RequestLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final RequestLogRepository requestLogRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String username = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                username = auth.getName();
            }

            String uri = request.getRequestURI();
            // Do not log actuator, h2-console and swagger
            if (!uri.startsWith("/actuator") && !uri.startsWith("/h2-console")
                    && !uri.startsWith("/swagger") && !uri.startsWith("/api-docs")) {

                RequestLog logEntry = RequestLog.builder()
                        .method(request.getMethod())
                        .uri(uri)
                        .username(username)
                        .statusCode(response.getStatus())
                        .durationMs(duration)
                        .build();

                try {
                    requestLogRepository.save(logEntry);
                } catch (Exception e) {
                    log.error("Error saving request log", e);
                }

                log.info("[{}] {} {} - Status: {} - {}ms - User: {}",
                        request.getMethod(), uri,
                        request.getQueryString() != null ? "?" + request.getQueryString() : "",
                        response.getStatus(), duration, username);
            }
        }
    }
}
