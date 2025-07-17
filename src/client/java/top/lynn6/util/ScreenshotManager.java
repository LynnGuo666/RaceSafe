package top.lynn6.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lynn6.api.ApiClient;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ScreenshotManager {

    public static final Logger LOGGER = LoggerFactory.getLogger(top.lynn6.RaceSafe.MOD_ID + "/Screenshot");
   
    public static void takeAndUploadScreenshot(String taskId, String submissionUrl) {
    	LOGGER.info("[{}] Taking screenshot for task ID: {}", top.lynn6.RaceSafe.MOD_ID, taskId);
    	MinecraftClient client = MinecraftClient.getInstance();
   
    	ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), (nativeImage) -> {
    		if (nativeImage == null) {
    			LOGGER.error("[{}] Failed to take screenshot: NativeImage is null.", top.lynn6.RaceSafe.MOD_ID);
    			return;
    		}
		new Thread(() -> {
			try {
				// 将NativeImage保存到临时文件并读取字节数组
				Path tempFile = Files.createTempFile("racesafe_screenshot", ".png");
				nativeImage.writeTo(tempFile);
				byte[] imageBytes = Files.readAllBytes(tempFile);
				Files.delete(tempFile);    				// 构建 metadata
    				com.google.gson.JsonObject metadata = new com.google.gson.JsonObject();
    				metadata.addProperty("taskId", taskId);
    				metadata.add("user", top.lynn6.data.DataCollector.getUserInfo());
    				metadata.addProperty("timestamp", java.time.Instant.now().toString());
   
    				String metadataJson = metadata.toString();
   
    				LOGGER.info("[{}] Uploading screenshot for task: {}", top.lynn6.RaceSafe.MOD_ID, taskId);
    				HttpResponse<String> response = ApiClient.sendScreenshot(submissionUrl, metadataJson, imageBytes);
    				LOGGER.info("[{}] Screenshot upload finished. Status: {}, Response: {}", top.lynn6.RaceSafe.MOD_ID, response.statusCode(), response.body());
   
    			} catch (IOException | InterruptedException | NoSuchAlgorithmException | InvalidKeyException e) {
    				LOGGER.error("[{}] Failed to upload screenshot for task {}: {}", top.lynn6.RaceSafe.MOD_ID, taskId, e.getMessage(), e);
    			} finally {
    				nativeImage.close();
    			}
    		}, "RaceSafe-ScreenshotUploader").start();
    	});
    }
}