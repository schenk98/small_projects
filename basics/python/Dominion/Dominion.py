import json
import random
import tkinter as tk
from tkinter import messagebox

class Card:
    def __init__(self, name, cost, card_type, description):
        self.name = name
        self.cost = cost
        self.card_type = card_type
        self.description = description

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
            self.hand.remove(card)
            if card.card_type == "Action":
                self.actions -= 1
                self.resolve_action(card)
                self.discard_pile.append(card)
            elif card.card_type == "Treasure":
                self.coins += card.description["value"]
                self.discard_pile.append(card)

    def resolve_action(self, card):
        if "cards" in card.description:
            for _ in range(card.description["cards"]):
                self.draw_card()
        if "actions" in card.description:
            self.actions += card.description["actions"]
        if "coins" in card.description:
            self.coins += card.description["coins"]

    def buy_card(self, card):
        if self.coins >= card.cost and self.buys > 0:
            self.coins -= card.cost
            self.buys -= 1
            self.discard_pile.append(Card(card.name, card.cost, card.card_type, card.description))

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

    def load_cards(self):
        with open("Mini_projects/basics/python/Dominion/cards.json", "r") as file:
            cards_data = json.load(file)
            for card_data in cards_data:
                card = Card(card_data["name"], card_data["cost"], card_data["type"], card_data["description"])
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
        self.root.geometry("800x600")

        self.hand_frame = tk.Frame(self.root)
        self.hand_frame.pack(side=tk.BOTTOM, fill=tk.X)

        self.supply_frame = tk.Frame(self.root)
        self.supply_frame.pack(side=tk.TOP, fill=tk.X)

        self.deck_frame = tk.Frame(self.root)
        self.deck_frame.pack(side=tk.RIGHT, fill=tk.Y)

        self.info_frame = tk.Frame(self.root)
        self.info_frame.pack(side=tk.RIGHT, fill=tk.Y)

        self.end_turn_button = tk.Button(self.root, text="End Turn", command=self.end_turn)
        self.end_turn_button.pack(side=tk.RIGHT)

        self.actions_label = tk.Label(self.info_frame, text=f"Actions: {self.player.actions}")
        self.actions_label.pack()

        self.buys_label = tk.Label(self.info_frame, text=f"Buys: {self.player.buys}")
        self.buys_label.pack()

        self.coins_label = tk.Label(self.info_frame, text=f"Coins: {self.player.coins}")
        self.coins_label.pack()

        self.update_gui()

    def update_gui(self):
        for widget in self.hand_frame.winfo_children():
            widget.destroy()
        for widget in self.supply_frame.winfo_children():
            widget.destroy()
        for widget in self.deck_frame.winfo_children():
            widget.destroy()

        for card in self.player.hand:
            button = tk.Button(self.hand_frame, text=str(card), command=lambda c=card: self.play_card(c.name))
            button.pack(side=tk.LEFT)

        for card in self.supply:
            button = tk.Button(self.supply_frame, text=str(card), command=lambda c=card: self.buy_card(c))
            button.pack(side=tk.LEFT)

        deck_button = tk.Button(self.deck_frame, text="Deck", command=self.show_deck)
        deck_button.pack(side=tk.TOP)

        discard_button = tk.Button(self.deck_frame, text="Discard", command=self.show_discard)
        discard_button.pack(side=tk.TOP)

        self.actions_label.config(text=f"Actions: {self.player.actions}")
        self.buys_label.config(text=f"Buys: {self.player.buys}")
        self.coins_label.config(text=f"Coins: {self.player.coins}")

    def play_card(self, card_name):
        self.player.play_card(card_name)
        self.update_gui()

    def buy_card(self, card):
        if self.player.buys > 0 and self.player.coins >= card.cost:
            self.player.buy_card(card)
            self.update_gui()

    def end_turn(self):
        self.player.end_turn()
        self.update_gui()

    def show_deck(self):
        messagebox.showinfo("Deck", "\n".join(str(card) for card in self.player.deck))

    def show_discard(self):
        messagebox.showinfo("Discard Pile", "\n".join(str(card) for card in self.player.discard_pile))

if __name__ == "__main__":
    root = tk.Tk()
    game = Game(root, "Player 1")
    root.mainloop()