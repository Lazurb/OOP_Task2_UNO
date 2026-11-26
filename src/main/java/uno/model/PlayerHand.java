package uno.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class PlayerHand {
    private List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        cards.add(card);
    }

    public boolean removeCard(Card card) {
        return cards.remove(card);
    }

    public List<Card> getCards() {
        return cards;
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
    public void sortHand() {
        cards.sort(Comparator.comparing(Card::getColor)
                .thenComparing(Card::getValue));
    }
    public List<Card> getValidCards(Predicate<Card> moveValidator) {
        return cards.stream()
                .filter(moveValidator)
                .toList();
    }

    public int calculatePoints() {
        return cards.stream()
                .mapToInt(card -> card.getValue().getScore())
                .sum();
    }

    public boolean hasColor(CardColor color) {
        return cards.stream().anyMatch(c -> c.getColor() == color);
    }
}