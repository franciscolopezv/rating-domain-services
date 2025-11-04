package com.ratings.query.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Service for application monitoring and metrics collection.
 * Provides methods to record metrics for various operations.
 */
@Service
public class MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    private final Counter graphqlQueryCounter;
    private final Counter graphqlErrorCounter;
    private final Timer graphqlQueryTimer;
    private final Counter kafkaEventCounter;
    private final Counter kafkaEventErrorCounter;
    private final Timer eventProjectionTimer;
    private final Counter databaseOperationCounter;
    private final Counter databaseErrorCounter;
    private final Timer databaseQueryTimer;

    @Autowired
    public MonitoringService(
            @Qualifier("graphqlQueryCounter") Counter graphqlQueryCounter,
            @Qualifier("graphqlErrorCounter") Counter graphqlErrorCounter,
            @Qualifier("graphqlQueryTimer") Timer graphqlQueryTimer,
            @Qualifier("kafkaEventCounter") Counter kafkaEventCounter,
            @Qualifier("kafkaEventErrorCounter") Counter kafkaEventErrorCounter,
            @Qualifier("eventProjectionTimer") Timer eventProjectionTimer,
            @Qualifier("databaseOperationCounter") Counter databaseOperationCounter,
            @Qualifier("databaseErrorCounter") Counter databaseErrorCounter,
            @Qualifier("databaseQueryTimer") Timer databaseQueryTimer) {
        
        this.graphqlQueryCounter = graphqlQueryCounter;
        this.graphqlErrorCounter = graphqlErrorCounter;
        this.graphqlQueryTimer = graphqlQueryTimer;
        this.kafkaEventCounter = kafkaEventCounter;
        this.kafkaEventErrorCounter = kafkaEventErrorCounter;
        this.eventProjectionTimer = eventProjectionTimer;
        this.databaseOperationCounter = databaseOperationCounter;
        this.databaseErrorCounter = databaseErrorCounter;
        this.databaseQueryTimer = databaseQueryTimer;
    }

    /**
     * Records a GraphQL query execution.
     *
     * @param queryName the name of the query
     * @param executionTimeMs the execution time in milliseconds
     */
    public void recordGraphQLQuery(String queryName, long executionTimeMs) {
        graphqlQueryCounter.increment();
        graphqlQueryTimer.record(executionTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        logger.debug("Recorded GraphQL query: {} ({}ms)", queryName, executionTimeMs);
    }

    /**
     * Records a GraphQL query error.
     *
     * @param queryName the name of the query that failed
     * @param errorType the type of error
     */
    public void recordGraphQLError(String queryName, String errorType) {
        graphqlErrorCounter.increment();
        
        logger.warn("Recorded GraphQL error: {} - {}", queryName, errorType);
    }

    /**
     * Records a Kafka event processing.
     *
     * @param eventType the type of event processed
     * @param processingTimeMs the processing time in milliseconds
     */
    public void recordKafkaEvent(String eventType, long processingTimeMs) {
        kafkaEventCounter.increment();
        eventProjectionTimer.record(processingTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        logger.debug("Recorded Kafka event: {} ({}ms)", eventType, processingTimeMs);
    }

    /**
     * Records a Kafka event processing error.
     *
     * @param eventType the type of event that failed
     * @param errorType the type of error
     */
    public void recordKafkaEventError(String eventType, String errorType) {
        kafkaEventErrorCounter.increment();
        
        logger.warn("Recorded Kafka event error: {} - {}", eventType, errorType);
    }

    /**
     * Records a database operation.
     *
     * @param operationType the type of database operation
     * @param executionTimeMs the execution time in milliseconds
     */
    public void recordDatabaseOperation(String operationType, long executionTimeMs) {
        databaseOperationCounter.increment();
        databaseQueryTimer.record(executionTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        logger.debug("Recorded database operation: {} ({}ms)", operationType, executionTimeMs);
    }

    /**
     * Records a database operation error.
     *
     * @param operationType the type of operation that failed
     * @param errorType the type of error
     */
    public void recordDatabaseError(String operationType, String errorType) {
        databaseErrorCounter.increment();
        
        logger.warn("Recorded database error: {} - {}", operationType, errorType);
    }

    /**
     * Creates a timer sample for measuring execution time.
     *
     * @return a timer sample
     */
    public Timer.Sample startTimer() {
        return Timer.start();
    }

    /**
     * Records the execution time using a timer sample.
     *
     * @param sample the timer sample
     * @param timer the timer to record to
     */
    public void recordTimer(Timer.Sample sample, Timer timer) {
        sample.stop(timer);
    }
}