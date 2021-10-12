package ca.bc.jx.ap.retry.kafka.server;

import ca.bc.jx.kafka.retry.worker.RetryableWorker;
import ca.bc.jx.kafka.retry.worker.RetryProperties;
import ca.bc.jx.kafka.retry.worker.RetryableWorkerFactory;
import ca.bc.jx.kafka.retry.exception.RetryableException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Random;

@SpringBootApplication
@ComponentScan(basePackageClasses = {RetryableWorkerFactory.class, MsgGeneratorController.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    @Slf4j
    static class Config {
        @Autowired
        private RetryableWorkerFactory factory;
        @Autowired
        private Topic1 topic1;

        @PostConstruct
        public void post() {
            RetryableWorker<String> worker = o -> {
                int num = Integer.parseInt(o);
                if (num < 0) {
                    throw new RetryableException(o);
                } else if (num == 0) {
                    // ok
                } else {
                    int anInt = Math.abs(new Random().nextInt());
                    int remain = anInt % 3;
                    log.info("Random {}, {}", anInt, remain);
                    if (remain == 0) {
                        // OK
                    } else if (remain == 1) {
                        throw new RetryableException(o);
                    } else {
                        throw new RuntimeException(o);
                    }
                }

            };
            factory.startKafkaContainer(topic1, worker);

        }
    }

    @Configuration
    @ConfigurationProperties(prefix = "config.kafka.consumer.topic1")
    @NoArgsConstructor
    static class Topic1 extends RetryProperties {

    }
}
