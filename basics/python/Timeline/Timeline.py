import json
import random
import os
import tkinter as tk
from tkinter import messagebox
from tkinter import ttk
from tkinter import filedialog
from PIL import Image, ImageTk

# Global variables
CONFIG = {}

def load_config(config_file):
    global CONFIG
    if not os.path.exists(config_file):
        print(f"Error: The config file '{config_file}' does not exist.")
        return
    try:
        with open(config_file, 'r', encoding='utf-8') as file:
            for line in file:
                key, value = line.strip().split('=')
                CONFIG[key] = value
    except Exception as e:
        print(f"Error: Failed to read the config file '{config_file}'. {e}")

class TimelineGame:
    def __init__(self, cards_file):
        self.cards = self.load_cards(cards_file)
        self.timeline = []
        self.player_hand = []
        if self.cards:
            self.initialize_game()

    def load_cards(self, cards_file):
        if not os.path.exists(cards_file):
            print(f"Error: The file '{cards_file}' does not exist.")
            return []
        try:
            with open(cards_file, 'r', encoding='utf-8') as file:
                cards = json.load(file)
                # Validate card data
                valid_cards = [card for card in cards if 'event' in card and 'year' in card and 'image' in card]
                if len(valid_cards) < len(cards):
                    print("Warning: Some cards were skipped due to missing values.")
                return valid_cards
        except json.JSONDecodeError:
            print(f"Error: The file '{cards_file}' is not a valid JSON file.")
            return []

    def initialize_game(self):
        # Shuffle the cards and draw the first card to start the timeline
        random.shuffle(self.cards)
        self.timeline.append(self.cards.pop())
        # Draw 3 cards for the player
        self.player_hand = [self.cards.pop() for _ in range(3)]

    def find_position_for_year(self, year):
        for i in range(len(self.timeline)):
            if year <= self.timeline[i]['year']:
                return i
        return len(self.timeline)

    def place_card(self, card_index, position):
        card = self.player_hand.pop(card_index)
        if position == 0:
            if card['year'] <= self.timeline[0]['year']:
                self.timeline.insert(0, card)
                return True, position
        elif position == len(self.timeline):
            if card['year'] >= self.timeline[-1]['year']:
                self.timeline.append(card)
                return True, position
        else:
            if self.timeline[position - 1]['year'] <= card['year'] <= self.timeline[position]['year']:
                self.timeline.insert(position, card)
                return True, position
        # If the card is placed incorrectly, find the correct position and place it there
        correct_position=0
        for i in range(len(self.timeline)):
            if card['year'] <= self.timeline[i]['year']:
                self.timeline.insert(i, card)
                correct_position = i
                break
        else:
            self.timeline.append(card)
        return False, correct_position

    def draw_card(self):
        if self.cards:
            self.player_hand.append(self.cards.pop())

class TimelineGameGUI:
    def __init__(self, root, game):
        self.game = game
        self.root = root
        self.root.title("Timeline Game")
        self.points = 0
        self.wrong = 0

        # Make the GUI window fullscreen but allow minimizing and closing
        self.root.state('zoomed')

        # Top left section for the player's input and stats
        self.input_frame = tk.Frame(self.root)
        self.input_frame.place(relx=0.05, rely=0.05, relwidth=0.3, relheight=0.3)

        self.year_entry = tk.Entry(self.input_frame, font=("Helvetica", 16))
        self.year_entry.grid(row=4, column=0, pady=5)
        self.year_entry.bind("<Return>", self.submit_year)

        self.submit_button = tk.Button(self.input_frame, text="OK", command=self.submit_year, state=tk.DISABLED, font=("Helvetica", 16))
        self.submit_button.grid(row=4, column=1, pady=5)

        self.load_button = tk.Button(self.input_frame, text="Vybrat karty", command=self.load_json_file, font=("Helvetica", 16))
        self.load_button.grid(row=0, column=0, pady=5, columnspan=2)

        self.export_button = tk.Button(self.input_frame, text="Export", command=self.export_stats, font=("Helvetica", 16))
        self.export_button.grid(row=0, column=2, pady=5)

        self.result_label = tk.Label(self.input_frame, text="", font=("Helvetica", 16))
        self.result_label.grid(row=1, column=0, columnspan=3, pady=5)

        self.points_label = tk.Label(self.input_frame, text=f"Body: {self.points}", font=("Helvetica", 16), fg="black")
        self.points_label.grid(row=2, column=0, columnspan=3, pady=5)
        self.wrong_label = tk.Label(self.input_frame, text=f"Chyby: {self.wrong}", font=("Helvetica", 16), fg="black")
        self.wrong_label.grid(row=3, column=0, columnspan=3, pady=5)

        # Bottom left section for the player's hand
        self.hand_frame = tk.Frame(self.root)
        self.hand_frame.place(relx=0.05, rely=0.35, relwidth=0.6, relheight=0.6)

        self.hand_buttons = []
        self.hand_images = []
        for i in range(3):
            button = tk.Button(self.hand_frame, text="", command=lambda i=i: self.select_card(i), wraplength=200, padx=5, pady=5, font=("Helvetica", 18), anchor="s", compound="top")
            button.place(relx=i*0.3, rely=0, relwidth=0.30, relheight=1, anchor="nw")
            self.hand_buttons.append(button)

        self.selected_card_index = None

        # Right section for the scrollable timeline
        self.timeline_frame = tk.Frame(self.root)
        self.timeline_frame.place(relx=0.65, rely=0.05, relwidth=0.3, relheight=0.9)

        self.timeline_canvas = tk.Canvas(self.timeline_frame)
        self.timeline_scrollbar = ttk.Scrollbar(self.timeline_frame, orient="vertical", command=self.timeline_canvas.yview)
        self.timeline_scrollable_frame = tk.Frame(self.timeline_canvas)

        self.timeline_scrollable_frame.bind(
            "<Configure>",
            lambda e: self.timeline_canvas.configure(
                scrollregion=self.timeline_canvas.bbox("all")
            )
        )

        self.timeline_canvas.create_window((0, 0), window=self.timeline_scrollable_frame, anchor="nw")
        self.timeline_canvas.configure(yscrollcommand=self.timeline_scrollbar.set)

        self.timeline_canvas.pack(side="left", fill="both", expand=True)
        self.timeline_scrollbar.pack(side="right", fill="y")

        # Enable mouse wheel scrolling
        self.timeline_canvas.bind_all("<MouseWheel>", self._on_mousewheel)

        self.update_gui()

    def _on_mousewheel(self, event):
        self.timeline_canvas.yview_scroll(int(-1*(event.delta/120)), "units")

    def update_gui(self):
        for widget in self.timeline_scrollable_frame.winfo_children():
            widget.destroy()

        tk.Label(self.timeline_scrollable_frame, text="Timeline:", font=("Helvetica", 24)).pack()
        for card in self.game.timeline:
            tk.Label(self.timeline_scrollable_frame, text=f"{card['event']} ({card['year']})", anchor="center", font=("Helvetica", 20), wraplength=self.timeline_frame.winfo_width() - 20, fg="black").pack(fill="x")

        self.root.after(100, self.update_hand_buttons)

    def update_hand_buttons(self):
        for i, card in enumerate(self.game.player_hand):
            image = Image.open(card['image'])
            image = image.resize((int(self.hand_buttons[i].winfo_width()), int(self.hand_buttons[i].winfo_height() * 0.8)), Image.Resampling.LANCZOS)
            photo = ImageTk.PhotoImage(image)
            self.hand_images.append(photo)  # Keep a reference to avoid garbage collection
            self.hand_buttons[i].config(image=photo, text=card['event'], compound="top", anchor="n")

        # Set button text to empty string if there are no more events
        for i in range(len(self.game.player_hand), 3):
            self.hand_buttons[i].config(image='', text="", bg="SystemButtonFace")

        self.result_label.config(text="")
        self.submit_button.config(state=tk.DISABLED)

    def select_card(self, index):
        # Unselect all cards
        for button in self.hand_buttons:
            button.config(bg="SystemButtonFace")
        
        # Select the clicked card
        self.selected_card_index = index
        self.hand_buttons[index].config(bg="lightblue")
        self.submit_button.config(state=tk.NORMAL)
        self.year_entry.focus_set()  # Set focus to the text field

    def submit_year(self, event=None):
        if self.selected_card_index is None:
            messagebox.showwarning("Upozornění", "Vyber kartu.")
            return

        try:
            year = int(self.year_entry.get())
            position = self.game.find_position_for_year(year)
            card = self.game.player_hand[self.selected_card_index]
            exact_year = card['year'] == year
            correct, correct_position = self.game.place_card(self.selected_card_index, position)
            if correct:
                if exact_year:
                    self.points += int(CONFIG.get('points_per_exact_year', 3))
                else:
                    self.points += int(CONFIG.get('points_per_correct_answer', 1))
                #self.result_label.config(text="Správně!", fg="green")
                self.points_label.config(fg="green")
                self.wrong_label.config(fg="green")
            else:
                self.wrong += 1
                #self.result_label.config(text="Chyba!", fg="red")
                self.wrong_label.config(fg="red")
                self.points_label.config(fg="red")

            self.points_label.config(text=f"Body: {self.points}")
            self.wrong_label.config(text=f"Chyby: {self.wrong}")
            self.game.draw_card()
            self.selected_card_index = None
            self.year_entry.delete(0, tk.END)
            self.update_gui()

            # Unselect all cards after submission
            for button in self.hand_buttons:
                button.config(bg="SystemButtonFace")

            # Update the timeline to color the last added card
            self.update_timeline(correct, exact_year, correct_position)

        except ValueError:            
            messagebox.showwarning("Upozornění", "Zadej platný rok.")

    def update_timeline(self, correct, exact_year, position):
        for widget in self.timeline_scrollable_frame.winfo_children():
            widget.destroy()

        tk.Label(self.timeline_scrollable_frame, text="Timeline:", font=("Helvetica", 24)).pack()
        for i, card in enumerate(self.game.timeline):
            color = "black"
            if i == position:
                color = "green" if correct else "red"
            tk.Label(self.timeline_scrollable_frame, text=f"{card['event']} ({card['year']})", anchor="center", font=("Helvetica", 20), wraplength=self.timeline_frame.winfo_width() - 20, fg=color).pack(fill="x")

    def load_json_file(self):
        # Open a file dialog to select a JSON file
        file_path = filedialog.askopenfilename(title="Select a JSON File", filetypes=[("JSON Files", "*.json")])
        if file_path:
            # Reload the game with the new file
            self.game = TimelineGame(file_path)
            self.update_gui()

    def export_stats(self):
        stats = {
            "points": self.points,
            "wrong": self.wrong,
            "timeline": self.game.timeline
        }
        file_path = filedialog.asksaveasfilename(defaultextension=".json", filetypes=[("JSON Files", "*.json")])
        if file_path:
            with open(file_path, 'w', encoding='utf-8') as file:
                json.dump(stats, file, ensure_ascii=False, indent=4)
            messagebox.showinfo("Export", "Statistiky byly úspěšně exportovány.")

def main():
    # Load configuration
    load_config('../config.txt')
    #load_config('Mini_projects/basics/python/Timeline/config.txt')
    root = tk.Tk()
    print("Current working directory:", os.getcwd())
    game = TimelineGame(CONFIG['cards_file'])
    gui = TimelineGameGUI(root, game)
    root.mainloop()

if __name__ == "__main__":
    main()