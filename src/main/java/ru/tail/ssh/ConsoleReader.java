package ru.tail.ssh;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class ConsoleReader {
    private final InputStream in;
    private final BlockingQueue<String> lineQueue = new LinkedBlockingQueue<>();
    private Thread readerThread;
    private volatile boolean running = true;

    public ConsoleReader(InputStream in) {
        this.in = in;
        start();
    }

    private void start() {
        readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                while (running) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    lineQueue.offer(line);
                }
            } catch (IOException e) {
                if (running) {
                    TailSSH.LOGGER.error("Error reading console input", e);
                }
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public String readLine() throws InterruptedException {
        return lineQueue.poll(1, TimeUnit.SECONDS);
    }

    public void stop() {
        running = false;
        if (readerThread != null) {
            readerThread.interrupt();
        }
    }
}