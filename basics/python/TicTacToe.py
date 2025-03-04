import tkinter as tk
from tkinter import messagebox
import random

class TicTacToe:
    def __init__(self, root, ai=False, ai_level="easy"):
        self.root = root
        self.root.title("Tic Tac Toe")
        self.current_player = "X"
        self.board = [""] * 9
        self.buttons = []
        self.ai = ai
        self.ai_level = ai_level

        self.create_board()

    def create_board(self):
        for i in range(9):
            button = tk.Button(self.root, text="", font=("Helvetica", 20), width=5, height=2,
                               command=lambda i=i: self.on_button_click(i))
            button.grid(row=i // 3, column=i % 3)
            self.buttons.append(button)

        self.back_button = tk.Button(self.root, text="Back to Main Menu", font=("Helvetica", 14), command=self.back_to_main_menu)
        self.back_button.grid(row=3, column=0, columnspan=3, pady=10)

    def on_button_click(self, index):
        if self.board[index] == "":
            self.board[index] = self.current_player
            self.buttons[index].config(text=self.current_player)
            if self.check_winner():
                messagebox.showinfo("Game Over", f"Player {self.current_player} wins!")
                self.reset_game()
            elif "" not in self.board:
                messagebox.showinfo("Game Over", "It's a tie!")
                self.reset_game()
            else:
                self.current_player = "O" if self.current_player == "X" else "X"
                if self.ai and self.current_player == "O":
                    self.ai_move()

    def ai_move(self):
        if self.ai_level == "easy":
            available_moves = [i for i, spot in enumerate(self.board) if spot == ""]
            if available_moves:
                move = random.choice(available_moves)
                self.on_button_click(move)
        elif self.ai_level == "hard":
            move = self.minimax(self.board, self.current_player)["index"]
            self.on_button_click(move)

    def minimax(self, new_board, player):
        avail_spots = [i for i, spot in enumerate(new_board) if spot == ""]

        if self.check_winner_board(new_board, "X"):
            return {"score": -10}
        elif self.check_winner_board(new_board, "O"):
            return {"score": 10}
        elif len(avail_spots) == 0:
            return {"score": 0}

        moves = []
        for i in avail_spots:
            move = {}
            move["index"] = i
            new_board[i] = player

            if player == "O":
                result = self.minimax(new_board, "X")
                move["score"] = result["score"]
            else:
                result = self.minimax(new_board, "O")
                move["score"] = result["score"]

            new_board[i] = ""
            moves.append(move)

        best_move = None
        if player == "O":
            best_score = -10000
            for move in moves:
                if move["score"] > best_score:
                    best_score = move["score"]
                    best_move = move
        else:
            best_score = 10000
            for move in moves:
                if move["score"] < best_score:
                    best_score = move["score"]
                    best_move = move

        return best_move

    def check_winner(self):
        winning_combinations = [
            [0, 1, 2], [3, 4, 5], [6, 7, 8],  # Rows
            [0, 3, 6], [1, 4, 7], [2, 5, 8],  # Columns
            [0, 4, 8], [2, 4, 6]              # Diagonals
        ]
        for combo in winning_combinations:
            if self.board[combo[0]] == self.board[combo[1]] == self.board[combo[2]] != "":
                return True
        return False

    def check_winner_board(self, board, player):
        winning_combinations = [
            [0, 1, 2], [3, 4, 5], [6, 7, 8],  # Rows
            [0, 3, 6], [1, 4, 7], [2, 5, 8],  # Columns
            [0, 4, 8], [2, 4, 6]              # Diagonals
        ]
        for combo in winning_combinations:
            if board[combo[0]] == board[combo[1]] == board[combo[2]] == player:
                return True
        return False

    def reset_game(self):
        self.board = [""] * 9
        self.current_player = "X"
        for button in self.buttons:
            button.config(text="")

    def back_to_main_menu(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        MainMenu(self.root)

class MainMenu:
    def __init__(self, root):
        self.root = root
        self.root.title("Tic Tac Toe")
        self.create_menu()

    def create_menu(self):
        self.root.geometry("400x400")
        tk.Label(self.root, text="Tic Tac Toe", font=("Helvetica", 24)).pack(pady=20)
        tk.Button(self.root, text="Two Players", font=("Helvetica", 18), command=self.start_two_player_game).pack(pady=10)
        tk.Button(self.root, text="Play Against Easy AI", font=("Helvetica", 18), command=self.start_easy_ai_game).pack(pady=10)
        tk.Button(self.root, text="Play Against Hard AI", font=("Helvetica", 18), command=self.start_hard_ai_game).pack(pady=10)

    def start_two_player_game(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        self.root.geometry("400x400")
        TicTacToe(self.root)

    def start_easy_ai_game(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        self.root.geometry("400x400")
        TicTacToe(self.root, ai=True, ai_level="easy")

    def start_hard_ai_game(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        self.root.geometry("400x400")
        TicTacToe(self.root, ai=True, ai_level="hard")

def main():
    root = tk.Tk()
    MainMenu(root)
    root.mainloop()

if __name__ == "__main__":
    main()