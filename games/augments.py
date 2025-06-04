import random

# Simple augment pool with 100 augments. Each augment has a name,
# description and a function that applies its effect to the game state.

class GameState:
    def __init__(self):
        self.paddle_width = 80
        self.ball_speed = 5
        self.xp_gain_bonus = 0
        self.extra_lives = 0


def wider_paddle(gs, amount=20):
    gs.paddle_width += amount

def faster_ball(gs, amount=1):
    gs.ball_speed += amount

def bonus_xp(gs, amount=1):
    gs.xp_gain_bonus += amount

def extra_life(gs, amount=1):
    gs.extra_lives += amount

# Define a set of effect functions to randomly choose from
EFFECTS = [
    lambda gs: wider_paddle(gs, 10),
    lambda gs: wider_paddle(gs, 20),
    lambda gs: faster_ball(gs, 1),
    lambda gs: faster_ball(gs, 2),
    lambda gs: bonus_xp(gs, 1),
    lambda gs: bonus_xp(gs, 2),
    lambda gs: extra_life(gs, 1),
]

AUGMENT_POOL = []
for i in range(100):
    effect = random.choice(EFFECTS)
    name = f"Augment {i+1}"
    description = [
        "Stretch your paddle", "Speed up the ball",
        "Earn more XP", "Get an extra life",
    ][i % 4]
    AUGMENT_POOL.append({
        "name": name,
        "description": description,
        "apply": effect,
    })
