package top.lynn6;

import net.fabricmc.api.ClientModInitializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lynn6.api.ApiClient;
import top.lynn6.config.ConfigManager;
import top.lynn6.data.DataCollector;
import top.lynn6.util.ScreenshotManager;
import top.lynn6.util.ChatHelper;
import top.lynn6.util.ReportSubmitter;

import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RaceSafeClient implements ClientModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger(RaceSafe.MOD_ID + "/Client");
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private static String taskUrl;

	@Override
	public void onInitializeClient() {
		LOGGER.info("[{}] RaceSafe client initializing...", RaceSafe.MOD_ID);

		// 加载配置
		ConfigManager.loadConfig();
		LOGGER.info("[{}] Configuration loaded successfully", RaceSafe.MOD_ID);

		// 注册服务器连接事件，在加入世界后发送初始报告
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			LOGGER.info("[{}] Joined a world, sending initial report...", RaceSafe.MOD_ID);
			sendInitialReport();
		});

		// 注册客户端命令
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("racesafe")
				.then(ClientCommandManager.literal("check")
					.executes(context -> {
						ReportSubmitter.submitReport();
						return 1;
					})
				)
			);
		});

		LOGGER.info("[{}] RaceSafe client initialized successfully", RaceSafe.MOD_ID);
	}

	private void sendInitialReport() {
		new Thread(() -> {
			try {
				LOGGER.info("[{}] Collecting and sending initial report...", RaceSafe.MOD_ID);
				JsonObject report = DataCollector.collectInitialReport();
				HttpResponse<String> response = ApiClient.sendPostRequest("/api/v1/race-safe/client/report", report.toString());

				LOGGER.info("[{}] Initial report sent to server", RaceSafe.MOD_ID);
				LOGGER.info("[{}] Response status: {}", RaceSafe.MOD_ID, response.statusCode());

				if (response.statusCode() == 200) {
					JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
					if (responseBody.has("data")) {
						JsonObject data = responseBody.getAsJsonObject("data");
						if (data.has("taskUrl")) {
							taskUrl = data.get("taskUrl").getAsString();
							LOGGER.info("[{}] Received task URL: {}", RaceSafe.MOD_ID, taskUrl);
							// 立即开始第一次任务获取，后续轮询
							startTaskPolling(taskUrl);
						} else {
							LOGGER.warn("[{}] Server response missing taskUrl", RaceSafe.MOD_ID);
						}
					} else {
						LOGGER.warn("[{}] Server response missing data object", RaceSafe.MOD_ID);
					}
				} else {
					String errorMsg = "初始报告提交失败 (状态码: " + response.statusCode() + ")";
					ChatHelper.sendErrorMessage(errorMsg);
					LOGGER.error("[{}] Server returned error status: {} - {}", RaceSafe.MOD_ID, response.statusCode(), response.body());
				}
			} catch (Exception e) {
				String errorMsg = "初始报告提交失败: " + e.getMessage();
				ChatHelper.sendErrorMessage(errorMsg);
				LOGGER.error("[{}] Failed to send initial report: {}", RaceSafe.MOD_ID, e.getMessage(), e);
			}
		}, "RaceSafe-InitialReport").start();
	}

	private void startTaskPolling(String initialTaskUrl) {
		LOGGER.info("[{}] Starting task polling every 30 seconds...", RaceSafe.MOD_ID);
		scheduler.scheduleAtFixedRate(() -> {
			if (taskUrl == null || taskUrl.isEmpty()) {
				LOGGER.warn("[{}] Task URL is null or empty, skipping poll", RaceSafe.MOD_ID);
				return;
			}

			try {
				// 从 taskUrl 中提取路径
				String path = new java.net.URL(taskUrl).getPath();
				HttpResponse<String> response = ApiClient.sendGetRequest(path);
				LOGGER.debug("[{}] Task poll response status: {}", RaceSafe.MOD_ID, response.statusCode());

				if (response.statusCode() == 200) {
					JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
					if (responseBody.has("data") && responseBody.getAsJsonObject("data").has("task")) {
						JsonObject task = responseBody.getAsJsonObject("data").getAsJsonObject("task");
						String taskType = task.get("type").getAsString();
						LOGGER.info("[{}] Received task of type: {}", RaceSafe.MOD_ID, taskType);

						if ("REQUEST_SCREENSHOT".equals(taskType)) {
							String taskId = task.get("taskId").getAsString();
							String submissionUrl = task.get("submissionUrl").getAsString();
							LOGGER.info("[{}] Screenshot task received. Task ID: {}, Submission URL: {}", RaceSafe.MOD_ID, taskId, submissionUrl);
							ScreenshotManager.takeAndUploadScreenshot(taskId, submissionUrl);
						} else if ("NO_OP".equals(taskType)) {
							LOGGER.info("[{}] No operation task received.", RaceSafe.MOD_ID);
						}
					} else {
						LOGGER.warn("[{}] Invalid task response structure from server.", RaceSafe.MOD_ID);
					}
				} else if (response.statusCode() == 404) {
					LOGGER.warn("[{}] Task not found on server (404), task may have expired. Stopping polling.", RaceSafe.MOD_ID);
					scheduler.shutdown(); // 任务不存在或已过期，停止轮询
				} else {
					LOGGER.warn("[{}] Task poll failed with status: {} - {}", RaceSafe.MOD_ID, response.statusCode(), response.body());
				}
			} catch (Exception e) {
				LOGGER.error("[{}] Failed to poll for tasks: {}", RaceSafe.MOD_ID, e.getMessage(), e);
			}
		}, 0, 30, TimeUnit.SECONDS);
	}
}