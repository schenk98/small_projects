from django.shortcuts import render
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
import json
from .timeline_game import TimelineGame  # Import třídy TimelineGame

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