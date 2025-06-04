import os, sys
sys.path.append(os.path.dirname(__file__))
import pygame
import random
from augments import AUGMENT_POOL, GameState

# Basic colors
BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
BLUE = (50, 50, 255)
RED = (255, 50, 50)
GREEN = (50, 255, 50)

SCREEN_WIDTH = 800
SCREEN_HEIGHT = 600

BLOCK_ROWS = 5
BLOCK_COLS = 8
BLOCK_WIDTH = 80
BLOCK_HEIGHT = 30

pygame.init()
font = pygame.font.SysFont(None, 24)
screen = pygame.display.set_mode((SCREEN_WIDTH, SCREEN_HEIGHT))
pygame.display.set_caption("Breakout XP")
clock = pygame.time.Clock()

class Paddle:
    def __init__(self, gs: GameState):
        self.gs = gs
        self.rect = pygame.Rect(0, 0, gs.paddle_width, 15)
        self.rect.midbottom = (SCREEN_WIDTH // 2, SCREEN_HEIGHT - 30)

    def update(self):
        self.rect.width = self.gs.paddle_width
        mouse_x = pygame.mouse.get_pos()[0]
        self.rect.centerx = mouse_x
        if self.rect.left < 0:
            self.rect.left = 0
        if self.rect.right > SCREEN_WIDTH:
            self.rect.right = SCREEN_WIDTH

    def draw(self, surf):
        pygame.draw.rect(surf, BLUE, self.rect)

class Ball:
    def __init__(self, gs: GameState):
        self.gs = gs
        self.rect = pygame.Rect(0, 0, 10, 10)
        self.rect.center = (SCREEN_WIDTH // 2, SCREEN_HEIGHT // 2)
        self.vel = [gs.ball_speed, -gs.ball_speed]

    def update(self):
        self.rect.x += self.vel[0]
        self.rect.y += self.vel[1]
        if self.rect.left <= 0 or self.rect.right >= SCREEN_WIDTH:
            self.vel[0] = -self.vel[0]
        if self.rect.top <= 0:
            self.vel[1] = -self.vel[1]

    def draw(self, surf):
        pygame.draw.rect(surf, RED, self.rect)

class Block:
    def __init__(self, x, y):
        self.rect = pygame.Rect(x, y, BLOCK_WIDTH, BLOCK_HEIGHT)

    def draw(self, surf):
        pygame.draw.rect(surf, GREEN, self.rect)


def create_blocks(level):
    blocks = []
    for row in range(BLOCK_ROWS + level):
        for col in range(BLOCK_COLS):
            x = col * (BLOCK_WIDTH + 2) + 60
            y = row * (BLOCK_HEIGHT + 2) + 40
            blocks.append(Block(x, y))
    return blocks


def show_text(text, y):
    img = font.render(text, True, WHITE)
    rect = img.get_rect(center=(SCREEN_WIDTH // 2, y))
    screen.blit(img, rect)


def choose_augment(gs):
    options = random.sample(AUGMENT_POOL, 3)
    choosing = True
    while choosing:
        screen.fill(BLACK)
        show_text("Level up! Choose an augment:", 150)
        for i, aug in enumerate(options):
            txt = f"{i+1}. {aug['name']} - {aug['description']}"
            img = font.render(txt, True, WHITE)
            screen.blit(img, (60, 200 + i * 40))
        pygame.display.flip()
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                pygame.quit(); exit()
            if event.type == pygame.KEYDOWN:
                if event.key in [pygame.K_1, pygame.K_2, pygame.K_3]:
                    idx = [pygame.K_1, pygame.K_2, pygame.K_3].index(event.key)
                    chosen = options[idx]
                    chosen['apply'](gs)
                    choosing = False
    screen.fill(BLACK)


def main():
    gs = GameState()
    paddle = Paddle(gs)
    ball = Ball(gs)
    blocks = create_blocks(gs.extra_lives)
    xp = 0
    level = 1
    xp_to_level = 5
    running = True
    while running:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
        paddle.update()
        ball.update()
        # collision with paddle
        if ball.rect.colliderect(paddle.rect) and ball.vel[1] > 0:
            ball.vel[1] = -ball.vel[1]
        # collision with blocks
        for block in blocks[:]:
            if ball.rect.colliderect(block.rect):
                ball.vel[1] = -ball.vel[1]
                blocks.remove(block)
                xp += 1 + gs.xp_gain_bonus
                break
        # check bottom
        if ball.rect.top > SCREEN_HEIGHT:
            if gs.extra_lives > 0:
                gs.extra_lives -= 1
                ball.rect.center = (SCREEN_WIDTH // 2, SCREEN_HEIGHT // 2)
                ball.vel = [gs.ball_speed, -gs.ball_speed]
            else:
                running = False
        if not blocks:
            level += 1
            blocks = create_blocks(level)
        if xp >= xp_to_level:
            xp -= xp_to_level
            xp_to_level += 5
            choose_augment(gs)
        ball.vel[0] = max(min(ball.vel[0], 10), -10)
        ball.vel[1] = max(min(ball.vel[1], 10), -10)
        screen.fill(BLACK)
        paddle.draw(screen)
        ball.draw(screen)
        for block in blocks:
            block.draw(screen)
        show_text(f"Level: {level} XP: {int(xp)}/{xp_to_level}", 20)
        show_text(f"Lives: {gs.extra_lives+1}", 40)
        pygame.display.flip()
        clock.tick(60)
    pygame.quit()

if __name__ == "__main__":
    main()
