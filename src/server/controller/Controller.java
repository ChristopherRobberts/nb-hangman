package server.controller;

import server.model.Game;

import java.util.concurrent.CompletableFuture;

public class Controller {
    private Game game;

    public void loadWordsIO() {
        CompletableFuture
                .runAsync(() -> {
                    this.game = new Game();
                    this.game.loadWordsFromFile();
                })
                .thenRun(() -> System.out.println("words loaded"));
    }

    public void startGame() {
        this.game.startGame();
    }


    public void processGuess(String guess) {
        this.game.processGuess(guess.toLowerCase());
    }

    public boolean gameIsOngoing() {
        return this.game.isOngoing();
    }

    public boolean gameIsLost() {
        return this.game.isLost();
    }

    public boolean gameIsWon() {
        return this.game.isWon();
    }

    public String getScoreState() {
        return Integer.toString(this.game.getScoreState());
    }

    public String getAttemptsState() {
        return Integer.toString(this.game.getRemainingAttemptsState());
    }

    public String getWordState() {
        return this.game.getWordProgress();
    }
}
