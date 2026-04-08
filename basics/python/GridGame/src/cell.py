class Cell:
    def __init__(self, position):
        self.position = position  # (row, col)
        self.player = 0  # 0 means no player, 1-4 for players
        self.color = "white"  # Default color

    def set_player(self, player_id, color):
        """Set the player occupying this cell."""
        self.player = player_id
        self.color = color

    def remove_player(self):
        """Remove the player from this cell."""
        self.player = 0

    def is_occupied(self):
        """Check if the cell is occupied by a player."""
        return self.player != 0