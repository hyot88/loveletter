package com.simiyami.loveletter.controller;

import com.simiyami.loveletter.dto.CPUAction;
import com.simiyami.loveletter.dto.CardPlayRequest;
import com.simiyami.loveletter.model.Card;
import com.simiyami.loveletter.model.Game;
import com.simiyami.loveletter.model.GameState;
import com.simiyami.loveletter.model.Player;
import com.simiyami.loveletter.service.AIService;
import com.simiyami.loveletter.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/game")
public class GameController {

    private final GameService gameService;
    private final AIService aiService;

    public GameController(GameService gameService, AIService aiService) {
        this.gameService = gameService;
        this.aiService = aiService;
    }

    @PostMapping("/start")
    public ResponseEntity<GameState> startGame(@RequestParam int cpuCount) {
        try {
            Game game = gameService.createGame(cpuCount);
            GameState state = GameState.fromGame(game, "player-human");
            return ResponseEntity.ok(state);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameId}/draw")
    public ResponseEntity<Map<String, Object>> drawCard(
        @PathVariable String gameId,
        @RequestParam String playerId
    ) {
        try {
            Game game = gameService.getGame(gameId);
            Player player = game.getPlayer(playerId);

            if (player == null) {
                return ResponseEntity.badRequest().build();
            }

            Card drawnCard = gameService.drawCardForPlayer(game, player);

            Map<String, Object> response = new HashMap<>();
            response.put("drawnCard", drawnCard);
            response.put("playableCards", gameService.getPlayableCards(player, drawnCard));
            response.put("gameState", GameState.fromGame(game, playerId));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameId}/play")
    public ResponseEntity<GameState> playCard(
        @PathVariable String gameId,
        @RequestBody CardPlayRequest request
    ) {
        try {
            Game game = gameService.getGame(gameId);
            Player player = game.getPlayer(request.getPlayerId());
            Player target = request.getTargetId() != null ? game.getPlayer(request.getTargetId()) : null;

            if (player == null) {
                return ResponseEntity.badRequest().build();
            }

            // 플레이어가 가진 카드 찾기 (손패 or 뽑은 카드)
            Card cardToPlay = player.getHandCard();
            if (cardToPlay == null || !cardToPlay.getId().equals(request.getCardId())) {
                return ResponseEntity.badRequest().build();
            }

            gameService.playCard(game, player, cardToPlay, target, request.getGuessNumber());

            GameState state = GameState.fromGame(game, request.getPlayerId());
            return ResponseEntity.ok(state);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{gameId}/cpu-turn")
    public ResponseEntity<CPUAction> getCPUAction(
        @PathVariable String gameId,
        @RequestParam String cpuPlayerId
    ) {
        try {
            Game game = gameService.getGame(gameId);
            Player cpuPlayer = game.getPlayer(cpuPlayerId);

            if (cpuPlayer == null || !cpuPlayer.getType().name().equals("CPU")) {
                return ResponseEntity.badRequest().build();
            }

            // CPU가 카드를 뽑음
            Card drawnCard = gameService.drawCardForPlayer(game, cpuPlayer);
            if (drawnCard == null) {
                return ResponseEntity.badRequest().build();
            }

            // AI가 행동 결정
            CPUAction action = aiService.decideCPUAction(game, cpuPlayer, drawnCard);

            return ResponseEntity.ok(action);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameId}/cpu-turn")
    public ResponseEntity<GameState> executeCPUTurn(
        @PathVariable String gameId,
        @RequestBody CPUAction action
    ) {
        try {
            Game game = gameService.getGame(gameId);
            Player cpuPlayer = game.getCurrentPlayer();

            if (cpuPlayer == null || !cpuPlayer.getType().name().equals("CPU")) {
                return ResponseEntity.badRequest().build();
            }

            Player target = action.getTargetId() != null ? game.getPlayer(action.getTargetId()) : null;

            gameService.playCard(game, cpuPlayer, action.getCardToPlay(), target, action.getGuessNumber());

            if (!game.isRoundOver()) {
                gameService.nextTurn(game);
            }

            GameState state = GameState.fromGame(game, "player-human");
            return ResponseEntity.ok(state);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{gameId}/state")
    public ResponseEntity<GameState> getGameState(
        @PathVariable String gameId,
        @RequestParam(required = false, defaultValue = "player-human") String playerId
    ) {
        try {
            Game game = gameService.getGame(gameId);
            GameState state = GameState.fromGame(game, playerId);
            return ResponseEntity.ok(state);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{gameId}/next-round")
    public ResponseEntity<GameState> startNextRound(@PathVariable String gameId) {
        try {
            Game game = gameService.getGame(gameId);
            gameService.startNextRound(game);
            GameState state = GameState.fromGame(game, "player-human");
            return ResponseEntity.ok(state);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}