// 게임 상태
let gameState = {
    gameId: null,
    myPlayerId: 'player-human',
    myCards: [],
    selectedCardIndex: 0,
    isMyTurn: false,
    currentAction: null,
    pendingCard: null,
    pendingTarget: null,
    discardPile: [] // 버린 카드 더미
};

// 카드 이름 매핑
const CARD_NAMES = {
    1: '경비병',
    2: '광대',
    3: '기사',
    4: '사제',
    5: '마법사',
    6: '장군',
    7: '후작',
    8: '공주'
};

// 유틸리티: 딜레이 함수
function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// 유틸리티: 에러 처리
function handleError(error, context, retryFn = null) {
    console.error(`${context} 오류:`, error);

    const errorMessages = {
        'Failed to fetch': '네트워크 연결을 확인해주세요.',
        'NetworkError': '네트워크 연결이 불안정합니다.',
        'TimeoutError': '요청 시간이 초과되었습니다.',
    };

    let message = errorMessages[error.name] || error.message || '알 수 없는 오류가 발생했습니다.';

    if (retryFn) {
        const retry = confirm(`${context} 중 오류가 발생했습니다.\n${message}\n\n다시 시도하시겠습니까?`);
        if (retry) {
            return retryFn();
        }
    } else {
        alert(`${context} 중 오류가 발생했습니다.\n${message}`);
    }

    return null;
}

// 유틸리티: 네트워크 요청 (재시도 포함)
async function fetchWithRetry(url, options = {}, retries = 2) {
    for (let i = 0; i <= retries; i++) {
        try {
            const response = await fetch(url, {
                ...options,
                signal: AbortSignal.timeout(10000) // 10초 타임아웃
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            return response;
        } catch (error) {
            if (i === retries) {
                throw error;
            }

            // 재시도 전 대기
            await delay(1000 * (i + 1));
        }
    }
}

// 게임 초기화
async function initializeGame(gameId) {
    gameState.gameId = gameId;
    showLoading('게임을 불러오는 중...');

    try {
        const response = await fetch(`/game/${gameId}/state?playerId=${gameState.myPlayerId}`);
        if (!response.ok) throw new Error('게임 상태를 불러올 수 없습니다.');

        const state = await response.json();
        updateGameUI(state);

        hideLoading();

        // 게임 시작 메시지
        addGameLog('게임이 시작되었습니다!');
        await delay(1000);

        // 턴 시작
        await startTurnCycle();

    } catch (error) {
        console.error('게임 초기화 오류:', error);
        hideLoading();
        alert('게임을 불러올 수 없습니다. 메뉴로 돌아갑니다.');
        window.location.href = '/';
    }
}

// 게임 UI 업데이트
function updateGameUI(state) {
    // 턴 정보 업데이트
    const currentPlayer = state.players.find(p => p.id === state.currentPlayerId);
    const turnInfo = document.getElementById('turnInfo');

    if (currentPlayer.id === gameState.myPlayerId) {
        turnInfo.textContent = '당신의 턴';
        turnInfo.style.color = '#4CAF50';
        turnInfo.classList.remove('cpu-turn');
        gameState.isMyTurn = true;
    } else {
        turnInfo.textContent = `${currentPlayer.name}의 턴`;
        turnInfo.style.color = '#2a5298';
        turnInfo.classList.add('cpu-turn');
        gameState.isMyTurn = false;
    }

    // 스코어 보드 업데이트
    updateScoreBoard(state.players);

    // 내 카드 업데이트 (내 턴이 아니고, 라운드가 끝나지 않았을 때만)
    const myPlayer = state.players.find(p => p.id === gameState.myPlayerId);
    if (myPlayer && myPlayer.handCard && !state.roundOver && state.currentPlayerId !== gameState.myPlayerId) {
        displayRemainingCard(myPlayer.handCard);
    }

    // 서버 게임 로그 업데이트
    if (state.recentLogs && state.recentLogs.length > 0) {
        updateGameLog(state.recentLogs);
    }
}

// 스코어 보드 업데이트
function updateScoreBoard(players) {
    const scoreBoard = document.getElementById('scoreBoard');
    scoreBoard.innerHTML = '';

    players.forEach(player => {
        const scoreItem = document.createElement('div');
        scoreItem.className = 'score-item';

        // 생존 여부에 따라 테두리 색상 변경
        if (player.alive) {
            scoreItem.classList.add('alive');
        } else {
            scoreItem.classList.add('eliminated');
        }

        scoreItem.innerHTML = `
            <span class="player-name">${player.name}</span>
            <span class="score" id="score-${player.id}">${player.roundsWon}</span>
        `;
        scoreBoard.appendChild(scoreItem);
    });
}

// 상대 플레이어 정보 업데이트
function updateOpponentsInfo(players) {
    const opponentsInfo = document.getElementById('opponentsInfo');
    opponentsInfo.innerHTML = '';

    players.forEach(player => {
        if (player.id === gameState.myPlayerId) return;

        const opponentCard = document.createElement('div');
        opponentCard.className = 'opponent-card';

        if (player.protected) {
            opponentCard.classList.add('protected');
        }

        if (!player.alive) {
            opponentCard.classList.add('eliminated');
        }

        let statusText = '생존';
        let statusClass = '';

        if (!player.alive) {
            statusText = '탈락';
            statusClass = 'eliminated';
        } else if (player.protected) {
            statusText = '보호 중';
            statusClass = 'protected';
        }

        opponentCard.innerHTML = `
            <div class="opponent-name">${player.name}</div>
            <div class="opponent-status ${statusClass}">${statusText}</div>
        `;
        opponentsInfo.appendChild(opponentCard);
    });
}

// 턴 사이클 시작
async function startTurnCycle() {
    while (true) {
        const state = await fetchGameState();

        if (state.roundOver) {
            await handleRoundOver(state);
            break;
        }

        const currentPlayer = state.players.find(p => p.id === state.currentPlayerId);

        if (currentPlayer.id === gameState.myPlayerId) {
            // 내 턴
            await handleMyTurn();
        } else {
            // CPU 턴
            await handleCPUTurn(currentPlayer);
        }

        // 다음 턴을 위한 짧은 딜레이
        await delay(500);
    }
}

// 게임 상태 가져오기
async function fetchGameState() {
    const response = await fetch(`/game/${gameState.gameId}/state?playerId=${gameState.myPlayerId}`);
    if (!response.ok) throw new Error('게임 상태를 가져올 수 없습니다.');
    const state = await response.json();
    updateGameUI(state);
    return state;
}

// 내 턴 처리
async function handleMyTurn() {
    addGameLog('당신의 턴입니다!');
    gameState.isMyTurn = true;

    // 카드 뽑기
    showLoading('카드를 뽑는 중...');
    await delay(1000);

    try {
        const response = await fetch(`/game/${gameState.gameId}/draw?playerId=${gameState.myPlayerId}`, {
            method: 'POST'
        });

        if (!response.ok) throw new Error('카드를 뽑을 수 없습니다.');

        const data = await response.json();
        hideLoading();

        addGameLog('카드를 뽑았습니다.');

        // 내 카드 표시
        displayMyCards(data.playableCards, data.drawnCard);

        // 사용자 입력 대기
        await waitForPlayerAction();

    } catch (error) {
        console.error('카드 뽑기 오류:', error);
        hideLoading();
        alert('카드를 뽑을 수 없습니다.');
    }
}

// 내 카드 표시 (2장 - 턴 시작 시)
function displayMyCards(playableCards, drawnCard) {
    const myCards = document.getElementById('myCards');
    const cardStack = myCards.querySelector('.card-stack');
    cardStack.innerHTML = '';

    gameState.myCards = playableCards;

    playableCards.forEach((card, index) => {
        const cardEl = document.createElement('div');
        cardEl.className = `my-card ${index === 0 ? 'front' : 'back'} card-draw-animation`;
        cardEl.dataset.index = index;
        cardEl.dataset.cardId = card.id;

        const img = document.createElement('img');
        img.src = `/image/${card.number}.png`;
        img.alt = card.name;
        cardEl.appendChild(img);

        cardStack.appendChild(cardEl);

        // 애니메이션 종료 후 클래스 제거
        setTimeout(() => {
            cardEl.classList.remove('card-draw-animation');
        }, 500);
    });

    // 터치 이벤트 초기화 (통합 핸들러)
    initCardTouch();
}

// 남은 카드 표시 (1장 - 상대방 턴일 때)
function displayRemainingCard(card) {
    const myCards = document.getElementById('myCards');
    const cardStack = myCards.querySelector('.card-stack');

    // 이미 카드가 표시되어 있으면 업데이트하지 않음 (내 턴일 때는 displayMyCards 사용)
    if (cardStack.children.length > 1) return;

    cardStack.innerHTML = '';
    gameState.myCards = [card];
    gameState.selectedCardIndex = 0;

    const cardEl = document.createElement('div');
    cardEl.className = 'my-card front';
    cardEl.dataset.index = 0;
    cardEl.dataset.cardId = card.id;

    const img = document.createElement('img');
    img.src = `/image/${card.number}.png`;
    img.alt = card.name;
    cardEl.appendChild(img);

    cardStack.appendChild(cardEl);
}

// 카드 터치 이벤트 통합 (스와이프 + 카드 사용)
function initCardTouch() {
    const cardStack = document.querySelector('.card-stack');
    if (!cardStack) return;

    // 이전 리스너 제거 (중복 방지)
    const newCardStack = cardStack.cloneNode(true);
    cardStack.parentNode.replaceChild(newCardStack, cardStack);

    let startX = 0;
    let startY = 0;
    let currentX = 0;
    let currentY = 0;
    let isDragging = false;
    let moveDirection = null; // 'horizontal' or 'vertical'

    newCardStack.addEventListener('touchstart', (e) => {
        if (!gameState.isMyTurn) return;

        startX = e.touches[0].clientX;
        startY = e.touches[0].clientY;
        currentX = startX;
        currentY = startY;
        isDragging = true;
        moveDirection = null;
    }, { passive: true });

    newCardStack.addEventListener('touchmove', (e) => {
        if (!gameState.isMyTurn || !isDragging) return;

        currentX = e.touches[0].clientX;
        currentY = e.touches[0].clientY;

        const diffX = Math.abs(currentX - startX);
        const diffY = Math.abs(currentY - startY);

        // 방향 결정 (처음 한 번만)
        if (!moveDirection && (diffX > 10 || diffY > 10)) {
            moveDirection = diffX > diffY ? 'horizontal' : 'vertical';
        }

        // 수직 드래그 (카드 사용)
        if (moveDirection === 'vertical') {
            e.preventDefault();
            const diff = startY - currentY;
            if (diff > 0) {
                const frontCard = newCardStack.querySelector('.my-card.front');
                if (frontCard) {
                    frontCard.style.transform = `translateY(-${diff}px)`;
                }
            }
        }
    }, { passive: false });

    newCardStack.addEventListener('touchend', (e) => {
        if (!gameState.isMyTurn || !isDragging) return;
        isDragging = false;

        const diffX = currentX - startX;
        const diffY = startY - currentY;

        if (moveDirection === 'horizontal' && Math.abs(diffX) > 50) {
            // 좌우 스와이프 - 카드 전환
            switchCard(diffX > 0 ? -1 : 1);
        } else if (moveDirection === 'vertical' && diffY > 100) {
            // 위로 스와이프 - 카드 사용
            const selectedCard = gameState.myCards[gameState.selectedCardIndex];
            playCard(selectedCard);
        } else {
            // 취소, 원위치
            const frontCard = newCardStack.querySelector('.my-card.front');
            if (frontCard) {
                frontCard.style.transform = '';
            }
        }

        moveDirection = null;
    }, { passive: true });
}

// 카드 전환
function switchCard(direction) {
    const cards = document.querySelectorAll('.my-card');
    if (cards.length <= 1) return;

    gameState.selectedCardIndex = (gameState.selectedCardIndex + direction + cards.length) % cards.length;

    cards.forEach((card, index) => {
        card.classList.remove('front', 'back');
        if (index === gameState.selectedCardIndex) {
            card.classList.add('front');
        } else {
            card.classList.add('back');
        }
    });
}

// 카드 사용
async function playCard(selectedCard) {
    gameState.isMyTurn = false;

    // front 카드의 실제 cardId를 DOM에서 직접 확인
    const frontCard = document.querySelector('.my-card.front');
    if (!frontCard) {
        console.error('front 카드를 찾을 수 없습니다.');
        return;
    }

    const frontCardId = frontCard.dataset.cardId;
    const actualCard = gameState.myCards.find(c => c.id === frontCardId);

    if (!actualCard) {
        console.error('카드를 찾을 수 없습니다:', frontCardId);
        return;
    }

    gameState.pendingCard = actualCard;

    addGameLog(`${actualCard.name}을(를) 선택했습니다.`);

    // 카드 애니메이션
    // 잔상 방지: transform 초기화 후 애니메이션 적용
    frontCard.style.transform = '';
    await delay(50); // 약간의 딜레이로 초기화 확실히 적용
    frontCard.classList.add('card-play-animation');

    await delay(300);

    // 타겟 선택이 필요한지 확인
    if (requiresTarget(actualCard.type)) {
        await selectTarget(actualCard);
    } else {
        // 타겟 불필요, 바로 실행
        await executeCardPlay(actualCard, null, null);
    }
}

// 타겟 선택 필요 여부
function requiresTarget(cardType) {
    return ['GUARD', 'PRIEST', 'BARON', 'PRINCE', 'KING'].includes(cardType);
}

// 타겟 선택
async function selectTarget(card) {
    const state = await fetchGameState();

    // 마법사 카드는 본인도 타겟 가능
    let availableTargets;
    if (card.type === 'PRINCE') {
        availableTargets = state.players.filter(p => p.alive && !p.protected);
    } else {
        availableTargets = state.players.filter(p =>
            p.id !== gameState.myPlayerId && p.alive && !p.protected
        );
    }

    // 타겟이 없으면 카드 버림
    if (availableTargets.length === 0) {
        await executeCardPlay(card, null, null);
        return;
    }

    // 타겟 선택 UI 표시
    showTargetSelection(availableTargets, card);
}

// 타겟 선택 UI 표시
function showTargetSelection(targets, card) {
    const overlay = document.getElementById('targetOverlay');
    const targetList = document.getElementById('targetList');
    targetList.innerHTML = '';

    targets.forEach(target => {
        const btn = document.createElement('button');
        btn.className = 'target-btn';
        btn.textContent = target.name;
        btn.onclick = async () => {
            overlay.classList.remove('show');
            gameState.pendingTarget = target.id;

            // 경비병인 경우 추측 UI 표시
            if (card.type === 'GUARD') {
                showGuessSelection(target);
            } else {
                await executeCardPlay(card, target.id, null);
            }
        };
        targetList.appendChild(btn);
    });

    overlay.classList.add('show');
}

// 타겟 선택 취소
function cancelTargetSelection() {
    const overlay = document.getElementById('targetOverlay');
    overlay.classList.remove('show');

    // 카드 원위치
    const frontCard = document.querySelector('.my-card.front');
    if (frontCard) {
        frontCard.classList.remove('card-play-animation');
        frontCard.style.transform = '';
    }

    gameState.isMyTurn = true;
    gameState.pendingCard = null;
    gameState.pendingTarget = null;
}

// 추측 선택 UI 표시
function showGuessSelection(target) {
    const overlay = document.getElementById('guessOverlay');
    overlay.classList.add('show');
}

// 추측 선택
async function selectGuess(guessNumber) {
    const overlay = document.getElementById('guessOverlay');
    overlay.classList.remove('show');

    await executeCardPlay(gameState.pendingCard, gameState.pendingTarget, guessNumber);
}

// 추측 취소
function cancelGuess() {
    const overlay = document.getElementById('guessOverlay');
    overlay.classList.remove('show');

    gameState.isMyTurn = true;
    gameState.pendingCard = null;
    gameState.pendingTarget = null;
}

// 카드 사용 실행
async function executeCardPlay(card, targetId, guessNumber) {
    showLoading('카드를 사용하는 중...');

    try {
        const request = {
            playerId: gameState.myPlayerId,
            cardId: card.id,
            targetId: targetId,
            guessNumber: guessNumber
        };

        const response = await fetch(`/game/${gameState.gameId}/play`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        });

        if (!response.ok) throw new Error('카드를 사용할 수 없습니다.');

        const state = await response.json();
        hideLoading();

        // 중앙 카드 영역에 표시
        showCentralCard(card);

        // 사용한 카드를 즉시 화면에서 제거
        const cardStack = document.querySelector('.card-stack');
        if (cardStack) {
            cardStack.innerHTML = '';
        }

        addGameLog(`${card.name}을(를) 사용했습니다.`);
        await delay(1500);

        // 게임 UI 업데이트 (남은 카드 자동 표시)
        updateGameUI(state);

        // actionResolve 호출하여 waitForPlayerAction 해제
        if (gameState.actionResolve) {
            gameState.actionResolve();
            gameState.actionResolve = null;
        }

    } catch (error) {
        console.error('카드 사용 오류:', error);
        hideLoading();
        alert('카드를 사용할 수 없습니다.');
        gameState.isMyTurn = true;  // 재시도를 위해 다시 활성화
    }
}

// 플레이어 액션 대기
function waitForPlayerAction() {
    return new Promise((resolve) => {
        gameState.actionResolve = resolve;
    });
}

// CPU 턴 처리
async function handleCPUTurn(cpuPlayer) {
    addGameLog(`${cpuPlayer.name}의 턴입니다.`);
    await delay(2000);

    // CPU 카드 뽑기
    addGameLog(`${cpuPlayer.name}가 카드를 뽑았습니다.`);
    await delay(2000);

    try {
        // CPU 행동 가져오기
        const actionResponse = await fetch(`/game/${gameState.gameId}/cpu-turn?cpuPlayerId=${cpuPlayer.id}`);
        if (!actionResponse.ok) throw new Error('CPU 행동을 가져올 수 없습니다.');

        const action = await actionResponse.json();

        addGameLog(`${cpuPlayer.name}가 생각하는 중...`);
        await delay(2000);

        // CPU 행동 실행
        const executeResponse = await fetch(`/game/${gameState.gameId}/cpu-turn`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(action)
        });

        if (!executeResponse.ok) throw new Error('CPU 행동을 실행할 수 없습니다.');

        const state = await executeResponse.json();

        // 카드 표시
        showCentralCard(action.cardToPlay);

        let logMessage = `${cpuPlayer.name}가 ${action.cardToPlay.name}을(를) 사용했습니다.`;
        if (action.targetId) {
            const target = state.players.find(p => p.id === action.targetId);
            logMessage += ` (대상: ${target.name})`;
        }
        addGameLog(logMessage);

        if (action.reasoning) {
            addGameLog(`사유: ${action.reasoning}`);
        }

        await delay(2000);

        updateGameUI(state);

    } catch (error) {
        console.error('CPU 턴 오류:', error);
        alert('CPU 턴 처리 중 오류가 발생했습니다.');
    }
}

// 중앙 카드 표시 (카드 더미로 겹쳐서 보여주기)
function showCentralCard(card) {
    const lastPlayedCard = document.getElementById('lastPlayedCard');

    // 버린 카드 더미에 추가
    gameState.discardPile.push(card);

    // 최대 5장까지만 표시 (성능 최적화)
    const cardsToShow = gameState.discardPile.slice(-5);

    // 카드 더미 재렌더링
    lastPlayedCard.innerHTML = '';

    cardsToShow.forEach((discardedCard, index) => {
        const cardEl = document.createElement('div');
        cardEl.className = 'discard-card';

        // 카드 겹치기: 각 카드를 살짝씩 offset
        const offset = index * 3; // 3px씩 offset
        cardEl.style.left = `${offset}px`;
        cardEl.style.top = `${offset}px`;
        cardEl.style.zIndex = index;

        // 최신 카드만 애니메이션
        if (index === cardsToShow.length - 1) {
            cardEl.classList.add('show');
        }

        const img = document.createElement('img');
        img.src = `/image/${discardedCard.number}.png`;
        img.alt = discardedCard.name;
        cardEl.appendChild(img);

        lastPlayedCard.appendChild(cardEl);
    });

    // 애니메이션 클래스 제거
    setTimeout(() => {
        const cards = lastPlayedCard.querySelectorAll('.discard-card');
        cards.forEach(c => c.classList.remove('show'));
    }, 600);

    // 클릭 이벤트 추가 - 버린 카드 리스트 모달 표시
    lastPlayedCard.onclick = showDiscardList;
}

// 버린 카드 리스트 모달 표시
function showDiscardList() {
    if (gameState.discardPile.length === 0) {
        return; // 버린 카드가 없으면 모달 표시 안 함
    }

    const overlay = document.getElementById('discardListOverlay');
    const content = document.getElementById('discardListContent');

    // 버린 카드 리스트 렌더링
    content.innerHTML = '';

    gameState.discardPile.forEach((card, index) => {
        const item = document.createElement('div');
        item.className = 'discard-card-item';

        item.innerHTML = `
            <img src="/image/${card.number}.png" alt="${card.name}">
            <div class="card-name">${card.name}</div>
        `;

        content.appendChild(item);
    });

    overlay.classList.add('show');
}

// 버린 카드 리스트 모달 닫기
function closeDiscardList() {
    const overlay = document.getElementById('discardListOverlay');
    overlay.classList.remove('show');
}

// 라운드 종료 처리
async function handleRoundOver(state) {
    gameState.roundOver = true;

    addGameLog('라운드가 종료되었습니다!');
    await delay(1000);

    const overlay = document.getElementById('roundOverOverlay');
    const finalCards = document.getElementById('finalCards');
    const winnerAnnounce = document.getElementById('winnerAnnounce');

    // 모든 플레이어의 최종 카드 표시
    finalCards.innerHTML = '';
    state.players.forEach(player => {
        if (player.handCard) {
            const item = document.createElement('div');
            item.className = 'final-card-item';
            item.innerHTML = `
                <img src="/image/${player.handCard.number}.png" alt="${player.handCard.name}">
                <div class="final-card-info">
                    <div class="player-name">${player.name}</div>
                    <div class="card-name">${player.handCard.name} (${player.handCard.number})</div>
                </div>
            `;
            finalCards.appendChild(item);
        }
    });

    // 승자 발표
    const winner = state.players.find(p => p.id === state.roundWinnerId);
    let announceText = `승자: ${winner.name}`;
    if (state.roundWinReason) {
        announceText += `\n승리 사유: ${state.roundWinReason}`;
    }
    winnerAnnounce.textContent = announceText;

    overlay.classList.add('show');
}

// 다음 라운드 시작
async function startNextRound() {
    const overlay = document.getElementById('roundOverOverlay');
    overlay.classList.remove('show');

    // 중앙 카드 영역 초기화
    const lastPlayedCard = document.getElementById('lastPlayedCard');
    lastPlayedCard.innerHTML = '';
    lastPlayedCard.classList.remove('show');
    lastPlayedCard.onclick = null; // 클릭 이벤트 제거

    // 버린 카드 더미 초기화
    gameState.discardPile = [];

    showLoading('다음 라운드를 준비하는 중...');

    try {
        const response = await fetch(`/game/${gameState.gameId}/next-round`, {
            method: 'POST'
        });

        if (!response.ok) throw new Error('다음 라운드를 시작할 수 없습니다.');

        const state = await response.json();
        hideLoading();

        gameState.roundOver = false;
        gameState.selectedCardIndex = 0; // 카드 인덱스 초기화
        updateGameUI(state);

        addGameLog('새 라운드가 시작되었습니다!');
        await delay(1000);

        await startTurnCycle();

    } catch (error) {
        console.error('다음 라운드 시작 오류:', error);
        hideLoading();
        alert('다음 라운드를 시작할 수 없습니다.');
    }
}

// 게임 로그 추가
function addGameLog(message, important = false) {
    const gameLog = document.getElementById('gameLog');
    const logEntry = document.createElement('div');
    logEntry.className = 'log-entry';

    // 중요한 메시지는 강조
    const importantKeywords = ['시작', '종료', '승리', '탈락', '사용했습니다', '턴입니다'];
    if (important || importantKeywords.some(keyword => message.includes(keyword))) {
        logEntry.classList.add('important');
    }

    logEntry.textContent = message;

    gameLog.insertBefore(logEntry, gameLog.firstChild);

    // 최대 10개만 유지
    while (gameLog.children.length > 10) {
        gameLog.removeChild(gameLog.lastChild);
    }
}

// 서버 게임 로그로 업데이트 (덮어쓰기)
function updateGameLog(logs) {
    const gameLog = document.getElementById('gameLog');
    gameLog.innerHTML = '';

    // 최근 로그부터 표시 (역순)
    for (let i = logs.length - 1; i >= 0; i--) {
        const logEntry = document.createElement('div');
        logEntry.className = 'log-entry';

        const message = logs[i];
        const importantKeywords = ['시작', '종료', '승리', '탈락', '사용'];
        if (importantKeywords.some(keyword => message.includes(keyword))) {
            logEntry.classList.add('important');
        }

        logEntry.textContent = message;
        gameLog.appendChild(logEntry);
    }
}

// 로딩 표시
function showLoading(message) {
    const overlay = document.getElementById('loadingOverlay');
    const text = document.getElementById('loadingText');
    text.textContent = message;
    overlay.classList.add('show');
}

// 로딩 숨김
function hideLoading() {
    const overlay = document.getElementById('loadingOverlay');
    overlay.classList.remove('show');
}