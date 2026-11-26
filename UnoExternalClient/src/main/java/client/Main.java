package client;

import uno.effect.EffectRegistry;
import uno.exception.InvalidMoveException;
import uno.extension.GameStorage;
import uno.model.Card;
import uno.model.CardColor;
import uno.model.CardValue;
import uno.model.GameContext;
import uno.view.ConsoleView;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        System.out.println("ЗАПУСК МОДЕЛИРОВАНИЯ ПАРТИИ UNO");


        long seed = resolveSeed(args);
        int playersCount = (args.length > 1) ? Integer.parseInt(args[1]) : 3;

        System.out.println("Параметры партии -> Игроков: " + playersCount + " | Seed: " + seed);

        GameContext game = new GameContext(playersCount, seed);
        ConsoleView view = new ConsoleView();

        game.startNewGame();
        view.renderEvents(game);

        try {
            Card fakeCard = new Card(CardColor.RED, CardValue.NINE, "NORMAL", EffectRegistry.get("NORMAL"));
            game.playCard(2, fakeCard, null, false);
        } catch (InvalidMoveException e) {
            System.out.println("[КЛИЕНТ] Библиотека корректно заблокировала недопустимый ход: " + e.getMessage());
        }

        int step = 1;
        while (!game.isGameOver() && step <= 300) {
            System.out.println("\n--- Ход #" + step + " ---");
            view.renderState(game);

            game.makeBotTurn();
            view.renderEvents(game);

            step++;
        }

        System.out.println(" ПАРТИЯ ЗАВЕРШЕНА");
        if (game.isGameOver()) {
            System.out.println("Победитель: Игрок " + game.getWinnerIndex());
            System.out.println("Набрано очков: " + game.getStats().getWinnerScore());
        }
        System.out.println("Всего сыграно карт: " + game.getStats().getTotalCardsPlayed());
        System.out.println("Распределение сыгранных цветов: " + game.getStats().getColorFrequency());

        try {
            Path saveFile = Path.of("external_uno_save.json");
            GameStorage.saveToFile(game, saveFile);
            GameContext restored = GameStorage.loadFromFile(saveFile);
            System.out.println("Партия сохранена в файл: " + saveFile.toAbsolutePath());
            System.out.println("Проверка загрузки из JSON -> победитель: Игрок " + restored.getWinnerIndex());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения JSON: " + e.getMessage());
        }
    }

    private static long resolveSeed(String[] args) {
        if (args.length > 0) {
            try {
                return Long.parseLong(args[0].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Переданный аргумент seed должен быть целым числом (long), получено: " + args[0]);
            }
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Введите зерно (seed) для запуска партии (целое число): ");
            if (!scanner.hasNextLine()) {
                throw new IllegalStateException("Ввод закрыт. Для запуска программы необходимо задать seed.");
            }
            String input = scanner.nextLine().trim();
            try {
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: '" + input + "' не является целым числом. Попробуйте снова.");
            }
        }
    }
}