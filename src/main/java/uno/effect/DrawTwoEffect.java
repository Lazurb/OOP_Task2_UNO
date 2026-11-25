package uno.effect;

import uno.model.GameContext;

public class DrawTwoEffect implements CardEffect {
    @Override
    public void apply(GameContext context) {
        context.addPendingPenalty(2);
        context.recordEvent("Накоплен штраф: +" + context.getPendingPenaltyDraw() + " карт.");
    }

    @Override
    public boolean canStackOn(CardEffect other) {
        return other instanceof DrawTwoEffect;
    }
}