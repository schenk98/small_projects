class Player:
    def __init__(self, player_id, color, algorithm):
        self.id = player_id
        self.color = color
        self.algorithm = algorithm
        self.position = None
        self.algorithm_menu = None