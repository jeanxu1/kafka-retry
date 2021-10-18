package ca.bc.jx.kafka.server;

import ca.bc.jx.kafka.retry.exception.RetryableException;
import ca.bc.jx.kafka.retry.worker.ConsumerWorker;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class SampleRetryableWorker implements ConsumerWorker<Object> {

    @Override
    public void accept(Object o) {
        log.info("**********************************************************");
        throw new RetryableException(o.toString());
//        int num = Integer.parseInt(o);
//        if (num < 0) {
//            throw new RetryableException(o);
//        } else if (num == 0) {
//            log.info("simulate it works");
//        } else {
//            int anInt = new Random().nextInt();
//            int remain = anInt % 3;
//            log.info("Random {}, {}, {}", anInt, remain, o);
//            if (remain == 0) {
//                log.info("simulate it works");
//            } else {
//                throw new RetryableException(o);
//            }
//        }
    }

//    @Override
//    @Retryable
//    public void retry(Object o) {
//        accept(o);
//    }
}
