package ca.bc.jx.kafka.server;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class Processor implements Consumer<String> {

    @Override
    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 1000))
    public void accept(String o) {
        throw new RuntimeException("it fails");
    }
}
