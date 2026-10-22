package org.example.cookbook.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css") || path.startsWith("/js") || path.startsWith("/favicon");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
            if (status >= 500) {
                log.error("HTTP {} {} -> {} ({} ms) user={}", request.getMethod(), request.getRequestURI(), status, duration, user);
            } else if (status >= 400) {
                log.warn("HTTP {} {} -> {} ({} ms) user={}", request.getMethod(), request.getRequestURI(), status, duration, user);
            } else {
                log.debug("HTTP {} {} -> {} ({} ms) user={}", request.getMethod(), request.getRequestURI(), status, duration, user);
            }
        }
    }
}
