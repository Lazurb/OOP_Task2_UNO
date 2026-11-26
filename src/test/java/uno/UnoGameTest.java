package uno;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uno.effect.EffectRegistry;
import uno.exception.InvalidMoveException;
import uno.extension.GameStats;
import uno.extension.GameStorage;
import uno.model.*;
import uno.view.ConsoleView;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UnoGameTest {

    // =========================================================================
    // БЛОК 1. НЕДОПУСТИМЫЕ ХОДЫ И ИСКЛЮЧЕНИЯ (Тесты 1–8)
    // =========================================================================

    @Test
    @DisplayName("01. Исключение при ходе картой, не совпадающей ни по цвету, ни по номиналу")
    public void test01_InvalidColorAndValueThrowsException() {
        System.out.println("\n[ТЕСТ 01] Несовпадение цвета и номинала");
        GameContext game = new GameContext(2, 100L);
        game.startNewGame();

        Card top = game.getDiscardPile().top();
        CardColor wrongColor = (top.getColor() == CardColor.BLUE) ? CardColor.GREEN : CardColor.BLUE;
        CardValue wrongValue = (top.getValue() == CardValue.NINE) ? CardValue.EIGHT : CardValue.NINE;
        Card illegal = new Card(wrongColor, wrongValue, "NORMAL", EffectRegistry.get("NORMAL"));
        game.getHands().get(0).addCard(illegal);

        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> game.playCard(0, illegal, null, false));
        System.out.println("Поймано: " + ex.getMessage());
    }

    @Test
    @DisplayName("02. Исключение при попытке походить не в свою очередь")
    public void test02_PlayOutOfTurnThrowsException() {
        System.out.println("\n[ТЕСТ 02] Ход вне очереди");
        GameContext game = new GameContext(3, 101L);
        game.startNewGame();

        Card p1Card = game.getHands().get(1).getCards().get(0);
        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> game.playCard(1, p1Card, CardColor.RED, false));
        System.out.println("Поймано: " + ex.getMessage());
    }

    @Test
    @DisplayName("03. Исключение при попытке сыграть карту, которой нет на руке")
    public void test03_PlayCardNotInHandThrowsException() {
        System.out.println("\n[ТЕСТ 03] Попытка сыграть чужую/отсутствующую карту");
        GameContext game = new GameContext(2, 102L);
        game.startNewGame();

        Card phantom = new Card(CardColor.WILD, CardValue.WILD, "WILD", EffectRegistry.get("WILD"));
        game.getHands().get(0).getCards().removeIf(c -> c.equals(phantom));

        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> game.playCard(0, phantom, CardColor.RED, false));
        System.out.println("Поймано: " + ex.getMessage());
    }

    @Test
    @DisplayName("04. Исключение при розыгрыше Wild без указания нового цвета (null)")
    public void test04_WildWithoutChosenColorThrowsException() {
        System.out.println("\n[ТЕСТ 04] Дикая карта без заказа цвета (null)");
        GameContext game = new GameContext(2, 103L);
        game.startNewGame();

        Card wild = new Card(CardColor.WILD, CardValue.WILD, "WILD", EffectRegistry.get("WILD"));
        game.getHands().get(0).addCard(wild);

        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> game.playCard(0, wild, null, false));
        System.out.println("Поймано: " + ex.getMessage());
    }

    @Test
    @DisplayName("05. Исключение при заказе цвета WILD для дикой карты")
    public void test05_WildWithWildColorThrowsException() {
        System.out.println("\n[ТЕСТ 05] Дикая карта с недопустимым цветом WILD");
        GameContext game = new GameContext(2, 104L);
        game.startNewGame();

        Card wild4 = new Card(CardColor.WILD, CardValue.WILD_DRAW_FOUR, "WILD_DRAW_FOUR", EffectRegistry.get("WILD_DRAW_FOUR"));
        game.getHands().get(0).addCard(wild4);

        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> game.playCard(0, wild4, CardColor.WILD, false));
        System.out.println("Поймано: " + ex.getMessage());
    }

    @Test
    @DisplayName("06. Исключение при попытке закрыть штраф +2 обычной картой того же цвета")
    public void test06_NormalCardCannotStackOnDrawTwoPenalty() {
        System.out.println("\n[ТЕСТ 06] Нельзя покрыть штраф +2 обычной картой");
        GameContext game = new GameContext(2, 105L);
        game.startNewGame();

        CardColor active = game.getCurrentColor();
        Card plusTwo = new Card(active, CardValue.DRAW_TWO, "DRAW_TWO", EffectRegistry.get("DRAW_TWO"));
        Card normalSameColor = new Card(active, CardValue.SEVEN, "NORMAL", EffectRegistry.get("NORMAL"));

        game.getHands().get(0).addCard(plusTwo);
        game.getHands().get(1).addCard(normalSameColor);

        game.playCard(0, plusTwo, null, false);
        assertEquals(2, game.getPendingPenaltyDraw());

        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> game.playCard(1, normalSameColor, null, false));
        System.out.println("Поймано: " + ex.getMessage());
    }

    @Test
    @DisplayName("07. Исключение при попытке совершить ход после завершения партии")
    public void test07_ActionAfterGameOverThrowsException() {
        System.out.println("\n[ТЕСТ 07] Действие после окончания партии");
        GameContext game = new GameContext(2, 106L);
        game.startNewGame();

        // Оставляем игроку 0 одну карту, совпадающую по цвету, и завершаем игру
        CardColor active = game.getCurrentColor();
        Card winningCard = new Card(active, CardValue.ONE, "NORMAL", EffectRegistry.get("NORMAL"));
        game.getHands().get(0).getCards().clear();
        game.getHands().get(0).addCard(winningCard);

        game.playCard(0, winningCard, null, true);
        assertTrue(game.isGameOver());

        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> game.drawCardOrAcceptPenalty(1));
        System.out.println("Поймано: " + ex.getMessage());
    }

    @Test
    @DisplayName("08. Исключение при создании игры с некорректным числом игроков (< 2 или > 10)")
    public void test08_InvalidPlayersCountThrowsException() {
        System.out.println("\n[ТЕСТ 08] Проверка границ числа игроков (1 и 11)");
        assertThrows(IllegalArgumentException.class, () -> new GameContext(1, 1L));
        assertThrows(IllegalArgumentException.class, () -> new GameContext(11, 1L));
        assertDoesNotThrow(() -> new GameContext(2, 1L));
        assertDoesNotThrow(() -> new GameContext(10, 1L));
        System.out.println("Границы [2..10] проверены успешно.");
    }

    // =========================================================================
    // БЛОК 2. ГРАНИЧНЫЕ СИТУАЦИИ ПРАВИЛ УНО (Тесты 9–20)
    // =========================================================================

    @Test
    @DisplayName("09. Граничный случай: автоматическая перетасовка сброса при пустой колоде")
    public void test09_EmptyDeckReshufflesDiscardPile() {
        System.out.println("\n[ТЕСТ 09] Перетасовка сброса при пустой колоде");
        GameContext game = new GameContext(2, 777L);
        ConsoleView view = new ConsoleView();

        Card c1 = new Card(CardColor.RED, CardValue.ONE, "NORMAL", EffectRegistry.get("NORMAL"));
        Card c2 = new Card(CardColor.BLUE, CardValue.TWO, "NORMAL", EffectRegistry.get("NORMAL"));
        Card top = new Card(CardColor.GREEN, CardValue.THREE, "NORMAL", EffectRegistry.get("NORMAL"));

        game.getDiscardPile().add(c1);
        game.getDiscardPile().add(c2);
        game.getDiscardPile().add(top);

        game.drawCardOrAcceptPenalty(0);
        view.renderEvents(game);

        assertEquals(1, game.getHands().get(0).size());
        assertEquals(1, game.getDeck().size());
        assertEquals(1, game.getDiscardPile().size());
        assertEquals(top, game.getDiscardPile().top());
    }

    @Test
    @DisplayName("10. Граничный случай: исчерпание всех карт в колоде и сбросе вызывает IllegalStateException")
    public void test10_CompletelyExhaustedCardsThrowsIllegalState() {
        System.out.println("\n[ТЕСТ 10] Полное исчерпание всех карт в игре");
        GameContext game = new GameContext(2, 888L);
        Card onlyTop = new Card(CardColor.RED, CardValue.ZERO, "NORMAL", EffectRegistry.get("NORMAL"));
        game.getDiscardPile().add(onlyTop);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> game.drawCardOrAcceptPenalty(0));
        System.out.println("Поймано: " + ex.getMessage());
    }

    @Test
    @DisplayName("11. Правило «УНО!»: своевременный выкрик спасает от штрафа")
    public void test11_SayUnoPreventsPenalty() {
        System.out.println("\n[ТЕСТ 11] Игрок вовремя сказал «УНО!»");
        GameContext game = new GameContext(2, 201L);
        game.startNewGame();

        CardColor active = game.getCurrentColor();
        Card c1 = new Card(active, CardValue.THREE, "NORMAL", EffectRegistry.get("NORMAL"));
        Card c2 = new Card(active, CardValue.FOUR, "NORMAL", EffectRegistry.get("NORMAL"));

        game.getHands().get(0).getCards().clear();
        game.getHands().get(0).addCard(c1);
        game.getHands().get(0).addCard(c2);

        game.playCard(0, c1, null, true);
        assertEquals(1, game.getHands().get(0).size(), "Штрафные карты не должны начисляться");
        System.out.println("На руке осталась ровно 1 карта без штрафа.");
    }

    @Test
    @DisplayName("12. Правило «УНО!»: забытый выкрик при 1 оставшейся карте даёт штраф +2")
    public void test12_ForgotUnoGivesTwoPenaltyCards() {
        System.out.println("\n[ТЕСТ 12] Игрок забыл сказать «УНО!» -> штраф +2 карты");
        GameContext game = new GameContext(2, 202L);
        ConsoleView view = new ConsoleView();
        game.startNewGame();

        CardColor active = game.getCurrentColor();
        Card c1 = new Card(active, CardValue.THREE, "NORMAL", EffectRegistry.get("NORMAL"));
        Card c2 = new Card(active, CardValue.FOUR, "NORMAL", EffectRegistry.get("NORMAL"));

        game.getHands().get(0).getCards().clear();
        game.getHands().get(0).addCard(c1);
        game.getHands().get(0).addCard(c2);

        game.playCard(0, c1, null, false);
        view.renderEvents(game);

        assertEquals(3, game.getHands().get(0).size(), "1 карта + 2 штрафные = 3 карты");
    }

    @Test
    @DisplayName("13. Карта Reverse при 2 игроках работает как пропуск хода (ход возвращается сыгравшему)")
    public void test13_ReverseWithTwoPlayersActsAsSkip() {
        System.out.println("\n[ТЕСТ 13] Reverse на 2 игроков работает как Skip");
        GameContext game = new GameContext(2, 203L);
        game.startNewGame();

        CardColor active = game.getCurrentColor();
        Card reverse = new Card(active, CardValue.REVERSE, "REVERSE", EffectRegistry.get("REVERSE"));
        game.getHands().get(0).addCard(reverse);

        assertEquals(0, game.getCurrentPlayerIndex());
        game.playCard(0, reverse, null, false);

        assertEquals(0, game.getCurrentPlayerIndex(), "При 2 игроках ход должен вернуться к игроку 0");
        System.out.println("Текущий игрок после Reverse: " + game.getCurrentPlayerIndex());
    }

    @Test
    @DisplayName("14. Карта Reverse при 4 игроках меняет направление круга на COUNTER_CLOCKWISE")
    public void test14_ReverseWithFourPlayersChangesDirection() {
        System.out.println("\n[ТЕСТ 14] Reverse на 4 игроков разворачивает направление");
        GameContext game = new GameContext(4, 204L);
        game.startNewGame();

        CardColor active = game.getCurrentColor();
        Card reverse = new Card(active, CardValue.REVERSE, "REVERSE", EffectRegistry.get("REVERSE"));
        game.getHands().get(0).addCard(reverse);

        assertEquals(GameDirection.По_часовой, game.getDirection());
        game.playCard(0, reverse, null, false);

        assertEquals(GameDirection.Против_часовой, game.getDirection());
        assertEquals(3, game.getCurrentPlayerIndex(), "После 0 при обратном ходе должен ходить игрок 3");
        System.out.println("Направление: " + game.getDirection() + ", следующий игрок: " + game.getCurrentPlayerIndex());
    }

    @Test
    @DisplayName("15. Карта Skip пропускает следующего игрока")
    public void test15_SkipSkipsNextPlayer() {
        System.out.println("\n[ТЕСТ 15] Проверка эффекта Skip");
        GameContext game = new GameContext(3, 205L);
        game.startNewGame();

        CardColor active = game.getCurrentColor();
        Card skip = new Card(active, CardValue.SKIP, "SKIP", EffectRegistry.get("SKIP"));
        game.getHands().get(0).addCard(skip);

        game.playCard(0, skip, null, false);
        assertEquals(2, game.getCurrentPlayerIndex(), "Игрок 1 должен быть пропущен, ход у игрока 2");
        System.out.println("Игрок 1 пропущен, ход перешёл к игроку " + game.getCurrentPlayerIndex());
    }

    @Test
    @DisplayName("16. Цепочка накопления штрафа +2 -> +4 -> +6 между тремя игроками")
    public void test16_PenaltyStackingChainPlusSix() {
        System.out.println("\n[ТЕСТ 16] Цепочка штрафов +2 -> +4 -> +6");
        GameContext game = new GameContext(3, 206L);
        ConsoleView view = new ConsoleView();
        game.startNewGame();

        CardColor active = game.getCurrentColor();
        Card dt0 = new Card(active, CardValue.DRAW_TWO, "DRAW_TWO", EffectRegistry.get("DRAW_TWO"));
        Card dt1 = new Card(CardColor.RED, CardValue.DRAW_TWO, "DRAW_TWO", EffectRegistry.get("DRAW_TWO"));
        Card dt2 = new Card(CardColor.BLUE, CardValue.DRAW_TWO, "DRAW_TWO", EffectRegistry.get("DRAW_TWO"));

        game.getHands().get(0).addCard(dt0);
        game.getHands().get(1).addCard(dt1);
        game.getHands().get(2).addCard(dt2);

        game.playCard(0, dt0, null, false);
        game.playCard(1, dt1, null, false);
        game.playCard(2, dt2, null, false);
        view.renderEvents(game);

        assertEquals(6, game.getPendingPenaltyDraw());
        int p0Before = game.getHands().get(0).size();
        game.drawCardOrAcceptPenalty(0);
        view.renderEvents(game);

        assertEquals(p0Before + 6, game.getHands().get(0).size());
        assertEquals(0, game.getPendingPenaltyDraw());
    }

    @Test
    @DisplayName("17. Успешное оспаривание блефа Wild Draw Four (+4): штраф +4 уходит блефовавшему")
    public void test17_ChallengeWildDrawFourSuccessWhenBluffing() {
        System.out.println("\n[ТЕСТ 17] Успешное оспаривание блефа +4");
        GameContext game = new GameContext(2, 207L);
        ConsoleView view = new ConsoleView();
        game.startNewGame();

        CardColor initialColor = game.getCurrentColor();
        CardColor newColor = (initialColor == CardColor.RED) ? CardColor.BLUE : CardColor.RED;

        // Игрок 0 имеет карту исходного цвета, но всё равно играет Wild +4 (блеф!)
        Card matchingCard = new Card(initialColor, CardValue.FIVE, "NORMAL", EffectRegistry.get("NORMAL"));
        Card wild4 = new Card(CardColor.WILD, CardValue.WILD_DRAW_FOUR, "WILD_DRAW_FOUR", EffectRegistry.get("WILD_DRAW_FOUR"));
        game.getHands().get(0).addCard(matchingCard);
        game.getHands().get(0).addCard(wild4);

        int p0HandBefore = game.getHands().get(0).size();
        game.playCard(0, wild4, newColor, false);
        view.renderEvents(game);

        boolean challengeSucceeded = game.challengeWildDrawFour(1);
        view.renderEvents(game);

        assertTrue(challengeSucceeded, "Оспаривание блефа должно быть успешным");
        assertEquals(p0HandBefore - 1 + 4, game.getHands().get(0).size(), "Блефовавший игрок 0 должен получить +4 карты");
        assertEquals(1, game.getCurrentPlayerIndex(), "Ход должен остаться у игрока 1");
        assertEquals(0, game.getPendingPenaltyDraw());
    }

    @Test
    @DisplayName("18. Проваленное оспаривание Wild Draw Four (+4): честный ход, оспоривший берёт 6 карт и пропускает ход")
    public void test18_ChallengeWildDrawFourFailsWhenHonest() {
        System.out.println("\n[ТЕСТ 18] Провал оспаривания честного +4 (штраф 6 карт)");
        GameContext game = new GameContext(3, 208L);
        ConsoleView view = new ConsoleView();
        game.startNewGame();

        CardColor initialColor = game.getCurrentColor();
        CardColor otherColor = (initialColor == CardColor.RED) ? CardColor.BLUE : CardColor.RED;

        // Убираем у игрока 0 все карты цвета initialColor -> ход +4 абсолютно честный
        game.getHands().get(0).getCards().removeIf(c -> c.getColor() == initialColor);
        Card wild4 = new Card(CardColor.WILD, CardValue.WILD_DRAW_FOUR, "WILD_DRAW_FOUR", EffectRegistry.get("WILD_DRAW_FOUR"));
        game.getHands().get(0).addCard(wild4);

        int p1HandBefore = game.getHands().get(1).size();
        game.playCard(0, wild4, otherColor, false);

        boolean challengeSucceeded = game.challengeWildDrawFour(1);
        view.renderEvents(game);

        assertFalse(challengeSucceeded, "Оспаривание честного хода должно провалиться");
        assertEquals(p1HandBefore + 6, game.getHands().get(1).size(), "Игрок 1 должен получить 4 + 2 = 6 карт");
        assertEquals(2, game.getCurrentPlayerIndex(), "Игрок 1 пропускает ход, очередь у игрока 2");
    }

    @Test
    @DisplayName("19. Нельзя оспорить Wild Draw Four, если предыдущей картой не была +4")
    public void test19_ChallengeWithoutWildDrawFourThrowsException() {
        System.out.println("\n[ТЕСТ 19] Попытка оспорить обычный ход");
        GameContext game = new GameContext(2, 209L);
        game.startNewGame();

        InvalidMoveException ex = assertThrows(InvalidMoveException.class,
                () -> game.challengeWildDrawFour(0));
        System.out.println("Поймано: " + ex.getMessage());
    }

    @Test
    @DisplayName("20. Корректный переход через 0-го игрока при обратном направлении (0 -> 2 -> 1 -> 0)")
    public void test20_CounterClockwiseWrapAround() {
        System.out.println("\n[ТЕСТ 20] Циклический переход индексов против часовой стрелки");
        GameContext game = new GameContext(3, 210L);
        game.reverseDirection(); // COUNTER_CLOCKWISE

        assertEquals(0, game.getCurrentPlayerIndex());
        game.advanceTurn();
        assertEquals(2, game.getCurrentPlayerIndex());
        game.advanceTurn();
        assertEquals(1, game.getCurrentPlayerIndex());
        game.advanceTurn();
        assertEquals(0, game.getCurrentPlayerIndex());
        System.out.println("Цикл 0 -> 2 -> 1 -> 0 отработал корректно.");
    }

    // =========================================================================
    // БЛОК 3. МОДЕЛИ, СТАТИСТИКА И СЕРИАЛИЗАЦИЯ (Тесты 21–27)
    // =========================================================================

    @Test
    @DisplayName("21. Стандартная колода Уно содержит ровно 108 карт с правильным распределением")
    public void test21_StandardDeckHas108Cards() {
        System.out.println("\n[ТЕСТ 21] Проверка состава стандартной колоды (108 карт)");
        Deck deck = new Deck();
        deck.initializeStandardUnoDeck();

        assertEquals(108, deck.size());
        int wildCount = 0;
        int zeroCount = 0;
        while (!deck.isEmpty()) {
            Card c = deck.draw();
            if (c.getColor() == CardColor.WILD) wildCount++;
            if (c.getValue() == CardValue.ZERO) zeroCount++;
        }
        assertEquals(8, wildCount, "Должно быть 4 WILD + 4 WILD_DRAW_FOUR = 8 диких карт");
        assertEquals(4, zeroCount, "Должно быть ровно по одному нулю каждого из 4 цветов");
        System.out.println("Состав колоды (108 карт, 8 диких, 4 нуля) подтверждён.");
    }

    @Test
    @DisplayName("22. Сортировка руки игрока упорядочивает карты по цвету и номиналу")
    public void test22_PlayerHandSorting() {
        System.out.println("\n[ТЕСТ 22] Проверка сортировки карт на руке");
        PlayerHand hand = new PlayerHand();
        hand.addCard(new Card(CardColor.BLUE, CardValue.NINE, "NORMAL", EffectRegistry.get("NORMAL")));
        hand.addCard(new Card(CardColor.RED, CardValue.FIVE, "NORMAL", EffectRegistry.get("NORMAL")));
        hand.addCard(new Card(CardColor.RED, CardValue.ONE, "NORMAL", EffectRegistry.get("NORMAL")));

        hand.sortHand();
        assertEquals(CardColor.RED, hand.getCards().get(0).getColor());
        assertEquals(CardValue.ONE, hand.getCards().get(0).getValue());
        assertEquals(CardValue.FIVE, hand.getCards().get(1).getValue());
        assertEquals(CardColor.BLUE, hand.getCards().get(2).getColor());
        System.out.println("Отсортированная рука: " + hand.getCards());
    }

    @Test
    @DisplayName("23. Фильтрация валидных ходов на руке по предикату isValidMove")
    public void test23_PlayerHandValidCardsFilter() {
        System.out.println("\n[ТЕСТ 23] Фильтрация допустимых карт на руке");
        GameContext game = new GameContext(2, 303L);
        game.getDiscardPile().add(new Card(CardColor.GREEN, CardValue.FOUR, "NORMAL", EffectRegistry.get("NORMAL")));
        // Устанавливаем currentColor через startNewGame или подмену:
        GameContext activeGame = new GameContext(2, 303L);
        activeGame.startNewGame();
        CardColor active = activeGame.getCurrentColor();

        PlayerHand customHand = new PlayerHand();
        Card validByColor = new Card(active, CardValue.SEVEN, "NORMAL", EffectRegistry.get("NORMAL"));
        Card validWild = new Card(CardColor.WILD, CardValue.WILD, "WILD", EffectRegistry.get("WILD"));
        CardColor badColor = (active == CardColor.RED) ? CardColor.BLUE : CardColor.RED;
        Card invalid = new Card(badColor, CardValue.NINE, "NORMAL", EffectRegistry.get("NORMAL"));

        customHand.addCard(validByColor);
        customHand.addCard(validWild);
        customHand.addCard(invalid);

        List<Card> valid = customHand.getValidCards(activeGame::isValidMove);
        assertTrue(valid.contains(validByColor));
        assertTrue(valid.contains(validWild));
        System.out.println("Найдено допустимых карт: " + valid.size() + " -> " + valid);
    }

    @Test
    @DisplayName("24. Подсчёт штрафных очков на руке проигравшего (цифры, активные +20, дикие +50)")
    public void test24_HandPointsCalculation() {
        System.out.println("\n[ТЕСТ 24] Подсчёт штрафных очков на руке");
        PlayerHand hand = new PlayerHand();
        hand.addCard(new Card(CardColor.RED, CardValue.SEVEN, "NORMAL", EffectRegistry.get("NORMAL"))); // 7
        hand.addCard(new Card(CardColor.BLUE, CardValue.SKIP, "SKIP", EffectRegistry.get("SKIP"))); // 20
        hand.addCard(new Card(CardColor.WILD, CardValue.WILD_DRAW_FOUR, "WILD_DRAW_FOUR", EffectRegistry.get("WILD_DRAW_FOUR"))); // 50

        assertEquals(77, hand.calculatePoints());
        System.out.println("Сумма очков (7 + 20 + 50): " + hand.calculatePoints());
    }

    @Test
    @DisplayName("25. Аналитика GameStats корректно считает частоту цветов и общее число сыгранных карт")
    public void test25_GameStatsAnalytics() {
        System.out.println("\n[ТЕСТ 25] Проверка аналитики GameStats");
        GameStats stats = new GameStats();
        stats.recordCardPlayed(0, new Card(CardColor.RED, CardValue.ONE, "NORMAL", EffectRegistry.get("NORMAL")));
        stats.recordCardPlayed(1, new Card(CardColor.RED, CardValue.TWO, "NORMAL", EffectRegistry.get("NORMAL")));
        stats.recordCardPlayed(0, new Card(CardColor.GREEN, CardValue.SKIP, "SKIP", EffectRegistry.get("SKIP")));

        assertEquals(3, stats.getTotalCardsPlayed());
        Map<CardColor, Long> freq = stats.getColorFrequency();
        assertEquals(2L, freq.get(CardColor.RED));
        assertEquals(1L, freq.get(CardColor.GREEN));
        System.out.println("Статистика цветов: " + freq);
    }

    @Test
    @DisplayName("26. Сериализация и десериализация в JSON сохраняет состояние игры и восстанавливает эффекты карт")
    public void test26_JsonSerializationPreservesStateAndEffects() {
        System.out.println("\n[ТЕСТ 26] JSON-сериализация и восстановление transient-эффектов");
        GameContext game = new GameContext(3, 404L);
        game.startNewGame();
        for (int i = 0; i < 10 && !game.isGameOver(); i++) {
            game.makeBotTurn();
        }

        String json = GameStorage.toJson(game);
        GameContext restored = GameStorage.fromJson(json);

        assertEquals(game.getCurrentPlayerIndex(), restored.getCurrentPlayerIndex());
        assertEquals(game.getCurrentColor(), restored.getCurrentColor());
        assertEquals(game.getDiscardPile().top(), restored.getDiscardPile().top());
        assertNotNull(restored.getDiscardPile().top().getEffect(), "Transient CardEffect должен восстановиться через EffectRegistry");
        System.out.println("JSON успешно восстановлен. Текущий игрок: " + restored.getCurrentPlayerIndex());
    }

    @Test
    @DisplayName("27. Сохранение и загрузка состояния партии в файл на диске через GameStorage")
    public void test27_SaveAndLoadToFile(@TempDir Path tempDir) throws IOException {
        System.out.println("\n[ТЕСТ 27] Запись и чтение файла сохранения партии");
        GameContext game = new GameContext(2, 505L);
        game.startNewGame();
        game.makeBotTurn();

        Path saveFile = tempDir.resolve("uno_save.json");
        GameStorage.saveToFile(game, saveFile);
        GameContext loaded = GameStorage.loadFromFile(saveFile);

        assertEquals(game.getCurrentPlayerIndex(), loaded.getCurrentPlayerIndex());
        assertEquals(game.getStats().getTotalCardsPlayed(), loaded.getStats().getTotalCardsPlayed());
        System.out.println("Файл успешно сохранён и прочитан: " + saveFile.getFileName());
    }

    // =========================================================================
    // БЛОК 4. ПОЛНЫЕ ПАРТИИ ПО ЗАФИКСИРОВАННОМУ ПОСЕВУ (SEED) (Тесты 28–32)
    // =========================================================================

    @Test
    @DisplayName("28. Полная партия по фиксированному seed (3 игрока): 100% воспроизводимость результата")
    public void test28_FullGameDeterminismThreePlayers() {
        System.out.println("\n[ТЕСТ 28] Полная партия по seed=123456789L (3 игрока)");
        long seed = 123456789L;

        GameContext g1 = runFullBotGame(3, seed);
        GameContext g2 = runFullBotGame(3, seed);

        assertTrue(g1.isGameOver());
        assertEquals(g1.getWinnerIndex(), g2.getWinnerIndex());
        assertEquals(g1.getStats().getWinnerScore(), g2.getStats().getWinnerScore());
        assertEquals(g1.getStats().getTotalCardsPlayed(), g2.getStats().getTotalCardsPlayed());
        assertEquals(g1.getDiscardPile().top(), g2.getDiscardPile().top());

        System.out.println("Итог (seed=" + seed + "): Победитель = Игрок " + g1.getWinnerIndex() +
                ", очки = " + g1.getStats().getWinnerScore() +
                ", сыграно карт = " + g1.getStats().getTotalCardsPlayed());
    }

    @Test
    @DisplayName("29. Полная партия по фиксированному seed (2 игрока — дуэль с правилом Reverse=Skip)")
    public void test29_FullGameDeterminismTwoPlayers() {
        System.out.println("\n[ТЕСТ 29] Полная партия по seed=2026L (2 игрока)");
        long seed = 2026L;

        GameContext g1 = runFullBotGame(2, seed);
        GameContext g2 = runFullBotGame(2, seed);

        assertTrue(g1.isGameOver());
        assertEquals(g1.getWinnerIndex(), g2.getWinnerIndex());
        assertEquals(g1.getStats().getTotalCardsPlayed(), g2.getStats().getTotalCardsPlayed());
        System.out.println("Дуэль завершена: Победитель = Игрок " + g1.getWinnerIndex() +
                ", сыграно карт = " + g1.getStats().getTotalCardsPlayed());
    }

    @Test
    @DisplayName("30. Полная партия по фиксированному seed (4 игрока)")
    public void test30_FullGameDeterminismFourPlayers() {
        System.out.println("\n[ТЕСТ 30] Полная партия по seed=424242L (4 игрока)");
        long seed = 424242L;

        GameContext g1 = runFullBotGame(4, seed);
        GameContext g2 = runFullBotGame(4, seed);

        assertTrue(g1.isGameOver());
        assertEquals(g1.getWinnerIndex(), g2.getWinnerIndex());
        assertEquals(g1.getStats().getWinnerScore(), g2.getStats().getWinnerScore());
        System.out.println("Партия на 4 игроков завершена: Победитель = Игрок " + g1.getWinnerIndex() +
                ", очки = " + g1.getStats().getWinnerScore());
    }

    @Test
    @DisplayName("31. Полная партия по фиксированному seed (6 игроков с многократной перетасовкой колоды)")
    public void test31_FullGameDeterminismSixPlayersWithReshuffles() {
        System.out.println("\n[ТЕСТ 31] Полная партия по seed=999999L (6 игроков)");
        long seed = 999999L;

        GameContext g1 = runFullBotGame(6, seed);
        GameContext g2 = runFullBotGame(6, seed);

        assertTrue(g1.isGameOver());
        assertEquals(g1.getWinnerIndex(), g2.getWinnerIndex());
        assertEquals(g1.getStats().getTotalCardsPlayed(), g2.getStats().getTotalCardsPlayed());
        System.out.println("Партия на 6 игроков завершена: Победитель = Игрок " + g1.getWinnerIndex() +
                ", сыграно карт = " + g1.getStats().getTotalCardsPlayed());
    }

    @Test
    @DisplayName("32. Партии с разными seed дают различный ход игры")
    public void test32_DifferentSeedsProduceDifferentGames() {
        System.out.println("\n[ТЕСТ 32] Сравнение партий с разными seed");
        GameContext g1 = runFullBotGame(3, 11111L);
        GameContext g2 = runFullBotGame(3, 98765L);

        assertTrue(g1.isGameOver() && g2.isGameOver());
        boolean differs = !g1.getWinnerIndex().equals(g2.getWinnerIndex())
                || g1.getStats().getTotalCardsPlayed() != g2.getStats().getTotalCardsPlayed();
        assertTrue(differs, "Разные посевы должны приводить к разному развитию партии");
        System.out.println("Seed 11111 -> карт: " + g1.getStats().getTotalCardsPlayed() +
                " | Seed 98765 -> карт: " + g2.getStats().getTotalCardsPlayed());
    }

    private GameContext runFullBotGame(int playersCount, long seed) {
        GameContext game = new GameContext(playersCount, seed);
        game.startNewGame();
        int safetyCounter = 0;
        while (!game.isGameOver() && safetyCounter < 5000) {
            game.makeBotTurn();
            safetyCounter++;
        }
        return game;
    }
}