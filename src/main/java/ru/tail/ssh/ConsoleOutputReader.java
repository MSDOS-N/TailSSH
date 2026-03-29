package ru.tail.ssh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class ConsoleOutputReader {
    private static final BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();
    private static OutputStream currentOutputStream;
    private static Thread outputThread;
    private static volatile boolean running = true;
    private static LogAppender logAppender;

    public ConsoleOutputReader(OutputStream out) {
        currentOutputStream = out;
    }

    public void start() {
        // Register log appender to capture console output
        if (logAppender == null) {
            logAppender = new LogAppender();
            logAppender.start();
            org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            rootLogger.addAppender(logAppender);
        }

        // Start output thread
        outputThread = new Thread(() -> {
            try {
                while (running) {
                    String line = outputQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (line != null && currentOutputStream != null) {
                        try {
                            currentOutputStream.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
                            currentOutputStream.flush();
                        } catch (Exception e) {
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        outputThread.setDaemon(true);
        outputThread.start();

        TailSSH.LOGGER.info("Console output reader started");
    }

    public void stop() {
        running = false;
        if (outputThread != null) {
            outputThread.interrupt();
        }
        if (logAppender != null) {
            org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            rootLogger.removeAppender(logAppender);
        }
    }

    public static void queueOutput(String message) {
        if (running && currentOutputStream != null) {
            String cleanMessage = message.replaceAll("\u001B\\[[;\\d]*m", "");
            outputQueue.offer(cleanMessage);
        }
    }

    private static class LogAppender extends AbstractAppender {
        private static final PatternLayout LAYOUT = PatternLayout.newBuilder()
                .withPattern("[%d{HH:mm:ss}] [%level] %msg%n")
                .build();

        protected LogAppender() {
            super("ConsoleOutputAppender", null, LAYOUT, true);
        }

        @Override
        public void append(LogEvent event) {
            String message = LAYOUT.toSerializable(event);
            queueOutput(message.trim());
        }
    }
}