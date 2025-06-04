const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const overlay = document.getElementById('overlay');
const startBtn = document.getElementById('startBtn');
const soundToggle = document.getElementById('soundToggle');
const highScoreEl = document.getElementById('highScore');

const WIDTH = canvas.width;
const HEIGHT = canvas.height;

let upPressed = false;
let downPressed = false;
let leftPressed = false;
let rightPressed = false;
let aPressed = false;
let dPressed = false;

let gameState = 'menu'; // 'menu', 'playing', 'paused', 'gameover'
let playerScore = 0;
let cpuScore = 0;
let highScore = parseInt(localStorage.getItem('pongHighScore') || '0');

class Paddle {
  constructor(x) {
    this.width = 100;
    this.height = 15;
    this.x = x;
    this.y = HEIGHT - 40;
    this.speed = 6;
  }
  update() {
    if (aPressed || leftPressed) this.x -= this.speed;
    if (dPressed || rightPressed) this.x += this.speed;
    this.x = Math.max(0, Math.min(WIDTH - this.width, this.x));
  }
  draw() {
    ctx.fillStyle = '#0f0';
    ctx.fillRect(this.x, this.y, this.width, this.height);
  }
}

class Ball {
  constructor() {
    this.size = 10;
    this.reset();
  }
  reset() {
    this.x = WIDTH / 2 - this.size/2;
    this.y = HEIGHT / 2 - this.size/2;
    const dir = Math.random() < 0.5 ? -1 : 1;
    this.vx = 4 * dir;
    this.vy = -4;
  }
  update(player) {
    this.x += this.vx;
    this.y += this.vy;
    if (this.x <= 0 || this.x + this.size >= WIDTH) {
      this.vx *= -1;
      beep();
    }
    if (this.y <= 0) {
      this.vy *= -1;
      beep();
    }
    // Paddle collision
    if (this.y + this.size >= player.y &&
        this.x + this.size >= player.x &&
        this.x <= player.x + player.width) {
      this.vy = -Math.abs(this.vy);
      this.y = player.y - this.size;
      beep();
    }
    // Bottom out
    if (this.y > HEIGHT) {
      cpuScore += 1;
      this.reset();
    }
  }
  draw() {
    ctx.fillStyle = '#f00';
    ctx.fillRect(this.x, this.y, this.size, this.size);
  }
}

let player = new Paddle(WIDTH / 2 - 50);
let ball = new Ball();

function beep() {
  if (!soundToggle.checked) return;
  const ctx = new (window.AudioContext || window.webkitAudioContext)();
  const osc = ctx.createOscillator();
  osc.type = 'square';
  osc.frequency.setValueAtTime(400, ctx.currentTime);
  osc.connect(ctx.destination);
  osc.start();
  osc.stop(ctx.currentTime + 0.05);
}

function drawCenterText(text, y) {
  ctx.fillStyle = '#fff';
  ctx.font = '24px Arial';
  const metrics = ctx.measureText(text);
  ctx.fillText(text, WIDTH/2 - metrics.width/2, y);
}

function draw() {
  ctx.clearRect(0, 0, WIDTH, HEIGHT);
  ctx.fillStyle = '#000';
  ctx.fillRect(0, 0, WIDTH, HEIGHT);

  if (gameState === 'menu') {
    // overlay handles menu
    return;
  }

  player.draw();
  ball.draw();
  drawCenterText(`${playerScore} : ${cpuScore}`, 30);
}

function update() {
  if (gameState !== 'playing') return;
  player.update();
  ball.update(player);
  if (cpuScore >= 5) {
    gameState = 'gameover';
    overlay.classList.remove('hidden');
    startBtn.textContent = 'Restart';
    if (playerScore > highScore) {
      highScore = playerScore;
      localStorage.setItem('pongHighScore', highScore);
    }
    highScoreEl.textContent = highScore;
  }
}

function gameLoop() {
  update();
  draw();
  requestAnimationFrame(gameLoop);
}

document.addEventListener('keydown', e => {
  if (e.code === 'ArrowLeft') leftPressed = true;
  if (e.code === 'ArrowRight') rightPressed = true;
  if (e.code === 'KeyA') aPressed = true;
  if (e.code === 'KeyD') dPressed = true;
  if (e.code === 'KeyP' && gameState === 'playing') {
    gameState = 'paused';
    overlay.classList.remove('hidden');
    startBtn.textContent = 'Resume';
  }
});

document.addEventListener('keyup', e => {
  if (e.code === 'ArrowLeft') leftPressed = false;
  if (e.code === 'ArrowRight') rightPressed = false;
  if (e.code === 'KeyA') aPressed = false;
  if (e.code === 'KeyD') dPressed = false;
});

startBtn.onclick = () => {
  if (gameState === 'menu' || gameState === 'paused' || gameState === 'gameover') {
    overlay.classList.add('hidden');
    playerScore = 0;
    cpuScore = 0;
    player = new Paddle(WIDTH / 2 - 50);
    ball = new Ball();
    gameState = 'playing';
  }
};

window.onload = () => {
  highScoreEl.textContent = highScore;
  overlay.classList.remove('hidden');
  gameLoop();
};
