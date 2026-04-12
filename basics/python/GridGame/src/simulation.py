"""Headless grid rules for bots (no Tk)."""

from __future__ import annotations

import random
from collections import deque
from collections.abc import MutableMapping

DIRS = ((-1, 0), (1, 0), (0, -1), (0, 1))


def clone_grid(grid_state: list[list[str]]) -> list[list[str]]:
    return [row[:] for row in grid_state]


def clone_occ(occupancy: list[list[int]]) -> list[list[int]]:
    return [row[:] for row in occupancy]


def positions_from_occupancy(occupancy: list[list[int]]) -> dict[int, tuple[int, int]]:
    pos: dict[int, tuple[int, int]] = {}
    for r in range(len(occupancy)):
        for c in range(len(occupancy[0])):
            pid = occupancy[r][c]
            if pid:
                pos[pid] = (r, c)
    return pos


def legal_moves(
    grid_state: list[list[str]],
    occupancy: list[list[int]],
    player_id: int,
    player_color: str,
    current_pos: tuple[int, int],
) -> list[tuple[int, int]]:
    r, c = current_pos
    h, w = len(grid_state), len(grid_state[0])
    out: list[tuple[int, int]] = []
    for dr, dc in DIRS:
        nr, nc = r + dr, c + dc
        if not (0 <= nr < h and 0 <= nc < w):
            continue
        if occupancy[nr][nc] != 0:
            continue
        col = grid_state[nr][nc]
        if col == "white" or col == player_color:
            out.append((nr, nc))
    return out


def apply_move(
    grid_state: list[list[str]],
    occupancy: list[list[int]],
    positions: MutableMapping[int, tuple[int, int]],
    player_id: int,
    player_color: str,
    dest: tuple[int, int],
) -> None:
    sr, sc = positions[player_id]
    dr, dc = dest
    if occupancy[sr][sc] != player_id:
        raise ValueError("apply_move: mover not on source cell")
    occupancy[sr][sc] = 0
    grid_state[dr][dc] = player_color
    occupancy[dr][dc] = player_id
    positions[player_id] = (dr, dc)


def count_color_cells(grid_state: list[list[str]], color: str) -> int:
    return sum(cell == color for row in grid_state for cell in row)


def evaluate_territory(
    grid_state: list[list[str]],
    player_colors_by_id: dict[int, str],
    perspective_id: int,
) -> float:
    myc = player_colors_by_id[perspective_id]
    my = count_color_cells(grid_state, myc)
    others = 0.0
    for pid, col in player_colors_by_id.items():
        if pid == perspective_id:
            continue
        others += count_color_cells(grid_state, col)
    return float(my - others)


def white_frontier_score(
    grid_state: list[list[str]],
    occupancy: list[list[int]],
    dest: tuple[int, int],
) -> int:
    r, c = dest
    h, w = len(grid_state), len(grid_state[0])
    n = 0
    for dr, dc in DIRS:
        nr, nc = r + dr, c + dc
        if 0 <= nr < h and 0 <= nc < w:
            if grid_state[nr][nc] == "white" and occupancy[nr][nc] == 0:
                n += 1
    return n


def reachable_white_touching(
    grid_state: list[list[str]],
    occupancy: list[list[int]],
    start: tuple[int, int],
    player_color: str,
) -> int:
    """Count distinct white cells reachable by walking only on white or own-color cells (empty targets)."""
    sr, sc = start
    h, w = len(grid_state), len(grid_state[0])
    q = deque([(sr, sc)])
    seen = {(sr, sc)}
    white_cells = 0
    while q:
        r, c = q.popleft()
        for dr, dc in DIRS:
            nr, nc = r + dr, c + dc
            if not (0 <= nr < h and 0 <= nc < w):
                continue
            if occupancy[nr][nc] != 0:
                continue
            col = grid_state[nr][nc]
            if col != "white" and col != player_color:
                continue
            if (nr, nc) in seen:
                continue
            seen.add((nr, nc))
            if col == "white":
                white_cells += 1
            q.append((nr, nc))
    return white_cells


def score_move_greedy(
    grid_state: list[list[str]],
    occupancy: list[list[int]],
    positions: dict[int, tuple[int, int]],
    player_colors_by_id: dict[int, str],
    player_id: int,
    player_color: str,
    dest: tuple[int, int],
) -> float:
    gs = clone_grid(grid_state)
    occ = clone_occ(occupancy)
    pos = dict(positions)
    was_white = gs[dest[0]][dest[1]] == "white"
    apply_move(gs, occ, pos, player_id, player_color, dest)
    pr, pc = pos[player_id]
    score = 0.0
    if was_white:
        score += 50.0
    score += white_frontier_score(gs, occ, dest) * 4.0
    score += reachable_white_touching(gs, occ, (pr, pc), player_color) * 0.15
    score += evaluate_territory(gs, player_colors_by_id, player_id) * 0.5
    return score


def greedy_best_move(
    rng: random.Random,
    grid_state: list[list[str]],
    occupancy: list[list[int]],
    positions: dict[int, tuple[int, int]],
    player_id: int,
    player_color: str,
    player_colors_by_id: dict[int, str],
    rejected: frozenset[tuple[int, int]] = frozenset(),
) -> tuple[int, int] | None:
    cur = positions[player_id]
    legal = legal_moves(grid_state, occupancy, player_id, player_color, cur)
    legal = [m for m in legal if m not in rejected]
    if not legal:
        return None
    scored: list[tuple[float, tuple[int, int]]] = []
    for m in legal:
        s = score_move_greedy(
            grid_state,
            occupancy,
            positions,
            player_colors_by_id,
            player_id,
            player_color,
            m,
        )
        scored.append((s, m))
    mx = max(s for s, _ in scored)
    best = [m for s, m in scored if s == mx]
    rng.shuffle(best)
    return best[0]


def clone_state(
    grid_state: list[list[str]],
    occupancy: list[list[int]],
    positions: dict[int, tuple[int, int]],
) -> tuple[list[list[str]], list[list[int]], dict[int, tuple[int, int]]]:
    return clone_grid(grid_state), clone_occ(occupancy), dict(positions)
