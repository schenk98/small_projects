from computer_algorithm import ComputerAlgorithm

class UserActions(ComputerAlgorithm):
    def __init__(self):
        self.next_move = None

    def set_next_move(self, direction):
        """Set the next move based on user input."""
        self.next_move = direction
        print(f"[DEBUG] UserActions: set_next_move called with direction {direction}")

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
        """Return the next move set by the user (rejected_positions unused for human input)."""
        print(
            f"[DEBUG] UserActions: get_next_move next_move={self.next_move} pos={current_position} color={player_color}"
        )
        if self.next_move is None:
            return current_position

        row, col = current_position
        dr, dc = self.next_move
        new_row, new_col = row + dr, col + dc

        if 0 <= new_row < len(grid_state) and 0 <= new_col < len(grid_state[0]):
            target_color = grid_state[new_row][new_col]
            occ = occupancy[new_row][new_col]
            print(
                f"[DEBUG] UserActions: target ({new_row},{new_col}) color={target_color} occ={occ}"
            )
            if (target_color == "white" or target_color == player_color) and occ == 0:
                self.next_move = None
                print(f"[DEBUG] UserActions: Move to ({new_row}, {new_col}) is valid.")
                return (new_row, new_col)

        print(f"[DEBUG] UserActions: Move to ({new_row}, {new_col}) is invalid.")
        self.next_move = None
        return current_position