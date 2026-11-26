package uno.view;

import uno.model.GameContext;

public class ConsoleView {
    public void renderEvents(GameContext context) {
        for (String event : context.consumeEvents()) {
            System.out.println("[СОБЫТИЕ] " + event);
        }
    }

    public void renderState(GameContext context) {
        System.out.println("Верхняя карта сброса: " + context.getDiscardPile().top() +
                " | Активный цвет: " + context.getCurrentColor());
        System.out.println("Ходит игрок: " + context.getCurrentPlayerIndex() +
                " | Направление: " + context.getDirection());
        if (context.getPendingPenaltyDraw() > 0) {
            System.out.println("ВНИМАНИЕ! Накоплен штраф: +" + context.getPendingPenaltyDraw() + " карт!");
        }
        System.out.println("Карты на руке текущего игрока: " +
                context.getHands().get(context.getCurrentPlayerIndex()).getCards());
    }
}
