import json
import random
import tkinter as tk
import ttkbootstrap as ttk
from ttkbootstrap.constants import *
from tkinter import messagebox, simpledialog, Text, END

class Card:
    def __init__(self, name, cost, card_type, description, quantity):
        self.name = name
        self.cost = cost
        self.card_type = card_type
        self.description = description
        self.quantity = quantity

    def __repr__(self):
        return f"{self.name} ({self.cost})"

class Player:
    def __init__(self, name):
        self.name = name
        self.deck = []
        self.hand = []
        self.discard_pile = []
        self.play_area = []
        self.actions = 1
        self.buys = 1
        self.coins = 0

    def draw_card(self):
        if not self.deck:
            self.deck = self.discard_pile
            self.discard_pile = []
            random.shuffle(self.deck)
        if self.deck:
            self.hand.append(self.deck.pop())

    def draw_hand(self):
        for _ in range(5):
            self.draw_card()

    def play_card(self, card_name):
        card = next((c for c in self.hand if c.name == card_name), None)
        if card:
            if card.card_type == "Action" and self.actions <= 0:
                messagebox.showinfo("No Actions", "You have no actions left to play this card.")
                return None
            self.hand.remove(card)
            special_action = None
            if card.card_type == "Action":
                self.actions -= 1
                special_action = self.resolve_action(card)
                if not special_action:
                    self.discard_pile.append(card)
            elif card.card_type == "Treasure":
                self.coins += card.description["value"]
                self.discard_pile.append(card)
            return special_action

    def resolve_action(self, card):
        if "cards" in card.description:
            for _ in range(card.description["cards"]):
                self.draw_card()
        if "actions" in card.description:
            self.actions += card.description["actions"]
        if "coins" in card.description:
            self.coins += card.description["coins"]
        if "special" in card.description:
            return card.description["special"]
        return None

    def buy_card(self, card):
        if self.coins >= card.cost and self.buys > 0:
            self.coins -= card.cost
            self.buys -= 1
            self.discard_pile.append(Card(card.name, card.cost, card.card_type, card.description, card.quantity))

    def end_turn(self):
        self.discard_pile.extend(self.hand)
        self.hand = []
        self.play_area = []
        self.actions = 1
        self.buys = 1
        self.coins = 0
        self.draw_hand()

class Game:
    def __init__(self, root, player_name):
        self.root = root
        self.supply = []
        self.load_cards()
        self.player = Player(player_name)
        self.setup_game()
        self.create_gui()
        self.workshop_active = False

    def load_cards(self):
        with open("Mini_projects/basics/python/Dominion/cards.json", "r") as file:
            cards_data = json.load(file)
            for card_data in cards_data:
                card = Card(card_data["name"], card_data["cost"], card_data["type"], card_data["description"], card_data["quantity"])
                self.supply.append(card)

    def setup_game(self):
        for _ in range(7):
            self.player.deck.append(next(c for c in self.supply if c.name == "Copper"))
        for _ in range(3):
            self.player.deck.append(next(c for c in self.supply if c.name == "Estate"))
        random.shuffle(self.player.deck)
        self.player.draw_hand()

    def create_gui(self):
        self.root.title("Dominion")
        self.root.geometry("1000x1000")

        self.shop_frame = ttk.Frame(self.root, width=800, height=800)
        self.shop_frame.place(x=0, y=0)

        self.text_frame = ttk.Frame(self.root, width=200, height=350)
        self.text_frame.place(x=800, y=0)

        self.info_frame = ttk.Frame(self.root, width=200, height=350)
        self.info_frame.place(x=800, y=400)

        self.hand_frame = ttk.Frame(self.root, width=800, height=200)
        self.hand_frame.place(x=0, y=800)

        self.supply_frame = ttk.Frame(self.shop_frame)
        self.supply_frame.pack(fill=tk.BOTH, expand=True)

        self.deck_frame = ttk.Frame(self.info_frame)
        self.deck_frame.pack(side=tk.TOP, fill=tk.X)

        self.actions_label = ttk.Label(self.info_frame, text=f"Actions: {self.player.actions}", font=("Helvetica", 16, "bold"))
        self.actions_label.pack(pady=10)

        self.buys_label = ttk.Label(self.info_frame, text=f"Buys: {self.player.buys}", font=("Helvetica", 16, "bold"))
        self.buys_label.pack(pady=10)

        self.coins_label = ttk.Label(self.info_frame, text=f"Coins: {self.player.coins}", font=("Helvetica", 16, "bold"))
        self.coins_label.pack(pady=10)

        self.special_action_label = ttk.Label(self.info_frame, text="", font=("Helvetica", 16, "bold"))
        self.special_action_label.pack(pady=10)

        self.end_turn_button = ttk.Button(self.info_frame, text="End Turn", command=self.end_turn, bootstyle=SUCCESS)
        self.end_turn_button.pack(pady=10)

        self.log_text = Text(self.text_frame, height=20, width=30)
        self.log_text.pack(pady=10)

        self.update_gui()

    def update_gui(self):
        for widget in self.hand_frame.winfo_children():
            widget.destroy()
        for widget in self.supply_frame.winfo_children():
            widget.destroy()
        for widget in self.deck_frame.winfo_children():
            widget.destroy()

        for card in self.player.hand:
            description = "\n".join([f"{key}: {value}" for key, value in card.description.items()])
            button = tk.Button(self.hand_frame, text=f"{card.name} ({card.cost})\n{description}", command=lambda c=card: self.play_card(c.name), width=15, height=10, wraplength=100, justify="center")
            button.pack(side=tk.LEFT, padx=10, pady=10)

        for i, card in enumerate(self.supply):
            if card.quantity > 0:
                description = "\n".join([f"{key}: {value}" for key, value in card.description.items()])
                if "effect" in card.description:
                    description += f"\nEffect: {card.description['effect']}"
                button = tk.Button(self.supply_frame, text=f"{{{card.cost}}} {card.name} (#{card.quantity})\n{description}", command=lambda c=card: self.buy_card(c), width=15, height=10, wraplength=100, justify="center")
                button.grid(row=i // 6, column=i % 6, padx=10, pady=10)
            else:
                button = tk.Button(self.supply_frame, text="Sold Out", state=tk.DISABLED, width=15, height=10, wraplength=100, justify="center")
                button.grid(row=i // 6, column=i % 6, padx=10, pady=10)

        deck_button = tk.Button(self.deck_frame, text="Deck", command=self.show_deck, width=15, height=10, wraplength=100, justify="center")
        deck_button.pack(side=tk.TOP, padx=10, pady=10)

        discard_button = tk.Button(self.deck_frame, text="Discard", command=self.show_discard, width=15, height=10, wraplength=100, justify="center")
        discard_button.pack(side=tk.TOP, padx=10, pady=10)

        self.actions_label.config(text=f"Actions: {self.player.actions}")
        self.buys_label.config(text=f"Buys: {self.player.buys}")
        self.coins_label.config(text=f"Coins: {self.player.coins}")

    def log(self, message, level="INFO"):
        self.log_text.insert(END, f"[{level}] {message}\n")
        self.log_text.see(END)

    def play_card(self, card_name):
        if self.workshop_active:
            messagebox.showinfo("Action Required", "You must complete the current action before playing another card.")
            return
        special_action = self.player.play_card(card_name)
        self.log(f"Player played {card_name}", "INFO")
        if special_action:
            self.special_action(special_action, card_name)
        self.update_gui()

    def special_action(self, action, card_name):
        if action == "workshop":
            self.special_action_label.config(text="Gain a card costing up to 4")
            self.workshop_action(card_name)

    def workshop_action(self, card_name):
        self.workshop_active = True
        self.workshop_card_name = card_name

    def buy_card(self, card):
        if self.workshop_active:
            if card.cost <= 4 and card.quantity > 0:
                self.player.discard_pile.append(Card(card.name, card.cost, card.card_type, card.description, card.quantity))
                self.player.discard_pile.append(Card(self.workshop_card_name, 3, "Action", {"special": "workshop"}, 10))
                card.quantity -= 1
                self.workshop_active = False
                self.special_action_label.config(text="")
                self.log(f"Player gained {card.name} using Workshop", "INFO")
                self.update_gui()
        elif self.player.buys > 0 and self.player.coins >= card.cost and card.quantity > 0:
            self.player.buy_card(card)
            card.quantity -= 1
            self.log(f"Player bought {card.name}", "INFO")
            self.update_gui()

    def end_turn(self):
        self.player.end_turn()
        self.log("Player ended turn", "INFO")
        self.check_end_game()
        self.update_gui()

    def check_end_game(self):
        empty_piles = sum(1 for card in self.supply if card.quantity == 0)
        provinces = next(c for c in self.supply if c.name == "Province")
        if empty_piles >= 3 or provinces.quantity == 0:
            self.log("Game Over", "INFO")
            messagebox.showinfo("Game Over", "The game is over!")
            self.root.quit()

    def show_deck(self):
        messagebox.showinfo("Deck", "\n".join(str(card) for card in self.player.deck))

    def show_discard(self):
        messagebox.showinfo("Discard Pile", "\n".join(str(card) for card in self.player.discard_pile))

if __name__ == "__main__":
    root = ttk.Window(themename="darkly")
    game = Game(root, "Player 1")
    root.mainloop()