package org.example.cookbook.chaos;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(0)
@ConditionalOnProperty(prefix = "cookbook.demo", name = "chaos-enabled", havingValue = "true")
public class ChaosFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ChaosFilter.class);

    private final ChaosState state;

    public ChaosFilter(ChaosState state) {
        this.state = state;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.equals("/recipes")
                || uri.startsWith("/recipes/")
                || uri.equals("/api/v1/recipes")
                || uri.startsWith("/api/v1/recipes/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (state.shouldFail()) {
            log.error("Chaos injected 500 method={} uri={} errorRate={}",
                    request.getMethod(), request.getRequestURI(), state.getErrorRate());
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Injected chaos failure");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
