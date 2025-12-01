package com.simiyami.loveletter.model;

import com.simiyami.loveletter.enums.CardType;

import java.util.*;

public class Game {
    private final String id;
    private final List<Player> players;
    private final Deque<Card> deck;
    private final List<Card> discardPile;
    private Card secretCard;
    private int currentPlayerIndex;
    private final List<String> gameLog;
    private boolean roundOver;
    private Player roundWinner;
    private int currentRound;

    public Game(String id, List<Player> players) {
        this.id = id;
        this.players = new ArrayList<>(players);
        this.deck = new ArrayDeque<>();
        this.discardPile = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.gameLog = new ArrayList<>();
        this.roundOver = false;
        this.currentRound = 1;
    }

    public String getId() {
        return id;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public Player getPlayer(String playerId) {
        return players.stream()
            .filter(p -> p.getId().equals(playerId))
            .findFirst()
            .orElse(null);
    }

    public Deque<Card> getDeck() {
        return deck;
    }

    public List<Card> getDiscardPile() {
        return new ArrayList<>(discardPile);
    }

    public Card getSecretCard() {
        return secretCard;
    }

    public void setSecretCard(Card card) {
        this.secretCard = card;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int index) {
        this.currentPlayerIndex = index;
    }

    public Player getCurrentPlayer() {
        if (currentPlayerIndex >= 0 && currentPlayerIndex < players.size()) {
            return players.get(currentPlayerIndex);
        }
        return null;
    }

    public List<String> getGameLog() {
        return new ArrayList<>(gameLog);
    }

    public void addLog(String message) {
        this.gameLog.add(message);
        System.out.println("[게임 로그] " + message);
    }

    public boolean isRoundOver() {
        return roundOver;
    }

    public void setRoundOver(boolean roundOver) {
        this.roundOver = roundOver;
    }

    public Player getRoundWinner() {
        return roundWinner;
    }

    public void setRoundWinner(Player winner) {
        this.roundWinner = winner;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void incrementRound() {
        this.currentRound++;
    }

    public void addToDiscardPile(Card card) {
        this.discardPile.add(card);
    }

    public Card drawCard() {
        if (deck.isEmpty()) {
            return null;
        }
        return deck.pollFirst();
    }

    public void nextTurn() {
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (!getCurrentPlayer().isAlive());
    }

    public List<Player> getAlivePlayers() {
        return players.stream()
            .filter(Player::isAlive)
            .toList();
    }

    public List<Player> getTargetablePlayers(Player currentPlayer) {
        return players.stream()
            .filter(p -> !p.getId().equals(currentPlayer.getId()))
            .filter(Player::isAlive)
            .filter(p -> !p.isProtected())
            .toList();
    }

    public void initializeDeck() {
        deck.clear();
        discardPile.clear();
        int cardId = 0;

        for (CardType type : CardType.values()) {
            for (int i = 0; i < type.getCount(); i++) {
                deck.add(new Card(type, "card-" + (cardId++)));
            }
        }

        List<Card> deckList = new ArrayList<>(deck);
        Collections.shuffle(deckList);
        deck.clear();
        deck.addAll(deckList);
    }

    public void resetForNewRound() {
        this.roundOver = false;
        this.roundWinner = null;
        this.gameLog.clear();
        this.discardPile.clear();
        this.secretCard = null;
        this.currentPlayerIndex = 0;

        for (Player player : players) {
            player.resetForNewRound();
        }

        initializeDeck();
    }
}