import random
import copy
import tkinter as tk
from tkinter import messagebox

class Game2048:
    def __init__(self):
        self.board = [[0] * 4 for _ in range(4)]
        self.add_random_tile()
        self.add_random_tile()

    def add_random_tile(self):
        empty_tiles = [(i, j) for i in range(4) for j in range(4) if self.board[i][j] == 0]
        if empty_tiles:
            i, j = random.choice(empty_tiles)
            self.board[i][j] = 2 if random.random() < 0.9 else 4

    def move_left(self):
        moved = False
        for i in range(4):
            new_row = [tile for tile in self.board[i] if tile != 0]
            new_row += [0] * (4 - len(new_row))
            for j in range(3):
                if new_row[j] == new_row[j + 1] and new_row[j] != 0:
                    new_row[j] *= 2
                    new_row[j + 1] = 0
                    moved = True
            new_row = [tile for tile in new_row if tile != 0]
            new_row += [0] * (4 - len(new_row))
            if self.board[i] != new_row:
                self.board[i] = new_row
                moved = True
        return moved

    def move_right(self):
        self.board = [row[::-1] for row in self.board]
        moved = self.move_left()
        self.board = [row[::-1] for row in self.board]
        return moved

    def move_up(self):
        self.board = [list(row) for row in zip(*self.board)]
        moved = self.move_left()
        self.board = [list(row) for row in zip(*self.board)]
        return moved

    def move_down(self):
        self.board = [list(row) for row in zip(*self.board)]
        moved = self.move_right()
        self.board = [list(row) for row in zip(*self.board)]
        return moved

    def can_move(self):
        for i in range(4):
            for j in range(4):
                if self.board[i][j] == 0:
                    return True
                if i < 3 and self.board[i][j] == self.board[i + 1][j]:
                    return True
                if j < 3 and self.board[i][j] == self.board[i][j + 1]:
                    return True
        return False

class Game2048GUI:
    def __init__(self, root):
        self.game = Game2048()
        self.root = root
        self.root.title("2048 Game")
        self.board_frame = tk.Frame(self.root)
        self.board_frame.pack()
        self.tiles = [[tk.Label(self.board_frame, text="", width=4, height=2, font=("Helvetica", 24), borderwidth=2, relief="groove") for _ in range(4)] for _ in range(4)]
        for i in range(4):
            for j in range(4):
                self.tiles[i][j].grid(row=i, column=j, padx=5, pady=5)
        self.update_board()
        self.root.bind("<Key>", self.handle_keypress)

    def update_board(self):
        for i in range(4):
            for j in range(4):
                value = self.game.board[i][j]
                self.tiles[i][j].config(text=str(value) if value != 0 else "", bg=self.get_tile_color(value))
        self.root.update_idletasks()

    def get_tile_color(self, value):
        colors = {
            0: "#cdc1b4",
            2: "#eee4da",
            4: "#ede0c8",
            8: "#f2b179",
            16: "#f59563",
            32: "#f67c5f",
            64: "#f65e3b",
            128: "#edcf72",
            256: "#edcc61",
            512: "#edc850",
            1024: "#edc53f",
            2048: "#edc22e",
        }
        return colors.get(value, "#cdc1b4")

    def handle_keypress(self, event):
        previous_board = copy.deepcopy(self.game.board)
        if event.keysym == 'Up' or event.keysym == 'w':
            moved = self.game.move_up()
        elif event.keysym == 'Left' or event.keysym == 'a':
            moved = self.game.move_left()
        elif event.keysym == 'Down' or event.keysym == 's':
            moved = self.game.move_down()
        elif event.keysym == 'Right' or event.keysym == 'd':
            moved = self.game.move_right()
        else:
            return

        if self.game.board != previous_board:
            self.game.add_random_tile()
            self.update_board()
        if not self.game.can_move():
            messagebox.showinfo("Game Over", "No more moves available!")
            self.root.unbind("<Key>")

def main():
    root = tk.Tk()
    game_gui = Game2048GUI(root)
    root.mainloop()

if __name__ == "__main__":
    main()