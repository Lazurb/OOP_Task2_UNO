package uno.effect;

import uno.model.GameContext;

public class ReverseEffect implements CardEffect {
    @Override
    public void apply(GameContext context) {
        if (context.getPlayersCount() == 2) {
            context.advanceTurn();
            context.recordEvent("Направление изменено (для 2 игроков работает как пропуск хода).");
        } else {
            context.reverseDirection();
            context.recordEvent("Направление игры изменено на " + context.getDirection());
        }
    }
}