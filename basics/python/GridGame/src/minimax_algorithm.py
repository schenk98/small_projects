from __future__ import annotations

import random

from computer_algorithm import ComputerAlgorithm
from simulation import (
    apply_move,
    clone_state,
    evaluate_territory,
    legal_moves,
    positions_from_occupancy,
)


class MinimaxAlgorithm(ComputerAlgorithm):
    """
    Paranoid minimax: root maximizes for the mover; all other players minimize the same score.
    `depth_plies` counts moves simulated after the root (each ply = one player moves).
    """

    def __init__(self, seed: int | None = None, depth_plies: int = 4) -> None:
        self._rng = random.Random(seed) if seed is not None else random.Random()
        self.depth_plies = max(1, int(depth_plies))

    def get_next_move(
        self,
        grid_state,
        occupancy,
        current_position,
        player_color: str,
        rejected_positions: frozenset[tuple[int, int]] | None = None,
        *,
        player_colors_by_id: dict[int, str] | None = None,
        turn_order: list[int] | None = None,
    ):
        rejected = rejected_positions or frozenset()
        if not player_colors_by_id or not turn_order:
            return current_position

        my_id = occupancy[current_position[0]][current_position[1]]
        pos0 = positions_from_occupancy(occupancy)
        root_moves = [
            m
            for m in legal_moves(grid_state, occupancy, my_id, player_color, current_position)
            if m not in rejected
        ]
        if not root_moves:
            return current_position

        self._rng.shuffle(root_moves)
        idx_me = turn_order.index(my_id)
        next_pid = turn_order[(idx_me + 1) % len(turn_order)]

        best_m = root_moves[0]
        best_v = -1e30
        for m in root_moves:
            gs, occ, pos = clone_state(grid_state, occupancy, pos0)
            apply_move(gs, occ, pos, my_id, player_color, m)
            v = self._minimax_value(
                gs,
                occ,
                pos,
                self.depth_plies - 1,
                next_pid,
                my_id,
                player_colors_by_id,
                turn_order,
                -1e30,
                1e30,
            )
            if v > best_v:
                best_v, best_m = v, m

        return best_m

    def _minimax_value(
        self,
        grid_state,
        occupancy,
        positions: dict[int, tuple[int, int]],
        rem: int,
        to_move: int,
        max_id: int,
        colors: dict[int, str],
        turn_order: list[int],
        alpha: float,
        beta: float,
    ) -> float:
        if rem == 0:
            return evaluate_territory(grid_state, colors, max_id)

        pcol = colors[to_move]
        cur = positions[to_move]
        moves = legal_moves(grid_state, occupancy, to_move, pcol, cur)
        self._rng.shuffle(moves)
        if not moves:
            return evaluate_territory(grid_state, colors, max_id)

        idx = turn_order.index(to_move)
        nxt = turn_order[(idx + 1) % len(turn_order)]

        if to_move == max_id:
            v = -1e30
            for m in moves:
                gs, occ, pos = clone_state(grid_state, occupancy, positions)
                apply_move(gs, occ, pos, to_move, pcol, m)
                v = max(
                    v,
                    self._minimax_value(
                        gs, occ, pos, rem - 1, nxt, max_id, colors, turn_order, alpha, beta
                    ),
                )
                alpha = max(alpha, v)
                if beta <= alpha:
                    break
            return v

        v = 1e30
        for m in moves:
            gs, occ, pos = clone_state(grid_state, occupancy, positions)
            apply_move(gs, occ, pos, to_move, pcol, m)
            v = min(
                v,
                self._minimax_value(
                    gs, occ, pos, rem - 1, nxt, max_id, colors, turn_order, alpha, beta
                ),
            )
            beta = min(beta, v)
            if beta <= alpha:
                break
        return v
