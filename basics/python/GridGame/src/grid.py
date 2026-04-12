import tkinter as tk
from cell import Cell

class Grid:
    def __init__(self, size, cell_size=20):
        self.size = size
        self.cell_size = cell_size
        self.canvas = None
        self.cells = []

    def setup_canvas(self, parent):
        """Setup the canvas for the grid."""
        width = self.size[0] * self.cell_size
        height = self.size[1] * self.cell_size
        self.canvas = tk.Canvas(parent, width=width, height=height)
        self.canvas.pack(fill="both", expand=True)

        for row in range(self.size[1]):
            row_cells = []
            for col in range(self.size[0]):
                cell = Cell((row, col))
                x0, y0, x1, y1 = self.get_cell_coords(row, col)
                cell_id = self.canvas.create_rectangle(
                    x0, y0, x1, y1,
                    fill=cell.color, outline="black"
                )
                cell.canvas_id = cell_id
                row_cells.append(cell)
            self.cells.append(row_cells)

    def place_player(self, player, position):
        """Place a player on the grid at the specified position."""
        row, col = position
        cell = self.cells[row][col]
        if not cell.is_occupied():
            cell.set_player(player.id, player.color)
            self.update_cell_visual(cell)
            player.position = position

    def move_player(self, player, new_position):
        """Move a player to a new position."""
        old_row, old_col = player.position
        old_cell = self.cells[old_row][old_col]
        old_cell.remove_player()
        self.update_cell_visual(old_cell)

        new_row, new_col = new_position
        new_cell = self.cells[new_row][new_col]
        new_cell.set_player(player.id, player.color)
        self.update_cell_visual(new_cell)

        player.position = new_position

    def update_cell_visual(self, cell):
        """Update the visual representation of a cell."""
        self.canvas.itemconfig(cell.canvas_id, fill=cell.color)
        if cell.is_occupied():
            self.canvas.itemconfig(cell.canvas_id, stipple="gray50")
        else:
            self.canvas.itemconfig(cell.canvas_id, stipple="")

    def get_state(self):
        """Get the current state of the grid."""
        state = []
        for row in self.cells:
            state_row = []
            for cell in row:
                state_row.append(cell.color)
            state.append(state_row)
        return state

    def get_occupancy(self):
        """Parallel to get_state: occupant player id per cell (0 = empty)."""
        occ = []
        for row in self.cells:
            occ.append([cell.player for cell in row])
        return occ

    def update_cell(self, position, color):
        """Update the color of a cell at the given position."""
        row, col = position
        cell = self.cells[row][col]
        cell.color = color
        self.update_cell_visual(cell)

    def get_cell_coords(self, row, col):
        """Calculate and return the coordinates of a cell."""
        x0 = col * self.cell_size
        y0 = row * self.cell_size
        x1 = (col + 1) * self.cell_size
        y1 = (row + 1) * self.cell_size
        return x0, y0, x1, y1

    def get_cell_owner(self, row, col):
        """Get the owner of the cell at the specified position."""
        return self.cells[row][col].player