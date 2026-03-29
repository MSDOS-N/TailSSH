package ru.tail.ssh;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class PasswordManager {
    private static final Path PASSWORD_FILE = Paths.get("config", TailSSH.MOD_ID, SSHConfig.PASSWORD_FILE);
    private static final ConcurrentHashMap<String, String> users = new ConcurrentHashMap<>();

    static {
        loadPasswords();
    }

    private static void loadPasswords() {
        try {
            if (Files.exists(PASSWORD_FILE)) {
                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(PASSWORD_FILE)) {
                    props.load(is);
                }

                for (String username : props.stringPropertyNames()) {
                    users.put(username, props.getProperty(username));
                }

                TailSSH.LOGGER.info("Loaded {} users from password file", users.size());
            } else {
                createDefaultPasswordFile();
            }
        } catch (Exception e) {
            TailSSH.LOGGER.error("Failed to load passwords", e);
        }
    }

    private static void createDefaultPasswordFile() throws IOException {
        Files.createDirectories(PASSWORD_FILE.getParent());
        Properties props = new Properties();
        props.setProperty("admin", "admin123");

        try (OutputStream os = Files.newOutputStream(PASSWORD_FILE)) {
            props.store(os, "TailSSH Users - Format: username=password");
        }

        users.put("admin", "admin123");
        TailSSH.LOGGER.info("Created default password file with admin user");
    }

    public static boolean authenticate(String username, String password) {
        String storedPassword = users.get(username);
        if (storedPassword != null && storedPassword.equals(password)) {
            TailSSH.LOGGER.info("User '{}' authenticated successfully", username);
            return true;
        }
        TailSSH.LOGGER.warn("Failed authentication attempt for user '{}'", username);
        return false;
    }

    public static boolean addUser(String username, String password) {
        try {
            users.put(username, password);
            savePasswords();
            TailSSH.LOGGER.info("Added user '{}'", username);
            return true;
        } catch (Exception e) {
            TailSSH.LOGGER.error("Failed to add user", e);
            return false;
        }
    }

    public static boolean removeUser(String username) {
        if (users.remove(username) != null) {
            try {
                savePasswords();
                TailSSH.LOGGER.info("Removed user '{}'", username);
                return true;
            } catch (Exception e) {
                TailSSH.LOGGER.error("Failed to remove user", e);
                return false;
            }
        }
        return false;
    }

    private static void savePasswords() throws IOException {
        Properties props = new Properties();
        props.putAll(users);

        try (OutputStream os = Files.newOutputStream(PASSWORD_FILE)) {
            props.store(os, "TailSSH Users");
        }
    }

    public static void listUsers() {
        TailSSH.LOGGER.info("Registered users: {}", users.keySet());
    }
}