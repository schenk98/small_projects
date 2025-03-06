import tkinter as tk
from tkinter import messagebox
import random
import time

class Connect4:
    def __init__(self, root, ai=False, ai_level="easy"):
        self.root = root
        self.root.title("Connect 4")
        self.current_player = "Red"
        self.board = [[""] * 7 for _ in range(6)]
        self.buttons = []
        self.ai = ai
        self.ai_level = ai_level
        self.create_board()

    def create_board(self):
        for col in range(7):
            button = tk.Button(self.root, text="Drop", font=("Helvetica", 14), command=lambda col=col: self.drop_token(col))
            button.grid(row=0, column=col)
            self.buttons.append(button)

        self.cells = []
        for row in range(1, 7):
            row_cells = []
            for col in range(7):
                cell = tk.Label(self.root, text="", font=("Helvetica", 20), width=5, height=2, borderwidth=2, relief="groove")
                cell.grid(row=row, column=col)
                row_cells.append(cell)
            self.cells.append(row_cells)

        self.back_button = tk.Button(self.root, text="Back to Main Menu", font=("Helvetica", 14), command=self.back_to_main_menu)
        self.back_button.grid(row=7, column=0, columnspan=7, pady=10)

    def drop_token(self, col):
        for row in range(5, -1, -1):
            if self.board[row][col] == "":
                self.board[row][col] = self.current_player
                self.cells[row][col].config(bg=self.current_player)
                self.print_board()  # Debugging výpis
                if self.check_winner(row, col):
                    messagebox.showinfo("Game Over", f"Player {self.current_player} wins!")
                    self.reset_game()
                elif all(self.board[r][c] != "" for r in range(6) for c in range(7)):
                    messagebox.showinfo("Game Over", "It's a tie!")
                    self.reset_game()
                else:
                    self.current_player = "Yellow" if self.current_player == "Red" else "Red"
                    if self.ai and self.current_player == "Yellow":
                        self.disable_buttons()
                        self.root.after(500, self.ai_move)  # Delay AI move slightly for better UX
                return

    def ai_move(self):
        start_time = time.time()
        if self.ai_level == "easy":
            available_moves = [col for col in range(7) if self.board[0][col] == ""]
            if available_moves:
                move = random.choice(available_moves)
                self.drop_token(move)
        elif self.ai_level == "hard":
            move = self.alpha_beta(self.board, "Yellow", depth=4, alpha=float('-inf'), beta=float('inf'), start_time=start_time, time_limit=2)["column"]
            self.drop_token(move)
        self.enable_buttons()

    def alpha_beta(self, board, player, depth, alpha, beta, start_time, time_limit):
        opponent = "Red" if player == "Yellow" else "Yellow"
        available_moves = [col for col in range(7) if board[0][col] == ""]

        if self.check_winner_board(board, "Red"):
            return {"score": -10}
        elif self.check_winner_board(board, "Yellow"):
            return {"score": 10}
        elif not available_moves or depth == 0 or time.time() - start_time > time_limit:
            return {"score": 0}

        if player == "Yellow":
            max_eval = float('-inf')
            best_move = None
            for col in available_moves:
                row = next(r for r in range(5, -1, -1) if board[r][col] == "")
                board[row][col] = player
                eval = self.alpha_beta(board, opponent, depth - 1, alpha, beta, start_time, time_limit)["score"]
                board[row][col] = ""
                if eval > max_eval:
                    max_eval = eval
                    best_move = col
                alpha = max(alpha, eval)
                if beta <= alpha:
                    break
            return {"column": best_move, "score": max_eval}
        else:
            min_eval = float('inf')
            best_move = None
            for col in available_moves:
                row = next(r for r in range(5, -1, -1) if board[r][col] == "")
                board[row][col] = player
                eval = self.alpha_beta(board, opponent, depth - 1, alpha, beta, start_time, time_limit)["score"]
                board[row][col] = ""
                if eval < min_eval:
                    min_eval = eval
                    best_move = col
                beta = min(beta, eval)
                if beta <= alpha:
                    break
            return {"column": best_move, "score": min_eval}

    def check_winner(self, row, col):
        directions = [(1, 0), (0, 1), (1, 1), (1, -1)]
        for dr, dc in directions:
            count = 1
            for i in range(1, 4):
                r, c = row + dr * i, col + dc * i
                if 0 <= r < 6 and 0 <= c < 7 and self.board[r][c] == self.current_player:
                    count += 1
                else:
                    break
            for i in range(1, 4):
                r, c = row - dr * i, col - dc * i
                if 0 <= r < 6 and 0 <= c < 7 and self.board[r][c] == self.current_player:
                    count += 1
                else:
                    break
            if count >= 4:
                return True
        return False

    def check_winner_board(self, board, player):
        directions = [(1, 0), (0, 1), (1, 1), (1, -1)]
        for row in range(6):
            for col in range(7):
                if board[row][col] == player:
                    for dr, dc in directions:
                        count = 1
                        for i in range(1, 4):
                            r, c = row + dr * i, col + dc * i
                            if 0 <= r < 6 and 0 <= c < 7 and board[r][c] == player:
                                count += 1
                            else:
                                break
                        for i in range(1, 4):
                            r, c = row - dr * i, col - dc * i
                            if 0 <= r < 6 and 0 <= c < 7 and board[r][c] == player:
                                count += 1
                            else:
                                break
                        if count >= 4:
                            return True
        return False

    def reset_game(self):
        self.board = [[""] * 7 for _ in range(6)]
        self.current_player = "Red"
        for row in self.cells:
            for cell in row:
                cell.config(bg="SystemButtonFace")
        self.enable_buttons()

    def disable_buttons(self):
        for button in self.buttons:
            button.config(state=tk.DISABLED)

    def enable_buttons(self):
        for button in self.buttons:
            button.config(state=tk.NORMAL)

    def back_to_main_menu(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        MainMenu(self.root)

    def print_board(self):
        def format_cell(cell):
            if cell == "Red":
                return "R"
            elif cell == "Yellow":
                return "Y"
            else:
                return "-"

        formatted_board = "\n".join(["| " + " | ".join(format_cell(cell) for cell in row) + " |" for row in self.board])
        print(formatted_board)
        print()

class MainMenu:
    def __init__(self, root):
        self.root = root
        self.root.title("Connect 4")
        self.create_menu()

    def create_menu(self):
        self.root.geometry("700x600")
        tk.Label(self.root, text="Connect 4", font=("Helvetica", 24)).pack(pady=20)
        tk.Button(self.root, text="Two Players", font=("Helvetica", 18), command=self.start_two_player_game).pack(pady=10)
        tk.Button(self.root, text="Play Against Easy AI", font=("Helvetica", 18), command=self.start_easy_ai_game).pack(pady=10)
        tk.Button(self.root, text="Play Against Hard AI", font=("Helvetica", 18), command=self.start_hard_ai_game).pack(pady=10)

    def start_two_player_game(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        self.root.geometry("700x600")
        Connect4(self.root)

    def start_easy_ai_game(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        self.root.geometry("700x600")
        Connect4(self.root, ai=True, ai_level="easy")

    def start_hard_ai_game(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        self.root.geometry("700x600")
        Connect4(self.root, ai=True, ai_level="hard")

def main():
    root = tk.Tk()
    MainMenu(root)
    root.mainloop()

if __name__ == "__main__":
    main()