import random
from Card import Card

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
            if card.card_type == "Victory":
                return "You cannot play Victory cards."
            if card.card_type == "Action" and self.actions <= 0:
                return "You have no actions left to play this card."
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