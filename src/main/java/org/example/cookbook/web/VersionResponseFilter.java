package org.example.cookbook.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Добавляет версию приложения и имя пода в каждый HTTP-ответ.
 * Заголовки позволяют видеть распределение трафика между версиями
 * без изменения HTML-шаблонов и API.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VersionResponseFilter extends OncePerRequestFilter {

    public static final String VERSION_HEADER = "X-App-Version";
    public static final String INSTANCE_HEADER = "X-App-Instance";

    private final String version;
    private final String instance;

    public VersionResponseFilter(@Value("${APP_VERSION:dev}") String version,
                                 @Value("${HOSTNAME:unknown}") String instance) {
        this.version = version;
        this.instance = instance;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader(VERSION_HEADER, version);
        response.setHeader(INSTANCE_HEADER, instance);
        filterChain.doFilter(request, response);
    }
}
