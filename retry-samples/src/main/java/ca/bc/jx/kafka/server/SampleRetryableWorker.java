package ca.bc.jx.kafka.server;

import ca.bc.jx.kafka.retry.exception.RetryableException;
import ca.bc.jx.kafka.retry.worker.ConsumerWorker;
import lombok.extern.log4j.Log4j2;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Random;

@Log4j2
@Component
public class SampleRetryableWorker implements ConsumerWorker<String> {

    @Override
//    @Retryable(value = RetryableException.class, backoff = @Backoff(delay = 1000))
    public void accept(String o) {
        log.info("**********************************************************");
        int num = Integer.parseInt(o);
        if (num < 0) {
            throw new RetryableException(o);
        } else if (num == 0) {
            log.info("simulate it works");
        } else {
            int anInt = Math.abs(new Random().nextInt());
            int remain = anInt % 5;
            log.info("Random {}, {}, {}", anInt, remain, o);
            if (remain == 0) {
                log.info("simulate it works");
            } else {
                throw new RetryableException(o);
            }
        }
    }

//    @Override
//    @Retryable(value = RetryableException.class, backoff = @Backoff(delay = 50))
//    public void retry(String o) {
//        accept(o);
//    }
}
