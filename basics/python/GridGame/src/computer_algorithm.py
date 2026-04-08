from abc import ABC, abstractmethod

class ComputerAlgorithm(ABC):
    @abstractmethod
    def get_next_move(self, grid_state, current_position):
        """Determine the next move for the computer player."""
        pass