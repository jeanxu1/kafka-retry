package ca.bc.jx.kafka.server;

import ca.bc.jx.kafka.retry.exception.RetryableException;
import ca.bc.jx.kafka.retry.worker.ConsumerWorker;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Random;

@Log4j2
@Component
public class SampleRetryableWorker implements ConsumerWorker<Object> {

    @Override
    public void accept(Object o) {
        log.info("**********************************************************");
        int num = Integer.parseInt(o.toString());
        if (num < 0) {
            throw new RetryableException(o.toString());
        } else if (num == 0) {
            log.info("simulate it works");
        } else {
            int anInt = new Random().nextInt();
            int remain = anInt % 3;
            log.info("Random {}, {}, {}", anInt, remain, o);
            if (remain == 0) {
                log.info("simulate it works");
            } else {
                throw new RetryableException(o.toString());
            }
        }
    }

}
