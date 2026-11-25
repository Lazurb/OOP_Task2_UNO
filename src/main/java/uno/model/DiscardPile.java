package uno.model;

import java.util.ArrayList;
import java.util.List;

public class DiscardPile {
    private List<Card> pile = new ArrayList<>();

    public void add(Card card) {
        pile.add(card);
    }

    public Card top() {
        if (pile.isEmpty()) return null;
        return pile.get(pile.size() - 1);
    }

    // Забирает все карты, кроме верхней, для перетасовки обратно в колоду
    public List<Card> takeAllExceptTop() {
        if (pile.size() <= 1) {
            return new ArrayList<>();
        }
        Card topCard = top();
        List<Card> recycled = new ArrayList<>(pile.subList(0, pile.size() - 1));
        pile.clear();
        pile.add(topCard);
        return recycled;
    }

    public int size() {
        return pile.size();
    }
}