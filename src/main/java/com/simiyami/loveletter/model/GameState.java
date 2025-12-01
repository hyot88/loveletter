package com.simiyami.loveletter.model;

import java.util.List;

public class GameState {
    private String gameId;
    private int currentRound;
    private String currentPlayerId;
    private String currentPlayerName;
    private List<PlayerInfo> players;
    private int deckSize;
    private List<String> recentLogs;
    private boolean roundOver;
    private String roundWinnerId;
    private String roundWinnerName;
    private Card secretCard;

    public static class PlayerInfo {
        private String id;
        private String name;
        private String type;
        private boolean isAlive;
        private boolean isProtected;
        private int roundsWon;
        private Card handCard;
        private List<Card> discardedCards;

        public PlayerInfo(Player player, boolean showHandCard) {
            this.id = player.getId();
            this.name = player.getName();
            this.type = player.getType().name();
            this.isAlive = player.isAlive();
            this.isProtected = player.isProtected();
            this.roundsWon = player.getRoundsWon();
            this.handCard = showHandCard ? player.getHandCard() : null;
            this.discardedCards = player.getDiscardedCards();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public boolean isAlive() { return isAlive; }
        public boolean isProtected() { return isProtected; }
        public int getRoundsWon() { return roundsWon; }
        public Card getHandCard() { return handCard; }
        public List<Card> getDiscardedCards() { return discardedCards; }
    }

    public GameState() {}

    public static GameState fromGame(Game game, String viewingPlayerId) {
        GameState state = new GameState();
        state.gameId = game.getId();
        state.currentRound = game.getCurrentRound();

        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null) {
            state.currentPlayerId = currentPlayer.getId();
            state.currentPlayerName = currentPlayer.getName();
        }

        state.players = game.getPlayers().stream()
            .map(p -> new PlayerInfo(p, p.getId().equals(viewingPlayerId) || !p.isAlive()))
            .toList();

        state.deckSize = game.getDeck().size();
        state.recentLogs = game.getGameLog().stream()
            .skip(Math.max(0, game.getGameLog().size() - 5))
            .toList();

        state.roundOver = game.isRoundOver();
        if (game.getRoundWinner() != null) {
            state.roundWinnerId = game.getRoundWinner().getId();
            state.roundWinnerName = game.getRoundWinner().getName();
            state.secretCard = game.getSecretCard();
        }

        return state;
    }

    public String getGameId() { return gameId; }
    public int getCurrentRound() { return currentRound; }
    public String getCurrentPlayerId() { return currentPlayerId; }
    public String getCurrentPlayerName() { return currentPlayerName; }
    public List<PlayerInfo> getPlayers() { return players; }
    public int getDeckSize() { return deckSize; }
    public List<String> getRecentLogs() { return recentLogs; }
    public boolean isRoundOver() { return roundOver; }
    public String getRoundWinnerId() { return roundWinnerId; }
    public String getRoundWinnerName() { return roundWinnerName; }
    public Card getSecretCard() { return secretCard; }
}