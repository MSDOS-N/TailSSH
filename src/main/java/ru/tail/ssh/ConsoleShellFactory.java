package ru.tail.ssh;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ConsoleShellFactory implements ShellFactory {

    @Override
    public Command createShell(ChannelSession channel) {
        return new ConsoleViewer();
    }

    private static class ConsoleViewer implements Command {
        private InputStream in;
        private OutputStream out;
        private ExitCallback callback;
        private boolean running = true;
        private ConsoleOutputReader outputReader;

        @Override
        public void setInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(OutputStream err) {
            // Not used
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        @Override
        public void start(ChannelSession channel, Environment env) throws IOException {
            // Ignore any input - read-only mode
            Thread inputIgnorer = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1024];
                    while (running) {
                        if (in.available() > 0) {
                            in.read(buffer);
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    // Ignore input errors
                }
            });
            inputIgnorer.setDaemon(true);
            inputIgnorer.start();

            // Start console output streaming
            outputReader = new ConsoleOutputReader(out);
            outputReader.start();

            // Send welcome message
            writeLine("");
            writeLine("=== Minecraft Server Console (Read-Only Mode) ===");
            writeLine("Press Ctrl+C or type 'exit' to disconnect");
            writeLine("Console output will appear below:");
            writeLine("");
        }

        private void writeLine(String line) throws IOException {
            out.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        @Override
        public void destroy(ChannelSession channel) {
            running = false;
            if (outputReader != null) {
                outputReader.stop();
            }
        }

        // Убираем @Override, так как в этой версии SSHD метод signal может отсутствовать
        public void signal(Signal signal) {
            if (signal == Signal.INT || signal == Signal.TERM) {
                running = false;
                if (callback != null) {
                    callback.onExit(0);
                }
            }
        }
    }
}