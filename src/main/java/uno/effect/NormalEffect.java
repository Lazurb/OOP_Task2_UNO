package uno.effect;

import uno.model.GameContext;

public class NormalEffect implements CardEffect {
    @Override
    public void apply(GameContext context) {
        context. recordEvent("Обычный ход.");
    }
}