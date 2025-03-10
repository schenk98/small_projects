import random

class TimelineGame:
    def __init__(self, cards):
        self.cards = cards
        self.timeline = []
        self.player_hand = []
        self.points = 0
        self.wrong = 0
        self.initialize_game()

    def initialize_game(self):
        random.shuffle(self.cards)
        first_card = self.cards.pop()
        first_card['correct'] = 0
        self.timeline.append(first_card)
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
                card['correct'] = 1
                self.timeline.insert(0, card)
                return True
        elif position == len(self.timeline):
            if card['year'] >= self.timeline[-1]['year']:
                card['correct'] = 1
                self.timeline.append(card)
                return True
        else:
            if self.timeline[position - 1]['year'] <= card['year'] <= self.timeline[position]['year']:
                card['correct'] = 1
                self.timeline.insert(position, card)
                return True
        card['correct'] = -1
        for i in range(len(self.timeline)):
            if card['year'] <= self.timeline[i]['year']:
                self.timeline.insert(i, card)
                break
        else:
            self.timeline.append(card)
        return False

    def draw_card(self):
        if self.cards:
            self.player_hand.append(self.cards.pop())