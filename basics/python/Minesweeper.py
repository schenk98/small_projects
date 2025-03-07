import tkinter as tk
from tkinter import messagebox
import random

class Minesweeper:
    def __init__(self, root, size_x, size_y, mine_count):
        self.root = root
        self.size_x = size_x
        self.size_y = size_y
        self.mine_count = mine_count
        self.board_frame = tk.Frame(self.root)
        self.board_frame.pack()
        self.board = [[tk.Button(self.board_frame, text="", width=4, height=2, font=("Helvetica", 12), borderwidth=1, relief="raised", bg="lightgrey") for j in range(self.size_y)] for i in range(self.size_x)]
        self.root.title("MineSweeper")
        for i in range(self.size_y):
            for j in range(self.size_x):
                self.board[i][j].grid(row=i, column=j, padx=1, pady=1)
                self.board[i][j].bind("<Button-1>", lambda event, i=i, j=j: self.reveal_tile(i, j))
                self.board[i][j].bind("<Button-3>", lambda event, i=i, j=j: self.mark_tile(i, j))
        self.values = self.set_board(self.size_x, self.size_y, self.mine_count)
        self.back_button = tk.Button(self.root, text="Back to Main Menu", font=("Helvetica", 14), command=self.back_to_main_menu)
        self.back_button.pack(pady=10)
        self.tiles_revealed = 0
        self.marked_tiles = 0

    def reveal_tile(self, x, y):
        if self.board[x][y].cget("text") == "X" or self.board[x][y].cget("relief") == "sunken":
            return
        value = self.values[x][y]
        self.board[x][y].config(text=str(value) if value != 0 else "", fg=self.get_text_color(value), font=("Helvetica", 12, "bold"), bg="lightgrey", relief="sunken")
        if value == 10:
            messagebox.showinfo("Game Over", "You clicked on a mine! Game Over!")
            self.reset_game()
            return
        elif value == 0:
            self.reveal_surrounding_tiles(x, y)
        self.tiles_revealed += 1
        if self.tiles_revealed == self.size_x * self.size_y - self.mine_count:
            messagebox.showinfo("Congratulations", "You win!")
            self.reset_game()

    def reveal_surrounding_tiles(self, x, y):
        for i in range(max(0, x - 1), min(self.size_x, x + 2)):
            for j in range(max(0, y - 1), min(self.size_y, y + 2)):
                if self.board[i][j].cget("relief") == "raised":
                    self.reveal_tile(i, j)

    def mark_tile(self, x, y):
        if self.board[x][y].cget("relief") == "raised":
            if self.board[x][y].cget("text") == "":
                self.board[x][y].config(text="X", bg="orange")
                self.marked_tiles += 1
            else:
                self.board[x][y].config(text="", bg="lightgrey")
                self.marked_tiles -= 1
        self.check_win_condition()

    def check_win_condition(self):
        correct_marks = 0
        for i in range(self.size_x):
            for j in range(self.size_y):
                if self.board[i][j].cget("text") == "X" and self.values[i][j] == 10:
                    correct_marks += 1
                elif self.board[i][j].cget("text") == "X" and self.values[i][j] != 10:
                    return
        if correct_marks == self.mine_count and self.marked_tiles == self.mine_count:
            messagebox.showinfo("Congratulations", "You win by marking all mines correctly!")
            self.reset_game()

    def reset_game(self):
        self.tiles_revealed = 0
        self.marked_tiles = 0
        for i in range(self.size_x):
            for j in range(self.size_y):
                self.board[i][j].config(text="", bg="lightgrey", relief="raised")
        self.values = self.set_board(self.size_x, self.size_y, self.mine_count)

    def set_board(self, size_x, size_y, mine_count):
        # Initialize the board with zeros
        values = [[0 for _ in range(size_y)] for _ in range(size_x)]

        # Place mines randomly
        mines_placed = 0
        while mines_placed < mine_count:
            x = random.randint(0, size_x - 1)
            y = random.randint(0, size_y - 1)
            if values[x][y] != 10:  # 10 represents a mine
                values[x][y] = 10
                mines_placed += 1

                # Update surrounding cells
                for i in range(max(0, x - 1), min(size_x, x + 2)):
                    for j in range(max(0, y - 1), min(size_y, y + 2)):
                        if values[i][j] != 10:
                            values[i][j] += 1

        return values

    def get_text_color(self, value):
        colors = {
            1: "#0000ff",  # blue
            2: "#008000",  # green
            3: "#ff0000",  # red
            4: "#000080",  # dark blue
            5: "#800000",  # maroon
            6: "#008080",  # teal
            7: "#000000",  # black
            8: "#808080",  # grey
            10: "red"  # bomb
        }
        return colors.get(value, "black")

    def back_to_main_menu(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        MainMenu(self.root)

class MainMenu:
    def __init__(self, root):
        self.root = root
        self.root.title("Minesweeper")
        self.create_menu()

    def create_menu(self):
        self.root.geometry("500x600")
        tk.Label(self.root, text="Minesweeper", font=("Helvetica", 24)).pack(pady=20)
        tk.Button(self.root, text="Start Game (easy)", font=("Helvetica", 18), command=lambda: self.start_game("easy")).pack(pady=10)
        tk.Button(self.root, text="Start Game (medium)", font=("Helvetica", 18), command=lambda: self.start_game("medium")).pack(pady=10)
        tk.Button(self.root, text="Start Game (hard)", font=("Helvetica", 18), command=lambda: self.start_game("hard")).pack(pady=10)
        
        tk.Label(self.root, text="Custom Size X:", font=("Helvetica", 18)).pack(pady=5)
        self.customSizeX = tk.Entry(self.root, font=("Helvetica", 18))
        self.customSizeX.pack(pady=5)
        
        tk.Label(self.root, text="Custom Size Y:", font=("Helvetica", 18)).pack(pady=5)
        self.customSizeY = tk.Entry(self.root, font=("Helvetica", 18))
        self.customSizeY.pack(pady=5)
        
        tk.Label(self.root, text="Mine Count:", font=("Helvetica", 18)).pack(pady=5)
        self.mineCount = tk.Entry(self.root, font=("Helvetica", 18))
        self.mineCount.pack(pady=5)
        
        tk.Button(self.root, text="Start Game (custom)", font=("Helvetica", 18), command=lambda: self.start_game("custom")).pack(pady=10)

    def start_game(self, type):
        for widget in self.root.winfo_children():
            widget.destroy()
        self.root.geometry("500x600")
        if type == "easy":
            Minesweeper(self.root, 10, 10, 10)
        elif type == "medium":
            Minesweeper(self.root, 20, 20, 50)
        elif type == "hard":
            Minesweeper(self.root, 40, 40, 200)
        else:
            Minesweeper(self.root, int(self.customSizeX.get()), int(self.customSizeY.get()), int(self.mineCount.get()))

def main():
    root = tk.Tk()
    MainMenu(root)
    root.mainloop()

if __name__ == "__main__":
    main()