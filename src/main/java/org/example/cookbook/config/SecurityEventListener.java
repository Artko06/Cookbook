package org.example.cookbook.config;

import org.example.cookbook.metrics.CookbookMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class SecurityEventListener {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventListener.class);

    private final CookbookMetrics metrics;

    public SecurityEventListener(CookbookMetrics metrics) {
        this.metrics = metrics;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        log.info("Login success username={}", event.getAuthentication().getName());
        metrics.loginSuccess();
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        log.warn("Login failed username={} reason={}",
                event.getAuthentication().getName(),
                event.getException().getClass().getSimpleName());
        metrics.loginFailure();
    }
}
