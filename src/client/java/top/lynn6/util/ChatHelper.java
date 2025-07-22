package top.lynn6.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatHelper {
    
    private static final String PREFIX = "[RaceSafe] ";
    
    public static void sendErrorMessage(String message) {
        sendMessage(message, Formatting.RED);
    }
    
    public static void sendSuccessMessage(String message) {
        sendMessage(message, Formatting.GREEN);
    }
    
    public static void sendInfoMessage(String message) {
        sendMessage(message, Formatting.YELLOW);
    }
    
    private static void sendMessage(String message, Formatting color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            Text text = Text.literal(PREFIX + message).formatted(color);
            client.player.sendMessage(text, false);
        }
    }
}