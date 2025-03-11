from django.shortcuts import render
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
import json
from .timeline_game import TimelineGame  # Import třídy TimelineGame
import random

# Načtení karet z JSON souboru
with open('c:/Users/jakub/My project/Mini_projects/basics/python/Timeline/Timeline_cz.json', 'r', encoding='utf-8') as file:
    cards = json.load(file)

# Inicializace hry
game = TimelineGame(cards)

def hello(request):
    return render(request, 'hello.html')

def timeline_game(request):
    context = {
        'timeline': game.timeline,
        'player_hand': game.player_hand,
        'points': game.points,
        'wrong': game.wrong,
    }
    return render(request, 'timeline.html', context)

@csrf_exempt
def submit_year(request):
    data = json.loads(request.body)
    card_index = data['card_index']
    year = int(data['year'])
    position = game.find_position_for_year(year)
    if game.place_card(card_index, position):
        game.points += 1
        result = "Correct!"
    else:
        game.wrong += 1
        result = "Wrong!"
    game.draw_card()
    response_data = {
        'result': result,
        'points': game.points,
        'wrong': game.wrong,
        'timeline': game.timeline,
        'player_hand': game.player_hand,
    }
    return JsonResponse(response_data)

@csrf_exempt
def restart_game(request):
    global game
    game = TimelineGame(cards)
    response_data = {
        'points': game.points,
        'wrong': game.wrong,
        'timeline': game.timeline,
        'player_hand': game.player_hand,
    }
    return JsonResponse(response_data)

# Dummy data for pexeso sets
PEXESO_SETS = [
    {
        'name': 'Set 1',
        'cards': [
            {'czech_word': 'kočka', 'english_word': 'cat'},
            {'czech_word': 'pes', 'english_word': 'dog'},
            {'czech_word': 'dům', 'english_word': 'house'},
            {'czech_word': 'auto', 'english_word': 'car'},
            {'czech_word': 'strom', 'english_word': 'tree'},
            {'czech_word': 'květina', 'english_word': 'flower'},
            {'czech_word': 'slunce', 'english_word': 'sun'},
            {'czech_word': 'měsíc', 'english_word': 'moon'},
        ]
    },
    {
        'name': 'Set 2',
        'cards': [
            {'czech_word': 'jablko', 'english_word': 'apple'},
            {'czech_word': 'banán', 'english_word': 'banana'},
            {'czech_word': 'hruška', 'english_word': 'pear'},
            {'czech_word': 'pomeranč', 'english_word': 'orange'},
            {'czech_word': 'hrozny', 'english_word': 'grapes'},
            {'czech_word': 'meloun', 'english_word': 'watermelon'},
            {'czech_word': 'jahoda', 'english_word': 'strawberry'},
            {'czech_word': 'malina', 'english_word': 'raspberry'},
        ]
    }
]

def hello(request):
    return render(request, 'hello.html')

def pexeso_game(request):
    selected_set = random.choice(PEXESO_SETS)
    cards = selected_set['cards'][:8]  # Vybereme prvních 8 párů
    cards = [{'word': card['czech_word'], 'translation': card['english_word']} for card in cards]
    translations = [{'word': card['english_word'], 'translation': card['czech_word']} for card in selected_set['cards'][:8]]
    cards = cards + translations  # Přidáme překlady
    random.shuffle(cards)  # Zamícháme karty
    context = {
        'cards': cards,
    }
    return render(request, 'pexeso.html', context)