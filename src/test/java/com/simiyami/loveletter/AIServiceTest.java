package com.simiyami.loveletter;

import com.simiyami.loveletter.dto.CPUAction;
import com.simiyami.loveletter.enums.CardType;
import com.simiyami.loveletter.model.Card;
import com.simiyami.loveletter.model.Game;
import com.simiyami.loveletter.model.Player;
import com.simiyami.loveletter.service.AIService;
import com.simiyami.loveletter.service.CardService;
import com.simiyami.loveletter.service.GameService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AIServiceTest {

    @Test
    void testCPUDecisionWithCountessForced() {
        System.out.println("=== 후작 강제 발동 AI 테스트 ===\n");

        AIService aiService = new AIService();
        CardService cardService = new CardService();
        GameService gameService = new GameService(cardService);

        Game game = gameService.createGame(1);
        Player cpu = game.getPlayers().get(1);

        // CPU에게 후작과 마법사 부여
        cpu.setHandCard(new Card(CardType.COUNTESS, "test-countess"));
        Card princeCard = new Card(CardType.PRINCE, "test-prince");

        CPUAction action = aiService.decideCPUAction(game, cpu, princeCard);

        assertNotNull(action, "CPU 행동이 결정되어야 합니다.");
        assertEquals(CardType.COUNTESS, action.getCardToPlay().getType(),
            "후작이 강제로 선택되어야 합니다.");
        assertEquals("후작 강제 발동", action.getReasoning());

        System.out.println("CPU 선택 카드: " + action.getCardToPlay());
        System.out.println("선택 이유: " + action.getReasoning());
        System.out.println("\n=== 테스트 완료 ===");
    }

    @Test
    void testCPUGuardAI() {
        System.out.println("=== 경비병 AI 추측 테스트 ===\n");

        AIService aiService = new AIService();
        CardService cardService = new CardService();
        GameService gameService = new GameService(cardService);

        Game game = gameService.createGame(1);
        Player cpu = game.getPlayers().get(1);
        Player human = game.getPlayers().get(0);

        // CPU에게 경비병 부여
        cpu.setHandCard(new Card(CardType.GUARD, "test-guard"));
        Card drawnCard = new Card(CardType.PRIEST, "test-priest");

        // 일부 카드를 버림 더미에 추가
        game.addToDiscardPile(new Card(CardType.GUARD, "discarded-1"));
        game.addToDiscardPile(new Card(CardType.GUARD, "discarded-2"));
        game.addToDiscardPile(new Card(CardType.BARON, "discarded-3"));

        CPUAction action = aiService.decideCPUAction(game, cpu, drawnCard);

        assertNotNull(action, "CPU 행동이 결정되어야 합니다.");
        assertNotNull(action.getGuessNumber(), "경비병 추측 숫자가 있어야 합니다.");
        assertNotEquals(1, action.getGuessNumber(),
            "경비병(1번)은 추측할 수 없습니다.");
        assertTrue(action.getGuessNumber() >= 2 && action.getGuessNumber() <= 8,
            "추측 숫자는 2-8 사이여야 합니다.");

        System.out.println("CPU 선택 카드: " + action.getCardToPlay());
        System.out.println("타겟: " + (action.getTargetId() != null ? action.getTargetId() : "없음"));
        System.out.println("추측 숫자: " + action.getGuessNumber());
        System.out.println("\n=== 테스트 완료 ===");
    }

    @Test
    void testCPUCardSelectionStrategy() {
        System.out.println("=== CPU 카드 선택 전략 테스트 ===\n");

        AIService aiService = new AIService();
        CardService cardService = new CardService();
        GameService gameService = new GameService(cardService);

        Game game = gameService.createGame(1);
        Player cpu = game.getPlayers().get(1);

        // CPU에게 바론과 낮은 카드 부여
        cpu.setHandCard(new Card(CardType.BARON, "test-baron"));
        Card princessCard = new Card(CardType.PRINCESS, "test-princess");

        CPUAction action = aiService.decideCPUAction(game, cpu, princessCard);

        assertNotNull(action, "CPU 행동이 결정되어야 합니다.");
        assertNotEquals(CardType.PRINCESS, action.getCardToPlay().getType(),
            "공주는 절대 선택되면 안 됩니다.");

        System.out.println("CPU 선택 카드: " + action.getCardToPlay());
        System.out.println("타겟: " + (action.getTargetId() != null ? action.getTargetId() : "없음"));
        System.out.println("\n=== 테스트 완료 ===");
    }

    @Test
    void testCPUActionWithNoTargets() {
        System.out.println("=== 타겟 없을 때 CPU 행동 테스트 ===\n");

        AIService aiService = new AIService();
        CardService cardService = new CardService();
        GameService gameService = new GameService(cardService);

        Game game = gameService.createGame(1);
        Player cpu = game.getPlayers().get(1);
        Player human = game.getPlayers().get(0);

        // 상대방을 보호 상태로 만듦
        human.setProtected(true);

        // CPU에게 경비병과 사제 부여
        cpu.setHandCard(new Card(CardType.GUARD, "test-guard"));
        Card handmaidCard = new Card(CardType.HANDMAID, "test-handmaid");

        CPUAction action = aiService.decideCPUAction(game, cpu, handmaidCard);

        assertNotNull(action, "CPU 행동이 결정되어야 합니다.");

        // 타겟이 없으면 사제를 선택해야 함
        if (action.getCardToPlay().getType() == CardType.GUARD) {
            assertNull(action.getTargetId(), "타겟이 없으면 타겟 ID는 null이어야 합니다.");
        }

        System.out.println("CPU 선택 카드: " + action.getCardToPlay());
        System.out.println("타겟: " + (action.getTargetId() != null ? action.getTargetId() : "없음"));
        System.out.println("\n=== 테스트 완료 ===");
    }

    @Test
    void testFullGameWithAI() {
        System.out.println("=== AI 포함 전체 게임 테스트 ===\n");

        AIService aiService = new AIService();
        CardService cardService = new CardService();
        GameService gameService = new GameService(cardService);

        Game game = gameService.createGame(2); // 3인 게임 (1 human + 2 CPU)

        assertNotNull(game, "게임이 생성되어야 합니다.");
        assertEquals(3, game.getPlayers().size(), "플레이어가 3명이어야 합니다.");

        int maxTurns = 30;
        int turnCount = 0;

        while (!game.isRoundOver() && turnCount < maxTurns) {
            turnCount++;
            System.out.println("\n--- 턴 " + turnCount + " ---");

            Player currentPlayer = game.getCurrentPlayer();
            System.out.println("현재 플레이어: " + currentPlayer.getName() + " (" + currentPlayer.getType() + ")");

            Card drawnCard = gameService.drawCardForPlayer(game, currentPlayer);
            if (drawnCard == null) {
                System.out.println("덱이 비었습니다.");
                break;
            }

            System.out.println("뽑은 카드: " + drawnCard);

            CPUAction action;
            Card cardToPlay;
            Card cardToKeep;
            Player target = null;
            Integer guessNumber = null;

            if (currentPlayer.getType().name().equals("CPU")) {
                // CPU 턴
                action = aiService.decideCPUAction(game, currentPlayer, drawnCard);
                cardToPlay = action.getCardToPlay();

                if (action.getTargetId() != null) {
                    target = game.getPlayer(action.getTargetId());
                }
                guessNumber = action.getGuessNumber();

                System.out.println("CPU 선택: " + cardToPlay + " (이유: " + action.getReasoning() + ")");
            } else {
                // Human 턴 (간단한 로직)
                cardToPlay = gameService.getPlayableCards(currentPlayer, drawnCard).get(0);

                if (cardToPlay.getType().requiresTarget() && !cardToPlay.getType().canTargetSelf()) {
                    var targetablePlayers = game.getTargetablePlayers(currentPlayer);
                    if (!targetablePlayers.isEmpty()) {
                        target = targetablePlayers.get(0);
                    }
                }

                if (cardToPlay.getType() == CardType.GUARD && target != null) {
                    guessNumber = 2;
                }

                System.out.println("Human 선택: " + cardToPlay);
            }

            if (target != null) {
                System.out.println("타겟: " + target.getName());
            }

            gameService.playCard(game, currentPlayer, cardToPlay, target, guessNumber);

            if (!game.isRoundOver()) {
                gameService.nextTurn(game);
            }

            System.out.println("생존 플레이어 수: " + game.getAlivePlayers().size());
        }

        assertTrue(game.isRoundOver(), "라운드가 종료되어야 합니다.");
        assertNotNull(game.getRoundWinner(), "승자가 결정되어야 합니다.");

        System.out.println("\n=== 라운드 승자: " + game.getRoundWinner().getName() + " ===");
        System.out.println("=== AI 포함 전체 게임 테스트 완료 ===");
    }
}