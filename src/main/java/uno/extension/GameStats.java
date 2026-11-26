package uno.extension;

import uno.model.Card;
import uno.model.CardColor;

import java.util.*;
import java.util.stream.Collectors;

public class GameStats {
    private Map<Integer, List<Card>> playedCardsByPlayer = new HashMap<>();
    private Integer winnerIndex;
    private int winnerScore;

    public void recordCardPlayed(int playerIndex, Card card) {
        playedCardsByPlayer.computeIfAbsent(playerIndex, k -> new ArrayList<>()).add(card);
    }

    public void recordWinner(int winnerIndex, int score) {
        this.winnerIndex = winnerIndex;
        this.winnerScore = score;
    }

    public int getTotalCardsPlayed() {
        return playedCardsByPlayer.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    public Map<CardColor, Long> getColorFrequency() {
        return playedCardsByPlayer.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(Card::getColor, Collectors.counting()));
    }

    public Integer getWinnerIndex() { return winnerIndex; }
    public int getWinnerScore() { return winnerScore; }
}