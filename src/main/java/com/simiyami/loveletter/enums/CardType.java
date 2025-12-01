package com.simiyami.loveletter.enums;

public enum CardType {
    GUARD(1, "경비병", 5, "상대방 한 명의 손에 있는 카드 숫자를 짐작해서 맞추면 그 상대방은 탈락합니다."),
    PRIEST(2, "광대", 2, "상대방 한 명의 카드를 볼 수 있습니다."),
    BARON(3, "기사", 2, "상대방 한 명과 카드 숫자를 겨뤄서 낮은 쪽은 탈락합니다."),
    HANDMAID(4, "사제", 2, "다음 차례가 올 때까지 다른 카드의 영향을 받지 않습니다."),
    PRINCE(5, "마법사", 2, "상대나 자신의 손에서 카드를 버리고, 새 카드 한 장을 뽑게 합니다."),
    KING(6, "장군", 1, "상대방 한 명과 카드를 교환합니다."),
    COUNTESS(7, "후작", 1, "마법사(5)나 장군(6) 카드와 함께 들고 있다면, 반드시 이 카드를 내려놓습니다."),
    PRINCESS(8, "공주", 1, "가장 숫자가 높은 카드입니다. 라운드 중 강제로 공개되거나 버리게 되면 즉시 탈락합니다.");

    private final int number;
    private final String name;
    private final int count;
    private final String description;

    CardType(int number, String name, int count, String description) {
        this.number = number;
        this.name = name;
        this.count = count;
        this.description = description;
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int getCount() {
        return count;
    }

    public String getDescription() {
        return description;
    }

    public static CardType fromNumber(int number) {
        for (CardType type : values()) {
            if (type.number == number) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid card number: " + number);
    }

    public boolean requiresTarget() {
        return this != HANDMAID && this != COUNTESS;
    }

    public boolean canTargetSelf() {
        return this == PRINCE;
    }
}