from __future__ import annotations

import tkinter as tk
from tkinter import ttk, messagebox, colorchooser
from typing import Optional

from player import Player
from random_moving_computer import RandomMoveAlgorithm
from user_actions import UserActions
from greedy_algorithm import GreedyAlgorithm
from lookahead_algorithm import LookaheadAlgorithm
from minimax_algorithm import MinimaxAlgorithm
from grid import Grid
from color_distance import colors_too_similar, MIN_CHANNEL_DELTA
import random
import time

DEFAULT_PLAYER_COLORS = [
    "red",
    "blue",
    "green",
    "yellow",
    "magenta",
    "cyan",
    "DarkOrange1",
    "purple",
]

MAX_COMPUTER_MOVE_ATTEMPTS = 64
MAX_PLAYERS = 4

# Pixels reserved outside the grid canvas (window chrome, taskbar, padding).
SCREEN_MARGIN_X = 48
SCREEN_MARGIN_Y = 120


class GridGameApp:
    def __init__(self, root: tk.Tk):
        self.root = root
        self.root.title("Grid Game")

        # Default settings
        self.grid_size = (20, 20)
        self.players = []
        self.grid = None
        self.current_player_index = 0
        self.algorithms: dict = {}

        # Initialize the paused state
        self.is_paused = False
        self.game_running = False

        # GUI setup
        self.setup_gui()

    def setup_gui(self):
        """Setup the GUI layout."""
        self.header_frame = ttk.Frame(self.root, padding="10")
        self.header_frame.grid(row=0, column=0, sticky="ew")

        self.grid_frame = ttk.Frame(self.root)
        self.grid_frame.grid(row=1, column=0, sticky="nsew")

        self.root.rowconfigure(1, weight=1)
        self.root.columnconfigure(0, weight=1)

        self.cell_counters = {}

        settings = ttk.Frame(self.header_frame)
        settings.pack(fill="x")

        board_lf = ttk.LabelFrame(settings, text="Board", padding=8)
        board_lf.grid(row=0, column=0, sticky="nw", padx=(0, 8), pady=0)

        ttk.Label(board_lf, text="Width (cols):").grid(row=0, column=0, sticky="w", pady=2)
        self.grid_width_entry = ttk.Entry(board_lf, width=8)
        self.grid_width_entry.grid(row=0, column=1, sticky="w", padx=(4, 0), pady=2)
        self.grid_width_entry.insert(0, "10")

        ttk.Label(board_lf, text="Height (rows):").grid(row=1, column=0, sticky="w", pady=2)
        self.grid_height_entry = ttk.Entry(board_lf, width=8)
        self.grid_height_entry.grid(row=1, column=1, sticky="w", padx=(4, 0), pady=2)
        self.grid_height_entry.insert(0, "10")

        ttk.Label(board_lf, text="Spawn:").grid(row=2, column=0, sticky="w", pady=2)
        self.initial_position_var = tk.StringVar(value="Corners")
        self.initial_position_menu = ttk.Combobox(
            board_lf,
            textvariable=self.initial_position_var,
            values=["Corners", "Random", "Middle"],
            state="readonly",
            width=10,
        )
        self.initial_position_menu.grid(row=2, column=1, sticky="w", padx=(4, 0), pady=2)

        players_lf = ttk.LabelFrame(settings, text="Players", padding=8)
        players_lf.grid(row=0, column=1, sticky="nsew", padx=8, pady=0)
        settings.columnconfigure(1, weight=1)

        self.add_player_button = ttk.Button(players_lf, text="Add player", command=self.add_player)
        self.add_player_button.pack(anchor="w", pady=(0, 6))

        self.root.update_idletasks()
        screen_h = max(600, self.root.winfo_screenheight())
        players_viewport_h = max(120, min(280, int(screen_h * 0.28)))
        players_outer = ttk.Frame(players_lf)
        players_outer.pack(fill="both", expand=True)

        pv_canvas = tk.Canvas(
            players_outer,
            highlightthickness=0,
            height=players_viewport_h,
        )
        pv_scroll = ttk.Scrollbar(players_outer, orient="vertical", command=pv_canvas.yview)
        pv_canvas.configure(yscrollcommand=pv_scroll.set)

        self.players_frame = ttk.Frame(pv_canvas)
        players_inner_id = pv_canvas.create_window((0, 0), window=self.players_frame, anchor="nw")

        def _players_inner_configure(_event=None):
            pv_canvas.configure(scrollregion=pv_canvas.bbox("all"))

        def _players_canvas_configure(event):
            pv_canvas.itemconfigure(players_inner_id, width=event.width)

        self.players_frame.bind("<Configure>", _players_inner_configure)
        pv_canvas.bind("<Configure>", _players_canvas_configure)

        pv_canvas.grid(row=0, column=0, sticky="nsew")
        pv_scroll.grid(row=0, column=1, sticky="ns")
        players_outer.rowconfigure(0, weight=1)
        players_outer.columnconfigure(0, weight=1)

        def _players_wheel(event):
            if event.delta:
                pv_canvas.yview_scroll(int(-event.delta / 120), "units")

        def _players_wheel_up(_event):
            pv_canvas.yview_scroll(-1, "units")

        def _players_wheel_down(_event):
            pv_canvas.yview_scroll(1, "units")

        pv_canvas.bind("<MouseWheel>", _players_wheel)
        pv_canvas.bind("<Button-4>", _players_wheel_up)
        pv_canvas.bind("<Button-5>", _players_wheel_down)
        pv_canvas.bind("<Enter>", lambda _e: pv_canvas.focus_set())

        self._players_list_canvas = pv_canvas

        run_lf = ttk.LabelFrame(settings, text="Run", padding=8)
        run_lf.grid(row=0, column=2, sticky="ne", padx=(8, 0), pady=0)

        ttk.Label(run_lf, text="Move delay (ms):").grid(row=0, column=0, sticky="w", pady=2)
        self.move_delay_entry = ttk.Entry(run_lf, width=8)
        self.move_delay_entry.grid(row=0, column=1, sticky="w", padx=(4, 0), pady=2)
        self.move_delay_entry.insert(0, "100")

        ttk.Label(run_lf, text="Bot RNG seed:").grid(row=1, column=0, sticky="w", pady=2)
        self.bot_seed_entry = ttk.Entry(run_lf, width=10)
        self.bot_seed_entry.grid(row=1, column=1, sticky="w", padx=(4, 0), pady=2)
        ttk.Label(run_lf, text="(empty = random)", font=("TkDefaultFont", 8)).grid(
            row=2, column=0, columnspan=2, sticky="w"
        )

        ttk.Label(run_lf, text="Lookahead plies:").grid(row=3, column=0, sticky="w", pady=2)
        self.lookahead_plies_entry = ttk.Entry(run_lf, width=8)
        self.lookahead_plies_entry.grid(row=3, column=1, sticky="w", padx=(4, 0), pady=2)
        self.lookahead_plies_entry.insert(0, "3")

        ttk.Label(run_lf, text="Minimax plies:").grid(row=4, column=0, sticky="w", pady=2)
        self.minimax_plies_entry = ttk.Entry(run_lf, width=8)
        self.minimax_plies_entry.grid(row=4, column=1, sticky="w", padx=(4, 0), pady=2)
        self.minimax_plies_entry.insert(0, "4")

        self.start_button = ttk.Button(run_lf, text="Start", command=self.start_game)
        self.start_button.grid(row=5, column=0, columnspan=2, sticky="ew", pady=(8, 4))

        self.pause_button = ttk.Button(run_lf, text="Pause", command=self.toggle_pause, state="disabled")
        self.pause_button.grid(row=6, column=0, columnspan=2, sticky="ew", pady=2)

        self.stop_button = ttk.Button(run_lf, text="Stop", command=self.stop_game, state="disabled")
        self.stop_button.grid(row=7, column=0, columnspan=2, sticky="ew", pady=2)

        self._rebuild_algorithms()
        self.add_default_players()
        self._sync_player_edit_controls()

    def _parse_bot_seed(self) -> int | None:
        raw = self.bot_seed_entry.get().strip()
        if not raw:
            return None
        try:
            return int(raw, 0)
        except ValueError:
            return None

    @staticmethod
    def _parse_positive_int(entry: ttk.Entry, default: int, lo: int, hi: int) -> int:
        try:
            v = int(entry.get().strip(), 0)
        except ValueError:
            return default
        return max(lo, min(hi, v))

    def _rebuild_algorithms(self) -> None:
        seed = self._parse_bot_seed()
        la = self._parse_positive_int(self.lookahead_plies_entry, 3, 0, 32)
        mm = self._parse_positive_int(self.minimax_plies_entry, 4, 1, 12)
        self.algorithms = {
            "User Actions": UserActions(),
            "Random Move": RandomMoveAlgorithm(seed=seed),
            "Greedy": GreedyAlgorithm(seed=seed),
            "Lookahead": LookaheadAlgorithm(seed=seed, sim_plies=la),
            "Minimax": MinimaxAlgorithm(seed=seed, depth_plies=mm),
        }

    def add_default_players(self):
        """Add default players to the game."""
        for i in range(2):
            self.add_player()

    def _schedule_players_scroll_sync(self) -> None:
        if not hasattr(self, "_players_list_canvas"):
            return

        def _sync_players_scroll():
            bbox = self._players_list_canvas.bbox("all")
            if bbox:
                self._players_list_canvas.configure(scrollregion=bbox)

        self.root.after_idle(_sync_players_scroll)

    def _teardown_grid_if_lobby_edit(self) -> None:
        """Drop a non-running board so roster edits cannot desync from the canvas."""
        if self.game_running or self.grid is None:
            return
        self.grid_frame.destroy()
        self.grid_frame = ttk.Frame(self.root)
        self.grid_frame.grid(row=1, column=0, sticky="nsew")
        self.grid = None
        self.current_player_index = 0
        self.update_scores()

    def _sync_player_edit_controls(self) -> None:
        at_cap = len(self.players) >= MAX_PLAYERS
        running = self.game_running
        self.add_player_button.config(state="disabled" if (at_cap or running) else "normal")
        only_one = len(self.players) <= 1
        for p in self.players:
            btn = getattr(p, "remove_btn", None)
            if btn is not None:
                btn.config(state="disabled" if (running or only_one) else "normal")

    def add_player(self):
        """Add a new player to the game."""
        if self.game_running:
            return
        if len(self.players) >= MAX_PLAYERS:
            return

        self._teardown_grid_if_lobby_edit()

        player_id = max((p.id for p in self.players), default=0) + 1
        default_color = self._pick_distinct_default_color()
        player = Player(player_id, default_color, "Random Move")
        self.players.append(player)

        player_frame = ttk.Frame(self.players_frame)
        player_frame.pack(fill="x", pady=2)
        player.player_frame = player_frame

        sw = tk.Canvas(
            player_frame,
            width=28,
            height=20,
            highlightthickness=1,
            highlightbackground="#999999",
            bd=0,
        )
        sw.pack(side="left", padx=(0, 6))
        player.color_swatch = sw
        player.swatch_id = sw.create_rectangle(2, 2, 26, 18, fill=player.color, outline="")

        ttk.Label(player_frame, text=f"P{player_id}").pack(side="left", padx=(0, 4))
        player.cells_label = ttk.Label(player_frame, text="0 cells", width=9, anchor="e")
        player.cells_label.pack(side="left", padx=(0, 6))

        color_button = ttk.Button(
            player_frame,
            text="Color",
            width=7,
            command=lambda: self.pick_color(player),
        )
        color_button.pack(side="left", padx=(0, 4))

        algorithm_menu = ttk.Combobox(
            player_frame,
            values=list(self.algorithms.keys()),
            state="readonly",
            width=14,
        )
        algorithm_menu.set(player.algorithm)
        algorithm_menu.pack(side="left", padx=(0, 0))
        player.algorithm_menu = algorithm_menu

        remove_btn = ttk.Button(
            player_frame,
            text="x",
            width=2,
            command=lambda pl=player: self.remove_player(pl),
        )
        remove_btn.pack(side="left", padx=(6, 0))
        player.remove_btn = remove_btn

        self.cell_counters[player_id] = 0
        self._schedule_players_scroll_sync()
        self._sync_player_edit_controls()

    def remove_player(self, player: Player) -> None:
        if self.game_running:
            messagebox.showinfo("Players", "Stop the game before removing players.")
            return
        if len(self.players) <= 1:
            return
        if player not in self.players:
            return

        self._teardown_grid_if_lobby_edit()

        self.players.remove(player)
        self.cell_counters.pop(player.id, None)
        player.player_frame.destroy()
        self._schedule_players_scroll_sync()
        self._sync_player_edit_controls()

    def _color_conflicts_with_others(self, color: str, exclude: Optional[Player] = None) -> bool:
        for p in self.players:
            if exclude is not None and p is exclude:
                continue
            if p.color == color or colors_too_similar(self.root, color, p.color):
                return True
        return False

    def _pick_distinct_default_color(self) -> str:
        for name in DEFAULT_PLAYER_COLORS:
            if not self._color_conflicts_with_others(name):
                return name
        for i in range(0, 256, 40):
            for j in range(0, 256, 40):
                for k in range(0, 256, 40):
                    candidate = f"#{i:02x}{j:02x}{k:02x}"
                    if not self._color_conflicts_with_others(candidate):
                        return candidate
        return "#7f7f7f"

    def pick_color(self, player: Player):
        """Pick a color for the player."""
        color_code = colorchooser.askcolor(title=f"Pick Color for Player {player.id}")[1]
        if not color_code:
            return
        if self._color_conflicts_with_others(color_code, exclude=player):
            messagebox.showwarning(
                "Color too close",
                f"Choose a color that differs more from other players "
                f"(at least {MIN_CHANNEL_DELTA} per RGB channel vs each other color).",
            )
            return
        player.color = color_code
        self._update_player_swatch(player)

    def _update_player_swatch(self, player: Player) -> None:
        if getattr(player, "color_swatch", None) is not None and getattr(player, "swatch_id", None) is not None:
            player.color_swatch.itemconfigure(player.swatch_id, fill=player.color)

    def _validate_player_colors(self) -> bool:
        n = len(self.players)
        for i in range(n):
            for j in range(i + 1, n):
                a, b = self.players[i].color, self.players[j].color
                if a == b or colors_too_similar(self.root, a, b):
                    messagebox.showerror(
                        "Player colors",
                        f"Players {self.players[i].id} and {self.players[j].id} use colors that are "
                        f"identical or too similar (need ≥{MIN_CHANNEL_DELTA} difference on some RGB channel). "
                        "Adjust colors before starting.",
                    )
                    return False
        return True

    def start_game(self):
        """Start the game."""
        print("[DEBUG] Start button pressed.")
        try:
            if not self._validate_player_colors():
                return
            self._rebuild_algorithms()
            width = int(self.grid_width_entry.get())
            height = int(self.grid_height_entry.get())
            self.grid_size = (width, height)

            self.root.update_idletasks()
            header_h = self.header_frame.winfo_reqheight()
            avail_w = max(64, self.root.winfo_screenwidth() - SCREEN_MARGIN_X)
            avail_h = max(64, self.root.winfo_screenheight() - header_h - SCREEN_MARGIN_Y)
            cell_w = avail_w // width if width else 1
            cell_h = avail_h // height if height else 1
            cell_size = max(5, min(cell_w, cell_h, 50))

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

            self.pause_button.config(state="normal", text="Pause")
            self.stop_button.config(state="normal")

            print("[DEBUG] Game started with grid size:", self.grid_size)

            self.game_running = True
            self._sync_player_edit_controls()
            self.bind_user_input()

            # Start the game loop directly (NO THREADING)
            self.play_turn()

        except ValueError:
            print("[DEBUG] Invalid grid size input.")

    def toggle_pause(self):
        """Pause or resume the game."""
        if self.is_paused:
            self.is_paused = False
            self.pause_button.config(text="Pause")
            self.play_turn()  # Kickstart the loop again
        else:
            self.is_paused = True
            self.pause_button.config(text="Resume")
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
        self._sync_player_edit_controls()
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
            self._play_computer_turn(current_player, algorithm)

    def _schedule_next_play_turn(self):
        try:
            delay = int(self.move_delay_entry.get())
        except ValueError:
            delay = 100
        self.turn_after_id = self.root.after(delay, self.play_turn)

    def _valid_adjacent_positions(self, player: Player) -> list[tuple[int, int]]:
        row, col = player.position
        out = []
        for dr, dc in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
            nr, nc = row + dr, col + dc
            if self.is_valid_move(player, (nr, nc)):
                out.append((nr, nc))
        return out

    def _play_computer_turn(self, current_player, algorithm):
        state = self.grid.get_state()
        occ = self.grid.get_occupancy()
        pos = current_player.position
        rejected: set[tuple[int, int]] = set()

        pid_to_color = {p.id: p.color for p in self.players}
        turn_order = [p.id for p in self.players]

        for _ in range(MAX_COMPUTER_MOVE_ATTEMPTS):
            frozen = frozenset(rejected)
            candidate = algorithm.get_next_move(
                state,
                occ,
                pos,
                current_player.color,
                frozen,
                player_colors_by_id=pid_to_color,
                turn_order=turn_order,
            )
            if self.is_valid_move(current_player, candidate):
                self.grid.move_player(current_player, candidate)
                self.update_scores()
                self.advance_turn()
                self._schedule_next_play_turn()
                return
            rejected.add(candidate)

        legal = self._valid_adjacent_positions(current_player)
        if legal:
            self.log_with_timestamp(
                "Computer move fallback: algorithm exhausted rejected set; picking a legal neighbor."
            )
            pick = random.choice(legal)
            self.grid.move_player(current_player, pick)
            self.update_scores()
        else:
            self.log_with_timestamp(
                f"No legal orthogonal move for player {current_player.id}; advancing turn without moving."
            )

        self.advance_turn()
        self._schedule_next_play_turn()

    def check_user_action(self):
        """Check if the user has made a move."""
        if not self.game_running or self.is_paused:
            return

        current_player = self.players[self.current_player_index]
        algorithm_name = current_player.algorithm_menu.get()
        algorithm = self.algorithms[algorithm_name]

        if algorithm.next_move is not None:
            # User has pressed a key
            pid_to_color = {p.id: p.color for p in self.players}
            turn_order = [p.id for p in self.players]
            new_position = algorithm.get_next_move(
                self.grid.get_state(),
                self.grid.get_occupancy(),
                current_player.position,
                current_player.color,
                player_colors_by_id=pid_to_color,
                turn_order=turn_order,
            )
            
            if self.is_valid_move(current_player, new_position):
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
        if self.grid is None:
            for player in self.players:
                if hasattr(player, "cells_label"):
                    player.cells_label.config(text="0 cells")
            return

        counters = {player.id: 0 for player in self.players}
        for row in self.grid.cells:
            for cell in row:
                for player in self.players:
                    if cell.color == player.color:
                        counters[player.id] += 1

        self.cell_counters = counters
        for player in self.players:
            if hasattr(player, "cells_label"):
                n = self.cell_counters.get(player.id, 0)
                player.cells_label.config(text=f"{n} cells")
            
    def is_valid_move(self, player, new_position):
        """Check if the move is valid."""
        row, col = new_position

        if player.position is None:
            return False

        old_row, old_col = player.position
        if abs(row - old_row) + abs(col - old_col) != 1:
            return False

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
        self.pause_button.config(state="disabled")
        self.stop_button.config(state="disabled")
        self.start_button.config(state="normal")
        self._sync_player_edit_controls()
        winner = max(self.players, key=lambda p: self.cell_counters[p.id])
        self.log_with_timestamp(f"Game over! Player {winner.id} wins!")
        messagebox.showinfo("Game Over", f"Player {winner.id} wins!")