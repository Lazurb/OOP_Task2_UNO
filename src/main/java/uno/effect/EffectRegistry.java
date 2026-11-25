package uno.effect;

import java.util.HashMap;
import java.util.Map;

public class EffectRegistry {
    private static final Map<String, CardEffect> EFFECTS = new HashMap<>();

    static {
        EFFECTS.put("NORMAL", new NormalEffect());
        EFFECTS.put("SKIP", new SkipEffect());
        EFFECTS.put("REVERSE", new ReverseEffect());
        EFFECTS.put("DRAW_TWO", new DrawTwoEffect());
        EFFECTS.put("WILD", new WildEffect());
        EFFECTS.put("WILD_DRAW_FOUR", new WildDrawFourEffect());
    }

    public static CardEffect get(String key) {
        return EFFECTS.getOrDefault(key, EFFECTS.get("NORMAL"));
    }
}