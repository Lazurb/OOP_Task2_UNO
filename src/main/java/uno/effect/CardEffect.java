package uno.effect;

import uno.model.GameContext;

@FunctionalInterface
public interface CardEffect {
    void apply(GameContext context);

    default boolean canStackOn(CardEffect otherPendingEffect) {
        return false;
    }
}