# 러브레터 (Love Letter) - 카나이 팩토리 에디션

## 프로젝트 개요
웹 기반 러브레터 보드게임 구현 프로젝트입니다. 카나이 팩토리 에디션 규칙을 따르며, CPU와 대전하거나 다른 유저와 온라인으로 대전할 수 있는 게임입니다.

## 기술 스택
- **Backend**: Spring Boot 3.x
- **Frontend**: Thymeleaf + HTML5 + CSS3 + JavaScript
- **Build Tool**: Gradle 또는 Maven
- **Java Version**: 21

## 게임 규칙 (카나이 팩토리 에디션)

### 카드 구성 (1-8번)

#### 1번 - 경비병 (Guard) [5장]
- **효과**: 상대방 한 명의 손에 있는 카드 숫자를 짐작해서 맞추면 그 상대방은 탈락합니다.
- **제약**: 경비병(1번)은 지목할 수 없습니다.

#### 2번 - 광대 (Priest) [2장]
- **효과**: 상대방 한 명의 카드를 볼 수 있습니다.

#### 3번 - 기사 (Baron) [2장]
- **효과**: 상대방 한 명과 카드 숫자를 겨뤄서 낮은 쪽은 탈락합니다.
- **특이사항**: 같으면 둘 다 무사합니다.

#### 4번 - 사제 (Handmaid) [2장]
- **효과**: 다음 차례가 올 때까지 다른 카드의 영향을 받지 않습니다.
- **보호 상태**: 이 효과가 활성화된 플레이어는 타겟 불가능

#### 5번 - 마법사 (Prince) [2장]
- **효과**: 상대나 자신의 손에서 카드를 버리고, 새 카드 한 장을 뽑게 합니다.
- **타겟**: 자신 또는 상대방 선택 필수

#### 6번 - 장군 (King) [1장]
- **효과**: 상대방 한 명과 카드를 교환합니다.

#### 7번 - 후작 (Countess) [1장]
- **효과**: 마법사(5)나 장군(6) 카드와 함께 들고 있다면, 반드시 이 카드를 내려놓습니다.
- **특이사항**: 5번이나 6번이 없어도 일부러 내려놓을 수 있습니다.

#### 8번 - 공주 (Princess) [1장]
- **효과**: 가장 숫자가 높은 카드입니다.
- **위험**: 라운드 중 강제로 공개되거나 버리게 되면 즉시 탈락합니다.

### 게임 진행
1. 각 플레이어는 카드 1장으로 시작
2. 자신의 턴에 카드 1장을 뽑아 2장이 됨
3. 2장 중 1장을 선택하여 사용하고 효과 발동
4. 마지막까지 살아남거나 가장 높은 숫자 카드를 가진 플레이어가 승리
5. 총 13라운드(승점 획득 방식) 또는 한 라운드 승자 결정

## 프로젝트 구조

```
src/
├── main/
│   ├── java/
│   │   └── com/loveletter/
│   │       ├── LoveLetterApplication.java
│   │       ├── controller/
│   │       │   ├── GameController.java      # 게임 메인 컨트롤러
│   │       │   └── MenuController.java      # 메뉴 컨트롤러
│   │       ├── service/
│   │       │   ├── GameService.java         # 게임 로직 서비스
│   │       │   ├── AIService.java           # CPU AI 로직
│   │       │   └── CardService.java         # 카드 효과 처리
│   │       ├── model/
│   │       │   ├── Card.java                # 카드 엔티티
│   │       │   ├── Player.java              # 플레이어 엔티티
│   │       │   ├── Game.java                # 게임 세션
│   │       │   └── GameState.java           # 게임 상태
│   │       └── enums/
│   │           ├── CardType.java            # 카드 타입 (1-8)
│   │           └── PlayerType.java          # HUMAN, CPU
│   └── resources/
│       ├── static/
│       │   ├── css/
│       │   │   └── game.css                 # 게임 스타일
│       │   ├── js/
│       │   │   └── game.js                  # 게임 인터랙션
│       │   └── image/
│       │       ├── 1.png                    # 경비병
│       │       ├── 2.png                    # 광대
│       │       ├── 3.png                    # 기사
│       │       ├── 4.png                    # 사제
│       │       ├── 5.png                    # 마법사
│       │       ├── 6.png                    # 장군
│       │       ├── 7.png                    # 후작
│       │       ├── 8.png                    # 공주
│       │       └── back.png                 # 카드 뒷면
│       └── templates/
│           ├── menu.html                    # 메뉴 화면
│           └── game.html                    # 게임 화면
```

## 핵심 기능 명세

### 1. 메뉴 시스템
```
메인 메뉴
├── vs CPU
│   ├── vs 1 CPU (2인 게임)
│   ├── vs 2 CPU (3인 게임)
│   └── vs 3 CPU (4인 게임)
└── vs User (2차 개발)
    ├── vs 1 User
    ├── vs 2 User
    └── vs 3 User
```

**1차 목표**: vs CPU 모드 완성
**2차 목표**: vs User 모드 (WebSocket 활용)

### 2. 게임 화면 UI/UX (모바일 웹 기반)

#### 화면 레이아웃
```
┌─────────────────────────────┐
│ [턴 정보] [스코어]           │ ← 상단 헤더
├─────────────────────────────┤
│                              │
│   [게임 로그 영역]           │ ← 상태 메시지 표시
│   "플레이어2가 카드를 뽑았습니다"  │
│   "플레이어2가 경비병을 사용..."   │
│                              │
├─────────────────────────────┤
│                              │
│     [중앙 카드 영역]          │ ← 방금 사용한 카드
│       (사용된 카드)           │
│                              │
├─────────────────────────────┤
│                              │
│    [내 카드 영역]             │ ← 좌우 스크롤 가능
│     [A] [B]                  │    위로 슬라이드로 발동
│   (겹쳐서 보이기)             │
│                              │
└─────────────────────────────┘
   [타겟 선택 UI]               ← 필요시 오른쪽에 표시
```

#### 카드 인터랙션
1. **카드 보기**:
    - 내 카드 2장이 겹쳐서 표시 (B가 앞, A가 뒤)
    - 좌우 스크롤로 카드 전환 (A를 앞으로 가져오기)

2. **카드 발동**:
    - 앞에 있는 카드를 위로 슬라이드
    - 슬라이드 모션으로 카드 사용 애니메이션
    - 엄지손가락 하나로 모든 조작 가능

3. **타겟 선택**:
    - 카드 효과에 타겟이 필요한 경우
    - 오른쪽에 플레이어 목록 표시
    - 탭하여 선택

4. **카드 활성화/비활성화**:
    - 내 턴: 카드 슬라이드 가능
    - 상대 턴: 카드 슬라이드 불가 (비활성화)

#### 화면 요소

**상단 헤더**:
- 현재 턴 정보: "당신의 턴" / "플레이어2의 턴"
- 스코어 표시: 각 플레이어의 라운드 승수

**게임 로그**:
- 최근 3-5개 액션 표시
- 예: "플레이어2가 카드를 뽑았습니다"
- 예: "플레이어3이 경비병을 사용하여 당신을 지목했습니다"
- 예: "틀렸습니다!"

**라운드 종료 시**:
- 모든 플레이어의 최종 카드 공개
- 승자 결정 및 애니메이션
- 다음 라운드로 진행 또는 게임 종료

### 3. 게임 로직 구현

#### GameService 핵심 메서드
```java
// 게임 초기화
public Game initializeGame(int cpuCount)

// 턴 시작 (카드 뽑기)
public void startTurn(Game game, Player player)

// 카드 사용
public GameState playCard(Game game, Player player, Card card, Player target, Integer guessNumber)

// 라운드 종료 체크
public boolean isRoundOver(Game game)

// 승자 결정
public Player determineWinner(Game game)

// 다음 턴으로
public void nextTurn(Game game)
```

#### CardService 카드 효과 구현
```java
// 1번 - 경비병
public void executeGuard(Player target, int guessNumber)

// 2번 - 광대
public Card executePriest(Player target)

// 3번 - 기사
public void executeBaron(Player player, Player target)

// 4번 - 사제
public void executeHandmaid(Player player)

// 5번 - 마법사
public void executePrince(Player target, Game game)

// 6번 - 장군
public void executeKing(Player player, Player target)

// 7번 - 후작
public void executeCountess(Player player)

// 8번 - 공주
// 자동 탈락 처리 (강제 공개 시)
```

#### 후작(7번) 자동 발동 로직
```java
// 턴 시작 시 체크
if (player.hasCard(COUNTESS) && (player.hasCard(PRINCE) || player.hasCard(KING))) {
    // 후작을 자동으로 버려야 함을 UI에 표시
    // 선택지 제한
}
```

### 4. AI 로직 (AIService)

#### CPU 행동 결정
```java
public CardAction decideCPUAction(Game game, Player cpuPlayer) {
    // 1. 후작(7) 강제 체크
    if (mustPlayCountess(cpuPlayer)) {
        return new CardAction(COUNTESS, null, null);
    }
    
    // 2. 카드 우선순위 결정
    Card selectedCard = selectBestCard(cpuPlayer, game);
    
    // 3. 타겟 선택
    Player target = selectTarget(game, cpuPlayer, selectedCard);
    
    // 4. 경비병인 경우 추측 숫자
    Integer guessNumber = null;
    if (selectedCard.getType() == GUARD) {
        guessNumber = guessCardNumber(game, target);
    }
    
    return new CardAction(selectedCard, target, guessNumber);
}
```

#### 경비병 AI 로직 (핵심!)
```java
private Integer guessCardNumber(Game game, Player target) {
    // 1. 이미 공개된 모든 카드 수집
    Set<Integer> revealedCards = new HashSet<>();
    
    // 버린 카드들
    revealedCards.addAll(game.getDiscardedCards());
    
    // CPU가 가진 카드
    revealedCards.add(cpuPlayer.getHandCard().getNumber());
    
    // 2. 가능한 카드 목록 (2-8번, 1번 제외)
    List<Integer> possibleCards = Arrays.asList(2, 3, 4, 5, 6, 7, 8);
    
    // 3. 이미 나온 카드 제외
    possibleCards.removeAll(revealedCards);
    
    // 4. 남은 카드 중 랜덤 선택
    return possibleCards.get(random.nextInt(possibleCards.size()));
}
```

#### CPU 행동 딜레이
- 카드 뽑기: 2초 대기
- 카드 선택 고민: 2초 대기
- 타겟 선택: 2초 대기
- 총 단계별로 2초씩 딜레이를 주어 자연스러운 플레이 연출

```javascript
// JavaScript에서 구현
async function executeCPUTurn(cpuPlayer) {
    await showMessage(`${cpuPlayer.name}의 턴입니다.`, 2000);
    
    await drawCardAnimation(cpuPlayer);
    await delay(2000);
    
    await thinkingAnimation(cpuPlayer);
    await delay(2000);
    
    const action = await fetchCPUAction(cpuPlayer.id);
    await playCardAnimation(cpuPlayer, action);
    await delay(2000);
    
    await showActionResult(action);
}
```

### 5. 게임 상태 관리

#### GameState
```java
public class GameState {
    private List<Player> players;
    private Deque<Card> deck;
    private List<Card> discardPile;
    private Card secretCard;  // 게임 시작 시 빼둔 카드 1장
    private int currentPlayerIndex;
    private List<String> gameLog;
    private boolean roundOver;
    private Player roundWinner;
}
```

#### Player
```java
public class Player {
    private String id;
    private String name;
    private PlayerType type;  // HUMAN, CPU
    private Card handCard;
    private List<Card> discardedCards;
    private boolean isAlive;
    private boolean isProtected;  // 사제(4) 효과
    private int roundsWon;
}
```

### 6. 프론트엔드 JavaScript 구조

```javascript
// game.js

// 게임 상태
let gameState = {
    myCards: [],
    selectedCardIndex: 0,
    isMyTurn: false,
    currentAction: null
};

// 카드 스크롤 이벤트
function initCardSwipe() {
    const cardContainer = document.getElementById('my-cards');
    let startX, currentX;
    
    cardContainer.addEventListener('touchstart', (e) => {
        startX = e.touches[0].clientX;
    });
    
    cardContainer.addEventListener('touchmove', (e) => {
        currentX = e.touches[0].clientX;
        const diff = currentX - startX;
        // 카드 슬라이드 애니메이션
        updateCardPosition(diff);
    });
    
    cardContainer.addEventListener('touchend', (e) => {
        const diff = currentX - startX;
        if (Math.abs(diff) > 50) {
            // 카드 전환
            switchCard(diff > 0 ? -1 : 1);
        }
    });
}

// 카드 발동 (위로 슬라이드)
function initCardPlay() {
    const cardContainer = document.getElementById('my-cards');
    let startY, currentY;
    
    cardContainer.addEventListener('touchstart', (e) => {
        if (!gameState.isMyTurn) return;
        startY = e.touches[0].clientY;
    });
    
    cardContainer.addEventListener('touchmove', (e) => {
        if (!gameState.isMyTurn) return;
        currentY = e.touches[0].clientY;
        const diff = startY - currentY;
        if (diff > 0) {
            // 위로 슬라이드 중
            animateCardUp(diff);
        }
    });
    
    cardContainer.addEventListener('touchend', (e) => {
        if (!gameState.isMyTurn) return;
        const diff = startY - currentY;
        if (diff > 100) {
            // 카드 발동!
            playCard(gameState.myCards[gameState.selectedCardIndex]);
        } else {
            // 취소, 원위치
            resetCardPosition();
        }
    });
}

// 타겟 선택 UI
function showTargetSelection(card) {
    const targetUI = document.getElementById('target-selection');
    const availableTargets = getAvailableTargets();
    
    targetUI.innerHTML = '';
    availableTargets.forEach(player => {
        const btn = createTargetButton(player);
        targetUI.appendChild(btn);
    });
    
    targetUI.classList.add('show');
}

// 게임 로그 추가
function addGameLog(message) {
    const logContainer = document.getElementById('game-log');
    const logEntry = document.createElement('div');
    logEntry.className = 'log-entry';
    logEntry.textContent = message;
    
    logContainer.insertBefore(logEntry, logContainer.firstChild);
    
    // 최대 5개만 유지
    while (logContainer.children.length > 5) {
        logContainer.removeChild(logContainer.lastChild);
    }
}

// CPU 턴 실행 (딜레이 포함)
async function executeCPUTurn(cpuPlayerId) {
    gameState.isMyTurn = false;
    
    addGameLog(`${cpuPlayerId}의 턴입니다.`);
    await delay(2000);
    
    addGameLog(`${cpuPlayerId}가 카드를 뽑았습니다.`);
    await delay(2000);
    
    const action = await fetchCPUAction(cpuPlayerId);
    
    addGameLog(`${cpuPlayerId}가 ${action.cardName}을 사용했습니다.`);
    showCentralCard(action.card);
    await delay(2000);
    
    if (action.targetId) {
        addGameLog(`대상: ${action.targetName}`);
        await delay(1000);
    }
    
    addGameLog(action.resultMessage);
    await delay(2000);
    
    await checkRoundOver();
}

// 라운드 종료 처리
async function handleRoundOver(result) {
    addGameLog('라운드가 종료되었습니다!');
    
    // 모든 플레이어의 카드 공개
    showAllPlayerCards(result.players);
    await delay(3000);
    
    addGameLog(`승자: ${result.winner.name}`);
    await delay(2000);
    
    if (result.gameOver) {
        showGameResult(result);
    } else {
        startNextRound();
    }
}
```

### 7. REST API 엔드포인트

```java
// GameController.java

@GetMapping("/")
public String menu() {
    return "menu";
}

@PostMapping("/game/start")
@ResponseBody
public GameState startGame(@RequestParam int cpuCount) {
    return gameService.createGame(cpuCount);
}

@PostMapping("/game/{gameId}/draw")
@ResponseBody
public GameState drawCard(@PathVariable String gameId) {
    return gameService.drawCard(gameId);
}

@PostMapping("/game/{gameId}/play")
@ResponseBody
public GameState playCard(
    @PathVariable String gameId,
    @RequestBody CardPlayRequest request
) {
    return gameService.playCard(
        gameId, 
        request.getCardId(), 
        request.getTargetId(), 
        request.getGuessNumber()
    );
}

@GetMapping("/game/{gameId}/cpu-turn")
@ResponseBody
public CPUAction getCPUAction(
    @PathVariable String gameId,
    @RequestParam String cpuPlayerId
) {
    return aiService.decideCPUAction(gameId, cpuPlayerId);
}

@PostMapping("/game/{gameId}/cpu-turn")
@ResponseBody
public GameState executeCPUTurn(
    @PathVariable String gameId,
    @RequestBody CPUAction action
) {
    return gameService.executeCPUAction(gameId, action);
}

@GetMapping("/game/{gameId}/state")
@ResponseBody
public GameState getGameState(@PathVariable String gameId) {
    return gameService.getGameState(gameId);
}
```

## 구현 우선순위

### Phase 1: 핵심 게임 로직 (필수)
1. ✅ 카드 엔티티 및 Enum 생성
2. ✅ 덱 생성 및 셔플 로직
3. ✅ 기본 게임 플로우 (턴 진행, 카드 뽑기, 카드 내기)
4. ✅ 각 카드 효과 구현 (1-8번)
5. ✅ 후작(7번) 강제 발동 로직
6. ✅ 공주(8번) 자동 탈락 처리
7. ✅ 라운드 종료 및 승자 결정

### Phase 2: AI 구현
1. ✅ 기본 CPU 행동 패턴
2. ✅ 경비병 AI (똑똑한 추측 로직)
3. ✅ CPU 행동 딜레이 (2초)
4. ✅ CPU 카드 선택 전략

### Phase 3: UI/UX (모바일 웹)
1. ✅ 메뉴 화면
2. ✅ 게임 화면 레이아웃
3. ✅ 카드 좌우 스크롤 (터치)
4. ✅ 카드 위로 슬라이드 발동
5. ✅ 타겟 선택 UI
6. ✅ 게임 로그 표시
7. ✅ 턴 정보 및 스코어 표시
8. ✅ 라운드 종료 시 카드 공개
9. ✅ 카드 활성화/비활성화

### Phase 4: 다듬기
1. ✅ 애니메이션 효과 강화
2. 사운드 효과 (선택)
3. ✅ 반응형 디자인 최적화
4. ✅ 에러 처리 및 예외 상황
5. 게임 저장/불러오기 (선택)

### Phase 5: 온라인 멀티플레이 (2차 개발)
1. WebSocket 구현
2. 방 생성/참가 시스템
3. 실시간 게임 동기화
4. 채팅 기능
5. 매칭 시스템

## 개발 시 주의사항

### 1. 카드 개수
- 경비병(1): 5장
- 광대(2): 2장
- 기사(3): 2장
- 사제(4): 2장
- 마법사(5): 2장
- 장군(6): 1장
- 후작(7): 1장
- 공주(8): 1장
- **총 16장**, 게임 시작 시 1장을 비밀 카드로 제외

### 2. 후작(7번) 처리
- 마법사(5)나 장군(6)과 함께 있으면 **반드시** 후작을 내야 함
- UI에서 후작 이외의 카드 선택 불가 처리
- 일부러 낼 수도 있음 (선택 가능)

### 3. 사제(4번) 보호 효과
- 다음 턴까지 유지
- 타겟 불가능 상태 표시
- 자신의 턴이 시작되면 보호 해제

### 4. 공주(8번) 즉시 탈락
- 마법사(5)로 강제로 버려지면 탈락
- 장군(6)으로 교환 받아도 안전
- 자발적으로 낼 수 없음 (UI에서 제한)

### 5. 모바일 터치 최적화
- 터치 영역 충분히 크게
- 스와이프 제스처 자연스럽게
- 오작동 방지 (최소 이동 거리)
- 햅틱 피드백 (가능하면)

### 6. 성능 최적화
- 게임 상태를 메모리에 관리 (세션 또는 캐시)
- 카드 이미지 사전 로딩
- 애니메이션 최적화

## 테스트 케이스

### 필수 테스트 시나리오
1. 2인 게임 (vs 1 CPU) 정상 진행
2. 4인 게임 (vs 3 CPU) 정상 진행
3. 경비병으로 맞추기/틀리기
4. 광대로 카드 확인
5. 기사로 대결 (승/패/무)
6. 사제로 보호 상태
7. 마법사로 자신/상대 카드 버리기
8. 장군으로 카드 교환
9. 후작 강제 발동
10. 공주 강제 공개 탈락
11. 라운드 종료 및 승자 결정
12. 여러 라운드 게임

## 실행 방법

```bash
# 프로젝트 빌드
./gradlew build

# 실행
./gradlew bootRun

# 접속
http://localhost:8080
```

## 이미지 리소스 위치
```
src/main/resources/static/image/
├── 1.png    # 경비병
├── 2.png    # 광대
├── 3.png    # 기사
├── 4.png    # 사제
├── 5.png    # 마법사
├── 6.png    # 장군
├── 7.png    # 후작
├── 8.png    # 공주
└── back.png # 카드 뒷면
```

## 향후 개발 계획
1. ✅ **Phase 1 (완료)**: 핵심 게임 로직 - 카드 시스템, 게임 플로우, 카드 효과
2. ✅ **Phase 2 (완료)**: AI 구현 - 스마트한 CPU 의사결정, 경비병 추측 로직, 카드 평가 전략
3. ✅ **Phase 3 (완료)**: UI/UX - 모바일 우선 웹 인터페이스, 제스처 컨트롤, 터치 인터랙션
4. ✅ **Phase 4 (완료)**: 다듬기 - 애니메이션 강화, 반응형 디자인, 에러 처리
5. **Phase 5 (다음 단계)**: 온라인 멀티플레이 - WebSocket 구현, 매칭 시스템, 실시간 게임플레이

---

## Claude Code 실행 명령어
이 프로젝트는 Claude Code에서 다음 명령어로 초기화할 수 있습니다:
```
/init
```

위 명령어 실행 시 이 README.md의 명세를 기반으로 전체 프로젝트 구조를 생성하고 핵심 기능을 구현합니다.