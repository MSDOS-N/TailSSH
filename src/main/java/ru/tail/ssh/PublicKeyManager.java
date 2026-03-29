package ru.tail.ssh;

import java.io.*;
import java.nio.file.*;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PublicKeyManager {
    private static final Path KEYS_DIR = Paths.get("config", TailSSH.MOD_ID, "keys");
    private static final ConcurrentHashMap<String, List<String>> userKeys = new ConcurrentHashMap<>();

    static {
        loadKeys();
    }

    private static void loadKeys() {
        try {
            Files.createDirectories(KEYS_DIR);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(KEYS_DIR, "*.pub")) {
                for (Path keyFile : stream) {
                    String username = keyFile.getFileName().toString().replace(".pub", "");
                    List<String> keys = Files.readAllLines(keyFile);
                    userKeys.put(username, keys);
                }
            }
            TailSSH.LOGGER.info("Loaded keys for {} users", userKeys.size());
        } catch (Exception e) {
            TailSSH.LOGGER.error("Failed to load public keys", e);
        }
    }

    public static boolean authenticate(String username, PublicKey key) {
        List<String> storedKeys = userKeys.get(username);
        if (storedKeys == null) return false;

        String keyString = Base64.getEncoder().encodeToString(key.getEncoded());
        for (String storedKey : storedKeys) {
            if (storedKey.contains(keyString)) {
                TailSSH.LOGGER.info("User '{}' authenticated with public key", username);
                return true;
            }
        }
        return false;
    }

    public static boolean addKey(String username, String publicKey) {
        try {
            Path keyFile = KEYS_DIR.resolve(username + ".pub");
            List<String> keys = new ArrayList<>();
            if (Files.exists(keyFile)) {
                keys.addAll(Files.readAllLines(keyFile));
            }
            keys.add(publicKey);
            Files.write(keyFile, keys);

            userKeys.put(username, keys);
            TailSSH.LOGGER.info("Added public key for user '{}'", username);
            return true;
        } catch (Exception e) {
            TailSSH.LOGGER.error("Failed to add public key", e);
            return false;
        }
    }
}