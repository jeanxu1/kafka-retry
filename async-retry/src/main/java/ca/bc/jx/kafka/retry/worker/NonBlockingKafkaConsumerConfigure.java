package ca.bc.jx.kafka.retry.worker;

import ca.bc.jx.kafka.retry.domain.RetryProperties;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;

@Configuration
@EnableAspectJAutoProxy
@Import(ConsumerManager.class)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class NonBlockingKafkaConsumerConfigure {
    @Configuration
    @ConfigurationProperties(prefix = "nonblocking.kafka-consumers")
    static class ConsumerPropertyList extends ArrayList<RetryProperties> {
    }

}
