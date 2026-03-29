package ru.tail.ssh;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class ConsoleWriter {
    private final OutputStream out;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private Thread writerThread;
    private volatile boolean running = true;

    public ConsoleWriter(OutputStream out) {
        this.out = out;
    }

    public void start() {
        writerThread = new Thread(() -> {
            try {
                while (running) {
                    String message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        out.write(message.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                TailSSH.LOGGER.error("Error writing console output", e);
            }
        });
        writerThread.setDaemon(true);
        writerThread.start();
    }

    public void write(String message) {
        if (running) {
            messageQueue.offer(message);
        }
    }

    public void stop() {
        running = false;
        if (writerThread != null) {
            writerThread.interrupt();
        }
    }
}