package uno.model;

import uno.effect.CardEffect;
import uno.effect.EffectRegistry;
import java.util.Objects;

public class Card {
    private CardColor color;
    private CardValue value;
    private String effectKey; // Храним ключ для сериализации в JSON
    private transient CardEffect effect;

    public Card(CardColor color, CardValue value, String effectKey, CardEffect effect) {
        this.color = color;
        this.value = value;
        this.effectKey = effectKey;
        this.effect = effect;
    }

    public CardColor getColor() { return color; }
    public CardValue getValue() { return value; }
    public String getEffectKey() { return effectKey; }

    public CardEffect getEffect() {
        if (effect == null) {
            effect = EffectRegistry.get(effectKey);
        }
        return effect;
    }

    public void applyEffect(GameContext context) {
        getEffect().apply(context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card card)) return false;
        return color == card.color && value == card.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, value);
    }

    @Override
    public String toString() {
        return color + "_" + value;
    }
}