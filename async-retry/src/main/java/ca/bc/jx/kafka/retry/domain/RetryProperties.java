package ca.bc.jx.kafka.retry.domain;

import ca.bc.jx.kafka.retry.worker.ConsumerWorker;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@NoArgsConstructor
public class RetryProperties {
    @NonNull
    private String id;
    @NonNull
    private String mainTopic;
    @NonNull
    private String retryTopic;
    private String dlQTopic;
    private int maxRetries = 5;
    private long retryInterval = 1000L;
    private boolean fixedInterval = false;
    @NonNull
    private Class<? extends ConsumerWorker> consumerWorkerClass;
}