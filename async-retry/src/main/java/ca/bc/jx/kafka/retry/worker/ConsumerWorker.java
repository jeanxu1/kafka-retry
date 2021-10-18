package ca.bc.jx.kafka.retry.worker;

import java.util.function.Consumer;

public interface ConsumerWorker<T> extends Consumer<T> {
    /**
     * Retry behavior, same as main as default
     *
     * @param o the message
     */
    default void retry(T o) {
        accept(o);
    }

    /**
     * Action before giving up the message
     *
     * @param o the message
     */
    default void drop(T o) {
        // default do nothing
    }
}
