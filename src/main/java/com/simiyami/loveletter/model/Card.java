package com.simiyami.loveletter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.simiyami.loveletter.enums.CardType;

public class Card {
    private final CardType type;
    private final String id;

    @JsonCreator
    public Card(
        @JsonProperty("type") CardType type,
        @JsonProperty("id") String id
    ) {
        this.type = type;
        this.id = id;
    }

    public CardType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public int getNumber() {
        return type.getNumber();
    }

    public String getName() {
        return type.getName();
    }

    public String getDescription() {
        return type.getDescription();
    }

    @Override
    public String toString() {
        return String.format("%d-%s", type.getNumber(), type.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return id.equals(card.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}