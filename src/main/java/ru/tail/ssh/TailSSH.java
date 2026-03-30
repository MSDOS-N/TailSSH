package ru.tail.ssh;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TailSSH.MOD_ID)
public class TailSSH {
    public static final String MOD_ID = "tailssh";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private SSHServer sshServer;

    public TailSSH(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
        NeoForge.EVENT_BUS.addListener(this::serverStopping);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("TailSSH mod initializing...");

        // Автоматическая настройка логгеров для всех модов
        DynamicLog4jConfig.setupLogging();

        // Отключаем все DEBUG сообщения
        DynamicLog4jConfig.disableAllDebugLogging();

        LOGGER.info("Dynamic logging configured - all DEBUG messages disabled");
    }

    private void serverStarted(final ServerStartedEvent event) {
        try {
            sshServer = new SSHServer();
            sshServer.start();
            LOGGER.info("SSH Server started successfully on port {}", SSHConfig.PORT);
        } catch (Exception e) {
            LOGGER.error("Failed to start SSH server", e);
        }
    }

    private void serverStopping(final ServerStoppingEvent event) {
        if (sshServer != null) {
            try {
                sshServer.stop();
                LOGGER.info("SSH Server stopped");
            } catch (Exception e) {
                LOGGER.error("Error stopping SSH server", e);
            }
        }
    }
}