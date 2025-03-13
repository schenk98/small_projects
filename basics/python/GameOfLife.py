import tkinter as tk
from tkinter import ttk
import random

class GameOfLife:
    def __init__(self, root):
        self.root = root
        self.root.title("Game of Life")
        
        self.canvas = tk.Canvas(root, width=800, height=600, bg="white")
        self.canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        
        self.controls_frame = tk.Frame(root)
        self.controls_frame.pack(side=tk.RIGHT, fill=tk.Y)
        
        self.play_button = tk.Button(self.controls_frame, text="Play/Pause", command=self.toggle_play)
        self.play_button.pack(pady=10)
        
        self.reset_button = tk.Button(self.controls_frame, text="Reset", command=self.reset_game)
        self.reset_button.pack(pady=10)
        
        self.randomize_button = tk.Button(self.controls_frame, text="Randomize", command=self.randomize_grid)
        self.randomize_button.pack(pady=10)
        
        self.speed_label = tk.Label(self.controls_frame, text="Speed:")
        self.speed_label.pack(pady=10)
        
        self.speed_slider = tk.Scale(self.controls_frame, from_=1, to=10, orient=tk.HORIZONTAL)
        self.speed_slider.set(5)
        self.speed_slider.pack(pady=10)
        
        self.playing = False
        self.cell_size = 10
        self.grid_width = 80
        self.grid_height = 60
        self.grid = {}
        
        self.canvas.bind("<Button-1>", self.toggle_cell)
        self.root.bind("<MouseWheel>", self.zoom)
        self.canvas.bind("<B3-Motion>", self.pan)
        self.canvas.bind("<Button-3>", self.start_pan)
        
        self.offset_x = 0
        self.offset_y = 0
        self.zoom_level = 1
        self.pan_start_x = 0
        self.pan_start_y = 0
        
        self.draw_grid()
        
    def draw_grid(self):
        self.canvas.delete(tk.ALL)
        for y in range(self.grid_height):
            for x in range(self.grid_width):
                cell_x = x * self.cell_size + self.offset_x
                cell_y = y * self.cell_size + self.offset_y
                if (x, y) in self.grid and self.grid[(x, y)] == 1:
                    color = "black"
                else:
                    color = "white"
                self.canvas.create_rectangle(
                    cell_x,
                    cell_y,
                    cell_x + self.cell_size,
                    cell_y + self.cell_size,
                    fill=color,
                    outline="gray"
                )
                
    def toggle_cell(self, event):
        x = (event.x - self.offset_x) // self.cell_size
        y = (event.y - self.offset_y) // self.cell_size
        if (x, y) in self.grid and self.grid[(x, y)] == 1:
            self.grid[(x, y)] = 0
        else:
            self.grid[(x, y)] = 1
        self.draw_grid()
            
    def zoom(self, event):
        if event.delta > 0:
            self.cell_size += 1
        elif event.delta < 0 and self.cell_size > 1:
            self.cell_size -= 1
        self.draw_grid()
        
    def start_pan(self, event):
        self.pan_start_x = event.x
        self.pan_start_y = event.y
        
    def pan(self, event):
        dx = event.x - self.pan_start_x
        dy = event.y - self.pan_start_y
        self.offset_x += dx
        self.offset_y += dy
        self.pan_start_x = event.x
        self.pan_start_y = event.y
        self.draw_grid()
        
    def toggle_play(self):
        self.playing = not self.playing
        if self.playing:
            self.run_simulation()
            
    def run_simulation(self):
        if self.playing:
            self.update_grid()
            self.draw_grid()
            self.root.after(1000 // self.speed_slider.get(), self.run_simulation)
            
    def update_grid(self):
        new_grid = {}
        for y in range(self.grid_height):
            for x in range(self.grid_width):
                state = self.grid.get((x, y), 0)
                live_neighbors = self.count_live_neighbors(x, y)
                if state == 1:
                    if live_neighbors < 2 or live_neighbors > 3:
                        new_grid[(x, y)] = 0
                    else:
                        new_grid[(x, y)] = 1
                else:
                    if live_neighbors == 3:
                        new_grid[(x, y)] = 1
        self.grid = new_grid
        
    def count_live_neighbors(self, x, y):
        count = 0
        for dy in [-1, 0, 1]:
            for dx in [-1, 0, 1]:
                if dx == 0 and dy == 0:
                    continue
                nx, ny = x + dx, y + dy
                if self.grid.get((nx, ny), 0) == 1:
                    count += 1
        return count
    
    def reset_game(self):
        self.grid = {}
        self.draw_grid()
        
    def randomize_grid(self):
        self.grid = {}
        for y in range(self.grid_height):
            for x in range(self.grid_width):
                if random.random() < 0.3:
                    self.grid[(x, y)] = 1
        self.draw_grid()

if __name__ == "__main__":
    root = tk.Tk()
    game = GameOfLife(root)
    root.mainloop()