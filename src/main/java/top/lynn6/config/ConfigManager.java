package top.lynn6.config;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import java.util.UUID;

public class ConfigManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("RaceSafe/Config");
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("racesafe.properties");
    private static final String BUILD_CONFIG_PATH = "/racesafe_build.properties";

    private static final Properties userProperties = new Properties();
    private static final Properties buildProperties = new Properties();

    public static String accessKey = "";
    public static String secretKey = "";
    public static String serverUrl = "";

    public static void loadConfig() {
        // Load build-time properties from resources
        try (InputStream buildInput = ConfigManager.class.getResourceAsStream(BUILD_CONFIG_PATH)) {
            if (buildInput == null) {
                LOGGER.error("Build config file not found in resources!");
            } else {
                buildProperties.load(buildInput);
                secretKey = buildProperties.getProperty("secret_key", "");
                serverUrl = buildProperties.getProperty("server_url", "http://localhost:8000"); // Default value
                LOGGER.info("Build configuration loaded successfully.");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load build configuration.", e);
        }

        // Load or create user-specific properties
        if (Files.exists(CONFIG_FILE)) {
            try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
                userProperties.load(input);
                accessKey = userProperties.getProperty("access_key", "");
            } catch (IOException e) {
                LOGGER.error("Failed to load user configuration file.", e);
            }
        }

        if (accessKey == null || accessKey.isEmpty()) {
            LOGGER.warn("Access key not found or empty, creating a new one.");
            generateAndSaveAccessKey();
        } else {
            LOGGER.info("User configuration loaded successfully.");
        }
    }

    private static void generateAndSaveAccessKey() {
        accessKey = UUID.randomUUID().toString();
        userProperties.setProperty("access_key", accessKey);
        try (OutputStream output = Files.newOutputStream(CONFIG_FILE)) {
            userProperties.store(output, "RaceSafe User Configuration");
            LOGGER.info("New access key generated and saved to: " + CONFIG_FILE);
        } catch (IOException e) {
            LOGGER.error("Failed to create or save user configuration file.", e);
        }
    }
}