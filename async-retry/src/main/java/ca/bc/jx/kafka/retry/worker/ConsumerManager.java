package ca.bc.jx.kafka.retry.worker;

import ca.bc.jx.kafka.retry.domain.NonBlockingKafkaConsumer;
import ca.bc.jx.kafka.retry.domain.RetryProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
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

    @Getter
    private Map<String, NonBlockingKafkaConsumer<K, V>> nonBlockingKafkaConsumerMap;

    @PostConstruct
    void startKafkaContainers() {
        nonBlockingKafkaConsumerMap = propertyList.stream().collect(Collectors.toUnmodifiableMap(RetryProperties::getId, property -> {
            ConsumerWorker<V> consumerWorker = applicationContext.getBean(property.getConsumerWorkerClass());
            ConsumerWorkerHandler<K, V> handler = new ConsumerWorkerHandler<K, V>(consumerWorker, kafkaTemplate, property);

            ContainerProperties mainContainerProperties = new ContainerProperties(property.getMainTopic());
            MessageListener<K, V> messageListener = handler::accept;

            mainContainerProperties.setMessageListener(messageListener);
            KafkaMessageListenerContainer<K, V> mainContainer = new KafkaMessageListenerContainer<>(consumerFactory, mainContainerProperties);

            ContainerProperties retryContainerProperties = new ContainerProperties(property.getRetryTopic());
            retryContainerProperties.setMessageListener(messageListener);
            KafkaMessageListenerContainer<K, V> retryContainer = new KafkaMessageListenerContainer<>(consumerFactory, retryContainerProperties);

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
