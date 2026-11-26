package uno.effect;

import uno.model.GameContext;

public class WildDrawFourEffect implements CardEffect {
    @Override
    public void apply(GameContext context) {
        context.addPendingPenalty(4);
        context.setChallengeableState(true);
        context.recordEvent("Цвет изменён" + context.getCurrentColor() +
                " и штраф +4! Следующий игрок может оспорить ход.");
    }
}