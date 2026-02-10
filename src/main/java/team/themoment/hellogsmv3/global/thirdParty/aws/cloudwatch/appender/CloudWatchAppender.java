package team.themoment.hellogsmv3.global.thirdParty.aws.cloudwatch.appender;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import lombok.Setter;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidSequenceTokenException;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogStream;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutRetentionPolicyRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceAlreadyExistsException;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;

public class CloudWatchAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    @Setter
    private String logGroupName;
    @Setter
    private String logStreamNamePrefix;
    @Setter
    private String region = "ap-northeast-2";
    @Setter
    private int maxBatchSize = 50;
    @Setter
    private long maxBatchTimeMillis = 10000;
    @Setter
    private long maxBlockTimeMillis = 5000;
    @Setter
    private int retentionTimeDays = 30;
    @Setter
    private long shutdownTimeoutMillis = 5000;
    @Setter
    private int maxRetries = 3;

    private CloudWatchLogsClient cloudWatchClient;
    private final BlockingQueue<ILoggingEvent> logQueue = new LinkedBlockingQueue<>();
    private final AtomicReference<String> sequenceToken = new AtomicReference<>(null);
    private Thread writerThread;
    private String actualLogStreamName;
    private volatile boolean running = false;

    @Setter
    private Encoder<ILoggingEvent> encoder;

    @Override
    public void start() {
        if (logGroupName == null || logGroupName.isBlank()) {
            addError("logGroupName must be set");
            return;
        }
        if (logStreamNamePrefix == null || logStreamNamePrefix.isBlank()) {
            addError("logStreamNamePrefix must be set");
            return;
        }
        if (encoder == null) {
            addError("encoder must be set");
            return;
        }

        actualLogStreamName = logStreamNamePrefix + UUID.randomUUID();

        try {
            cloudWatchClient = CloudWatchLogsClient.builder().region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.builder().build()).build();

            initializeLogGroup();
            initializeLogStream();

            running = true;
            writerThread = new Thread(this::runWriter, "CloudWatchAppender-Writer-" + getName());
            writerThread.start();

            super.start();
            addInfo("CloudWatchAppender started for log group: " + logGroupName + ", stream: " + actualLogStreamName);
        } catch (Exception e) {
            addError("Failed to start CloudWatchAppender", e);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (writerThread != null) {
            writerThread.interrupt();
            try {
                writerThread.join(shutdownTimeoutMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                addError("Interrupted while waiting for writer thread to stop", e);
            }
        }

        try {
            flushLogs();
        } catch (Exception e) {
            addError("Error flushing logs during shutdown", e);
        }

        if (cloudWatchClient != null) {
            try {
                cloudWatchClient.close();
            } catch (Exception e) {
                addError("Error closing CloudWatch client", e);
            }
        }

        super.stop();
        addInfo("CloudWatchAppender stopped");
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (!isStarted()) {
            return;
        }

        try {
            boolean success = logQueue.offer(eventObject, maxBlockTimeMillis, TimeUnit.MILLISECONDS);
            if (!success) {
                addWarn("Log queue is full, dropping log event");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            addError("Interrupted while adding log event to queue", e);
        }
    }

    private void initializeLogGroup() {
        try {
            CreateLogGroupRequest request = CreateLogGroupRequest.builder().logGroupName(logGroupName).build();
            cloudWatchClient.createLogGroup(request);
            addInfo("Created log group: " + logGroupName);
        } catch (ResourceAlreadyExistsException e) {
            addInfo("Log group already exists: " + logGroupName);
        } catch (Exception e) {
            addError("Failed to create log group: " + logGroupName, e);
            throw e;
        }

        if (retentionTimeDays > 0) {
            try {
                PutRetentionPolicyRequest retentionRequest = PutRetentionPolicyRequest.builder()
                        .logGroupName(logGroupName).retentionInDays(retentionTimeDays).build();
                cloudWatchClient.putRetentionPolicy(retentionRequest);
                addInfo("Set retention policy to " + retentionTimeDays + " days for log group: " + logGroupName);
            } catch (Exception e) {
                addError("Failed to set retention policy for log group: " + logGroupName, e);
            }
        }
    }

    private void initializeLogStream() {
        try {
            CreateLogStreamRequest request = CreateLogStreamRequest.builder().logGroupName(logGroupName)
                    .logStreamName(actualLogStreamName).build();
            cloudWatchClient.createLogStream(request);
            addInfo("Created log stream: " + actualLogStreamName);
        } catch (ResourceAlreadyExistsException e) {
            addInfo("Log stream already exists: " + actualLogStreamName);
            refreshSequenceToken();
        } catch (Exception e) {
            addError("Failed to create log stream: " + actualLogStreamName, e);
            throw e;
        }
    }

    private void refreshSequenceToken() {
        try {
            DescribeLogStreamsRequest request = DescribeLogStreamsRequest.builder().logGroupName(logGroupName)
                    .logStreamNamePrefix(actualLogStreamName).build();

            DescribeLogStreamsResponse response = cloudWatchClient.describeLogStreams(request);
            LogStream logStream = response.logStreams().stream()
                    .filter(ls -> ls.logStreamName().equals(actualLogStreamName)).findFirst().orElse(null);

            if (logStream != null) {
                sequenceToken.set(logStream.uploadSequenceToken());
            }
        } catch (Exception e) {
            addError("Failed to refresh sequence token", e);
        }
    }

    private void runWriter() {
        List<ILoggingEvent> batch = new ArrayList<>();
        long lastFlushTime = System.currentTimeMillis();

        while (running || !logQueue.isEmpty()) {
            try {
                ILoggingEvent event = logQueue.poll(1000, TimeUnit.MILLISECONDS);

                if (event != null) {
                    batch.add(event);
                }

                long now = System.currentTimeMillis();
                boolean shouldFlush = batch.size() >= maxBatchSize
                        || (!batch.isEmpty() && (now - lastFlushTime) >= maxBatchTimeMillis);

                if (shouldFlush) {
                    flushBatch(batch);
                    batch.clear();
                    lastFlushTime = now;
                }
            } catch (InterruptedException e) {
                addInfo("Writer thread interrupted, flushing remaining logs");
                if (!running) {
                    break;
                }
            } catch (Exception e) {
                addError("Error in writer thread", e);
            }
        }

        if (!batch.isEmpty()) {
            try {
                flushBatch(batch);
            } catch (Exception e) {
                addError("Error flushing final batch", e);
            }
        }
    }

    private void flushLogs() {
        List<ILoggingEvent> batch = new ArrayList<>();
        logQueue.drainTo(batch);
        if (!batch.isEmpty()) {
            flushBatch(batch);
        }
    }

    private void flushBatch(List<ILoggingEvent> batch) {
        if (batch.isEmpty()) {
            return;
        }

        try {
            List<InputLogEvent> logEvents = batch.stream()
                    .map(event -> InputLogEvent.builder().timestamp(event.getTimeStamp())
                            .message(new String(encoder.encode(event), StandardCharsets.UTF_8).trim()).build())
                    .sorted(Comparator.comparing(InputLogEvent::timestamp)).collect(Collectors.toList());

            for (int retryCount = 0; retryCount < maxRetries; retryCount++) {
                try {
                    PutLogEventsRequest.Builder requestBuilder = PutLogEventsRequest.builder()
                            .logGroupName(logGroupName).logStreamName(actualLogStreamName).logEvents(logEvents);

                    String currentToken = sequenceToken.get();
                    if (currentToken != null) {
                        requestBuilder.sequenceToken(currentToken);
                    }

                    PutLogEventsResponse response = cloudWatchClient.putLogEvents(requestBuilder.build());
                    sequenceToken.set(response.nextSequenceToken());
                    return;
                } catch (InvalidSequenceTokenException e) {
                    sequenceToken.set(e.expectedSequenceToken());
                    if (retryCount >= maxRetries - 1) {
                        throw e;
                    }
                } catch (ResourceNotFoundException e) {
                    addError("Log group or stream not found, attempting to recreate", e);
                    initializeLogGroup();
                    initializeLogStream();
                    if (retryCount >= maxRetries - 1) {
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            addError("Failed to send log events to CloudWatch", e);
        }
    }
}
