package com.simiyami.loveletter;

import com.simiyami.loveletter.enums.CardType;
import com.simiyami.loveletter.model.Card;
import com.simiyami.loveletter.model.Game;
import com.simiyami.loveletter.model.Player;
import com.simiyami.loveletter.service.CardService;
import com.simiyami.loveletter.service.GameService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameFlowTest {

    @Test
    void testBasic2PlayerGame() {
        System.out.println("=== 2인 게임 기본 테스트 시작 ===\n");

        CardService cardService = new CardService();
        GameService gameService = new GameService(cardService);

        Game game = gameService.createGame(1);

        assertNotNull(game, "게임이 생성되어야 합니다.");
        assertEquals(2, game.getPlayers().size(), "플레이어가 2명이어야 합니다.");
        assertEquals(2, game.getAlivePlayers().size(), "생존 플레이어가 2명이어야 합니다.");

        assertNotNull(game.getSecretCard(), "비밀 카드가 있어야 합니다.");
        assertEquals(13, game.getDeck().size(), "덱에 13장이 있어야 합니다 (16 - 1 비밀 - 2 초기).");

        for (Player player : game.getPlayers()) {
            assertNotNull(player.getHandCard(), player.getName() + "의 초기 카드가 있어야 합니다.");
            System.out.println(player.getName() + "의 초기 카드: " + player.getHandCard());
        }

        System.out.println("\n=== 게임 초기화 성공 ===\n");
    }

    @Test
    void testFullGameRound() {
        System.out.println("=== 전체 라운드 테스트 시작 ===\n");

        CardService cardService = new CardService();
        GameService gameService = new GameService(cardService);

        Game game = gameService.createGame(1);

        Player human = game.getPlayers().get(0);
        Player cpu = game.getPlayers().get(1);

        int maxTurns = 20;
        int turnCount = 0;

        while (!game.isRoundOver() && turnCount < maxTurns) {
            turnCount++;
            System.out.println("\n--- 턴 " + turnCount + " ---");

            Player currentPlayer = game.getCurrentPlayer();
            System.out.println("현재 플레이어: " + currentPlayer.getName());

            Card drawnCard = gameService.drawCardForPlayer(game, currentPlayer);
            if (drawnCard == null) {
                System.out.println("덱이 비었습니다.");
                break;
            }

            System.out.println("뽑은 카드: " + drawnCard);
            System.out.println("손에 있던 카드: " + currentPlayer.getHandCard());

            List<Card> playableCards = gameService.getPlayableCards(currentPlayer, drawnCard);
            System.out.println("플레이 가능한 카드: " + playableCards);

            Card cardToPlay = playableCards.get(0);

            Player target = null;
            Integer guessNumber = null;

            if (cardToPlay.getType().requiresTarget() && !cardToPlay.getType().canTargetSelf()) {
                List<Player> targetablePlayers = game.getTargetablePlayers(currentPlayer);
                if (!targetablePlayers.isEmpty()) {
                    target = targetablePlayers.get(0);
                }
            }

            if (cardToPlay.getType() == CardType.GUARD && target != null) {
                guessNumber = 2;
            }

            System.out.println("플레이할 카드: " + cardToPlay);
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

        System.out.println("\n=== 게임 로그 ===");
        game.getGameLog().forEach(System.out::println);

        System.out.println("\n=== 라운드 승자: " + game.getRoundWinner().getName() + " ===");
        System.out.println("=== 전체 라운드 테스트 완료 ===");
    }

    @Test
    void testCountessForcedPlay() {
        System.out.println("=== 후작 강제 발동 테스트 ===\n");

        CardService cardService = new CardService();
        GameService gameService = new GameService(cardService);

        Game game = gameService.createGame(1);
        Player player = game.getPlayers().get(0);

        player.setHandCard(new Card(CardType.COUNTESS, "test-countess"));
        Card princeCard = new Card(CardType.PRINCE, "test-prince");
        player.setDrawnCard(princeCard);

        assertTrue(player.hasCard(CardType.COUNTESS), "플레이어가 후작을 가져야 합니다.");

        List<Card> playableCards = gameService.getPlayableCards(player, princeCard);

        assertEquals(1, playableCards.size(), "후작과 마법사를 가지면 후작만 플레이 가능해야 합니다.");
        assertEquals(CardType.COUNTESS, playableCards.get(0).getType(), "플레이 가능한 카드는 후작이어야 합니다.");

        System.out.println("후작 강제 발동 로직 정상 작동!");
        System.out.println("=== 후작 강제 발동 테스트 완료 ===");
    }

    @Test
    void testPrincessAutoElimination() {
        System.out.println("=== 공주 자동 탈락 테스트 ===\n");

        CardService cardService = new CardService();
        GameService gameService = new GameService(cardService);

        Game game = gameService.createGame(1);
        Player player1 = game.getPlayers().get(0);
        Player player2 = game.getPlayers().get(1);

        player1.setHandCard(new Card(CardType.PRINCE, "test-prince"));
        player2.setHandCard(new Card(CardType.PRINCESS, "test-princess"));

        assertTrue(player2.isAlive(), "플레이어2는 초기에 생존 상태여야 합니다.");

        gameService.playCard(game, player1, player1.getHandCard(), player2, null);

        assertFalse(player2.isAlive(), "공주를 버린 플레이어2는 탈락해야 합니다.");

        System.out.println("공주 자동 탈락 로직 정상 작동!");
        System.out.println("=== 공주 자동 탈락 테스트 완료 ===");
    }

    @Test
    void testHandmaidProtection() {
        System.out.println("=== 사제 보호 테스트 ===\n");

        CardService cardService = new CardService();
        GameService gameService = new GameService(cardService);

        Game game = gameService.createGame(1);
        Player player1 = game.getPlayers().get(0);
        Player player2 = game.getPlayers().get(1);

        player2.setHandCard(new Card(CardType.HANDMAID, "test-handmaid"));

        assertFalse(player2.isProtected(), "초기에는 보호 상태가 아니어야 합니다.");

        gameService.playCard(game, player2, player2.getHandCard(), null, null);

        assertTrue(player2.isProtected(), "사제를 사용한 후 보호 상태여야 합니다.");

        List<Player> targetablePlayers = game.getTargetablePlayers(player1);
        assertFalse(targetablePlayers.contains(player2), "보호 상태인 플레이어는 타겟팅 불가능해야 합니다.");

        System.out.println("사제 보호 로직 정상 작동!");
        System.out.println("=== 사제 보호 테스트 완료 ===");
    }
}