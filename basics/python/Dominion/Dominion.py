import json
import random
import tkinter as tk
import ttkbootstrap as ttk
from ttkbootstrap.constants import *
from tkinter import messagebox, simpledialog, Text, END
from Card import Card
from Player import Player
from datetime import datetime  # Import datetime module

class Game:
    def __init__(self, root, player_name):
        self.root = root
        self.log_text = Text(self.root, height=20, width=30)  # Initialize log_text here
        self.end_already = False
        self.workshop_active = False
        self.chapel_active = False
        self.remodel_active = False
        self.remodel_gain_active = False
        self.supply = []
        self.trash = []
        self.log("Initializing game", "INFO")
        self.load_config()
        self.load_cards()
        self.player = Player(player_name)
        self.setup_game()
        self.create_gui()


    def load_config(self):
        self.log("Loading configuration", "INFO")
        with open("Mini_projects/basics/python/Dominion/config.json", "r") as file:
            self.config = json.load(file)

    def load_cards(self):
        self.log("Loading cards", "INFO")
        with open("Mini_projects/basics/python/Dominion/cards.json", "r") as file:
            cards_data = json.load(file)
            for card_data in cards_data:
                if card_data["name"] in self.config["cards_in_game"]:
                    card = Card(card_data["name"], card_data["cost"], card_data["type"], card_data["description"], card_data["quantity"])
                    self.supply.append(card)

    def setup_game(self):
        self.log("Setting up game", "INFO")
        starting_cards = self.config["starting_cards"]
        with open("Mini_projects/basics/python/Dominion/cards.json", "r") as file:
            cards_data = json.load(file)
            for starting_card in starting_cards:
                for card_data in cards_data:
                    if card_data["name"] == starting_card:
                        card = Card(card_data["name"], card_data["cost"], card_data["type"], card_data["description"], card_data["quantity"])
                        self.player.deck.append(card)
        random.shuffle(self.player.deck)
        self.player.draw_hand()

    def create_gui(self):
        self.log("Creating GUI", "INFO")
        self.root.title("Dominion")
        self.root.geometry("1200x900")

        self.main_frame = ttk.Frame(self.root)
        self.main_frame.pack(fill=tk.BOTH, expand=True)

        self.main_frame.columnconfigure(0, weight=2)  
        self.main_frame.columnconfigure(1, weight=1)  
        self.main_frame.rowconfigure(0, weight=1)     
        self.main_frame.rowconfigure(1, weight=1)     

        self.shop_frame = ttk.Frame(self.main_frame)
        self.shop_frame.grid(row=0, column=0, sticky="nsew")

        self.hand_frame = ttk.Frame(self.main_frame)
        self.hand_frame.grid(row=1, column=0, sticky="nsew")

        self.info_frame = ttk.Frame(self.main_frame)
        self.info_frame.grid(row=0, column=1, rowspan=2, sticky="nsew")

        self.supply_frame = ttk.Frame(self.shop_frame)
        self.supply_frame.pack(fill=tk.BOTH, expand=True)

        self.end_game_button = ttk.Button(self.info_frame, text="End Game", command=self.end_game, bootstyle=INFO)
        self.end_game_button.pack(side=tk.BOTTOM, anchor="s", pady=5)

        self.deck_frame = ttk.Frame(self.info_frame)
        self.deck_frame.pack(side=tk.TOP, fill=tk.X)

        self.actions_label = ttk.Label(self.info_frame, text=f"Actions: {self.player.actions}", font=("Helvetica", 16, "bold"))
        self.actions_label.pack(side=tk.BOTTOM, anchor="s", pady=5)

        self.buys_label = ttk.Label(self.info_frame, text=f"Buys: {self.player.buys}", font=("Helvetica", 16, "bold"))
        self.buys_label.pack(side=tk.BOTTOM, anchor="s", pady=5)

        self.coins_label = ttk.Label(self.info_frame, text=f"Coins: {self.player.coins}", font=("Helvetica", 16, "bold"))
        self.coins_label.pack(side=tk.BOTTOM, anchor="s", pady=5)

        self.special_action_label = ttk.Label(self.hand_frame, text="", font=("Helvetica", 16, "bold"))
        self.special_action_label.pack(side=tk.TOP, pady=10)

        self.log_text = Text(self.info_frame, height=20, width=30)
        self.log_text.pack(side=tk.BOTTOM, anchor="s", pady=5)

        self.update_gui()

    def update_gui(self):
        self.log("Updating GUI", "INFO")
        for widget in self.hand_frame.winfo_children():
            if widget != self.special_action_label:
                widget.destroy()
        for widget in self.supply_frame.winfo_children():
            widget.destroy()
        for widget in self.deck_frame.winfo_children():
            widget.destroy()

        for card in self.player.hand:
            description = "\n".join([f"{key}: {value}" for key, value in card.description.items()])
            if self.remodel_active:
                button = tk.Button(self.hand_frame, text=f"{{{card.cost}}} {card.name} \n{description}", command=lambda c=card: self.select_card_for_remodel(c), width=15, height=10, wraplength=100, justify="center")
            elif self.chapel_active:
                button = tk.Button(self.hand_frame, text=f"{{{card.cost}}} {card.name} \n{description}", command=lambda c=card: self.select_card_for_chapel(c), width=15, height=10, wraplength=100, justify="center")
            else:
                button = tk.Button(self.hand_frame, text=f"{{{card.cost}}} {card.name} \n{description}", command=lambda c=card: self.play_card(c.name), width=15, height=10, wraplength=100, justify="center")
            if card.card_type == "Action":
                button.config(bg="white", fg="black", font=("Helvetica", 10, "bold"))
            elif card.card_type == "Treasure":
                button.config(bg="yellow", fg="black", font=("Helvetica", 10, "bold"))
            elif card.card_type == "Victory":
                button.config(bg="green", fg="black", font=("Helvetica", 10, "bold"))
            button.pack(side=tk.LEFT, padx=10, pady=10)

        for i, card in enumerate(self.supply):
            if card.quantity > 0:
                description = "\n".join([f"{key}: {value}" for key, value in card.description.items()])
                button = tk.Button(self.supply_frame, text=f"{{{card.cost}}} {card.name} (#{card.quantity})\n{description}", command=lambda c=card: self.buy_card(c), width=15, height=10, wraplength=100, justify="center")
                if card.card_type == "Action":
                    button.config(bg="white", fg="black", font=("Helvetica", 10, "bold"))
                elif card.card_type == "Treasure":
                    button.config(bg="yellow", fg="black", font=("Helvetica", 10, "bold"))
                elif card.card_type == "Victory":
                    button.config(bg="green", fg="black", font=("Helvetica", 10, "bold"))
                button.grid(row=i // 6, column=i % 6, padx=10, pady=10)
            else:
                button = tk.Button(self.supply_frame, text="Sold Out", state=tk.DISABLED, width=15, height=10, wraplength=100, justify="center")
                button.grid(row=i // 6, column=i % 6, padx=10, pady=10)

        trash_button = tk.Button(self.supply_frame, text="Trash", command=self.show_trash, width=15, height=10, wraplength=100, justify="center", bg="brown", fg="black", font=("Helvetica", 10, "bold"))
        trash_button.grid(row=2, column=5, padx=10, pady=10)

        deck_button = tk.Button(self.deck_frame, text="Deck", command=self.show_deck, width=15, height=10, wraplength=100, justify="center", bg="brown", fg="black", font=("Helvetica", 10, "bold"))
        deck_button.pack(side=tk.BOTTOM, padx=10, pady=10)

        discard_button = tk.Button(self.deck_frame, text="Discard", command=self.show_discard, width=15, height=10, wraplength=100, justify="center", bg="brown", fg="black", font=("Helvetica", 10, "bold"))
        discard_button.pack(side=tk.BOTTOM, padx=10, pady=10)

        self.actions_label.config(text=f"Actions: {self.player.actions}")
        self.buys_label.config(text=f"Buys: {self.player.buys}")
        self.coins_label.config(text=f"Coins: {self.player.coins}")

        self.end_turn_button = ttk.Button(self.info_frame, text="End Turn", command=self.end_turn, bootstyle=SUCCESS)
        self.end_turn_button.pack(side=tk.BOTTOM, anchor="s", pady=5)

    def log(self, message, level="INFO"):
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")  # Get current timestamp
        log_message = f"[{timestamp}] [{level}] {message}"
        print(log_message)
        self.log_text.insert(END, log_message + "\n")
        self.log_text.see(END)

    def show_trash(self):
        self.log("Showing trash", "INFO")
        messagebox.showinfo("Trash", "\n".join(str(card) for card in self.trash))

    def play_card(self, card_name):
        self.log(f"Attempting to play card: {card_name}", "INFO")
        if self.workshop_active or self.remodel_active or self.chapel_active:
            self.special_action_label.config(text="You must complete the current action before playing another card.")
            return
        special_action = self.player.play_card(card_name)
        if isinstance(special_action, str):
            self.special_action_label.config(text=special_action)
        self.log(f"Player played {card_name}", "INFO")
        if special_action:
            self.special_action(special_action, card_name)
        else:
            self.special_action_label.config(text="")
        self.update_gui()

    def special_action(self, action, card_name):
        self.log(f"Performing special action: {action} for card: {card_name}", "INFO")
        if action == "workshop":
            self.special_action_label.config(text="Gain a card costing up to 4")
            self.workshop_action(card_name)
        elif action == "remodel":
            self.special_action_label.config(text="Trash a card from your hand and gain a card costing up to 2 more")
            self.remodel_action(card_name)
        elif action == "chapel":
            self.special_action_label.config(text="Trash up to 4 cards from your hand")
            self.chapel_action()

    def chapel_action(self):
        self.log("Starting chapel action", "INFO")
        self.chapel_active = True
        self.chapel_counter = 0
        self.update_gui()

    def select_card_for_chapel(self, card):
        if self.chapel_active and self.chapel_counter < 4:
            self.log(f"Trashing card: {card.name}", "INFO")
            self.player.hand.remove(card)
            self.trash.append(card)
            self.chapel_counter += 1
            self.special_action_label.config(text=f"Trashed {self.chapel_counter} cards. You can trash {4 - self.chapel_counter} more.")
            if self.chapel_counter >= 4:
                self.end_chapel_action()
            self.update_gui()

    def end_chapel_action(self):
        self.log("Ending chapel action", "INFO")
        self.chapel_active = False
        self.special_action_label.config(text="")
        self.player.discard_pile.append(Card("Chapel", 2, "Action", {"special": "chapel", "effect": "Trash up to 4 cards"}, 10))
        self.update_gui()

    def remodel_action(self, card_name):
        self.log(f"Starting remodel action for card: {card_name}", "INFO")
        self.remodel_active = True
        self.remodel_card_name = card_name
        self.special_action_label.config(text="Select a card from your hand to trash")

    def workshop_action(self, card_name):
        self.log(f"Starting workshop action for card: {card_name}", "INFO")
        self.workshop_active = True
        self.workshop_card_name = card_name
        self.special_action_label.config(text="Select a card from shop that costs 4 or less")

    def select_card_for_remodel(self, card):
        self.log(f"Selecting card for remodel: {card.name}", "INFO")
        self.player.hand.remove(card)
        self.trash.append(card)
        self.log(f"Player trashed {card.name}", "INFO")
        self.special_action_label.config(text="Select a card to gain with cost up to " + str(card.cost + 2))
        self.remodel_card_cost = card.cost
        self.remodel_active = False
        self.remodel_gain_active = True
        self.update_gui()

    def buy_card(self, card):
        self.log(f"Attempting to buy card: {card.name}", "INFO")
        if self.chapel_active:
            self.end_chapel_action()
        if self.workshop_active:
            if card.cost <= 4 and card.quantity > 0:
                self.player.discard_pile.append(Card(card.name, card.cost, card.card_type, card.description, card.quantity))
                self.player.discard_pile.append(Card(self.workshop_card_name, 3, "Action", {"special": "workshop", "effect": "Gain a card costing up to 4"}, 10))
                card.quantity -= 1
                self.workshop_active = False
                self.special_action_label.config(text="")
                self.log(f"Player gained {card.name} using Workshop", "INFO")
                self.update_gui()
        elif self.remodel_gain_active:
            if card.cost <= self.remodel_card_cost + 2 and card.quantity > 0:
                self.player.discard_pile.append(Card(card.name, card.cost, card.card_type, card.description, card.quantity))
                self.player.discard_pile.append(Card(self.remodel_card_name, 4, "Action", {"special": "remodel", "effect": "Trash a card from your hand and gain a card costing up to 2 more"}, 10))
                card.quantity -= 1
                self.remodel_gain_active = False
                self.special_action_label.config(text="")
                self.log(f"Player gained {card.name} using Remodel", "INFO")
                self.update_gui()
        elif self.player.buys > 0 and self.player.coins >= card.cost and card.quantity > 0:
            self.player.buy_card(card)
            card.quantity -= 1
            self.log(f"Player bought {card.name}", "INFO")
            self.update_gui()
        elif self.player.buys <= 0:
            self.log("Player has no more buys", "INFO")
            self.special_action_label.config(text="You have no more buys")
        elif self.player.coins < card.cost:
            self.log("Player doesn't have enough coins", "INFO")
            self.special_action_label.config(text="You don't have enough coins")
        else:
            self.log("Player can't buy this card", "INFO")
            self.special_action_label.config(text="You can't buy this card")

    def show_deck(self):
        self.log("Showing deck", "INFO")
        messagebox.showinfo("Deck", "\n".join(str(card) for card in self.player.deck))

    def show_discard(self):
        self.log("Showing discard pile", "INFO")
        messagebox.showinfo("Discard Pile", "\n".join(str(card) for card in self.player.discard_pile))

    def end_turn(self):
        self.log("Ending turn", "INFO")
        self.player.end_turn()
        self.log("Player ended turn", "INFO")
        self.check_end_game()
        self.update_gui()
        self.special_action_label.config(text="")

    def count_points(self, deck):
        self.log("Counting points", "INFO")
        points = 0
        for card in deck:
            if card.name == "Estate":
                points += 1
            elif card.name == "Duchy":
                points += 3
            elif card.name == "Province":
                points += 6
        self.log(f"Player scored {points} points", "INFO")
        return points
    
    def check_end_game(self):
        self.log("Checking end game conditions", "INFO")
        empty_piles = sum(1 for card in self.supply if card.quantity == 0)
        provinces = next(c for c in self.supply if c.name == "Province")
        if empty_piles >= 3 or provinces.quantity == 0 or self.end_already:
            self.log("Game Over", "INFO")
            player_points = self.count_points(self.player.deck+self.player.discard_pile+self.player.hand)
            messagebox.showinfo("Game Over", "The game is over! You earned " + str(player_points) + " points.")
            self.restart_game()

    def end_game(self):
        self.log("Ending game", "INFO")
        self.end_already = True
        self.check_end_game()
    
    def restart_game(self):
        self.log("Restarting game", "INFO")
        for widget in self.root.winfo_children():
            widget.destroy()
        self.__init__(self.root, "Player 1")

if __name__ == "__main__":
    root = ttk.Window(themename="darkly")
    game = Game(root, "Player 1")
    root.mainloop()