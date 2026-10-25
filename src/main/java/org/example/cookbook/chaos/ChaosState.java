package org.example.cookbook.chaos;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class ChaosState {

    private volatile double errorRate;

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        if (!Double.isFinite(errorRate) || errorRate < 0.0 || errorRate > 1.0) {
            throw new IllegalArgumentException("errorRate must be a finite number between 0 and 1");
        }
        this.errorRate = errorRate;
    }

    public void reset() {
        errorRate = 0.0;
    }

    public boolean shouldFail() {
        double rate = errorRate;
        return rate > 0.0 && ThreadLocalRandom.current().nextDouble() < rate;
    }
}
