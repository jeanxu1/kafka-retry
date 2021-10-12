package ca.bc.jx.kafka.retry.exception;

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
