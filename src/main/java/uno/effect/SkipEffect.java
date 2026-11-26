package uno.effect;

import uno.model.GameContext;

public class SkipEffect implements CardEffect {
    @Override
    public void apply(GameContext context) {
        context.advanceTurn();
        context.recordEvent("Игрок " + context.getCurrentPlayerIndex() + " пропускает ход!");
    }
}