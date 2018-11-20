package server.model;

import java.util.Arrays;

public class Game {
    private GameState gameState;
    private String word;
    private char[] unknownWord;
    private char[] knownWord;
    private boolean isOngoing = false;
    private boolean isWon = false;
    private boolean isLost = false;
    private int score = 0;
    private Word wordGenerator;


    public void loadWordsFromFile() {
        this.wordGenerator = new Word();
    }

    public void startGame() {
        try {
            this.isOngoing = true;
            this.word = this.wordGenerator.getWord().toLowerCase();
            this.unknownWord = this.wordGenerator.getHiddenWord().replaceAll("\\s", "").toCharArray();
            this.knownWord = this.word.toCharArray();
            this.gameState = new GameState(formatString(this.unknownWord), this.score, this.word.length());
            System.out.println(this.word);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void processGuess(String guess) {
        boolean isWord = (guess.length() > 1);
        boolean wasRight = false;

        if (isWord) {
            if (guess.equals(this.word)) {
                this.isWon = true;
                this.score++;
                this.gameState.score++;
                this.gameState.wordProgress = formatString(this.knownWord);
                this.isOngoing = false;
                return;
            }
            this.gameState.remainingAttempts--;
        } else {
            for (int i = 0; i < this.knownWord.length; i++) {
                if (this.knownWord[i] == guess.charAt(0)) {
                    this.unknownWord[i] = guess.charAt(0);
                    this.gameState.wordProgress = formatString(this.unknownWord);
                    wasRight = true;
                }
            }

            if (!wasRight)
                this.gameState.remainingAttempts--;

            if (Arrays.equals(this.knownWord, this.unknownWord)){
                this.isWon = true;
                this.score++;
                this.gameState.score++;
                this.isOngoing = false;
            }
        }

        if (this.gameState.remainingAttempts == 0) {
            this.score--;
            this.gameState.score--;
            this.isOngoing = false;
            this.isLost = true;
        }
    }

    public boolean isOngoing() {
        return this.isOngoing;
    }

    public boolean isWon() { return this.isWon; }

    public boolean isLost() { return this.isLost; }

    public int getScoreState() {
        return this.gameState.score;
    }

    public int getRemainingAttemptsState() {
        return this.gameState.remainingAttempts;
    }

    public String getWordProgress() {
        return this.gameState.wordProgress;
    }

    private String formatString(char[] arr) {
        StringBuilder strBuilder = new StringBuilder();

        for (char c : arr) {
            strBuilder.append(c);
            strBuilder.append(" ");
        }

        return strBuilder.toString().trim();
    }

    private class GameState {
        private String wordProgress;
        private int score;
        private int remainingAttempts;

        GameState(String wordProgress, int score, int remainingAttempts) {
            this.wordProgress = wordProgress;
            this.score = score;
            this.remainingAttempts = remainingAttempts;
        }
    }
}


