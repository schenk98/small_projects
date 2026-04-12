# GridGame — developer & agent specification

This document describes **behavior, contracts, and extension points** for implementing new computer players and maintaining the codebase. It is not end-user documentation.

## Domain: what the simulation is

- **Grid**: a rectangular board of cells. Dimensions are **(width, height)** in code as `grid_size = (width, height)` — i.e. **columns × rows**.
- **Cell**: has a **fill color** (Tk color string: named color or `#rrggbb`) and optional **occupant** (`player` id, `0` = empty).
- **Turn order**: cyclic over `players` in registration order (`current_player_index`).
- **One move per turn**: exactly one orthogonal step **up / down / left / right** (no diagonals) to an adjacent cell, subject to validity rules below.
- **Painting**: after a **valid** move, the **destination** cell’s color becomes the moving player’s color. The mover’s **previous** cell is cleared of occupant but **retains its color** (territory is permanent).
- **Termination**: when every cell’s color is **not** `"white"`, the game ends. **Winner**: player with the highest count of cells whose `cell.color == player.color` (ties are possible; UI picks one `max`).
- **Lobby**: at most **4** players (`MAX_PLAYERS` in `gui.py`). Add/remove are disabled while a run is active; the last remaining player cannot be removed.

## Coordinate system

- Positions are **`(row, col)`** with `row ∈ [0, height)`, `col ∈ [0, width)`.
- Internal storage: `grid.cells[row][col]`.

## Valid move predicate (normative)

A move from `current_player.position` to `new_position = (row, col)` is **valid** iff:

1. **In bounds**: `0 <= row < height` and `0 <= col < width`.
2. **Orthogonal adjacency**: Manhattan distance exactly 1 from the current position.
3. **Target cell color**: `target_cell.color == "white"` **or** `target_cell.color == player.color`.
4. **Occupancy**: the target must not hold another player — i.e. `not target_cell.is_occupied()` **or** `target_cell.player == player.id` (for an adjacent step onto a **new** cell, this reduces to **empty**).

Reference implementation: `GridGameApp.is_valid_move` in `gui.py`.

## State exposed to algorithms

Algorithms receive a **snapshot** each time they are asked for a move (no hidden Tk state).

### `Grid.get_state() -> list[list[str]]`

- **Colors only**, one string per cell.
- **Convention**: `"white"` means *unclaimed* for heuristics.

### `Grid.get_occupancy() -> list[list[int]]`

- **Same shape** as `get_state`.
- Value: **occupant player id**, or **`0`** if the cell has no pawn.
- Lets policies see **blocking** (e.g. white cell occupied by another player) without inferring from colors alone.

### Player color argument

- `get_next_move` also receives **`player_color: str`** (the moving player’s configured color). Use this for rules that depend on **territory color**, not only the color of the cell currently under the pawn (that cell may still be `"white"`).

## Player colors (lobby rules)

- **No duplicates or near-duplicates**: any new color must differ enough from every other player’s color. Implemented with Tk `winfo_rgb` mapped to 8-bit channels; two colors are **too similar** when the **maximum channel delta** is **strictly less than** `MIN_CHANNEL_DELTA` in `color_distance.py` (default **51**, i.e. need ≥51 on at least one RGB channel). Change that constant to tune strictness.
- Enforced when picking a color and again on **Start**.

## Algorithm interface (primary extension point)

### `ComputerAlgorithm` (`computer_algorithm.py`)

```text
get_next_move(
    grid_state,
    occupancy,
    current_position,
    player_color,
    rejected_positions=None,
) -> (new_row, new_col)
```

- **`grid_state`**: `Grid.get_state()`.
- **`occupancy`**: `Grid.get_occupancy()`.
- **`current_position`**: `(row, col)` of the mover.
- **`player_color`**: mover’s territory color string.
- **`rejected_positions`**: `frozenset` of `(row, col)` the engine has **already rejected this turn** after validation. Implementations **must not** return the same coordinate again when a different choice exists (avoids deterministic loops on impossible moves).
- **Return**: absolute destination `(row, col)` (not a delta). The engine re-validates with `is_valid_move`.

### Built-in implementations

| Class | Module | Role |
|--------|--------|------|
| `RandomMoveAlgorithm` | `random_moving_computer.py` | Prefer adjacent empty `"white"`; BFS over legal steps toward `"white"`; else own-color step; uses `occupancy` so pawns block targets. |
| `UserActions` | `user_actions.py` | `set_next_move((dr, dc))` from keys; `get_next_move` applies one orthogonal step using `player_color` + `occupancy`. |

## Computer turns: invalid proposals do **not** consume the turn

1. The engine calls `get_next_move` with `rejected_positions` (initially empty).
2. If `is_valid_move` **fails**, the candidate is added to **`rejected`**, and **`get_next_move` is called again** (same turn, no `advance_turn`).
3. After **`MAX_COMPUTER_MOVE_ATTEMPTS`** (`gui.py`) without success, the engine picks a **uniform random legal neighbor** if any; if **none**, it logs and **advances the turn without moving** (deadlock escape).

Human turns already block advance until a **valid** key-produced move.

## Runtime & game loop

- **Entry**: `src/main.py` — `tk.Tk`, `GridGameApp(root)`, `mainloop()`.
- **Working directory**: flat imports (`from gui import …`). Run from `src`, e.g. `cd src && python main.py`.
- **Scheduling**: `tk.after` for turn delay and user polling (~100 ms). No background game thread.

### Turn flow (`GridGameApp.play_turn`)

1. Game over → `end_game()`.
2. Resolve algorithm from combobox → `self.algorithms[label]`.
3. **`UserActions`** → `check_user_action()`.
4. **Computer** → `_play_computer_turn()` (retry / fallback as above).

## GUI ↔ model wiring

- **`Grid`**: canvas, cells, `get_state`, `get_occupancy`, `place_player`, `move_player`.
- **`Player`**: `id`, `color`, combobox ref `algorithm_menu`, UI row widgets (`player_frame`, `remove_btn`, swatch, `cells_label`).
- **New AI**: subclass `ComputerAlgorithm`, register in `GridGameApp.algorithms` in `gui.py`.

## File map

| File | Responsibility |
|------|----------------|
| `main.py` | Bootstrap Tk app. |
| `gui.py` | `GridGameApp`: layout, loop, scoring, `MAX_PLAYERS`, computer retry/fallback. |
| `grid.py` | Board + canvas; `get_state`, `get_occupancy`, moves. |
| `cell.py` | Color + occupant id. |
| `player.py` | Player metadata. |
| `computer_algorithm.py` | Abstract move policy. |
| `random_moving_computer.py` | Reference bot. |
| `user_actions.py` | Human adapter. |
| `color_distance.py` | `MIN_CHANNEL_DELTA`, `colors_too_similar`, RGB helpers. |

## Dependencies

- **Python 3** with **Tkinter** (stdlib). No `requirements.txt` in this folder.

## Extension guidelines

1. Subclass `ComputerAlgorithm` and implement `get_next_move` with the full signature.
2. Use **`occupancy`** for blocking; **`player_color`** for territory rules; **`grid_state`** for paint.
3. Respect **`rejected_positions`** to cooperate with the retry loop.
4. Register the instance in `GridGameApp.algorithms`.

## Planned algorithms (spec for implementers)

Implementations should live in new modules (e.g. `greedy_algorithm.py`, `lookahead_algorithm.py`, `minimax_algorithm.py`), subclass `ComputerAlgorithm`, and register in `gui.py`. All stochastic or tie-breaking behavior must go through a **seeded RNG** (see *RNG seeding* below).

### 1. Greedy

**Idea:** Among **legal** neighbors (respect `occupancy`, `player_color`, engine rules), pick the move that maximizes a cheap **static score**; no deep search.

**Baseline heuristics (pick one or combine with weights):**

- **Immediate paint gain:** +1 if the destination is `"white"` (new territory), 0 if stepping on own color only.
- **Frontier / exposure:** count adjacent `"white"` cells from the destination after the *hypothetical* paint (or proxy: count white neighbors of the destination before the move).
- **Reachable white mass:** BFS/DFS from the destination over cells that are `"white"` or `player_color`, respecting **empty** targets only (`occupancy == 0` except as needed for path semantics), to estimate the size of the **reachable unclaimed** component you touch or grow.
- **Distance to nearest white:** minimize Manhattan or BFS steps to any reachable empty `"white"` cell (escape enclosed territory faster than random).

**Improvements:**

- **Tie-breaking:** many neighbors tie on score — shuffle candidates with **`self._rng.shuffle`** before `max`, or add tiny noise `+ self._rng.random() * ε` to scores so seeds still fix behavior.
- **`rejected_positions`:** never return a coordinate in `rejected_positions` if any other legal neighbor exists; filter candidates first.
- **Opponents:** optional penalty if the move **opens** a large white region toward another pawn (requires mapping colors to ids or passing more context in a future API).
- **Endgame:** when few whites remain, switch weight to **direct claiming** (prefer any move that paints white).

### 2. Lookahead (shallow fixed-depth search)

**Idea:** Build a **headless clone** of the board (lists of colors + occupancy + positions; **no Tk**). From the current snapshot, search **K plies** (K small, e.g. 1–3): at each ply, assume **all** players play with a simple policy (e.g. greedy or random-with-seed) or only expand **your** moves and treat others as one-step greedy.

**Evaluation at leaves (and optionally at shallow nodes):**

- Your **cell count** (cells with `player_color`) minus a weighted sum of opponents’ counts, or ratio vs. sum.
- **Potential:** count of white cells still **reachable** from your pawn (BFS with rules above).
- **Mobility:** number of legal moves from your position (deadlock avoidance).

**Improvements:**

- **Alpha-beta** is optional at depth 1–2 if you fix opponent branching order; for **3+ players**, expect **paranoid** or **max^n** approximations unless you keep depth tiny.
- **Iterative deepening** only if time-budgeted (not required for K ≤ 2).
- Use **`rejected_positions`** at the **root** only: the real engine retries root proposals; do not second-guess deeper branches with the same set.

**Seeding:** any **simulated opponent** stochasticity inside the tree must use a **derived RNG** (e.g. `random.Random(self._seed ^ ply_hash ^ player_id)`) so the same root state and seed yield the same lookahead choice.

### 3. Minimax (and multi-agent variants)

**Idea:** Treat the game as **turn-based**; each node is a full snapshot `(grid_state, occupancy, positions_by_player)`. Children apply **one legal move** for the player-to-act. **Terminal** nodes: game over by full coloring or depth limit.

**Horizon:** cap depth **D** and use the same **eval heuristic** as lookahead at cut nodes.

**Multi-agent (3–4 players):**

- **Max^n:** each node is `max` for P1, `max` for P2, … in turn order — correct but branchy.
- **Paranoid:** assume all opponents collude to minimize your score — smaller tree, pessimistic.
- **Shapley / coalition** approximations are overkill here; prefer **max^n with strong move ordering** (sort moves by greedy score first) or paranoid for a first implementation.

**Improvements:**

- **Move ordering** (greedy sort) improves pruning if you add alpha-beta on 2-player paranoid reduction.
- **Transposition table** keyed by `(tuple of colors, tuple of occupancy, current_player, positions)` — optional; watch memory on large grids.
- **Quiescence** not usually needed (no capture chains); one special case: prefer not to end the ply on a “pass” if you add pass moves later.

**Seeding:** tie-breaking among equally good moves at any `max`/`min` node must use **`self._rng`** (e.g. shuffle or random index among ties).

### 4. Monte Carlo tree search (optional extension)

**Idea:** MCTS with **random rollout** to terminal or depth cap; **UCB1** selection. Rollouts must use a **`random.Random` seeded** per bot or per rollout root for reproducibility.

**Improvements:** progressive widening, heuristic playout policy (greedy toward white), virtual loss for parallelization (if ever batching).

---

## RNG seeding (required for greedy, lookahead, minimax, MCTS)

**Contract for every bot class:**

1. **`__init__(self, seed: int | None = None)`** (or explicit `rng: random.Random`).
2. If `seed is not None`: **`self._rng = random.Random(seed)`**. Else: **`self._rng = random.Random()`** (non-deterministic session).
3. **Never** use `random.randint` / `shuffle` from the module `random` for decisions; use **`self._rng`** only.
4. **Document** the seed in the combobox label if multiple instances need distinct streams (e.g. `"Greedy (seed=0)"`) or pass `seed=player_id * 1000 + 7` from `gui.py` when constructing per-player algorithms.

**Testing:** same `(seed, grid_state, occupancy, position, player_color, rejected_positions)` ⇒ same returned move.

**Derived seeds for lookahead rollouts:** use `random.Random((self._seed, ply, player_id, zobrist_key))` or `hash` to a 32-bit int — avoid mutating `self._rng` order when probing hypothetical branches unless you snapshot/restore RNG state (prefer separate `Random` per simulation branch).

---

## Other systems (non-algorithm)

- **Headless / batch runner:** CLI that steps the same rules without `tk.Canvas` (reuse move/score logic extracted from `gui.py`) for tuning heuristics and measuring Elo or win rates across seeds.
- **Telemetry:** log `(seed, move, snapshot_hash)` per turn for regression diffs.

---

When changing behavior, update this document **in the same change** so agents and humans can rely on it as the working spec.
