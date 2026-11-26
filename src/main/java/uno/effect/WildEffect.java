package uno.effect;

import uno.model.GameContext;

public class WildEffect implements CardEffect {
    @Override
    public void apply(GameContext context) {
        context.recordEvent("Карта джокер назначила новый цвет: " + context.getCurrentColor());
    }
}