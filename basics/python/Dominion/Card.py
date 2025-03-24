class Card:
    def __init__(self, name, cost, card_type, description, quantity):
        self.name = name
        self.cost = cost
        self.card_type = card_type
        self.description = description
        self.quantity = quantity

    def __repr__(self):
        return f"{self.name} ({self.cost})"