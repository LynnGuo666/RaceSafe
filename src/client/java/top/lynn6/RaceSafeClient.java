package top.lynn6;

import net.fabricmc.api.ClientModInitializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import top.lynn6.api.ApiClient;
import top.lynn6.config.ConfigManager;
import top.lynn6.data.DataCollector;
import top.lynn6.util.ScreenshotManager;

import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RaceSafeClient implements ClientModInitializer {

	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private static String taskId;

	@Override
	public void onInitializeClient() {
		ConfigManager.loadConfig();

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			new Thread(() -> {
				try {
					Thread.sleep(5000); // Wait for everything to load

					JsonObject report = DataCollector.collectInitialReport();
					HttpResponse<String> response = ApiClient.sendPostRequest("/api/v1/race-safe/client/report", report.toString());

					ApiClient.LOGGER.info("Initial report sent. Status: " + response.statusCode());
					if (response.statusCode() == 200) {
						JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
						if (responseBody.has("taskId")) {
							taskId = responseBody.get("taskId").getAsString();
							ApiClient.LOGGER.info("Received task ID: " + taskId);
							startTaskPolling();
						}
					}
				} catch (Exception e) {
					ApiClient.LOGGER.error("Failed to send initial report.", e);
				}
			}).start();
		});
	}

	private void startTaskPolling() {
		scheduler.scheduleAtFixedRate(() -> {
			if (taskId == null) return;

			try {
				HttpResponse<String> response = ApiClient.sendGetRequest("/api/v1/race-safe/client/tasks/" + taskId);
				ApiClient.LOGGER.info("Task poll status: " + response.statusCode());

				if (response.statusCode() == 200) {
					JsonObject task = JsonParser.parseString(response.body()).getAsJsonObject();
					if (task.has("action") && "screenshot".equals(task.get("action").getAsString())) {
						ApiClient.LOGGER.info("Screenshot requested.");
						ScreenshotManager.takeAndUploadScreenshot(taskId);
					}
				}
			} catch (Exception e) {
				ApiClient.LOGGER.error("Failed to poll for tasks.", e);
			}
		}, 0, 30, TimeUnit.SECONDS); // Poll every 30 seconds
	}
}