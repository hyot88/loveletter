package com.simiyami.loveletter.service;

import com.simiyami.loveletter.dto.CPUAction;
import com.simiyami.loveletter.enums.CardType;
import com.simiyami.loveletter.model.Card;
import com.simiyami.loveletter.model.Game;
import com.simiyami.loveletter.model.Player;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
                // 바론: 가장 약한 것으로 예상되는 플레이어 선택 (버린 카드가 낮은 플레이어)
                return targetablePlayers.stream()
                    .min(Comparator.comparingInt(p ->
                        p.getDiscardedCards().stream()
                            .mapToInt(Card::getNumber)
                            .max()
                            .orElse(0)))
                    .orElse(targetablePlayers.get(0));

            case PRINCE:
                // 프린스: 높은 카드를 가진 것으로 의심되는 플레이어 (버린 카드가 낮은 플레이어)
                return targetablePlayers.stream()
                    .min(Comparator.comparingInt(p ->
                        p.getDiscardedCards().stream()
                            .mapToInt(Card::getNumber)
                            .sum()))
                    .orElse(targetablePlayers.get(0));

            default:
                // 기본: 랜덤 선택
                return targetablePlayers.get(random.nextInt(targetablePlayers.size()));
        }
    }

    private Integer guessCardNumber(Game game, Player cpuPlayer, Player target) {
        // 1. 이미 공개된 모든 카드 수집
        Set<Integer> revealedCards = new HashSet<>();

        // 버린 카드들 (공개 더미)
        game.getDiscardPile().forEach(card -> revealedCards.add(card.getNumber()));

        // 모든 플레이어의 버린 카드들
        game.getPlayers().forEach(player ->
            player.getDiscardedCards().forEach(card -> revealedCards.add(card.getNumber())));

        // CPU가 가진 카드들
        if (cpuPlayer.getHandCard() != null) {
            revealedCards.add(cpuPlayer.getHandCard().getNumber());
        }

        // 2. 가능한 카드 목록 (2-8번, 1번 제외)
        List<Integer> possibleCards = new ArrayList<>();
        for (CardType type : CardType.values()) {
            if (type.getNumber() == 1) continue; // 경비병은 추측 불가

            for (int i = 0; i < type.getCount(); i++) {
                possibleCards.add(type.getNumber());
            }
        }

        // 3. 이미 나온 카드 제외
        List<Integer> remainingCards = new ArrayList<>();
        for (Integer num : possibleCards) {
            long revealedCount = revealedCards.stream().filter(r -> r.equals(num)).count();
            long totalCount = Arrays.stream(CardType.values())
                .filter(t -> t.getNumber() == num)
                .mapToInt(CardType::getCount)
                .sum();

            // 남은 카드가 있으면 추가
            if (revealedCount < totalCount) {
                remainingCards.add(num);
            }
        }

        // 4. 타겟이 버린 카드 기반 추론
        if (!target.getDiscardedCards().isEmpty()) {
            // 타겟이 낮은 카드를 많이 버렸다면 높은 카드 추측
            double avgDiscarded = target.getDiscardedCards().stream()
                .mapToInt(Card::getNumber)
                .average()
                .orElse(4.0);

            if (avgDiscarded < 4 && remainingCards.stream().anyMatch(n -> n >= 5)) {
                // 높은 카드 우선 추측
                List<Integer> highCards = remainingCards.stream()
                    .filter(n -> n >= 5)
                    .collect(Collectors.toList());
                if (!highCards.isEmpty()) {
                    return highCards.get(random.nextInt(highCards.size()));
                }
            }
        }

        // 5. 남은 카드 중 랜덤 선택 (없으면 기본값 2)
        if (remainingCards.isEmpty()) {
            return 2; // 기본값
        }

        return remainingCards.get(random.nextInt(remainingCards.size()));
    }
}