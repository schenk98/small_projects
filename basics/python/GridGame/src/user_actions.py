from computer_algorithm import ComputerAlgorithm

class UserActions(ComputerAlgorithm):
    def __init__(self):
        self.next_move = None

    def set_next_move(self, direction):
        """Set the next move based on user input."""
        self.next_move = direction
        print(f"[DEBUG] UserActions: set_next_move called with direction {direction}")

    def get_next_move(self, grid_state, current_position):
        """Return the next move set by the user."""
        print(f"[DEBUG] UserActions: get_next_move called with next_move {self.next_move} and current_position {current_position}")
        if self.next_move is None:
            return current_position  # No move set, stay in place

        row, col = current_position
        dr, dc = self.next_move
        new_row, new_col = row + dr, col + dc
        
        # Grab the player's actual color from the grid
        my_color = grid_state[row][col]

        # Validate the move
        if 0 <= new_row < len(grid_state) and 0 <= new_col < len(grid_state[0]):
            target_color = grid_state[new_row][new_col]
            print(f"[DEBUG] UserActions: Validating move to ({new_row}, {new_col}) with target_color {target_color}")
            
            # Compare the target cell's color to the player's color
            if target_color == "white" or target_color == my_color:
                self.next_move = None  # Reset after a valid move
                print(f"[DEBUG] UserActions: Move to ({new_row}, {new_col}) is valid.")
                return (new_row, new_col)

        print(f"[DEBUG] UserActions: Move to ({new_row}, {new_col}) is invalid.")
        self.next_move = None  # Reset if the move is invalid
        return current_position