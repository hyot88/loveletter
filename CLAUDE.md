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
1. Game starts with 1 secret card removed from deck
2. Each player draws 1 card initially
3. On turn: draw 1 card (total 2), play 1 card
4. Round ends when deck empty or only 1 player alive
5. Winner is last player alive or highest card holder

### Critical Game Rules to Implement

**Countess (7) Auto-Play Logic**: If player holds Countess with Prince (5) or King (6), Countess must be played. This check happens at turn start and restricts UI choices.

**Princess (8) Auto-Elimination**: If Princess is forced to discard (via Prince card or any forced discard), player is immediately eliminated. Princess cannot be voluntarily played.

**Handmaid (4) Protection**: Protected players cannot be targeted by any card effects until their next turn begins. Track `isProtected` flag on Player.

## Expected Package Structure

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
│   ├── Player.java              # Player state (hand, discards, alive, protected)
│   ├── Game.java                # Game session
│   └── GameState.java           # Full game state snapshot
└── enums/
    ├── CardType.java            # Enum for cards 1-8
    └── PlayerType.java          # HUMAN vs CPU
```

## AI Implementation Strategy

**Guard AI Logic (Critical)**: When CPU plays Guard (1), it must intelligently guess opponent's card:
1. Collect all revealed cards (discarded pile + CPU's own hand)
2. Remove revealed cards from possible guesses (2-8, excluding 1)
3. Weight remaining possibilities based on card distribution
4. Make educated guess from remaining cards

**CPU Turn Delays**: Implement 2-second delays between actions (draw, think, play) for natural gameplay feel. This should be handled in frontend JavaScript with async/await.

**Card Priority Logic**: AI should evaluate card value based on game state - prefer high-impact plays (Baron when holding high card, Prince to force Princess discard if suspected).

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

Expected endpoints for game flow:
- `POST /game/start?cpuCount={1-3}` - Initialize new game
- `POST /game/{gameId}/draw` - Draw card at turn start
- `POST /game/{gameId}/play` - Play card with optional target/guess
- `GET /game/{gameId}/cpu-turn?cpuPlayerId={id}` - Get CPU decision
- `POST /game/{gameId}/cpu-turn` - Execute CPU action
- `GET /game/{gameId}/state` - Get current game state

## Development Phases

- ✅ **Phase 1 (Completed)**: Core game logic - Card system, game flow, and all card effects
- ✅ **Phase 2 (Completed)**: AI implementation - Smart CPU decision-making, Guard guessing logic, card evaluation strategy
- **Phase 3 (Ready to Start)**: UI/UX - Mobile-first web interface with Thymeleaf, gesture controls, animations
- **Phase 4**: Polish - Responsive design, enhanced animations, user experience improvements
- **Phase 5**: Online Multiplayer - WebSocket implementation, matchmaking, real-time gameplay

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

## Important Notes

- Game state should be managed in-memory (session/cache) for Phase 1
- Card images are in `src/main/resources/static/image/` (1.png through 8.png, plus back.png)
- Project uses Korean language for UI strings and logs
- Initial implementation targets 2-4 player games (1 human + 1-3 CPUs)
- Avoid over-engineering - implement only requested features, no premature abstractions