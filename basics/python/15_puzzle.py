import tkinter as tk
from tkinter import messagebox
import random
import heapq
import time

class Game15Puzzle:
    def __init__(self):
        self.board = self.create_board()
        self.shuffle_board()

    def create_board(self):
        return [[4 * i + j + 1 for j in range(4)] for i in range(4)]

    def shuffle_board(self):
        flat_board = [tile for row in self.board for tile in row]
        flat_board[-1] = 0  # Ensure the last tile is the empty space
        random.shuffle(flat_board)
        while not self.is_solvable(flat_board) or self.is_solved(flat_board):
            random.shuffle(flat_board)
        self.board = [flat_board[i * 4:(i + 1) * 4] for i in range(4)]

    def is_solvable(self, flat_board):
        inversions = 0
        for i in range(len(flat_board)):
            for j in range(i + 1, len(flat_board)):
                if flat_board[i] > flat_board[j] != 0:
                    inversions += 1
        empty_row = 3 - (flat_board.index(0) // 4)
        return (inversions + empty_row) % 2 == 0

    def is_solved(self, flat_board=None):
        if flat_board is None:
            flat_board = [tile for row in self.board for tile in row]
        return flat_board == list(range(1, 16)) + [0]

    def find_empty(self):
        for i in range(4):
            for j in range(4):
                if self.board[i][j] == 0:
                    return i, j

    def move_tile(self, i, j):
        empty_i, empty_j = self.find_empty()
        if (abs(empty_i - i) == 1 and empty_j == j) or (abs(empty_j - j) == 1 and empty_i == i):
            self.board[empty_i][empty_j], self.board[i][j] = self.board[i][j], self.board[empty_i][empty_j]
            return True
        return False

    def move_up(self):
        empty_i, empty_j = self.find_empty()
        if empty_i < 3:
            self.move_tile(empty_i + 1, empty_j)

    def move_down(self):
        empty_i, empty_j = self.find_empty()
        if empty_i > 0:
            self.move_tile(empty_i - 1, empty_j)

    def move_left(self):
        empty_i, empty_j = self.find_empty()
        if empty_j < 3:
            self.move_tile(empty_i, empty_j + 1)

    def move_right(self):
        empty_i, empty_j = self.find_empty()
        if empty_j > 0:
            self.move_tile(empty_i, empty_j - 1)

    def get_solution(self, timeout=10):
        def heuristic(board):
            """Heuristic function for A* algorithm: sum of Manhattan distances of tiles from their goal positions."""
            distance = 0
            for i in range(4):
                for j in range(4):
                    value = board[i][j]
                    if value != 0:
                        target_x = (value - 1) // 4
                        target_y = (value - 1) % 4
                        distance += abs(i - target_x) + abs(j - target_y)
            return distance

        def get_neighbors(board):
            """Generate all possible moves from the current board state."""
            neighbors = []
            empty_i, empty_j = [(i, j) for i in range(4) for j in range(4) if board[i][j] == 0][0]
            directions = [(-1, 0, 'Up'), (1, 0, 'Down'), (0, -1, 'Left'), (0, 1, 'Right')]
            for di, dj, direction in directions:
                new_i, new_j = empty_i + di, empty_j + dj
                if 0 <= new_i < 4 and 0 <= new_j < 4:
                    new_board = [row[:] for row in board]
                    new_board[empty_i][empty_j], new_board[new_i][new_j] = new_board[new_i][new_j], new_board[empty_i][empty_j]
                    neighbors.append((new_board, (new_i, new_j), direction))
            return neighbors

        start_time = time.time()
        start_board = [row[:] for row in self.board]
        start_empty = self.find_empty()
        goal_board = [[4 * i + j + 1 for j in range(4)] for i in range(4)]
        goal_board[3][3] = 0

        open_set = []
        heapq.heappush(open_set, (heuristic(start_board), 0, start_board, start_empty, []))
        closed_set = set()

        while open_set:
            if time.time() - start_time > timeout:
                print("Timeout reached")
                return []

            _, cost, current_board, current_empty, path = heapq.heappop(open_set)
            if current_board == goal_board:
                return path

            closed_set.add(tuple(tuple(row) for row in current_board))

            for neighbor, new_empty, direction in get_neighbors(current_board):
                if tuple(tuple(row) for row in neighbor) in closed_set:
                    continue
                new_path = path + [(new_empty, direction)]
                heapq.heappush(open_set, (cost + 1 + heuristic(neighbor), cost + 1, neighbor, new_empty, new_path))

        return []

class Game15PuzzleGUI:
    def __init__(self, root):
        self.game = Game15Puzzle()
        self.root = root
        self.root.title("15 Puzzle Game")
        self.board_frame = tk.Frame(self.root)
        self.board_frame.pack()
        self.tiles = [[tk.Button(self.board_frame, text="", width=4, height=2, font=("Helvetica", 24), command=lambda i=i, j=j: self.handle_tile_click(i, j)) for j in range(4)] for i in range(4)]
        for i in range(4):
            for j in range(4):
                self.tiles[i][j].grid(row=i, column=j, padx=5, pady=5)
        self.update_board()

        # Add a button to the GUI
        self.message_button = tk.Button(self.root, text="Show Solution", command=self.show_solution)
        self.message_button.pack(pady=10)

        self.root.bind("<Key>", self.handle_keypress)

    def update_board(self):
        for i in range(4):
            for j in range(4):
                value = self.game.board[i][j]
                self.tiles[i][j].config(text=str(value) if value != 0 else "", bg=self.get_tile_color(value))
        self.root.update_idletasks()

    def get_tile_color(self, value):
        return "#cdc1b4" if value == 0 else "#eee4da"

    def handle_tile_click(self, i, j):
        if self.game.move_tile(i, j):
            self.update_board()
            if self.game.is_solved():
                messagebox.showinfo("Congratulations!", "You solved the puzzle!")
                self.root.unbind("<Key>")

    def handle_keypress(self, event):
        if event.keysym in ['Up', 'w']:
            self.game.move_up()
        elif event.keysym in ['Down', 's']:
            self.game.move_down()
        elif event.keysym in ['Left', 'a']:
            self.game.move_left()
        elif event.keysym in ['Right', 'd']:
            self.game.move_right()
        self.update_board()
        if self.game.is_solved():
            messagebox.showinfo("Congratulations!", "You solved the puzzle!")
            self.root.unbind("<Key>")

    def show_solution(self):
        solution = self.game.get_solution()
        if not solution:
            messagebox.showinfo("Solution", "No solution found or timeout reached!")
        else:
            solution_str = "\n".join([f"Move empty space to {move[0]} ({move[1]})" for move in solution])
            messagebox.showinfo("Solution", solution_str)
            print(solution_str)

def main():
    root = tk.Tk()
    game_gui = Game15PuzzleGUI(root)
    root.mainloop()

if __name__ == "__main__":
    main()