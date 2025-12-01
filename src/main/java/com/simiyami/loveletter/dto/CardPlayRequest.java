package com.simiyami.loveletter.dto;

public class CardPlayRequest {
    private String playerId;
    private String cardId;
    private String targetId;
    private Integer guessNumber;

    public CardPlayRequest() {}

    public CardPlayRequest(String playerId, String cardId, String targetId, Integer guessNumber) {
        this.playerId = playerId;
        this.cardId = cardId;
        this.targetId = targetId;
        this.guessNumber = guessNumber;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Integer getGuessNumber() {
        return guessNumber;
    }

    public void setGuessNumber(Integer guessNumber) {
        this.guessNumber = guessNumber;
    }
}