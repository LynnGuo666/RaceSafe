package top.lynn6.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lynn6.api.ApiClient;
import top.lynn6.util.ChatHelper;

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
   
    				// Debug模式下输出请求构建的内容
    				LOGGER.debug("[{}] Request details for task {}: URL={}, MetadataSize={} bytes, ImageSize={} bytes", 
    					top.lynn6.RaceSafe.MOD_ID, taskId, submissionUrl, metadataJson.getBytes().length, imageBytes.length);
    				LOGGER.debug("[{}] Metadata content: {}", top.lynn6.RaceSafe.MOD_ID, metadataJson);
   
    				LOGGER.info("[{}] Uploading screenshot for task: {}", top.lynn6.RaceSafe.MOD_ID, taskId);
    				HttpResponse<String> response = ApiClient.sendScreenshot(submissionUrl, metadataJson, imageBytes);
    				LOGGER.info("[{}] Screenshot upload finished. Status: {}, Response: {}", top.lynn6.RaceSafe.MOD_ID, response.statusCode(), response.body());
    				
    				if (response.statusCode() == 200) {
    					ChatHelper.sendSuccessMessage("截图上传成功");
    				} else {
    					String errorMsg = "截图上传失败 (状态码: " + response.statusCode() + ")";
    					ChatHelper.sendErrorMessage(errorMsg);
    				}
   
    			} catch (IOException | InterruptedException | NoSuchAlgorithmException | InvalidKeyException e) {
    				String errorMsg = "截图上传失败: " + e.getMessage();
    				ChatHelper.sendErrorMessage(errorMsg);
    				LOGGER.error("[{}] Failed to upload screenshot for task {}: {}", top.lynn6.RaceSafe.MOD_ID, taskId, e.getMessage(), e);
    			} finally {
    				nativeImage.close();
    			}
    		}, "RaceSafe-ScreenshotUploader").start();
    	});
    }
}