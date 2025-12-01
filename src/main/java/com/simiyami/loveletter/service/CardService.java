package com.simiyami.loveletter.service;

import com.simiyami.loveletter.enums.CardType;
import com.simiyami.loveletter.model.Card;
import com.simiyami.loveletter.model.Game;
import com.simiyami.loveletter.model.Player;
import org.springframework.stereotype.Service;

@Service
public class CardService {

    public void executeCardEffect(Game game, Player player, Card playedCard, Player target, Integer guessNumber) {
        game.addLog(String.format("%s가 %s를 사용했습니다.", player.getName(), playedCard.getName()));

        switch (playedCard.getType()) {
            case GUARD -> executeGuard(game, player, target, guessNumber);
            case PRIEST -> executePriest(game, player, target);
            case BARON -> executeBaron(game, player, target);
            case HANDMAID -> executeHandmaid(game, player);
            case PRINCE -> executePrince(game, player, target);
            case KING -> executeKing(game, player, target);
            case COUNTESS -> executeCountess(game, player);
            case PRINCESS -> executePrincess(game, player);
        }
    }

    private void executeGuard(Game game, Player player, Player target, Integer guessNumber) {
        if (target == null || guessNumber == null) {
            game.addLog("경비병: 타겟이나 추측 숫자가 없습니다.");
            return;
        }

        if (guessNumber == 1) {
            game.addLog("경비병: 경비병(1번)은 추측할 수 없습니다.");
            return;
        }

        game.addLog(String.format("%s가 %s의 카드를 %d번으로 추측했습니다.",
            player.getName(), target.getName(), guessNumber));

        if (target.getHandCard() != null && target.getHandCard().getNumber() == guessNumber) {
            game.addLog(String.format("정답! %s가 탈락했습니다!", target.getName()));
            target.eliminate();
        } else {
            game.addLog("틀렸습니다!");
        }
    }

    private void executePriest(Game game, Player player, Player target) {
        if (target == null) {
            game.addLog("광대: 타겟이 없습니다.");
            return;
        }

        Card targetCard = target.getHandCard();
        if (targetCard != null) {
            game.addLog(String.format("%s가 %s의 카드를 확인했습니다: %s",
                player.getName(), target.getName(), targetCard.toString()));
        }
    }

    private void executeBaron(Game game, Player player, Player target) {
        if (target == null) {
            game.addLog("기사: 타겟이 없습니다.");
            return;
        }

        Card playerCard = player.getHandCard();
        Card targetCard = target.getHandCard();

        if (playerCard == null || targetCard == null) {
            game.addLog("기사: 카드가 없습니다.");
            return;
        }

        game.addLog(String.format("%s(%d) vs %s(%d)",
            player.getName(), playerCard.getNumber(),
            target.getName(), targetCard.getNumber()));

        if (playerCard.getNumber() > targetCard.getNumber()) {
            game.addLog(String.format("%s가 승리! %s가 탈락했습니다.",
                player.getName(), target.getName()));
            target.eliminate();
        } else if (playerCard.getNumber() < targetCard.getNumber()) {
            game.addLog(String.format("%s가 승리! %s가 탈락했습니다.",
                target.getName(), player.getName()));
            player.eliminate();
        } else {
            game.addLog("무승부! 둘 다 무사합니다.");
        }
    }

    private void executeHandmaid(Game game, Player player) {
        player.setProtected(true);
        game.addLog(String.format("%s가 다음 턴까지 보호 상태입니다.", player.getName()));
    }

    private void executePrince(Game game, Player player, Player target) {
        if (target == null) {
            game.addLog("마법사: 타겟이 없습니다.");
            return;
        }

        Card targetCard = target.getHandCard();
        if (targetCard == null) {
            game.addLog("마법사: 타겟의 카드가 없습니다.");
            return;
        }

        game.addLog(String.format("%s가 %s의 카드를 버리게 했습니다: %s",
            player.getName(), target.getName(), targetCard.toString()));

        game.addToDiscardPile(targetCard);
        target.addDiscardedCard(targetCard);

        if (targetCard.getType() == CardType.PRINCESS) {
            game.addLog(String.format("%s가 공주를 버려서 탈락했습니다!", target.getName()));
            target.eliminate();
            target.setHandCard(null);
            return;
        }

        Card newCard = game.drawCard();
        if (newCard != null) {
            target.setHandCard(newCard);
            game.addLog(String.format("%s가 새 카드를 뽑았습니다.", target.getName()));
        } else {
            if (game.getSecretCard() != null) {
                target.setHandCard(game.getSecretCard());
                game.addLog(String.format("%s가 비밀 카드를 받았습니다.", target.getName()));
                game.setSecretCard(null);
            } else {
                game.addLog("덱에 카드가 없습니다!");
                target.setHandCard(null);
            }
        }
    }

    private void executeKing(Game game, Player player, Player target) {
        if (target == null) {
            game.addLog("장군: 타겟이 없습니다.");
            return;
        }

        Card playerCard = player.getHandCard();
        Card targetCard = target.getHandCard();

        if (playerCard == null || targetCard == null) {
            game.addLog("장군: 카드가 없습니다.");
            return;
        }

        player.setHandCard(targetCard);
        target.setHandCard(playerCard);

        game.addLog(String.format("%s와 %s가 카드를 교환했습니다.",
            player.getName(), target.getName()));
    }

    private void executeCountess(Game game, Player player) {
        game.addLog(String.format("%s가 후작을 내려놓았습니다.", player.getName()));
    }

    private void executePrincess(Game game, Player player) {
        game.addLog(String.format("%s가 공주를 버려서 즉시 탈락했습니다!", player.getName()));
        player.eliminate();
    }
}