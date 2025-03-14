import tkinter as tk
from tkinter import ttk

class ClickerGame:
    def __init__(self, root):
        self.root = root
        self.root.title("Clicker Game")
        self.root.geometry("800x600")
        
        self.score = 999
        self.points_per_click = 1
        self.auto_clicks = [0, 0, 0, 0, 0, 0]  # Different levels of auto clicks
        
        self.score_label = tk.Label(root, text=f"Score: {self.score}", font=("Arial", 24))
        self.score_label.pack(pady=20)
        
        self.click_button = tk.Button(root, text="Click me!", font=("Arial", 24), command=self.click)
        self.click_button.pack(pady=20)
        
        self.upgrade_frame = tk.Frame(root)
        self.upgrade_frame.pack(pady=20, fill=tk.BOTH, expand=True)
        
        self.canvas = tk.Canvas(self.upgrade_frame)
        self.scrollbar = ttk.Scrollbar(self.upgrade_frame, orient="vertical", command=self.canvas.yview)
        self.scrollable_frame = tk.Frame(self.canvas)
        
        self.scrollable_frame.bind(
            "<Configure>",
            lambda e: self.canvas.configure(
                scrollregion=self.canvas.bbox("all")
            )
        )
        
        self.canvas.create_window((0, 0), window=self.scrollable_frame, anchor="nw")
        self.canvas.configure(yscrollcommand=self.scrollbar.set)
        
        self.canvas.pack(side="left", fill="both", expand=True)
        self.scrollbar.pack(side="right", fill="y")
        
        self.canvas.bind_all("<MouseWheel>", self._on_mousewheel)
        
        self.create_upgrade("Upgrade Click", self.get_upgrade_click_cost, self.upgrade_click)
        self.create_upgrade("Auto Click (1 point/sec)", lambda: self.get_upgrade_auto_cost(0), lambda: self.upgrade_auto(0))
        self.create_upgrade("Auto Click (2 points/sec)", lambda: self.get_upgrade_auto_cost(1), lambda: self.upgrade_auto(1))
        self.create_upgrade("Auto Click (3 points/sec)", lambda: self.get_upgrade_auto_cost(2), lambda: self.upgrade_auto(2))
        self.create_upgrade("Auto Click (5 points/sec)", lambda: self.get_upgrade_auto_cost(3), lambda: self.upgrade_auto(3))
        self.create_upgrade("Auto Click (10 points/sec)", lambda: self.get_upgrade_auto_cost(4), lambda: self.upgrade_auto(4))
        self.create_upgrade("Auto Click (20 points/sec)", lambda: self.get_upgrade_auto_cost(5), lambda: self.upgrade_auto(5))
        
        self.root.after(1000, self.auto_click)
        
    def _on_mousewheel(self, event):
        self.canvas.yview_scroll(int(-1*(event.delta/120)), "units")
        
    def create_upgrade(self, name, cost_func, command):
        frame = tk.Frame(self.scrollable_frame)
        frame.pack(fill="x", pady=5)
        
        label = tk.Label(frame, text=name, font=("Arial", 18))
        label.pack(side="left", padx=10)
        
        button = tk.Button(frame, text=f"Cost: {cost_func()}", font=("Arial", 18), command=lambda: self.upgrade(command, button, cost_func))
        button.pack(side="right", padx=10)
        
        return button
        
    def click(self):
        self.score += self.points_per_click
        self.update_score()
        
    def upgrade(self, command, button, cost_func):
        command()
        self.update_upgrades()
        button.config(text=f"Cost: {cost_func()}")
        
    def upgrade_click(self):
        cost = self.get_upgrade_click_cost()
        if self.score >= cost:
            self.score -= cost
            self.points_per_click += 1
            self.update_score()
        
    def upgrade_auto(self, level):
        cost = self.get_upgrade_auto_cost(level)
        if self.score >= cost:
            self.score -= cost
            self.auto_clicks[level] += 1
            self.update_score()
        
    def auto_click(self):
        self.score += sum([(i + 1) * self.auto_clicks[i] for i in range(len(self.auto_clicks))])
        self.update_score()
        self.root.after(1000, self.auto_click)
        
    def update_score(self):
        self.score_label.config(text=f"Score: {self.score}")
        
    def update_upgrades(self):
        for widget in self.scrollable_frame.winfo_children():
            if isinstance(widget, tk.Button):
                if "Click" in widget.cget("text"):
                    widget.config(text=f"Cost: {self.get_upgrade_click_cost()}")
                elif "Auto Click (1 point/sec)" in widget.cget("text"):
                    widget.config(text=f"Cost: {self.get_upgrade_auto_cost(0)}")
                elif "Auto Click (2 points/sec)" in widget.cget("text"):
                    widget.config(text=f"Cost: {self.get_upgrade_auto_cost(1)}")
                elif "Auto Click (3 points/sec)" in widget.cget("text"):
                    widget.config(text=f"Cost: {self.get_upgrade_auto_cost(2)}")
                elif "Auto Click (5 points/sec)" in widget.cget("text"):
                    widget.config(text=f"Cost: {self.get_upgrade_auto_cost(3)}")
                elif "Auto Click (10 points/sec)" in widget.cget("text"):
                    widget.config(text=f"Cost: {self.get_upgrade_auto_cost(4)}")
                elif "Auto Click (20 points/sec)" in widget.cget("text"):
                    widget.config(text=f"Cost: {self.get_upgrade_auto_cost(5)}")
        
    def get_upgrade_click_cost(self):
        return 10 * self.points_per_click
    
    def get_upgrade_auto_cost(self, level):
        return 50 * (self.auto_clicks[level] + 1) * (level + 1)

if __name__ == "__main__":
    root = tk.Tk()
    game = ClickerGame(root)
    root.mainloop()