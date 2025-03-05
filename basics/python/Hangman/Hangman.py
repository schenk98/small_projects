import tkinter as tk
from tkinter import messagebox
import random

class Hangman:
    def __init__(self, root, language="en"):
        self.root = root
        self.root.title("Hangman")
        self.words = self.load_words(language)
        self.word = random.choice(self.words)
        self.guesses = []
        self.wrong_guesses = []
        self.max_attempts = 9
        self.attempts = 0
        self.create_widgets()

    def load_words(self, language):
        if language == "en":
            filepath = "Mini_projects/basics/python/Hangman/words_en.txt"
        else:
            filepath = "Mini_projects/basics/python/Hangman/words_cz.txt"
        with open(filepath, "r", encoding="utf-8") as file:
            words = [line.strip() for line in file.readlines()]
        return words

    def create_widgets(self):
        self.word_label = tk.Label(self.root, text=self.get_display_word(), font=("Helvetica", 24))
        self.word_label.pack(pady=20)

        self.guess_entry = tk.Entry(self.root, font=("Helvetica", 18))
        self.guess_entry.pack(pady=10)
        self.guess_entry.bind("<Return>", self.make_guess)

        self.guess_button = tk.Button(self.root, text="Guess", font=("Helvetica", 18), command=self.make_guess)
        self.guess_button.pack(pady=10)

        self.attempts_label = tk.Label(self.root, text=f"Attempts left: {self.max_attempts - self.attempts}", font=("Helvetica", 18))
        self.attempts_label.pack(pady=10)

        self.hangman_canvas = tk.Canvas(self.root, width=200, height=200)
        self.hangman_canvas.pack(pady=20)
        self.draw_hangman()

        self.wrong_guesses_label = tk.Label(self.root, text="Wrong guesses: ", font=("Helvetica", 18))
        self.wrong_guesses_label.pack(pady=10)

        self.back_button = tk.Button(self.root, text="Back to Main Menu", font=("Helvetica", 14), command=self.back_to_main_menu)
        self.back_button.pack(pady=10)

    def get_display_word(self):
        return " ".join([letter if letter in self.guesses else "_" for letter in self.word])

    def make_guess(self, event=None):
        guess = self.guess_entry.get().lower()
        self.guess_entry.delete(0, tk.END)

        if len(guess) != 1 or not guess.isalpha():
            messagebox.showwarning("Invalid guess", "Please enter a single letter.")
            return

        if guess in self.guesses or guess in self.wrong_guesses:
            messagebox.showwarning("Already guessed", "You have already guessed that letter.")
            return

        if guess in self.word:
            self.guesses.append(guess)
        else:
            self.wrong_guesses.append(guess)
            self.attempts += 1
            self.draw_hangman()

        self.word_label.config(text=self.get_display_word())
        self.attempts_label.config(text=f"Attempts left: {self.max_attempts - self.attempts}")
        self.wrong_guesses_label.config(text=f"Wrong guesses: {', '.join(self.wrong_guesses)}")

        if "_" not in self.get_display_word():
            messagebox.showinfo("Hangman", "Congratulations! You guessed the word!")
            self.reset_game()
        elif self.attempts >= self.max_attempts:
            messagebox.showinfo("Hangman", f"Game Over! The word was: {self.word}")
            self.reset_game()

    def draw_hangman(self):
        self.hangman_canvas.delete("all")
        if self.attempts > 0:
            self.hangman_canvas.create_line(50, 150, 150, 150)  # Base
        if self.attempts > 1:
            self.hangman_canvas.create_line(100, 150, 100, 50)  # Pole
        if self.attempts > 2:
            self.hangman_canvas.create_line(100, 50, 150, 50)  # Top
        if self.attempts > 3:
            self.hangman_canvas.create_line(150, 50, 150, 70)  # Rope
        if self.attempts > 4:
            self.hangman_canvas.create_oval(140, 70, 160, 90)  # Head
        if self.attempts > 5:
            self.hangman_canvas.create_line(150, 90, 150, 120)  # Body
        if self.attempts > 6:
            self.hangman_canvas.create_line(150, 100, 130, 110)  # Left Arm
        if self.attempts > 7:
            self.hangman_canvas.create_line(150, 100, 170, 110)  # Right Arm
        if self.attempts > 8:
            self.hangman_canvas.create_line(150, 120, 130, 140)  # Left Leg
        if self.attempts > 9:
            self.hangman_canvas.create_line(150, 120, 170, 140)  # Right Leg

    def reset_game(self):
        self.word = random.choice(self.words)
        self.guesses = []
        self.wrong_guesses = []
        self.attempts = 0
        self.word_label.config(text=self.get_display_word())
        self.attempts_label.config(text=f"Attempts left: {self.max_attempts}")
        self.wrong_guesses_label.config(text="Wrong guesses: ")
        self.draw_hangman()

    def back_to_main_menu(self):
        for widget in self.root.winfo_children():
            widget.destroy()
        MainMenu(self.root)

class MainMenu:
    def __init__(self, root):
        self.root = root
        self.root.title("Hangman")
        self.create_menu()

    def create_menu(self):
        self.root.geometry("500x600")
        tk.Label(self.root, text="Hangman", font=("Helvetica", 24)).pack(pady=20)
        tk.Button(self.root, text="Start Game (English)", font=("Helvetica", 18), command=lambda: self.start_game("en")).pack(pady=10)
        tk.Button(self.root, text="Start Game (Czech)", font=("Helvetica", 18), command=lambda: self.start_game("cz")).pack(pady=10)

    def start_game(self, language):
        for widget in self.root.winfo_children():
            widget.destroy()
        self.root.geometry("500x600")
        Hangman(self.root, language)

def main():
    root = tk.Tk()
    MainMenu(root)
    root.mainloop()

if __name__ == "__main__":
    main()