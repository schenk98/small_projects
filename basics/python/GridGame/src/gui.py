import tkinter as tk
from tkinter import ttk, messagebox, colorchooser
from player import Player
from random_moving_computer import RandomMoveAlgorithm
from user_actions import UserActions
from grid import Grid
import random
import threading
import time

class GridGameApp:
    def __init__(self, root: tk.Tk):
        self.root = root
        self.root.title("Grid Game")

        # Default settings
        self.grid_size = (10, 10)
        self.players = []
        self.grid = None
        self.current_player_index = 0
        self.algorithms = {
            "Random Move": RandomMoveAlgorithm(),
            "User Actions": UserActions()
        }

        # Initialize the paused state
        self.is_paused = False

        # GUI setup
        self.setup_gui()

    def setup_gui(self):
        """Setup the GUI layout."""
        self.header_frame = ttk.Frame(self.root, padding="10")
        self.header_frame.grid(row=0, column=0, sticky="nsew")

        self.grid_frame = ttk.Frame(self.root)
        self.grid_frame.grid(row=1, column=0, sticky="nsew")

        self.root.rowconfigure(1, weight=1)
        self.root.columnconfigure(0, weight=1)

        # Grid size configuration
        ttk.Label(self.header_frame, text="Grid Width:").grid(row=0, column=0, sticky="w")
        self.grid_width_entry = ttk.Entry(self.header_frame, width=10)
        self.grid_width_entry.grid(row=0, column=1)
        self.grid_width_entry.insert(0, "10")

        ttk.Label(self.header_frame, text="Grid Height:").grid(row=1, column=0, sticky="w")
        self.grid_height_entry = ttk.Entry(self.header_frame, width=10)
        self.grid_height_entry.grid(row=1, column=1)
        self.grid_height_entry.insert(0, "10")

        # Initial position configuration
        ttk.Label(self.header_frame, text="Initial Positions:").grid(row=2, column=0, sticky="w")
        self.initial_position_var = tk.StringVar(value="Corners")
        self.initial_position_menu = ttk.Combobox(self.header_frame, textvariable=self.initial_position_var, values=["Corners", "Random", "Middle"], state="readonly")
        self.initial_position_menu.grid(row=2, column=1, sticky="w")

        # Player configuration
        ttk.Label(self.header_frame, text="Players:").grid(row=3, column=0, sticky="w")
        self.add_player_button = ttk.Button(self.header_frame, text="Add Player", command=self.add_player)
        self.add_player_button.grid(row=3, column=1, sticky="w")

        self.players_frame = ttk.Frame(self.header_frame)
        self.players_frame.grid(row=4, column=0, columnspan=2, sticky="nsew")

        # Delay configuration
        ttk.Label(self.header_frame, text="Move Delay (ms):").grid(row=5, column=0, sticky="w")
        self.move_delay_entry = ttk.Entry(self.header_frame, width=10)
        self.move_delay_entry.grid(row=5, column=1)
        self.move_delay_entry.insert(0, "100")

        # Control buttons
        self.start_button = ttk.Button(self.header_frame, text="Start Game", command=self.start_game)
        self.start_button.grid(row=6, column=0, pady=10)

        self.pause_button = ttk.Button(self.header_frame, text="Pause Game", command=self.toggle_pause, state="disabled")
        self.pause_button.grid(row=6, column=1, pady=10)

        self.stop_button = ttk.Button(self.header_frame, text="Stop Game", command=self.stop_game, state="disabled")
        self.stop_button.grid(row=7, column=0, columnspan=2, pady=10)

        # Cell counters
        self.cell_counters = {}
        self.cell_counter_labels = {}
        for i in range(4):
            self.cell_counters[i + 1] = 0
            label = ttk.Label(self.header_frame, text=f"Player {i + 1}: 0 cells")
            label.grid(row=8 + i, column=0, columnspan=2, sticky="w")
            self.cell_counter_labels[i + 1] = label

        self.add_default_players()

    def add_default_players(self):
        """Add default players to the game."""
        for i in range(2):
            self.add_player()

    def add_player(self):
        """Add a new player to the game."""
        player_id = len(self.players) + 1
        default_color = ["red", "blue", "green", "yellow"][player_id % 4]
        player = Player(player_id, default_color, "Random Move")
        self.players.append(player)

        player_frame = ttk.Frame(self.players_frame)
        player_frame.pack(fill="x", pady=2)

        ttk.Label(player_frame, text=f"Player {player_id}:").pack(side="left")

        color_button = ttk.Button(player_frame, text="Pick Color", command=lambda: self.pick_color(player))
        color_button.pack(side="left", padx=5)

        algorithm_menu = ttk.Combobox(player_frame, values=list(self.algorithms.keys()), state="readonly")
        algorithm_menu.set(player.algorithm)
        algorithm_menu.pack(side="left", padx=5)
        player.algorithm_menu = algorithm_menu

    def pick_color(self, player: Player):
        """Pick a color for the player."""
        color_code = colorchooser.askcolor(title=f"Pick Color for Player {player.id}")[1]
        if color_code:
            player.color = color_code

    def start_game(self):
        """Start the game."""
        print("[DEBUG] Start button pressed.")
        try:
            width = int(self.grid_width_entry.get())
            height = int(self.grid_height_entry.get())
            self.grid_size = (width, height)

            max_canvas_size = 600  # Target maximum size in pixels
            # Find the largest dimension so it fits on screen perfectly square
            max_cells = max(width, height)
            cell_size = max_canvas_size // max_cells
            # Keep the squares between 5px and 50px so it doesn't get ridiculous
            cell_size = max(5, min(cell_size, 50))

            # Reset the grid
            if self.grid:
                self.grid_frame.destroy()
                self.grid_frame = ttk.Frame(self.root)
                self.grid_frame.grid(row=1, column=0, sticky="nsew")

            # Pass the new dynamic cell size here
            self.grid = Grid(self.grid_size, cell_size)
            self.grid.setup_canvas(self.grid_frame)
            self.initialize_player_positions()

            # Reset counters
            self.cell_counters = {player.id: 0 for player in self.players}

            self.pause_button.config(state="normal")
            self.stop_button.config(state="normal")

            print("[DEBUG] Game started with grid size:", self.grid_size)

            self.game_running = True
            self.bind_user_input()  
            
            # Start the game loop directly (NO THREADING)
            self.play_turn()

        except ValueError:
            print("[DEBUG] Invalid grid size input.")

    def toggle_pause(self):
        """Pause or resume the game."""
        if self.is_paused:
            self.is_paused = False
            self.pause_button.config(text="Pause Game")
            self.play_turn()  # Kickstart the loop again
        else:
            self.is_paused = True
            self.pause_button.config(text="Resume Game")
            # Cancel any pending loop events so the game actually stops
            if hasattr(self, 'turn_after_id') and self.turn_after_id:
                self.root.after_cancel(self.turn_after_id)
                self.turn_after_id = None

    def stop_game(self):
        """Stop the game and reset the state."""
        self.log_with_timestamp("Stop button pressed.")
        self.game_running = False
        if hasattr(self, 'turn_after_id') and self.turn_after_id:
            self.root.after_cancel(self.turn_after_id)
            self.turn_after_id = None
        self.pause_button.config(state="disabled")
        self.stop_button.config(state="disabled")
        self.start_button.config(state="normal")
        self.log_with_timestamp("Game stopped.")

    def initialize_player_positions(self):
        """Set initial positions for all players based on the selected option."""
        starting_positions = self.get_starting_positions()
        for player, position in zip(self.players, starting_positions):
            self.grid.place_player(player, position)
            self.update_scores()  # Ensure starting cells are counted

    def get_starting_positions(self):
        """Determine starting positions based on user selection."""
        option = self.initial_position_var.get()
        positions = []

        if option == "Corners":
            positions = [(0, 0), (0, self.grid_size[0] - 1), (self.grid_size[1] - 1, 0), (self.grid_size[1] - 1, self.grid_size[0] - 1)]
        elif option == "Random":
            while len(positions) < len(self.players):
                row = random.randint(0, self.grid_size[1] - 1)
                col = random.randint(0, self.grid_size[0] - 1)
                if (row, col) not in positions:
                    positions.append((row, col))
        elif option == "Middle":
            center_row, center_col = self.grid_size[1] // 2, self.grid_size[0] // 2
            for i in range(len(self.players)):
                positions.append((center_row + i, center_col + i))

        return positions[:len(self.players)]

    def bind_user_input(self):
        """Bind user input events to the game."""
        self.root.bind("<KeyPress>", self.handle_keypress)

    def handle_keypress(self, event):
        """Handle keypress events."""
        print(f"[DEBUG] Key pressed: {event.keysym}")
        current_player = self.players[self.current_player_index]
        algorithm_name = current_player.algorithm_menu.get()
        algorithm = self.algorithms[algorithm_name]

        if isinstance(algorithm, UserActions):
            direction_map = {
                "Up": (-1, 0),
                "Down": (1, 0),
                "Left": (0, -1),
                "Right": (0, 1),
            }
            if event.keysym in direction_map:
                algorithm.set_next_move(direction_map[event.keysym])

    def is_game_over(self):
        """Check if all cells are colored."""
        total_cells = self.grid_size[0] * self.grid_size[1]
        colored_cells = sum(self.cell_counters.values())
        return colored_cells == total_cells

    def log_with_timestamp(self, message):
        """Log a message with a timestamp."""
        print(f"[{time.strftime('%H:%M:%S')}] {message}")

    def play_turn(self):
        """Play a single turn."""
        if not self.game_running or self.is_paused:
            return

        if self.is_game_over():
            self.end_game()
            return

        current_player = self.players[self.current_player_index]
        algorithm_name = current_player.algorithm_menu.get()
        algorithm = self.algorithms[algorithm_name]

        if isinstance(algorithm, UserActions):
            # Hand over control to the user check
            self.check_user_action()
        else:
            # Computer turn
            new_position = algorithm.get_next_move(self.grid.get_state(), current_player.position)
            if self.is_valid_move(current_player, new_position):
                self.grid.move_player(current_player, new_position)
                self.update_scores()
            
            self.advance_turn()
            
            # Schedule the next turn based on the delay setting
            try:
                delay = int(self.move_delay_entry.get())
            except ValueError:
                delay = 100
            self.turn_after_id = self.root.after(delay, self.play_turn)

    def check_user_action(self):
        """Check if the user has made a move."""
        if not self.game_running or self.is_paused:
            return

        current_player = self.players[self.current_player_index]
        algorithm_name = current_player.algorithm_menu.get()
        algorithm = self.algorithms[algorithm_name]

        if algorithm.next_move is not None:
            # User has pressed a key
            new_position = algorithm.get_next_move(self.grid.get_state(), current_player.position)
            
            if new_position != current_player.position and self.is_valid_move(current_player, new_position):
                self.grid.move_player(current_player, new_position)
                self.update_scores()
                self.advance_turn()
                
                # Move was valid, proceed to the next player's turn
                try:
                    delay = int(self.move_delay_entry.get())
                except ValueError:
                    delay = 100
                self.turn_after_id = self.root.after(delay, self.play_turn)
            else:
                # Invalid move, reset and wait for a new keypress
                algorithm.next_move = None
                self.turn_after_id = self.root.after(100, self.check_user_action)
        else:
            # No key pressed yet, check again in 100ms
            self.turn_after_id = self.root.after(100, self.check_user_action)

    def update_scores(self):
        """Recalculate scores based on the current grid colors."""
        counters = {player.id: 0 for player in self.players}
        for row in self.grid.cells:
            for cell in row:
                for player in self.players:
                    if cell.color == player.color:
                        counters[player.id] += 1
        
        self.cell_counters = counters
        for player in self.players:
            self.cell_counter_labels[player.id].config(text=f"Player {player.id}: {self.cell_counters[player.id]} cells")
            
    def is_valid_move(self, player, new_position):
        """Check if the move is valid."""
        row, col = new_position

        # Ensure the position is within bounds
        if not (0 <= row < len(self.grid.cells) and 0 <= col < len(self.grid.cells[0])):
            return False

        target_cell = self.grid.cells[row][col]

        # Valid if the cell is white OR matches the player's color
        if target_cell.color == "white" or target_cell.color == player.color:
            # Prevent moving onto a cell currently occupied by another player
            if not target_cell.is_occupied() or target_cell.player == player.id:
                return True

        return False
    def advance_turn(self):
        """Advance to the next player's turn."""
        self.current_player_index = (self.current_player_index + 1) % len(self.players)
        self.log_with_timestamp(f"Player {self.players[self.current_player_index].id}'s turn using algorithm: {self.players[self.current_player_index].algorithm_menu.get()}")

    def end_game(self):
        """End the game and display the winner."""
        self.game_running = False
        winner = max(self.players, key=lambda p: self.cell_counters[p.id])
        self.log_with_timestamp(f"Game over! Player {winner.id} wins!")
        messagebox.showinfo("Game Over", f"Player {winner.id} wins!")