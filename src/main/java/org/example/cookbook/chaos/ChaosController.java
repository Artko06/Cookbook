package org.example.cookbook.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/chaos")
@ConditionalOnProperty(prefix = "cookbook.demo", name = "chaos-enabled", havingValue = "true")
public class ChaosController {

    private static final Logger log = LoggerFactory.getLogger(ChaosController.class);

    private final ChaosState state;

    public ChaosController(ChaosState state) {
        this.state = state;
    }

    @PostMapping
    public ChaosStatus set(@RequestParam(defaultValue = "0.0") double errorRate) {
        try {
            state.setErrorRate(errorRate);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        log.info("Chaos error rate set to {}", errorRate);
        return status();
    }

    @GetMapping
    public ChaosStatus status() {
        return new ChaosStatus(true, state.getErrorRate());
    }

    @PostMapping("/reset")
    public ChaosStatus reset() {
        state.reset();
        log.info("Chaos reset");
        return status();
    }

    public record ChaosStatus(boolean enabled, double errorRate) {
    }
}
