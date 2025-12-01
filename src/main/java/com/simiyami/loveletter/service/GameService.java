package com.simiyami.loveletter.service;

import com.simiyami.loveletter.enums.CardType;
import com.simiyami.loveletter.enums.PlayerType;
import com.simiyami.loveletter.model.Card;
import com.simiyami.loveletter.model.Game;
import com.simiyami.loveletter.model.Player;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameService {

    private final CardService cardService;
    private final Map<String, Game> games = new HashMap<>();

    public GameService(CardService cardService) {
        this.cardService = cardService;
    }

    public Game createGame(int cpuCount) {
        if (cpuCount < 1 || cpuCount > 3) {
            throw new IllegalArgumentException("CPU 수는 1-3명이어야 합니다.");
        }

        String gameId = UUID.randomUUID().toString();
        List<Player> players = new ArrayList<>();

        players.add(new Player("player-human", "당신", PlayerType.HUMAN));

        for (int i = 0; i < cpuCount; i++) {
            players.add(new Player("player-cpu-" + (i + 1), "CPU " + (i + 1), PlayerType.CPU));
        }

        Game game = new Game(gameId, players);
        games.put(gameId, game);

        initializeRound(game);

        return game;
    }

    public Game getGame(String gameId) {
        Game game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("게임을 찾을 수 없습니다: " + gameId);
        }
        return game;
    }

    public void initializeRound(Game game) {
        game.addLog("=== 라운드 " + game.getCurrentRound() + " 시작 ===");

        game.initializeDeck();

        Card secretCard = game.drawCard();
        game.setSecretCard(secretCard);
        game.addLog("비밀 카드 1장을 제외했습니다.");

        for (Player player : game.getPlayers()) {
            Card card = game.drawCard();
            player.setHandCard(card);
            game.addLog(String.format("%s가 초기 카드를 받았습니다.", player.getName()));
        }

        game.addLog(String.format("%s의 턴입니다.", game.getCurrentPlayer().getName()));
    }

    public Card drawCardForPlayer(Game game, Player player) {
        if (!player.isAlive()) {
            throw new IllegalStateException(player.getName() + "은(는) 이미 탈락했습니다.");
        }

        Card drawnCard = game.drawCard();
        if (drawnCard == null) {
            game.addLog("덱에 카드가 없습니다. 라운드를 종료합니다.");
            endRound(game);
            return null;
        }

        game.addLog(String.format("%s가 카드를 뽑았습니다.", player.getName()));
        return drawnCard;
    }

    public void playCard(Game game, Player player, Card cardToPlay, Player target, Integer guessNumber) {
        if (!player.isAlive()) {
            throw new IllegalStateException(player.getName() + "은(는) 이미 탈락했습니다.");
        }

        if (player.getHandCard() == null) {
            throw new IllegalStateException(player.getName() + "의 손에 카드가 없습니다.");
        }

        game.addToDiscardPile(cardToPlay);
        player.addDiscardedCard(cardToPlay);

        cardService.executeCardEffect(game, player, cardToPlay, target, guessNumber);

        if (checkRoundEnd(game)) {
            endRound(game);
        }
    }

    public boolean checkRoundEnd(Game game) {
        long aliveCount = game.getAlivePlayers().size();

        if (aliveCount <= 1) {
            game.addLog("생존자가 1명 이하입니다. 라운드를 종료합니다.");
            return true;
        }

        if (game.getDeck().isEmpty()) {
            game.addLog("덱이 비었습니다. 라운드를 종료합니다.");
            return true;
        }

        return false;
    }

    public void endRound(Game game) {
        game.setRoundOver(true);
        Player winner = determineWinner(game);

        if (winner != null) {
            game.setRoundWinner(winner);
            winner.incrementRoundsWon();
            game.addLog(String.format("=== 라운드 %d 승자: %s ===",
                game.getCurrentRound(), winner.getName()));

            game.addLog("\n=== 최종 카드 공개 ===");
            for (Player player : game.getPlayers()) {
                if (player.isAlive() && player.getHandCard() != null) {
                    game.addLog(String.format("%s: %s",
                        player.getName(), player.getHandCard().toString()));
                }
            }

            if (game.getSecretCard() != null) {
                game.addLog(String.format("비밀 카드: %s", game.getSecretCard().toString()));
            }
        } else {
            game.addLog("=== 무승부 ===");
        }
    }

    public Player determineWinner(Game game) {
        List<Player> alivePlayers = game.getAlivePlayers();

        if (alivePlayers.isEmpty()) {
            return null;
        }

        if (alivePlayers.size() == 1) {
            return alivePlayers.get(0);
        }

        return alivePlayers.stream()
            .max(Comparator.comparingInt(p -> p.getHandCard().getNumber()))
            .orElse(null);
    }

    public void startNextRound(Game game) {
        game.incrementRound();
        game.resetForNewRound();
        initializeRound(game);
    }

    public void nextTurn(Game game) {
        Player currentPlayer = game.getCurrentPlayer();
        if (currentPlayer != null && currentPlayer.isProtected()) {
            currentPlayer.setProtected(false);
            game.addLog(String.format("%s의 보호 상태가 해제되었습니다.", currentPlayer.getName()));
        }

        game.nextTurn();
        game.addLog(String.format("\n%s의 턴입니다.", game.getCurrentPlayer().getName()));
    }

    public boolean canPlayCard(Player player, Card card) {
        if (card.getType() == CardType.COUNTESS) {
            return true;
        }

        if (player.mustPlayCountess()) {
            return card.getType() == CardType.COUNTESS;
        }

        return true;
    }

    public List<Card> getPlayableCards(Player player, Card drawnCard) {
        List<Card> cards = new ArrayList<>();
        cards.add(player.getHandCard());
        cards.add(drawnCard);

        boolean hasCountess = cards.stream().anyMatch(c -> c.getType() == CardType.COUNTESS);
        boolean hasPrinceOrKing = cards.stream().anyMatch(c ->
            c.getType() == CardType.PRINCE || c.getType() == CardType.KING);

        if (hasCountess && hasPrinceOrKing) {
            return cards.stream()
                .filter(c -> c.getType() == CardType.COUNTESS)
                .toList();
        }

        return cards;
    }
}