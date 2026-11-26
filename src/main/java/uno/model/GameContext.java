package uno.model;

import uno.exception.InvalidMoveException;
import uno.extension.GameStats;

import java.util.*;

public class GameContext {
    private long seed;
    private transient Random random;

    private Deck deck;
    private DiscardPile discardPile;
    private List<PlayerHand> hands;
    private GameStats stats;

    private int currentPlayerIndex;
    private GameDirection direction;
    private CardColor currentColor;
    private boolean gameOver;
    private Integer winnerIndex;

    // ЭТАП 3: Поля для накопления штрафов и оспаривания Wild Draw Four
    private int pendingPenaltyDraw = 0;
    private boolean challengeableState = false;
    private CardColor colorBeforeWild = null;
    private int lastPlayerWhoPlayedWild = -1;

    private List<String> eventLog = new ArrayList<>();

    public GameContext(int playersCount, long seed) {
        if (playersCount < 2 || playersCount > 10) {
            throw new IllegalArgumentException("Количество игроков должно быть от 2 до 10");
        }
        this.seed = seed;
        this.random = new Random(seed);
        this.deck = new Deck();
        this.discardPile = new DiscardPile();
        this.hands = new ArrayList<>(playersCount);
        this.stats = new GameStats();
        this.direction = GameDirection.По_часовой;
        this.currentPlayerIndex = 0;

        for (int i = 0; i < playersCount; i++) {
            hands.add(new PlayerHand());
        }
    }

    public void restoreRandomAfterDeserialization() {
        this.random = new Random(this.seed);
    }

    public void startNewGame() {
        deck.initializeStandardUnoDeck();
        deck.shuffle(random);

        for (int i = 0; i < hands.size(); i++) {
            for (int c = 0; c < 7; c++) {
                hands.get(i).addCard(drawSingleCardFromDeck());
            }
            hands.get(i).sortHand();
        }

        Card firstCard = drawSingleCardFromDeck();
        while (firstCard.getColor() == CardColor.WILD) {
            deck.addCards(List.of(firstCard));
            deck.shuffle(random);
            firstCard = drawSingleCardFromDeck();
        }
        discardPile.add(firstCard);
        currentColor = firstCard.getColor();
        recordEvent("Игра началась! Первая карта: " + firstCard);
    }

    public boolean isValidMove(Card card) {
        Card topCard = discardPile.top();
        if (pendingPenaltyDraw > 0) {
            return card.getEffect().canStackOn(topCard.getEffect());
        }
        if (card.getColor() == CardColor.WILD) {
            return true;
        }
        return card.getColor() == currentColor || card.getValue() == topCard.getValue();
    }

    public void playCard(int playerIndex, Card card, CardColor chosenWildColor, boolean saidUno) {
        validateTurnAndState(playerIndex);

        PlayerHand hand = hands.get(playerIndex);
        if (!hand.getCards().contains(card)) {
            throw new InvalidMoveException("У игрока " + playerIndex + " нет карты " + card);
        }
        if (!isValidMove(card)) {
            throw new InvalidMoveException("Недопустимый ход картой " + card + " поверх " + discardPile.top() + " (активный цвет: " + currentColor + ")");
        }

        // Сбрасываем флаг оспаривания предыдущего хода
        this.challengeableState = false;
        this.colorBeforeWild = this.currentColor;
        this.lastPlayerWhoPlayedWild = playerIndex;

        hand.removeCard(card);
        discardPile.add(card);
        stats.recordCardPlayed(playerIndex, card);

        if (card.getColor() == CardColor.WILD) {
            if (chosenWildColor == null || chosenWildColor == CardColor.WILD) {
                throw new InvalidMoveException("Для дикой карты необходимо заказать цвет (RED, YELLOW, GREEN, BLUE)");
            }
            this.currentColor = chosenWildColor;
        } else {
            this.currentColor = card.getColor();
        }

        recordEvent("Игрок " + playerIndex + " сыграл " + card);

        // Проверка правила «Уно!» (если осталась 1 карта, но игрок промолчал -> штраф 2 карты)
        if (hand.size() == 1) {
            if (saidUno) {
                recordEvent("Игрок " + playerIndex + " крикнул «УНО!»");
            } else {
                recordEvent("Игрок " + playerIndex + " забыл крикнуть «УНО!» и получает 2 штрафные карты!");
                drawCardsForPlayer(playerIndex, 2);
            }
        }

        // Проверка победы
        if (hand.isEmpty()) {
            this.gameOver = true;
            this.winnerIndex = playerIndex;
            int totalScore = calculateWinnerPoints(playerIndex);
            stats.recordWinner(playerIndex, totalScore);
            recordEvent("Игрок " + playerIndex + " ПОБЕДИЛ! Заработано очков: " + totalScore);
            return;
        }

        card.applyEffect(this);

        advanceTurn();
    }

    public void drawCardOrAcceptPenalty(int playerIndex) {
        validateTurnAndState(playerIndex);
        this.challengeableState = false;

        if (pendingPenaltyDraw > 0) {
            recordEvent("Игрок " + playerIndex + " принимает штраф и берет карт: " + pendingPenaltyDraw);
            drawCardsForPlayer(playerIndex, pendingPenaltyDraw);
            pendingPenaltyDraw = 0;
            advanceTurn();
            return;
        }

        Card drawn = drawSingleCardFromDeck();
        hands.get(playerIndex).addCard(drawn);
        recordEvent("Игрок " + playerIndex + " берет карту из колоды.");
        advanceTurn();
    }

    public boolean challengeWildDrawFour(int challengerIndex) {
        validateTurnAndState(challengerIndex);
        if (!challengeableState || pendingPenaltyDraw < 4) {
            throw new InvalidMoveException("Сейчас нельзя оспорить ход!");
        }

        this.challengeableState = false;
        PlayerHand guiltyHand = hands.get(lastPlayerWhoPlayedWild);

        boolean wasBluffing = guiltyHand.hasColor(colorBeforeWild);

        if (wasBluffing) {
            recordEvent("Оспаривание УСПЕШНО! Игрок " + lastPlayerWhoPlayedWild +
                    " сблефовал (имел цвет " + colorBeforeWild + ") и забирает штраф +4 себе!");
            drawCardsForPlayer(lastPlayerWhoPlayedWild, pendingPenaltyDraw);
            pendingPenaltyDraw = 0;
            return true;
        } else {
            int increasedPenalty = pendingPenaltyDraw + 2; // По правилам Уно: 4 + 2 = 6 карт
            recordEvent("Оспаривание ПРОВАЛЕНО! У игрока " + lastPlayerWhoPlayedWild +
                    " действительно не было цвета " + colorBeforeWild +
                    ". Игрок " + challengerIndex + " берет " + increasedPenalty + " карт и пропускает ход!");
            drawCardsForPlayer(challengerIndex, increasedPenalty);
            pendingPenaltyDraw = 0;
            advanceTurn();
            return false;
        }
    }

    private Card drawSingleCardFromDeck() {
        if (deck.isEmpty()) {
            List<Card> recycled = discardPile.takeAllExceptTop();
            if (recycled.isEmpty()) {
                throw new IllegalStateException("В игре закончились абсолютно все карты (и в колоде, и в сбросе)!");
            }
            deck.addCards(recycled);
            deck.shuffle(random);
            recordEvent("Колода закончилась. Сброс перетасован в новую колоду (" + deck.size() + " карт).");
        }
        return deck.draw();
    }

    public void drawCardsForPlayer(int playerIndex, int count) {
        for (int i = 0; i < count; i++) {
            hands.get(playerIndex).addCard(drawSingleCardFromDeck());
        }
    }

    public void makeBotTurn() {
        if (gameOver) return;
        int botIdx = currentPlayerIndex;
        PlayerHand hand = hands.get(botIdx);

        List<Card> validCards = hand.getValidCards(this::isValidMove);
        if (validCards.isEmpty()) {
            drawCardOrAcceptPenalty(botIdx);
        } else {
            Card chosenCard = validCards.get(random.nextInt(validCards.size()));
            CardColor wildColor = null;
            if (chosenCard.getColor() == CardColor.WILD) {
                CardColor[] colors = {CardColor.RED, CardColor.YELLOW, CardColor.GREEN, CardColor.BLUE};
                wildColor = colors[random.nextInt(colors.length)];
            }
            boolean sayUno = (hand.size() == 2); // Бот всегда честно кричит Уно
            playCard(botIdx, chosenCard, wildColor, sayUno);
        }
    }

    private int calculateWinnerPoints(int winnerIdx) {
        return hands.stream()
                .filter(h -> h != hands.get(winnerIdx))
                .mapToInt(PlayerHand::calculatePoints)
                .sum();
    }

    private void validateTurnAndState(int playerIndex) {
        if (gameOver) {
            throw new InvalidMoveException("Игра уже окончена!");
        }
        if (playerIndex != currentPlayerIndex) {
            throw new InvalidMoveException("Сейчас ход игрока " + currentPlayerIndex + ", а не " + playerIndex);
        }
    }

    public void advanceTurn() {
        int n = hands.size();
        currentPlayerIndex = ((currentPlayerIndex + direction.getStep()) % n + n) % n;
    }

    public void reverseDirection() {
        this.direction = this.direction.reverse();
    }

    public void addPendingPenalty(int amount) {
        this.pendingPenaltyDraw += amount;
    }

    public void setChallengeableState(boolean state) {
        this.challengeableState = state;
    }

    public void recordEvent(String event) {
        eventLog.add(event);
    }

    public List<String> consumeEvents() {
        List<String> copy = new ArrayList<>(eventLog);
        eventLog.clear();
        return copy;
    }

    // Геттеры для View и Тестов
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public int getPlayersCount() { return hands.size(); }
    public GameDirection getDirection() { return direction; }
    public CardColor getCurrentColor() { return currentColor; }
    public int getPendingPenaltyDraw() { return pendingPenaltyDraw; }
    public boolean isGameOver() { return gameOver; }
    public Integer getWinnerIndex() { return winnerIndex; }
    public List<PlayerHand> getHands() { return hands; }
    public Deck getDeck() { return deck; }
    public DiscardPile getDiscardPile() { return discardPile; }
    public GameStats getStats() { return stats; }
}