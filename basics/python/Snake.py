import tkinter as tk
import random

class SnakeGame:
    def __init__(self, root):
        self.root = root
        self.root.title("Snake Game")
        self.canvas = tk.Canvas(root, width=400, height=400, bg="black")
        self.canvas.pack()
        self.snake = [(20, 40), (20, 30), (20, 20)]
        self.snake_direction = "Down"
        self.next_direction = self.snake_direction
        self.food_position = self.set_new_food_position()
        self.score = 0
        self.game_over = False
        self.paused = False

        self.root.bind("<KeyPress>", self.change_direction)
        self.run_game()

    def run_game(self):
        if not self.game_over and not self.paused:
            self.update_snake()
            self.check_collisions()
            self.check_food_collision()
            self.draw_elements()
            self.root.after(100, self.run_game)
        elif self.game_over:
            self.canvas.create_text(200, 200, text="Game Over", fill="red", font=("Arial", 24))
        else:
            self.root.after(100, self.run_game)

    def update_snake(self):
        self.snake_direction = self.next_direction
        head_x, head_y = self.snake[0]

        if self.snake_direction == "Left":
            new_head = (head_x - 10, head_y)
        elif self.snake_direction == "Right":
            new_head = (head_x + 10, head_y)
        elif self.snake_direction == "Up":
            new_head = (head_x, head_y - 10)
        elif self.snake_direction == "Down":
            new_head = (head_x, head_y + 10)

        self.snake = [new_head] + self.snake[:-1]

    def change_direction(self, event):
        new_direction = event.keysym
        all_directions = ["Left", "Right", "Up", "Down"]
        opposites = [{"Left", "Right"}, {"Up", "Down"}]

        if new_direction in ["Escape", "space"]:
            if self.game_over:
                self.reset_game()
            else:
                self.paused = not self.paused
                print(f"Game {'paused' if self.paused else 'resumed'}")
                if not self.paused:
                    self.run_game()
        elif (new_direction in all_directions and
              {new_direction, self.snake_direction} not in opposites):
            self.next_direction = new_direction
            print(f"Direction changed to {self.next_direction}")

    def check_collisions(self):
        head_x, head_y = self.snake[0]

        if head_x < 0 or head_x >= 400 or head_y < 0 or head_y >= 400:
            self.game_over = True
            print("Collision with wall")

        if len(self.snake) != len(set(self.snake)):
            self.game_over = True
            print("Collision with self")

    def check_food_collision(self):
        if self.snake[0] == self.food_position:
            self.snake.append(self.snake[-1])
            self.food_position = self.set_new_food_position()
            self.score += 1
            print(f"Food eaten, score: {self.score}")

    def set_new_food_position(self):
        while True:
            x_position = random.randint(0, 39) * 10
            y_position = random.randint(0, 39) * 10
            food_position = (x_position, y_position)
            if food_position not in self.snake:
                return food_position

    def draw_elements(self):
        self.canvas.delete(tk.ALL)
        self.canvas.create_rectangle(0, 0, 400, 400, fill="black")
        for x_position, y_position in self.snake:
            self.canvas.create_rectangle(x_position, y_position, x_position + 10, y_position + 10, fill="green")
        food_x, food_y = self.food_position
        self.canvas.create_rectangle(food_x, food_y, food_x + 10, food_y + 10, fill="red")
        self.canvas.create_text(50, 10, text=f"Score: {self.score}", fill="white", font=("Arial", 12))

    def reset_game(self):
        self.snake = [(20, 40), (20, 30), (20, 20)]
        self.snake_direction = "Down"
        self.next_direction = self.snake_direction
        self.food_position = self.set_new_food_position()
        self.score = 0
        self.game_over = False
        self.paused = False
        self.run_game()

if __name__ == "__main__":
    root = tk.Tk()
    game = SnakeGame(root)
    root.mainloop()