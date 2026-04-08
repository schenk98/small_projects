import random
from computer_algorithm import ComputerAlgorithm
from collections import deque

class RandomMoveAlgorithm(ComputerAlgorithm):
    def get_next_move(self, grid_state, current_position):
        """Randomly choose empty spaces, or use BFS pathfinding to escape own color."""
        row, col = current_position
        my_color = grid_state[row][col]
        directions = [(-1, 0), (1, 0), (0, -1), (0, 1)]
        random.shuffle(directions)

        # 1. Try to move to an immediately adjacent empty space
        for dr, dc in directions:
            new_row, new_col = row + dr, col + dc
            if 0 <= new_row < len(grid_state) and 0 <= new_col < len(grid_state[0]):
                if grid_state[new_row][new_col] == "white":
                    return (new_row, new_col)

        # 2. If completely surrounded by own color, use BFS to find a path to the nearest white tile
        queue = deque([(row, col, [])])  # Stores (current_row, current_col, path_taken)
        visited = set()
        visited.add((row, col))

        while queue:
            curr_r, curr_c, path = queue.popleft()

            for dr, dc in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                nr, nc = curr_r + dr, curr_c + dc

                if 0 <= nr < len(grid_state) and 0 <= nc < len(grid_state[0]):
                    if (nr, nc) not in visited:
                        visited.add((nr, nc))
                        cell_color = grid_state[nr][nc]

                        if cell_color == "white":
                            # We found the closest white tile! Return the FIRST step of the path to get there.
                            if path:
                                return path[0]
                            else:
                                return (nr, nc) 
                        elif cell_color == my_color:
                            # We can walk through our own color, so add it to the search queue
                            queue.append((nr, nc, path + [(nr, nc)]))

        # 3. If trapped with NO reachable white tiles, just do a random legal move
        for dr, dc in directions:
            new_row, new_col = row + dr, col + dc
            if 0 <= new_row < len(grid_state) and 0 <= new_col < len(grid_state[0]):
                if grid_state[new_row][new_col] == my_color:
                    return (new_row, new_col)

        return current_position  # No valid move at all