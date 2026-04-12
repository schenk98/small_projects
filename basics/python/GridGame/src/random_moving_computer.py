import random
from computer_algorithm import ComputerAlgorithm
from collections import deque


class RandomMoveAlgorithm(ComputerAlgorithm):
    def __init__(self, seed: int | None = None) -> None:
        self._rng = random.Random(seed) if seed is not None else random.Random()

    def get_next_move(
        self,
        grid_state,
        occupancy,
        current_position,
        player_color,
        rejected_positions=None,
        *,
        player_colors_by_id=None,
        turn_order=None,
    ):
        """Randomly choose empty spaces, or use BFS pathfinding to escape own color."""
        rejected = rejected_positions or frozenset()
        for candidate in self._candidate_moves(
            grid_state, occupancy, current_position, player_color
        ):
            if candidate not in rejected:
                return candidate
        return current_position

    @staticmethod
    def _can_enter_cell(occupancy, row: int, col: int) -> bool:
        """Distinct destination cell must be empty (no other pawn)."""
        return occupancy[row][col] == 0

    def _step_ok(
        self,
        grid_state,
        occupancy,
        player_color: str,
        src: tuple[int, int],
        dst: tuple[int, int],
    ) -> bool:
        sr, sc = src
        dr, dc = dst
        if abs(dr - sr) + abs(dc - sc) != 1:
            return False
        h, w = len(grid_state), len(grid_state[0])
        if not (0 <= dr < h and 0 <= dc < w):
            return False
        tcol = grid_state[dr][dc]
        if tcol != "white" and tcol != player_color:
            return False
        return self._can_enter_cell(occupancy, dr, dc)

    def _candidate_moves(
        self,
        grid_state,
        occupancy,
        current_position,
        player_color,
    ):
        row, col = current_position
        directions = [(-1, 0), (1, 0), (0, -1), (0, 1)]
        self._rng.shuffle(directions)

        for dr, dc in directions:
            nr, nc = row + dr, col + dc
            if 0 <= nr < len(grid_state) and 0 <= nc < len(grid_state[0]):
                if grid_state[nr][nc] == "white" and self._can_enter_cell(occupancy, nr, nc):
                    yield (nr, nc)

        queue = deque([(row, col, [])])
        visited = {(row, col)}

        while queue:
            curr_r, curr_c, path = queue.popleft()

            for dr, dc in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                nr, nc = curr_r + dr, curr_c + dc

                if 0 <= nr < len(grid_state) and 0 <= nc < len(grid_state[0]):
                    if (nr, nc) in visited:
                        continue
                    if not self._step_ok(
                        grid_state,
                        occupancy,
                        player_color,
                        (curr_r, curr_c),
                        (nr, nc),
                    ):
                        continue
                    visited.add((nr, nc))
                    cell_color = grid_state[nr][nc]

                    if cell_color == "white":
                        if path:
                            yield path[0]
                        else:
                            yield (nr, nc)
                        return
                    if cell_color == player_color:
                        queue.append((nr, nc, path + [(nr, nc)]))

        for dr, dc in directions:
            nr, nc = row + dr, col + dc
            if 0 <= nr < len(grid_state) and 0 <= nc < len(grid_state[0]):
                if grid_state[nr][nc] == player_color and self._can_enter_cell(occupancy, nr, nc):
                    yield (nr, nc)
