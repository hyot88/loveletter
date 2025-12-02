package com.simiyami.loveletter.model;

import com.simiyami.loveletter.enums.CardType;
import com.simiyami.loveletter.enums.PlayerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    private final String id;
    private final String name;
    private final PlayerType type;
    private Card handCard;
    private Card drawnCard;  // 턴에 드로우한 카드
    private final List<Card> discardedCards;
    private boolean isAlive;
    private boolean isProtected;
    private int roundsWon;

    // CPU 메모리 시스템: 상대방의 카드 기억
    private final Map<String, CardType> knownOpponentCards;  // playerId -> 알려진 카드

    public Player(String id, String name, PlayerType type) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.discardedCards = new ArrayList<>();
        this.knownOpponentCards = new HashMap<>();
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

    public Card getDrawnCard() {
        return drawnCard;
    }

    public void setDrawnCard(Card card) {
        this.drawnCard = card;
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

    public boolean hasDrawnCard(CardType cardType) {
        return drawnCard != null && drawnCard.getType() == cardType;
    }

    public boolean mustPlayCountess() {
        boolean hasCountess = hasCard(CardType.COUNTESS) || hasDrawnCard(CardType.COUNTESS);
        boolean hasPrinceOrKing = hasCard(CardType.PRINCE) || hasDrawnCard(CardType.PRINCE) ||
                                   hasCard(CardType.KING) || hasDrawnCard(CardType.KING);
        return hasCountess && hasPrinceOrKing;
    }

    public void eliminate() {
        this.isAlive = false;
        this.isProtected = false;
    }

    public void resetForNewRound() {
        this.handCard = null;
        this.drawnCard = null;
        this.discardedCards.clear();
        this.knownOpponentCards.clear();
        this.isAlive = true;
        this.isProtected = false;
    }

    // CPU 메모리 관련 메서드
    public void rememberOpponentCard(String playerId, CardType cardType) {
        this.knownOpponentCards.put(playerId, cardType);
    }

    public CardType getKnownOpponentCard(String playerId) {
        return this.knownOpponentCards.get(playerId);
    }

    public void forgetOpponentCard(String playerId) {
        this.knownOpponentCards.remove(playerId);
    }

    public boolean knowsOpponentCard(String playerId) {
        return this.knownOpponentCards.containsKey(playerId);
    }

    public Map<String, CardType> getAllKnownCards() {
        return new HashMap<>(knownOpponentCards);
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