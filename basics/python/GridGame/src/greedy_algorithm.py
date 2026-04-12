from __future__ import annotations

import random

from computer_algorithm import ComputerAlgorithm
from simulation import greedy_best_move, positions_from_occupancy


class GreedyAlgorithm(ComputerAlgorithm):
    def __init__(self, seed: int | None = None) -> None:
        self._rng = random.Random(seed) if seed is not None else random.Random()

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
        my_id = occupancy[current_position[0]][current_position[1]]
        colors = player_colors_by_id or {my_id: player_color}
        pos = positions_from_occupancy(occupancy)
        pick = greedy_best_move(
            self._rng,
            grid_state,
            occupancy,
            pos,
            my_id,
            player_color,
            colors,
            rejected,
        )
        return pick if pick is not None else current_position
