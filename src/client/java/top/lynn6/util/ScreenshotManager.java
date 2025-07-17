package top.lynn6.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lynn6.api.ApiClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ScreenshotManager {

    public static final Logger LOGGER = LoggerFactory.getLogger("RaceSafe/Screenshot");

    public static void takeAndUploadScreenshot(String taskId) {
        MinecraftClient client = MinecraftClient.getInstance();
        ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), (nativeImage) -> {
            if (nativeImage == null) {
                LOGGER.error("Failed to take screenshot: nativeImage is null");
                return;
            }
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                nativeImage.writeTo(outputStream);
                byte[] imageBytes = outputStream.toByteArray();
                HttpResponse<String> response = ApiClient.sendScreenshot(taskId, imageBytes);
                LOGGER.info("Screenshot uploaded. Status: " + response.statusCode());
                LOGGER.info("Response body: " + response.body());
            } catch (IOException | InterruptedException | NoSuchAlgorithmException | InvalidKeyException e) {
                LOGGER.error("Failed to upload screenshot.", e);
            } finally {
                nativeImage.close();
            }
        });
    }
}