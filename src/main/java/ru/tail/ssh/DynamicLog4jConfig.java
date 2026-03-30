package ru.tail.ssh;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DynamicLog4jConfig {

    private static boolean configured = false;

    public static void setupLogging() {
        if (configured) return;

        try {
            // Автоматически сканируем все пакеты из загруженных классов
            Set<String> allPackages = scanAllPackages();

            // Создаем конфигурацию программно
            ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

            builder.setStatusLevel(Level.WARN);
            builder.setConfigurationName("AutoDynamicConfig");

            // Консольный аппендер
            AppenderComponentBuilder consoleAppender = builder.newAppender("Console", "CONSOLE")
                    .addAttribute("target", "SYSTEM_OUT")
                    .add(builder.newLayout("PatternLayout")
                            .addAttribute("pattern", "[%d{HH:mm:ss}] [%level] %msg%n"));
            builder.add(consoleAppender);

            // Файловый аппендер
            AppenderComponentBuilder fileAppender = builder.newAppender("File", "RollingRandomAccessFile")
                    .addAttribute("fileName", "logs/latest.log")
                    .addAttribute("filePattern", "logs/%d{yyyy-MM-dd}-%i.log.gz")
                    .add(builder.newLayout("PatternLayout")
                            .addAttribute("pattern", "[%d{HH:mm:ss}] [%level] %msg%n"))
                    .addComponent(builder.newComponent("Policies")
                            .addComponent(builder.newComponent("TimeBasedTriggeringPolicy"))
                            .addComponent(builder.newComponent("OnStartupTriggeringPolicy")));
            builder.add(fileAppender);

            // Корневой логгер
            RootLoggerComponentBuilder rootLogger = builder.newRootLogger(Level.INFO);
            rootLogger.add(builder.newAppenderRef("Console"));
            rootLogger.add(builder.newAppenderRef("File"));
            builder.add(rootLogger);

            // Автоматически добавляем логгер для каждого найденного пакета
            for (String packageName : allPackages) {
                Level level = packageName.contains("tailssh") ? Level.INFO : Level.WARN;

                LoggerComponentBuilder logger = builder.newLogger(packageName, level);
                logger.add(builder.newAppenderRef("Console"));
                logger.add(builder.newAppenderRef("File"));
                builder.add(logger);
            }

            // Применяем конфигурацию
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = builder.build();
            ctx.start(config);

            configured = true;
            TailSSH.LOGGER.info("Auto-configured {} package loggers", allPackages.size());

        } catch (Exception e) {
            TailSSH.LOGGER.error("Failed to setup dynamic logging", e);
        }
    }

    private static Set<String> scanAllPackages() {
        Set<String> packages = new TreeSet<>();

        try {
            // Получаем все загруженные классы через ClassLoader
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // Сканируем все доступные ресурсы
            scanClassLoader(classLoader, packages);

            // Добавляем пакеты из системного ClassLoader
            scanClassLoader(ClassLoader.getSystemClassLoader(), packages);

            // Добавляем стандартные пакеты Java
            packages.add("java.lang");
            packages.add("java.util");
            packages.add("java.io");
            packages.add("java.nio");

        } catch (Exception e) {
            TailSSH.LOGGER.warn("Package scan failed: {}", e.getMessage());
        }

        // Если сканирование не дало результатов, добавляем базовые пакеты
        if (packages.isEmpty()) {
            packages.add("net.minecraft");
            packages.add("net.neoforged");
            packages.add("org.spongepowered");
            packages.add("cpw.mods");
        }

        return packages;
    }

    private static void scanClassLoader(ClassLoader classLoader, Set<String> packages) {
        if (classLoader == null) return;

        try {
            // Получаем все загруженные классы через отражение
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(classLoader);

            for (Class<?> clazz : classes) {
                Package pkg = clazz.getPackage();
                if (pkg != null) {
                    String packageName = pkg.getName();
                    if (packageName != null && !packageName.isEmpty()) {
                        packages.add(packageName);
                        // Добавляем все родительские пакеты
                        addParentPackages(packages, packageName);
                    }
                }
            }
        } catch (Exception e) {
            // Игнорируем, если не удалось получить доступ
        }

        // Рекурсивно сканируем родительский ClassLoader
        scanClassLoader(classLoader.getParent(), packages);
    }

    private static void addParentPackages(Set<String> packages, String packageName) {
        String[] parts = packageName.split("\\.");
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) current.append(".");
            current.append(parts[i]);
            packages.add(current.toString());
        }
    }

    public static void disableAllDebugLogging() {
        try {
            // Сканируем все пакеты и отключаем DEBUG для них
            Set<String> allPackages = scanAllPackages();

            for (String packageName : allPackages) {
                if (!packageName.contains("tailssh")) {
                    Configurator.setLevel(packageName, Level.WARN);
                }
            }

            // Дополнительно отключаем известные системные логгеры
            Configurator.setLevel("org.spongepowered", Level.WARN);
            Configurator.setLevel("cpw.mods", Level.WARN);

            TailSSH.LOGGER.info("Debug logging disabled for {} packages", allPackages.size());
        } catch (Exception e) {
            TailSSH.LOGGER.error("Failed to disable debug logging", e);
        }
    }
}