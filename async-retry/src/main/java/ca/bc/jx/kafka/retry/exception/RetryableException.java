package ca.bc.jx.kafka.retry.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RetryableException extends RuntimeException {
    public RetryableException(String s) {
        super(s);
    }

    public RetryableException(String s, Throwable t) {
        super(s, t);
    }

    public RetryableException(Throwable t) {
        super(t);
    }

}
