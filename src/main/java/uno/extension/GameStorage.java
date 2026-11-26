package uno.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import uno.model.GameContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GameStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static String toJson(GameContext context) {
        return GSON.toJson(context);
    }

    public static GameContext fromJson(String json) {
        GameContext context = GSON.fromJson(json, GameContext.class);
        context.restoreRandomAfterDeserialization();
        return context;
    }

    public static void saveToFile(GameContext context, Path filePath) throws IOException {
        Files.writeString(filePath, toJson(context));
    }

    public static GameContext loadFromFile(Path filePath) throws IOException {
        String json = Files.readString(filePath);
        return fromJson(json);
    }
}