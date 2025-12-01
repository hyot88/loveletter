package com.simiyami.loveletter.dto;

import com.simiyami.loveletter.model.Card;

public class CPUAction {
    private Card cardToPlay;
    private String targetId;
    private Integer guessNumber;
    private String reasoning;

    public CPUAction(Card cardToPlay, String targetId, Integer guessNumber) {
        this.cardToPlay = cardToPlay;
        this.targetId = targetId;
        this.guessNumber = guessNumber;
    }

    public CPUAction(Card cardToPlay, String targetId, Integer guessNumber, String reasoning) {
        this.cardToPlay = cardToPlay;
        this.targetId = targetId;
        this.guessNumber = guessNumber;
        this.reasoning = reasoning;
    }

    public Card getCardToPlay() {
        return cardToPlay;
    }

    public void setCardToPlay(Card cardToPlay) {
        this.cardToPlay = cardToPlay;
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

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }
}