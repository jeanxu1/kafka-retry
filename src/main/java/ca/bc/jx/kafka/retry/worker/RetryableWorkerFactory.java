package ca.bc.jx.kafka.retry.worker;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@Service
@AllArgsConstructor
public class RetryableWorkerFactory {
    private final Environment environment;
    private final KafkaTemplate<?, ?> kafkaTemplate;
    private final ConsumerFactory<?, ?> consumerFactory;

    public <T> void startKafkaContainer(RetryProperties retryProperties, RetryableWorker<T> retryableWorker) {
        Pair<ContainerProperties, ContainerProperties> pair = createContainerProperties(retryProperties, retryableWorker);
        Consumer<ContainerProperties> consumer = (ContainerProperties props) -> {
            new KafkaMessageListenerContainer<>(consumerFactory, props).start();
            log.info("container for {} started", Arrays.asList(Objects.requireNonNull(props.getTopics())));
        };
        consumer.accept(pair.getLeft());
        consumer.accept(pair.getRight());
    }

    public Pair<ContainerProperties, ContainerProperties> createContainerProperties(
            RetryProperties retryProperties, RetryableWorker<?> retryableWorker) {
        return new KafkaRetryableWorker(retryableWorker, kafkaTemplate, retryProperties)
                .prepareContainerProperties();
    }
}
