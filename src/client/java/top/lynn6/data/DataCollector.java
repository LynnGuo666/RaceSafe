package top.lynn6.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;

import java.util.Collection;
import java.util.stream.Collectors;

public class DataCollector {

    public static JsonObject getPlayerInfo() {
        JsonObject playerInfo = new JsonObject();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            playerInfo.addProperty("username", client.player.getName().getString());
            playerInfo.addProperty("uuid", client.player.getUuid().toString());
        }
        return playerInfo;
    }

    public static JsonArray getModList() {
        JsonArray modList = new JsonArray();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            JsonObject modInfo = new JsonObject();
            modInfo.addProperty("id", mod.getMetadata().getId());
            modInfo.addProperty("version", mod.getMetadata().getVersion().getFriendlyString());
            modList.add(modInfo);
        }
        return modList;
    }

    public static JsonArray getResourcePackList() {
        JsonArray resourcePackList = new JsonArray();
        ResourcePackManager manager = MinecraftClient.getInstance().getResourcePackManager();
        manager.scanAndFindPacks(); // Ensure packs are discovered
        for (ResourcePackProfile profile : manager.getAvailableProfiles()) {
            resourcePackList.add(profile.getName());
        }
        return resourcePackList;
    }

    public static JsonArray getEnabledResourcePackList() {
        JsonArray enabledResourcePackList = new JsonArray();
        ResourcePackManager manager = MinecraftClient.getInstance().getResourcePackManager();
        for (ResourcePackProfile profile : manager.getEnabledProfiles()) {
            enabledResourcePackList.add(profile.getName());
        }
        return enabledResourcePackList;
    }

    public static JsonObject collectInitialReport() {
        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", "1.2");
        report.add("player", getPlayerInfo());
        report.add("mods", getModList());
        report.add("availableResourcePacks", getResourcePackList());
        report.add("enabledResourcePacks", getEnabledResourcePackList());
        return report;
    }
}