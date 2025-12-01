package com.simiyami.loveletter.model;

import com.simiyami.loveletter.enums.CardType;
import com.simiyami.loveletter.enums.PlayerType;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private final String id;
    private final String name;
    private final PlayerType type;
    private Card handCard;
    private final List<Card> discardedCards;
    private boolean isAlive;
    private boolean isProtected;
    private int roundsWon;

    public Player(String id, String name, PlayerType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.discardedCards = new ArrayList<>();
        this.isAlive = true;
        this.isProtected = false;
        this.roundsWon = 0;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PlayerType getType() {
        return type;
    }

    public Card getHandCard() {
        return handCard;
    }

    public void setHandCard(Card card) {
        this.handCard = card;
    }

    public List<Card> getDiscardedCards() {
        return new ArrayList<>(discardedCards);
    }

    public void addDiscardedCard(Card card) {
        this.discardedCards.add(card);
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public void setProtected(boolean protected_) {
        isProtected = protected_;
    }

    public int getRoundsWon() {
        return roundsWon;
    }

    public void incrementRoundsWon() {
        this.roundsWon++;
    }

    public boolean hasCard(CardType cardType) {
        return handCard != null && handCard.getType() == cardType;
    }

    public boolean mustPlayCountess() {
        if (!hasCard(CardType.COUNTESS)) {
            return false;
        }
        return hasCard(CardType.PRINCE) || hasCard(CardType.KING);
    }

    public void eliminate() {
        this.isAlive = false;
        this.isProtected = false;
    }

    public void resetForNewRound() {
        this.handCard = null;
        this.discardedCards.clear();
        this.isAlive = true;
        this.isProtected = false;
    }

    @Override
    public String toString() {
        return String.format("%s(%s) - 손패: %s, 생존: %s, 보호: %s",
            name, type.getDisplayName(),
            handCard != null ? handCard.toString() : "없음",
            isAlive ? "O" : "X",
            isProtected ? "O" : "X");
    }
}