from __future__ import annotations

from abc import ABC, abstractmethod


class ComputerAlgorithm(ABC):
    @abstractmethod
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
        """
        Propose a destination cell (row, col) for this turn.

        grid_state: colors per cell (see README). occupancy: same shape, player id or 0.
        player_color: moving player's territory color (may differ from cell underfoot on white).
        rejected_positions: engine-blacklisted coordinates for this turn; avoid repeating them
        when any alternative exists.
        player_colors_by_id / turn_order: optional multi-agent context (GUI passes on each call).
        """
        pass