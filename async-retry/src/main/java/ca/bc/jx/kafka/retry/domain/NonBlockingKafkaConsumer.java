package ca.bc.jx.kafka.retry.domain;

import ca.bc.jx.kafka.retry.worker.ConsumerWorker;
import lombok.Builder;
import lombok.Data;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;

@Data
@Builder
public class NonBlockingKafkaConsumer<K,V> {
    private AbstractMessageListenerContainer<K,V> mainMessageListenerContainer;
    private AbstractMessageListenerContainer<K,V> retryMessageListenerContainer;
    private ConsumerWorker<V> consumerWorker;
    private RetryProperties retryProperties;
}
