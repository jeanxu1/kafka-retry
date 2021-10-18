package ca.bc.jx.kafka.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RetryableKafkaListener {
    @Autowired
    private Processor processor;

    @KafkaListener(topics = "0-spring-retry")
    public void listen(String in) {
        log.info("got {}", in);
        try {
            processor.accept(in);
        } catch (Exception e) {
            log.info("should it dead");
        }
    }
}
