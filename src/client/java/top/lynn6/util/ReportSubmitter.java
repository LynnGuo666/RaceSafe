package top.lynn6.util;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.lynn6.RaceSafe;
import top.lynn6.api.ApiClient;
import top.lynn6.data.DataCollector;

import java.net.http.HttpResponse;

public class ReportSubmitter {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(RaceSafe.MOD_ID + "/ReportSubmitter");
    
    public static void submitReport() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            ChatHelper.sendErrorMessage("无法提交报告：未加入游戏世界");
            return;
        }
        
        ChatHelper.sendInfoMessage("正在收集并提交报告信息...");
        
        new Thread(() -> {
            try {
                LOGGER.info("[{}] Manual report submission started", RaceSafe.MOD_ID);
                JsonObject report = DataCollector.collectInitialReport();
                HttpResponse<String> response = ApiClient.sendPostRequest("/api/v1/race-safe/client/report", report.toString());
                
                LOGGER.info("[{}] Manual report submission completed with status: {}", RaceSafe.MOD_ID, response.statusCode());
                
                if (response.statusCode() == 200) {
                    ChatHelper.sendSuccessMessage("报告提交成功！");
                    LOGGER.info("[{}] Manual report submitted successfully", RaceSafe.MOD_ID);
                } else {
                    String errorMsg = "报告提交失败 (状态码: " + response.statusCode() + ")";
                    ChatHelper.sendErrorMessage(errorMsg);
                    LOGGER.error("[{}] Manual report submission failed: {} - {}", RaceSafe.MOD_ID, response.statusCode(), response.body());
                }
                
            } catch (Exception e) {
                String errorMsg = "报告提交失败: " + e.getMessage();
                ChatHelper.sendErrorMessage(errorMsg);
                LOGGER.error("[{}] Manual report submission failed with exception: {}", RaceSafe.MOD_ID, e.getMessage(), e);
            }
        }, "RaceSafe-ManualReport").start();
    }
}