package ca.bc.jx.kafka.server;

import ca.bc.jx.kafka.retry.worker.EnableNonBlockingKafkaConsumers;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
@EnableNonBlockingKafkaConsumers
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
