package ru.tail.ssh;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class SSHConfig {
    public static int PORT = 2222;
    public static String HOST_KEY_PATH = "ssh_host_key";
    public static String PASSWORD_FILE = "ssh_passwords.properties";
    public static boolean ALLOW_PASSWORD_AUTH = true;
    public static boolean ALLOW_PUBLIC_KEY_AUTH = false;
    public static String MODE = "readonly"; // readonly or interactive (interactive disabled)

    private static final Path CONFIG_DIR = Paths.get("config", TailSSH.MOD_ID);
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");

    public static void load() {
        try {
            Files.createDirectories(CONFIG_DIR);

            if (Files.exists(CONFIG_FILE)) {
                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(CONFIG_FILE)) {
                    props.load(is);
                }

                PORT = Integer.parseInt(props.getProperty("port", "2222"));
                HOST_KEY_PATH = props.getProperty("host_key_path", "ssh_host_key");
                PASSWORD_FILE = props.getProperty("password_file", "ssh_passwords.properties");
                ALLOW_PASSWORD_AUTH = Boolean.parseBoolean(props.getProperty("allow_password_auth", "true"));
                ALLOW_PUBLIC_KEY_AUTH = Boolean.parseBoolean(props.getProperty("allow_public_key_auth", "false"));
                MODE = props.getProperty("mode", "readonly");

                TailSSH.LOGGER.info("SSH Config loaded: port={}, mode={}, password_auth={}",
                        PORT, MODE, ALLOW_PASSWORD_AUTH);

                if (!"readonly".equalsIgnoreCase(MODE)) {
                    TailSSH.LOGGER.warn("Only 'readonly' mode is supported. Interactive mode is disabled.");
                    MODE = "readonly";
                }
            } else {
                saveDefaultConfig();
            }
        } catch (Exception e) {
            TailSSH.LOGGER.error("Failed to load SSH config", e);
        }
    }

    private static void saveDefaultConfig() {
        try {
            Properties props = new Properties();
            props.setProperty("port", "2222");
            props.setProperty("host_key_path", "ssh_host_key");
            props.setProperty("password_file", "ssh_passwords.properties");
            props.setProperty("allow_password_auth", "true");
            props.setProperty("allow_public_key_auth", "false");
            props.setProperty("mode", "readonly");

            try (OutputStream os = Files.newOutputStream(CONFIG_FILE)) {
                props.store(os, "TailSSH Configuration - READ-ONLY MODE");
            }

            TailSSH.LOGGER.info("Default SSH config created at {}", CONFIG_FILE);
        } catch (Exception e) {
            TailSSH.LOGGER.error("Failed to save default config", e);
        }
    }
}