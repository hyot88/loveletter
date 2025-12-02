package com.simiyami.loveletter.service;

import com.simiyami.loveletter.dto.CPUAction;
import com.simiyami.loveletter.enums.CardType;
import com.simiyami.loveletter.model.Card;
import com.simiyami.loveletter.model.Game;
import com.simiyami.loveletter.model.Player;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AIService {

    private final Random random = new Random();

    public CPUAction decideCPUAction(Game game, Player cpuPlayer, Card drawnCard) {
        List<Card> availableCards = Arrays.asList(cpuPlayer.getHandCard(), drawnCard);

        // 1. 후작(7) 강제 체크
        boolean hasCountess = availableCards.stream().anyMatch(c -> c.getType() == CardType.COUNTESS);
        boolean hasPrinceOrKing = availableCards.stream().anyMatch(c ->
            c.getType() == CardType.PRINCE || c.getType() == CardType.KING);

        if (hasCountess && hasPrinceOrKing) {
            Card countess = availableCards.stream()
                .filter(c -> c.getType() == CardType.COUNTESS)
                .findFirst()
                .orElseThrow();
            return new CPUAction(countess, null, null, "후작 강제 발동");
        }

        // 2. 카드 우선순위 결정
        Card selectedCard = selectBestCard(game, cpuPlayer, availableCards);

        // 3. 타겟 선택
        Player target = null;
        if (selectedCard.getType().requiresTarget()) {
            target = selectTarget(game, cpuPlayer, selectedCard);
        }

        // 4. 경비병인 경우 추측 숫자
        Integer guessNumber = null;
        if (selectedCard.getType() == CardType.GUARD && target != null) {
            guessNumber = guessCardNumber(game, cpuPlayer, target);
        }

        String reasoning = String.format("%s 선택", selectedCard.getName());
        return new CPUAction(selectedCard, target != null ? target.getId() : null, guessNumber, reasoning);
    }

    private Card selectBestCard(Game game, Player cpuPlayer, List<Card> availableCards) {
        // 카드 우선순위 점수 계산
        Map<Card, Integer> cardScores = new HashMap<>();

        for (Card card : availableCards) {
            int score = evaluateCard(game, cpuPlayer, card);
            cardScores.put(card, score);
        }

        // 가장 높은 점수의 카드 선택
        return cardScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(availableCards.get(0));
    }

    private int evaluateCard(Game game, Player cpuPlayer, Card card) {
        List<Player> availableTargets = game.getTargetablePlayers(cpuPlayer);

        switch (card.getType()) {
            case GUARD:
                // 타겟이 있으면 높은 점수 (정보 수집 가능)
                return availableTargets.isEmpty() ? 1 : 6;

            case PRIEST:
                // 상대 카드를 볼 수 있으면 중간 점수
                return availableTargets.isEmpty() ? 1 : 5;

            case BARON:
                // 높은 카드를 들고 있으면 바론 우선
                if (cpuPlayer.getHandCard().getNumber() >= 5) {
                    return availableTargets.isEmpty() ? 1 : 8;
                }
                return availableTargets.isEmpty() ? 1 : 3;

            case HANDMAID:
                // 보호가 필요한 상황이면 높은 점수
                return game.getAlivePlayers().size() > 2 ? 7 : 4;

            case PRINCE:
                // 상대에게 사용하거나 낮은 카드를 버릴 때 유용
                if (cpuPlayer.getHandCard().getNumber() <= 3) {
                    return 7; // 자신의 낮은 카드 버리기
                }
                return availableTargets.isEmpty() ? 1 : 6;

            case KING:
                // 타겟이 있고 자신의 카드가 낮으면 높은 점수
                if (cpuPlayer.getHandCard().getNumber() <= 4) {
                    return availableTargets.isEmpty() ? 1 : 7;
                }
                return availableTargets.isEmpty() ? 1 : 4;

            case COUNTESS:
                // 별 효과 없으므로 낮은 점수
                return 2;

            case PRINCESS:
                // 절대 내면 안 됨
                return -100;

            default:
                return 1;
        }
    }

    private Player selectTarget(Game game, Player cpuPlayer, Card card) {
        List<Player> targetablePlayers = game.getTargetablePlayers(cpuPlayer);

        if (targetablePlayers.isEmpty()) {
            // 타겟이 없으면 자신을 타겟으로 (Prince만 가능)
            if (card.getType().canTargetSelf()) {
                return cpuPlayer;
            }
            return null;
        }

        // 카드 종류에 따라 타겟 선택 전략
        switch (card.getType()) {
            case BARON:
                // 바론: 알고 있는 카드가 낮은 플레이어 우선, 없으면 버린 카드 기반 추론
                for (Player target : targetablePlayers) {
                    if (cpuPlayer.knowsOpponentCard(target.getId())) {
                        CardType knownCard = cpuPlayer.getKnownOpponentCard(target.getId());
                        // 상대가 낮은 카드를 가지고 있고, 내 카드가 더 높으면 타겟팅
                        if (knownCard.getNumber() < cpuPlayer.getHandCard().getNumber()) {
                            System.out.println(String.format("[AI] 바론: %s의 카드(%s)가 내 카드(%d)보다 낮음 - 타겟!",
                                target.getName(), knownCard.getName(), cpuPlayer.getHandCard().getNumber()));
                            return target;
                        }
                    }
                }
                // 정보가 없으면 버린 카드가 가장 낮은 플레이어 선택
                return targetablePlayers.stream()
                    .min(Comparator.comparingInt(p ->
                        p.getDiscardedCards().stream()
                            .mapToInt(Card::getNumber)
                            .max()
                            .orElse(0)))
                    .orElse(targetablePlayers.get(0));

            case PRINCE:
                // 프린스: 공주를 가진 플레이어 우선 타겟
                for (Player target : targetablePlayers) {
                    if (cpuPlayer.knowsOpponentCard(target.getId())) {
                        CardType knownCard = cpuPlayer.getKnownOpponentCard(target.getId());
                        if (knownCard == CardType.PRINCESS) {
                            System.out.println(String.format("[AI] 마법사: %s가 공주 소유 확인 - 타겟하여 제거!",
                                target.getName()));
                            return target;
                        }
                    }
                }
                // 공주가 없으면 높은 카드를 가진 것으로 의심되는 플레이어
                return targetablePlayers.stream()
                    .min(Comparator.comparingInt(p ->
                        p.getDiscardedCards().stream()
                            .mapToInt(Card::getNumber)
                            .sum()))
                    .orElse(targetablePlayers.get(0));

            case KING:
                // 장군: 높은 카드를 가진 플레이어 우선, 또는 공주를 줄 수 있는 플레이어
                // 1. 내가 공주를 가지고 있다면 상대에게 주기
                if (cpuPlayer.getHandCard().getType() == CardType.PRINCESS) {
                    // 아무나 선택 (공주를 줘서 나중에 경비병으로 제거)
                    System.out.println(String.format("[AI] 장군: 공주를 %s에게 전달",
                        targetablePlayers.get(0).getName()));
                    return targetablePlayers.get(0);
                }

                // 2. 높은 카드를 가진 플레이어와 교환
                for (Player target : targetablePlayers) {
                    if (cpuPlayer.knowsOpponentCard(target.getId())) {
                        CardType knownCard = cpuPlayer.getKnownOpponentCard(target.getId());
                        // 상대가 높은 카드를 가지고 있으면 교환
                        if (knownCard.getNumber() > cpuPlayer.getHandCard().getNumber()) {
                            System.out.println(String.format("[AI] 장군: %s의 카드(%s)가 내 카드(%d)보다 높음 - 교환!",
                                target.getName(), knownCard.getName(), cpuPlayer.getHandCard().getNumber()));
                            return target;
                        }
                    }
                }
                // 정보가 없으면 랜덤 선택
                return targetablePlayers.get(random.nextInt(targetablePlayers.size()));

            default:
                // 기본: 랜덤 선택
                return targetablePlayers.get(random.nextInt(targetablePlayers.size()));
        }
    }

    private Integer guessCardNumber(Game game, Player cpuPlayer, Player target) {
        // 1. 메모리에서 타겟의 카드를 알고 있는지 확인
        if (cpuPlayer.knowsOpponentCard(target.getId())) {
            CardType knownCard = cpuPlayer.getKnownOpponentCard(target.getId());

            // 타겟이 마지막으로 카드를 사용했는지 확인
            if (!target.getDiscardedCards().isEmpty()) {
                Card lastDiscarded = target.getDiscardedCards().get(target.getDiscardedCards().size() - 1);

                // 마지막으로 버린 카드가 우리가 알던 카드가 아니라면, 아직 가지고 있을 수 있음
                if (lastDiscarded.getType() != knownCard) {
                    System.out.println(String.format("[AI] %s가 기억한 %s의 카드: %s - 지목!",
                        cpuPlayer.getName(), target.getName(), knownCard.getName()));
                    return knownCard.getNumber();
                } else {
                    // 알던 카드를 사용했으므로 더 이상 유효하지 않음
                    cpuPlayer.forgetOpponentCard(target.getId());
                    System.out.println(String.format("[AI] %s가 %s를 사용했으므로 기억 삭제",
                        target.getName(), knownCard.getName()));
                }
            } else {
                // 카드를 버리지 않았다면 아직 가지고 있을 가능성이 높음
                System.out.println(String.format("[AI] %s가 기억한 %s의 카드: %s - 지목!",
                    cpuPlayer.getName(), target.getName(), knownCard.getName()));
                return knownCard.getNumber();
            }
        }

        // 2. 이미 공개된 모든 카드 수집
        Map<Integer, Integer> revealedCardCounts = new HashMap<>();

        // 버린 카드들 (공개 더미)
        game.getDiscardPile().forEach(card ->
            revealedCardCounts.merge(card.getNumber(), 1, Integer::sum));

        // 모든 플레이어의 버린 카드들
        game.getPlayers().forEach(player ->
            player.getDiscardedCards().forEach(card ->
                revealedCardCounts.merge(card.getNumber(), 1, Integer::sum)));

        // CPU가 가진 카드들
        if (cpuPlayer.getHandCard() != null) {
            revealedCardCounts.merge(cpuPlayer.getHandCard().getNumber(), 1, Integer::sum);
        }

        // 3. 남은 카드 확률 계산 (2-8번, 1번 제외)
        Map<Integer, Integer> remainingCardCounts = new HashMap<>();
        for (CardType type : CardType.values()) {
            if (type.getNumber() == 1) continue; // 경비병은 추측 불가

            int totalCount = type.getCount();
            int revealedCount = revealedCardCounts.getOrDefault(type.getNumber(), 0);
            int remaining = totalCount - revealedCount;

            if (remaining > 0) {
                remainingCardCounts.put(type.getNumber(), remaining);
            }
        }

        // 4. 타겟이 버린 카드 기반 추론
        if (!target.getDiscardedCards().isEmpty()) {
            // 타겟이 낮은 카드를 많이 버렸다면 높은 카드 추측
            double avgDiscarded = target.getDiscardedCards().stream()
                .mapToInt(Card::getNumber)
                .average()
                .orElse(4.0);

            if (avgDiscarded < 4) {
                // 높은 카드 우선 (확률적으로)
                List<Integer> weightedGuesses = new ArrayList<>();
                for (Map.Entry<Integer, Integer> entry : remainingCardCounts.entrySet()) {
                    int cardNum = entry.getKey();
                    int count = entry.getValue();

                    // 5번 이상 카드에 가중치 부여
                    int weight = cardNum >= 5 ? count * 2 : count;
                    for (int i = 0; i < weight; i++) {
                        weightedGuesses.add(cardNum);
                    }
                }

                if (!weightedGuesses.isEmpty()) {
                    return weightedGuesses.get(random.nextInt(weightedGuesses.size()));
                }
            }
        }

        // 5. 확률 기반 추측
        List<Integer> weightedGuesses = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : remainingCardCounts.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                weightedGuesses.add(entry.getKey());
            }
        }

        // 6. 남은 카드 중 확률적 선택 (없으면 기본값 2)
        if (weightedGuesses.isEmpty()) {
            return 2; // 기본값
        }

        return weightedGuesses.get(random.nextInt(weightedGuesses.size()));
    }
}