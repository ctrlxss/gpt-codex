import pygame
import random

WIDTH, HEIGHT = 800, 400
PADDLE_WIDTH, PADDLE_HEIGHT = 10, 60
BALL_SIZE = 10

BLACK = (0, 0, 0)
WHITE = (255, 255, 255)

# Q-learning parameters
ALPHA = 0.1
GAMMA = 0.9
EPSILON = 0.1

pygame.init()
screen = pygame.display.set_mode((WIDTH, HEIGHT))
clock = pygame.time.Clock()

class Paddle:
    def __init__(self, x):
        self.rect = pygame.Rect(x, HEIGHT//2 - PADDLE_HEIGHT//2, PADDLE_WIDTH, PADDLE_HEIGHT)
        self.speed = 5

    def move(self, dy):
        self.rect.y += dy * self.speed
        self.rect.y = max(0, min(HEIGHT - PADDLE_HEIGHT, self.rect.y))

class Ball:
    def __init__(self):
        self.reset()

    def reset(self, direction=1):
        self.rect = pygame.Rect(WIDTH//2, HEIGHT//2, BALL_SIZE, BALL_SIZE)
        self.dx = direction * 4
        self.dy = random.choice([-3, -2, 2, 3])

    def update(self):
        self.rect.x += self.dx
        self.rect.y += self.dy
        if self.rect.top <= 0 or self.rect.bottom >= HEIGHT:
            self.dy *= -1

# Q-learning AI
class QPaddle(Paddle):
    def __init__(self, x):
        super().__init__(x)
        self.q = {}

    def state(self, ball):
        rel = ball.rect.centery - self.rect.centery
        if rel < -15:
            rel_state = -1
        elif rel > 15:
            rel_state = 1
        else:
            rel_state = 0
        dy_dir = -1 if ball.dy < 0 else 1
        return (rel_state, dy_dir)

    def choose_action(self, state):
        if random.random() < EPSILON or state not in self.q:
            return random.choice([-1, 0, 1])
        values = self.q[state]
        return max(range(3), key=lambda a: values[a]) - 1

    def update_q(self, state, action, reward, next_state):
        self.q.setdefault(state, [0, 0, 0])
        self.q.setdefault(next_state, [0, 0, 0])
        a = action + 1
        best_next = max(self.q[next_state])
        self.q[state][a] += ALPHA * (reward + GAMMA * best_next - self.q[state][a])

player = Paddle(20)
ai = QPaddle(WIDTH - 20 - PADDLE_WIDTH)
ball = Ball()

player_score = 0
ai_score = 0

running = True
prev_state = ai.state(ball)
prev_action = 0

while running:
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            running = False

    keys = pygame.key.get_pressed()
    move = 0
    if keys[pygame.K_w]:
        move = -1
    elif keys[pygame.K_s]:
        move = 1
    player.move(move)

    # AI chooses action and moves
    state = ai.state(ball)
    action = ai.choose_action(state)
    ai.move(action)

    ball.update()

    # collision with paddles
    reward = 0
    if ball.rect.colliderect(player.rect) and ball.dx < 0:
        ball.dx *= -1
    if ball.rect.colliderect(ai.rect) and ball.dx > 0:
        ball.dx *= -1
        reward = 1

    # check score
    if ball.rect.left <= 0:
        ai_score += 1
        reward = 1
        ball.reset(direction=1)
    elif ball.rect.right >= WIDTH:
        player_score += 1
        reward = -1
        ball.reset(direction=-1)

    # update Q-table
    next_state = ai.state(ball)
    ai.update_q(prev_state, prev_action, reward, next_state)
    prev_state, prev_action = state, action

    screen.fill(BLACK)
    pygame.draw.rect(screen, WHITE, player.rect)
    pygame.draw.rect(screen, WHITE, ai.rect)
    pygame.draw.rect(screen, WHITE, ball.rect)

    pygame.display.flip()
    clock.tick(60)

pygame.quit()
