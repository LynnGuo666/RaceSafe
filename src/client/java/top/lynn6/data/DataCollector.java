package top.lynn6.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

public class DataCollector {

    public static JsonObject getUserInfo() {
        JsonObject userInfo = new JsonObject();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            userInfo.addProperty("username", client.player.getName().getString());
            userInfo.addProperty("uuid", client.player.getUuid().toString());
        }
        return userInfo;
    }

    public static JsonObject getGameInfo() {
        JsonObject gameInfo = new JsonObject();
        gameInfo.addProperty("minecraftVersion", MinecraftClient.getInstance().getGameVersion());
        FabricLoader.getInstance().getModContainer("fabricloader").ifPresent(modContainer ->
                gameInfo.addProperty("fabricLoaderVersion", modContainer.getMetadata().getVersion().getFriendlyString()));
        return gameInfo;
    }

    public static JsonArray getModList() {
        JsonArray modList = new JsonArray();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            JsonObject modInfo = new JsonObject();
            modInfo.addProperty("modId", mod.getMetadata().getId());
            modInfo.addProperty("name", mod.getMetadata().getName());
            modInfo.addProperty("version", mod.getMetadata().getVersion().getFriendlyString());
            modList.add(modInfo);
        }
        return modList;
    }

    public static JsonObject getResourcePacksInfo() {
        JsonObject resourcePacksInfo = new JsonObject();
        ResourcePackManager manager = MinecraftClient.getInstance().getResourcePackManager();

        JsonArray availablePacks = new JsonArray();
        manager.getProfiles().forEach(profile -> availablePacks.add(profile.getId()));
        resourcePacksInfo.add("available", availablePacks);

        JsonArray enabledPacks = new JsonArray();
        manager.getEnabledProfiles().forEach(profile -> enabledPacks.add(profile.getId()));
        resourcePacksInfo.add("enabled", enabledPacks);

        return resourcePacksInfo;
    }

    public static JsonObject collectInitialReport() {
        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", "1.0");
        report.addProperty("timestamp", Instant.now().toString());
        report.add("user", getUserInfo());
        report.add("game", getGameInfo());
        report.add("mods", getModList());
        report.add("resourcePacks", getResourcePacksInfo());
        return report;
    }
}