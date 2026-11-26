package uno.model;

import uno.effect.EffectRegistry;
import java.util.*;

public class Deck {
    private Deque<Card> cards = new ArrayDeque<>();

    public Deck() {}

    public void initializeStandardUnoDeck() {
        cards.clear();
        for (CardColor color : new CardColor[]{CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE}) {
            cards.push(createCard(color, CardValue.ZERO, "Стандартная"));
            for (int i = 0; i < 2; i++) {
                cards.push(createCard(color, CardValue.ONE, "Стандартная"));
                cards.push(createCard(color, CardValue.TWO, "Стандартная"));
                cards.push(createCard(color, CardValue.THREE, "Стандартная"));
                cards.push(createCard(color, CardValue.FOUR, "Стандартная"));
                cards.push(createCard(color, CardValue.FIVE, "Стандартная"));
                cards.push(createCard(color, CardValue.SIX, "Стандартная"));
                cards.push(createCard(color, CardValue.SEVEN, "Стандартная"));
                cards.push(createCard(color, CardValue.EIGHT, "Стандартная"));
                cards.push(createCard(color, CardValue.NINE, "Стандартная"));
                cards.push(createCard(color, CardValue.SKIP, "Пропуск хода"));
                cards.push(createCard(color, CardValue.REVERSE, "Изменить порядок ходов"));
                cards.push(createCard(color, CardValue.DRAW_TWO, "Взять две"));
            }
        }
        for (int i = 0; i < 4; i++) {
            cards.push(createCard(CardColor.WILD, CardValue.WILD, "Изменить цвет"));
            cards.push(createCard(CardColor.WILD, CardValue.WILD_DRAW_FOUR, "Изменить цвет + взять 4"));
        }
    }

    private Card createCard(CardColor color, CardValue value, String effectKey) {
        return new Card(color, value, effectKey, EffectRegistry.get(effectKey));
    }
    public void shuffle(Random random) {
        List<Card> tempList = new ArrayList<>(cards);
        Collections.shuffle(tempList, random);
        cards.clear();
        tempList.forEach(cards::push);
    }

    public Card draw() {
        return cards.pollFirst();
    }

    public void addCards(Collection<Card> newCards) {
        cards.addAll(newCards);
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int size() {
        return cards.size();
    }
}