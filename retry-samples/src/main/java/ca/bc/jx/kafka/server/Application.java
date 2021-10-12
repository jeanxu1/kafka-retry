package ca.bc.jx.kafka.server;

import ca.bc.jx.kafka.retry.worker.RetryProperties;
import ca.bc.jx.kafka.retry.worker.RetryableWorkerFactory;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import javax.annotation.PostConstruct;

@SpringBootApplication
@ComponentScan(basePackageClasses = {RetryableWorkerFactory.class, MsgGeneratorController.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Configuration
    @Log4j2
    @RequiredArgsConstructor
    @EnableRetry
    static class Config {
        private final RetryableWorkerFactory factory;
        private final Topic1 topic1;
        private final SampleRetryableWorker sampleRetryableWorker;


        @PostConstruct
        public void post() {
            factory.startKafkaContainer(topic1, sampleRetryableWorker);
        }
    }

    @Configuration
    @ConfigurationProperties(prefix = "config.kafka.consumer.topic1")
    @NoArgsConstructor
    static class Topic1 extends RetryProperties {

    }
}
