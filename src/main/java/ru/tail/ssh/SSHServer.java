package ru.tail.ssh;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.io.*;
import java.nio.file.*;
import java.security.PublicKey;

public class SSHServer {
    private SshServer sshd;
    private boolean running = false;

    public void start() throws Exception {
        SSHConfig.load();

        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(SSHConfig.PORT);

        // Setup host key
        Path hostKeyPath = Paths.get(SSHConfig.HOST_KEY_PATH);
        if (!Files.exists(hostKeyPath)) {
            Files.createFile(hostKeyPath);
        }
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(hostKeyPath));

        // Setup authenticators
        if (SSHConfig.ALLOW_PASSWORD_AUTH) {
            sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
                @Override
                public boolean authenticate(String username, String password, ServerSession session) {
                    return PasswordManager.authenticate(username, password);
                }
            });
        }

        if (SSHConfig.ALLOW_PUBLIC_KEY_AUTH) {
            sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
                @Override
                public boolean authenticate(String username, PublicKey key, ServerSession session) {
                    return PublicKeyManager.authenticate(username, key);
                }
            });
        }

        // Set shell factory for read-only console viewing
        sshd.setShellFactory(new ConsoleShellFactory());

        // Disable command execution
        sshd.setCommandFactory(null);

        // Start server
        sshd.start();
        running = true;
        TailSSH.LOGGER.info("SSH Server started in READ-ONLY mode on port {}", SSHConfig.PORT);
    }

    public void stop() throws Exception {
        if (sshd != null && running) {
            sshd.stop();
            running = false;
        }
    }

    public boolean isRunning() {
        return running;
    }
}