package ca.bc.jx.kafka.retry.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class NoRetryableException extends RuntimeException {
    public NoRetryableException(String s) {
        super(s);
    }

    public NoRetryableException(String s, Throwable t) {
        super(s, t);
    }

    public NoRetryableException(Throwable t) {
        super(t);
    }
}
