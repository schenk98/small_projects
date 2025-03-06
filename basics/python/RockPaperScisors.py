import tkinter as tk
from tkinter import messagebox
import random
class RockPaperScissorsExtended:
    def __init__(self, root):
        self.root = root
        self.root.title("Rock Paper Scissors")
        self.score = 0
        self.playerGuess = ""
        self.computerGuess = ""
        self.result_text = ""
        self.create_widgets()

    def create_widgets(self):
        self.computerChoice = tk.Label(self.root, text="Computer chose: \n"+self.computerGuess, font=("Helvetica", 18))
        self.computerChoice.pack(pady=20)

        self.rock_button = tk.Button(self.root, text="Rock", font=("Helvetica", 18), command=self.guess_rock)
        self.rock_button.pack(pady=10)
        self.paper_button = tk.Button(self.root, text="Paper", font=("Helvetica", 18), command=self.guess_paper)
        self.paper_button.pack(pady=10)
        self.scissors_button = tk.Button(self.root, text="Scissors", font=("Helvetica", 18), command=self.guess_scissors)
        self.scissors_button.pack(pady=10)
        self.lizard_button = tk.Button(self.root, text="Lizard", font=("Helvetica", 18), command=self.guess_lizard)
        self.lizard_button.pack(pady=10)
        self.spock_button = tk.Button(self.root, text="Spock", font=("Helvetica", 18), command=self.guess_spock)
        self.spock_button.pack(pady=10)

        self.score_label = tk.Label(self.root, text="Score: "+str(self.score), font=("Helvetica", 18))
        self.score_label.pack(pady=10)

        self.result_label = tk.Label(self.root, text="Result: "+str(self.result_text), font=("Helvetica", 18))
        self.result_label.pack(pady=10)

        self.back_button = tk.Button(self.root, text="Back to Main Menu", font=("Helvetica", 14), command=self.back_to_main_menu)
        self.back_button.pack(pady=10)


    def guess_rock(self):
        self.playerGuess = "Rock"
        self.computer_guess()
        self.check_winner()
    
    def guess_paper(self):
        self.playerGuess = "Paper"
        self.computer_guess()
        self.check_winner()
    
    def guess_scissors(self):
        self.playerGuess = "Scissors"
        self.computer_guess()
        self.check_winner()

    def guess_lizard(self):
        self.playerGuess = "Lizard"
        self.computer_guess()
        self.check_winner()
    
    def guess_spock(self):
        self.playerGuess = "Spock"
        self.computer_guess()
        self.check_winner()

    def computer_guess(self):
        self.computerGuess = random.choice(["Rock", "Paper", "Scissors", "Lizard", "Spock"])

    def check_winner(self):
        if self.playerGuess == self.computerGuess:
            self.result_text = f"Draw! You both chose {self.playerGuess}"
        elif self.playerGuess == "Rock" and self.computerGuess == "Scissors":
            self.result_text = "You win! Rock beats Scissors"
            self.score += 1
        elif self.playerGuess == "Paper" and self.computerGuess == "Rock":
            self.result_text = "You win! Paper beats Rock"
            self.score += 1
        elif self.playerGuess == "Scissors" and self.computerGuess == "Paper":
            self.result_text = "You win! Scissors beats Paper"
            self.score += 1
        elif self.playerGuess == "Rock" and self.computerGuess == "Lizard":
            self.result_text = "You win! Rock beats Lizard"
            self.score += 1
        elif self.playerGuess == "Lizard" and self.computerGuess == "Spock":
            self.result_text = "You win! Lizard beats Spock"
            self.score += 1
        elif self.playerGuess == "Spock" and self.computerGuess == "Scissors":
            self.result_text = "You win! Spock beats Scissors"
            self.score += 1
        elif self.playerGuess == "Scissors" and self.computerGuess == "Lizard":
            self.result_text = "You win! Scissors beats Lizard"
            self.score += 1
        elif self.playerGuess == "Lizard" and self.computerGuess == "Paper":
            self.result_text = "You win! Lizard beats Paper"
            self.score += 1
        elif self.playerGuess == "Paper" and self.computerGuess == "Spock":
            self.result_text = "You win! Paper beats Spock"
            self.score += 1
        elif self.playerGuess == "Spock" and self.computerGuess == "Rock":
            self.result_text = "You win! Spock beats Rock"
            self.score += 1

        else:
            self.result_text = f"You lose! {self.computerGuess} beats {self.playerGuess}"
            self.score = 0
        
        self.update_labels()


    def update_labels(self):
        self.computerChoice.config(text="Computer chose: \n" + self.computerGuess)
        self.score_label.config(text="Score: " + str(self.score))
        self.result_label.config(text="Result: " + self.result_text)

    def back_to_main_menu(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        MainMenu(self.root)

class RockPaperScissors:
    def __init__(self, root):
        self.root = root
        self.root.title("Rock Paper Scissors")
        self.score = 0
        self.playerGuess = ""
        self.computerGuess = ""
        self.result_text = ""
        self.create_widgets()

    def create_widgets(self):
        self.computerChoice = tk.Label(self.root, text="Computer chose: \n"+self.computerGuess, font=("Helvetica", 18))
        self.computerChoice.pack(pady=20)

        self.rock_button = tk.Button(self.root, text="Rock", font=("Helvetica", 18), command=self.guess_rock)
        self.rock_button.pack(pady=10)
        self.paper_button = tk.Button(self.root, text="Paper", font=("Helvetica", 18), command=self.guess_paper)
        self.paper_button.pack(pady=10)
        self.scissors_button = tk.Button(self.root, text="Scissors", font=("Helvetica", 18), command=self.guess_scissors)
        self.scissors_button.pack(pady=10)

        self.score_label = tk.Label(self.root, text="Score: "+str(self.score), font=("Helvetica", 18))
        self.score_label.pack(pady=10)

        self.result_label = tk.Label(self.root, text="Result: "+str(self.result_text), font=("Helvetica", 18))
        self.result_label.pack(pady=10)

        self.back_button = tk.Button(self.root, text="Back to Main Menu", font=("Helvetica", 14), command=self.back_to_main_menu)
        self.back_button.pack(pady=10)

    def guess_rock(self):
        self.playerGuess = "Rock"
        self.computer_guess()
        self.check_winner()
    
    def guess_paper(self):
        self.playerGuess = "Paper"
        self.computer_guess()
        self.check_winner()
    
    def guess_scissors(self):
        self.playerGuess = "Scissors"
        self.computer_guess()
        self.check_winner()
    
    def computer_guess(self):
        self.computerGuess = random.choice(["Rock", "Paper", "Scissors"])

    def check_winner(self):
        if self.playerGuess == self.computerGuess:
            self.result_text = f"Draw! You both chose {self.playerGuess}"
        elif self.playerGuess == "Rock" and self.computerGuess == "Scissors":
            self.result_text = "You win! Rock beats Scissors"
            self.score += 1
        elif self.playerGuess == "Paper" and self.computerGuess == "Rock":
            self.result_text = "You win! Paper beats Rock"
            self.score += 1
        elif self.playerGuess == "Scissors" and self.computerGuess == "Paper":
            self.result_text = "You win! Scissors beats Paper"
            self.score += 1
        else:
            self.result_text = f"You lose! {self.computerGuess} beats {self.playerGuess}"
            self.score = 0
        
        self.update_labels()

        
    def update_labels(self):
        self.computerChoice.config(text="Computer chose: \n" + self.computerGuess)
        self.score_label.config(text="Score: " + str(self.score))
        self.result_label.config(text="Result: " + self.result_text)

    def back_to_main_menu(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        MainMenu(self.root)


class MainMenu:
    def __init__(self, root):
        self.root = root
        self.root.title("Rock Paper Scissors")
        self.create_menu()

    def create_menu(self):
        self.root.geometry("500x600")
        tk.Label(self.root, text="Rock Paper Scissors", font=("Helvetica", 24)).pack(pady=20)
        tk.Button(self.root, text="Start Game (basic)", font=("Helvetica", 18), command=lambda: self.start_game("rps")).pack(pady=10)
        tk.Button(self.root, text="Start Game (expanded)", font=("Helvetica", 18), command=lambda: self.start_game("rpsls")).pack(pady=10)

    def start_game(self, type):
        for widget in self.root.winfo_children():
            widget.destroy()
        self.root.geometry("500x600")
        if type == "rpsls":
            RockPaperScissorsExtended(self.root)
        else:
            RockPaperScissors(self.root)
        

def main():
    root = tk.Tk()
    MainMenu(root)
    root.mainloop()

if __name__ == "__main__":
    main()