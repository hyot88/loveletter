# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Love Letter (러브레터) - Web-based implementation of the Love Letter card game (Kanai Factory Edition). This is a Spring Boot application that allows players to play against CPU opponents, with plans for future online multiplayer.

**Tech Stack**: Spring Boot 4.0.0, Java 21, Thymeleaf, Gradle

## Build and Run Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Clean build
./gradlew clean build

# Run specific test class
./gradlew test --tests ClassName

# Run single test method
./gradlew test --tests ClassName.methodName
```

Access the application at `http://localhost:8080` after starting.

## Game Architecture

### Card System (1-8)
The game uses 16 cards total with specific distributions:
- **1 (Guard)**: 5 cards - Guess opponent's card
- **2 (Priest)**: 2 cards - View opponent's card
- **3 (Baron)**: 2 cards - Compare cards, lower loses
- **4 (Handmaid)**: 2 cards - Protection until next turn
- **5 (Prince)**: 2 cards - Force discard and draw
- **6 (King)**: 1 card - Swap hands
- **7 (Countess)**: 1 card - Must play if holding 5 or 6
- **8 (Princess)**: 1 card - Instant loss if discarded

### Core Game Flow
1. Game starts with 1 secret card removed from deck (16 total → 15 playable)
2. Each player draws 1 card initially (stored in `handCard`)
3. On turn: draw 1 card into `drawnCard` (total 2), play 1 card, keep 1 card
4. After playing a card, the remaining card becomes the new `handCard`
5. Round ends when deck empty or only 1 player alive
6. Winner is last player alive or highest card holder

**Critical Implementation Detail**: Player model uses TWO fields for the 2-card system:
- `handCard`: Persistent card from previous turn
- `drawnCard`: Temporary card drawn at turn start
- When a card is played, the remaining card becomes the new `handCard` and `drawnCard` is set to null

### Critical Game Rules to Implement

**Countess (7) Auto-Play Logic**: If player holds Countess with Prince (5) or King (6), Countess must be played. This check happens at turn start and restricts UI choices.

**Princess (8) Auto-Elimination**: If Princess is forced to discard (via Prince card or any forced discard), player is immediately eliminated. Princess cannot be voluntarily played.

**Handmaid (4) Protection**: Protected players cannot be targeted by any card effects until their next turn begins. Track `isProtected` flag on Player.

## Package Structure

```
com.simiyami.loveletter/
├── controller/
│   ├── GameController.java      # REST endpoints for game actions
│   └── MenuController.java      # Menu and game setup
├── service/
│   ├── GameService.java         # Core game logic and state management
│   ├── AIService.java           # CPU decision-making algorithms
│   └── CardService.java         # Card effect implementations
├── model/
│   ├── Card.java                # Card entity
│   ├── Player.java              # Player state (hand, discards, alive, protected, CPU memory)
│   ├── Game.java                # Game session
│   └── GameState.java           # Full game state snapshot with PlayerInfo
├── dto/
│   ├── CardPlayRequest.java     # Request DTO for playing cards
│   └── CPUAction.java           # CPU action decision result
└── enums/
    ├── CardType.java            # Enum for cards 1-8 with card counts
    └── PlayerType.java          # HUMAN vs CPU
```

## AI Implementation Strategy

**CPU Memory System**: The AI maintains a memory of opponent cards learned through PRIEST (viewing) and KING (exchanging). This memory is stored in `Player.knownOpponentCards` (Map<String, CardType>) and is automatically managed by CardService.

**Strategic Card Play**:
- **Guard (1)**: Prioritizes known opponent cards with validation (checks if opponent hasn't played it yet), falls back to probability-based guessing from unrevealed cards
- **Priest (2)**: Automatically remembers viewed cards for future decision-making
- **Baron (3)**: Only plays against opponents with known lower cards, otherwise uses discard pile analysis
- **Handmaid (4)**: Uses when multiple opponents exist and protection is valuable
- **Prince (5)**: Targets known PRINCESS holders for elimination, otherwise targets players with few discards (likely high cards)
- **King (6)**: Exchanges PRINCESS to opponents (for later GUARD elimination), or acquires known high-value cards
- **Countess (7)**: Mandatory play enforced when holding PRINCE or KING (score: 2, low priority)
- **Princess (8)**: Never plays (score: -100)

**CPU Turn Delays**: Implement 2-second delays between actions (draw, think, play) for natural gameplay feel. This should be handled in frontend JavaScript with async/await.

## Mobile-First UI/UX Design

The frontend uses touch-based gestures optimized for mobile:
- **Horizontal swipe**: Switch between 2 cards in hand (cards displayed overlapped)
- **Vertical swipe up**: Play the front card
- **Tap**: Select target player when required

UI layout from top to bottom:
1. Turn indicator and score header
2. Game log (recent 3-5 actions)
3. Central card area (last played card)
4. Player's hand (2 overlapping cards, swipe to switch)
5. Target selection panel (appears when needed)

## REST API Endpoints

Implemented endpoints for game flow:
- `POST /game/start?cpuCount={1-3}` - Initialize new game, returns GameState with gameId
- `POST /game/{gameId}/draw` - Draw card at turn start
- `POST /game/{gameId}/play` - Play card with optional target/guess parameters
  - Request body: `{cardType, targetPlayerId?, guessCardType?}`
- `GET /game/{gameId}/cpu-turn?cpuPlayerId={id}` - Get CPU's next action decision
- `POST /game/{gameId}/cpu-turn` - Execute CPU action with body from GET decision
- `GET /game/{gameId}/state` - Get current game state
- `POST /game/{gameId}/next-round` - Start next round after current round ends
- `GET /` - Menu page (MenuController)
- `GET /game?gameId={id}` - Game page (MenuController)

## Development Phases

- ✅ **Phase 1 (Completed)**: Core game logic - Card system, game flow, and all card effects
- ✅ **Phase 2 (Completed)**: AI implementation - Smart CPU decision-making, Guard guessing logic, card evaluation strategy
- ✅ **Phase 3 (Completed)**: UI/UX - Mobile-first web interface with Thymeleaf, gesture controls, touch interactions, game flow UI
- ✅ **Phase 4 (Completed)**: Polish - Enhanced animations, responsive design (desktop/tablet/mobile/landscape), error handling with retry logic
- **Phase 5 (Ready to Start)**: Online Multiplayer - WebSocket implementation, matchmaking, real-time gameplay

## Testing Focus Areas

Essential test scenarios:
- Countess forced play when holding Prince or King
- Princess auto-elimination on forced discard
- Handmaid protection preventing targeting
- Guard guess validation (cannot guess Guard)
- Baron tie (both players survive)
- Round end with multiple survivors (highest card wins)
- Secret card reveal at round end
- Multi-round game scoring

## Frontend Implementation Details

**File Locations**:
- Templates: `src/main/resources/templates/` (menu.html, game.html)
- CSS: `src/main/resources/static/css/game.css`
- JavaScript: `src/main/resources/static/js/game.js`
- Card images: `src/main/resources/static/image/` (1.png through 8.png, plus back.png)

**Touch Interaction Implementation**:
- Card swipe threshold: 50px horizontal for switching, 100px vertical for playing
- Movement direction is determined on first move (horizontal vs vertical) to prevent conflicts
- Use `touchstart`, `touchmove`, `touchend` events for gesture detection
- Unified touch handler prevents duplicate event listeners by cloning and replacing the element
- Vertical swipe applies `transform: translateY()` for visual feedback during drag

**Game State Management (JavaScript)**:
The frontend maintains a `gameState` object tracking:
- `gameId`, `myPlayerId`, `isMyTurn`
- `myCards`, `selectedCardIndex` (0 or 1 for which card is front)
- `pendingCard`, `pendingTarget` for multi-step card actions
- `discardPile` array for tracking all discarded cards in current round
- `actionResolve` promise resolver for synchronizing player actions with turn cycle

**Async Turn Cycle**:
The `startTurnCycle()` function loops through players:
1. Fetch game state and check if round is over
2. If my turn: call `handleMyTurn()` which draws card, displays cards, then waits for `actionResolve` promise
3. If CPU turn: call `handleCPUTurn()` with 2-second delays between steps (draw, think, play)
4. After each turn completes, add 500ms delay before next turn
5. When round ends, call `handleRoundOver()` which breaks the loop

**Critical**: Use the promise-based `waitForPlayerAction()` pattern to avoid duplicate turn cycle calls. The `executeCardPlay()` function resolves the promise when the player action is complete.

## Important Notes

- Game state is managed in-memory using HashMap in GameService
- Each game gets a unique UUID as gameId
- GameState.PlayerInfo has `showHandCard` logic - shows card to owner or if using Priest
- Project uses Korean language for UI strings and logs
- Initial implementation targets 2-4 player games (1 human + 1-3 CPUs)
- Avoid over-engineering - implement only requested features, no premature abstractions
- Frontend implements error handling with retry logic (`fetchWithRetry`) and user-friendly error messages
- Network requests have 10-second timeout with automatic retry (up to 2 retries)

## Common Pitfalls and Debugging

**Two-Card System**: The most common mistake is forgetting that Player has both `handCard` and `drawnCard` fields. When implementing card logic:
- `getPlayableCards()` must check BOTH fields for Countess forced play detection
- `playCard()` must determine which card was played and properly manage the remaining card
- Tests should NOT manually set `handCard` after playing - let the game logic handle it

**Turn Advancement**: GameController must call `gameService.nextTurn()` after `playCard()` completes (unless round is over). Without this, the game will get stuck on the same player.

**Frontend Turn Cycle**: Avoid duplicate `startTurnCycle()` calls. Use Promise resolution pattern (`actionResolve`) to signal when player action completes, rather than recursively calling `startTurnCycle()` from within action handlers.

**Card Visibility Management**: The frontend must maintain card visibility throughout all game phases:
- During opponent's turn: `displayRemainingCard()` shows the player's single `handCard` from server state
- During player's turn: `displayMyCards()` shows both `handCard` and `drawnCard` for selection
- After playing a card: `updateGameUI()` automatically displays remaining card from server state
- The `updateGameUI()` function checks `myPlayer.handCard` and calls `displayRemainingCard()` when `!state.roundOver`
- Never manually clear card display with `innerHTML = ''` - rely on `updateGameUI()` for proper state synchronization
- `displayRemainingCard()` checks if multiple cards are already displayed (length > 1) to avoid overwriting during player's turn

**Card Swipe Artifact Fix**: To prevent ghost images when swiping up to play a card:
1. Reset `transform` to empty string before applying animation
2. Add 50ms delay after reset to ensure CSS updates
3. Apply `card-play-animation` class after the delay

**Discard Pile Display**: The frontend displays up to 5 most recent cards stacked with offset positioning (3px offset per card, increasing z-index). Click on the discard pile opens a modal showing all discarded cards in a grid layout.

**Test Updates**: When modifying core game flow (like adding `drawnCard`), existing tests may fail. Update tests to work WITH the new logic rather than AROUND it - remove manual card management and let GameService handle it naturally.