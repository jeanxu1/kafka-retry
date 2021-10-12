package ca.bc.jx.kafka.retry.worker;

import java.util.function.Consumer;

public interface RetryableWorker<T> extends Consumer<T> {

    default void retry(T o) {
        accept(o);
    }

    default void drop(T o) {
        // default do nothing
    }
}
