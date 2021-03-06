package ca.bc.jx.kafka.retry.worker;

import ca.bc.jx.kafka.retry.domain.NonBlockingKafkaConsumer;
import ca.bc.jx.kafka.retry.domain.RetryProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
@AllArgsConstructor
public class ConsumerManager<K, V> {
    private final ApplicationContext applicationContext;
    private final KafkaTemplate<K, V> kafkaTemplate;
    private final ConsumerFactory<K, V> consumerFactory;
    private final NonBlockingKafkaConsumerConfigure.ConsumerPropertyList propertyList;

    /**
     * get the ConsumerMap, in case of custom behavior, such as pause / resume the consumer.
     */
    @Getter
    private Map<String, NonBlockingKafkaConsumer<K, V>> nonBlockingKafkaConsumerMap;

    @PostConstruct
    void startKafkaContainers() {
        nonBlockingKafkaConsumerMap = propertyList.stream().collect(Collectors.toUnmodifiableMap(RetryProperties::getId, property -> {
            ConsumerWorker<V> consumerWorker = applicationContext.getBean(property.getConsumerWorkerClass());
            ConsumerWorkerHandler<K, V> handler = new ConsumerWorkerHandler<>(consumerWorker, kafkaTemplate, property);

            ContainerProperties mainContainerProperties = new ContainerProperties(property.getTopicMain());
            MessageListener<K, V> messageListener = handler::accept;

            mainContainerProperties.setMessageListener(messageListener);
            AbstractMessageListenerContainer<K, V> mainContainer;

            if (property.getTopicMainListenerNumber() > 1) {
                ConcurrentMessageListenerContainer<K, V> container =
                        new ConcurrentMessageListenerContainer<>(consumerFactory, mainContainerProperties);
                container.setConcurrency(property.getTopicMainListenerNumber());
                mainContainer = container;
            } else {
                mainContainer = new KafkaMessageListenerContainer<>(consumerFactory, mainContainerProperties);
            }

            ContainerProperties retryContainerProperties = new ContainerProperties(property.getTopicRetry());
            retryContainerProperties.setMessageListener(messageListener);
            AbstractMessageListenerContainer<K, V> retryContainer;

            if (property.getTopicRetryListenerNumber() > 1) {
                ConcurrentMessageListenerContainer<K, V> container =
                        new ConcurrentMessageListenerContainer<>(consumerFactory, retryContainerProperties);
                container.setConcurrency(property.getTopicRetryListenerNumber());
                retryContainer = container;
            } else {
                retryContainer = new KafkaMessageListenerContainer<>(consumerFactory, retryContainerProperties);
            }

            mainContainer.start();
            retryContainer.start();

            return NonBlockingKafkaConsumer.<K, V>builder()
                    .consumerWorker(consumerWorker)
                    .mainMessageListenerContainer(mainContainer)
                    .retryMessageListenerContainer(retryContainer)
                    .retryProperties(property)
                    .build();
        }));
    }
}
