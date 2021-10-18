package ca.bc.jx.ap.retry.kafka;

import ca.bc.jx.kafka.retry.exception.RetryableException;
import ca.bc.jx.kafka.retry.worker.ConsumerWorker;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@Component
public class SimpleConsumer implements ConsumerWorker<String> {
    AtomicInteger acceptCalled = new AtomicInteger();
    boolean dropCalled = false;

    @Override
    public void accept(String o) {
        acceptCalled.incrementAndGet();
        log.info("got {}", o);
        throw new RetryableException();
    }

    @Override
    public void drop(String o) {
        dropCalled = true;
    }
}
