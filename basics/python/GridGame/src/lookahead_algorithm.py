from __future__ import annotations

import random

from computer_algorithm import ComputerAlgorithm
from simulation import (
    apply_move,
    clone_state,
    evaluate_territory,
    greedy_best_move,
    legal_moves,
    positions_from_occupancy,
)


class LookaheadAlgorithm(ComputerAlgorithm):
    """Greedy rollout for a fixed number of plies after the root move (cyclic turn order)."""

    def __init__(self, seed: int | None = None, sim_plies: int = 3) -> None:
        self._rng = random.Random(seed) if seed is not None else random.Random()
        self.sim_plies = max(0, int(sim_plies))

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
        best_m = root_moves[0]
        best_v = -1e30

        idx_me = turn_order.index(my_id)
        for m in root_moves:
            gs, occ, pos = clone_state(grid_state, occupancy, pos0)
            apply_move(gs, occ, pos, my_id, player_color, m)
            for t in range(self.sim_plies):
                pid = turn_order[(idx_me + 1 + t) % len(turn_order)]
                pc = player_colors_by_id[pid]
                mv = greedy_best_move(
                    self._rng,
                    gs,
                    occ,
                    pos,
                    pid,
                    pc,
                    player_colors_by_id,
                )
                if mv is None:
                    break
                apply_move(gs, occ, pos, pid, pc, mv)
            v = evaluate_territory(gs, player_colors_by_id, my_id)
            if v > best_v:
                best_v, best_m = v, m

        return best_m
