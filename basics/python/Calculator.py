import tkinter as tk
import math

class Calculator:
    def __init__(self, root):
        self.root = root
        self.root.title("Calculator")
        self.root.geometry("400x600")
        
        self.expression = ""
        self.history = []
        self.epsilon = 1e-10
        
        self.display = tk.Entry(root, font=("Arial", 24), borderwidth=2, relief="solid")
        self.display.pack(pady=20, fill="both", expand=True)
        
        self.history_display = tk.Text(root, font=("Arial", 12), height=5, borderwidth=2, relief="solid")
        self.history_display.pack(pady=10, fill="both", expand=True)
        self.history_display.config(state=tk.DISABLED)
        
        self.buttons_frame = tk.Frame(root)
        self.buttons_frame.pack(fill="both", expand=True)
        
        self.create_buttons()
        
        self.root.bind("<Key>", self.key_press)
        
    def create_buttons(self):
        buttons = [
            '7', '8', '9', '/', 'C',
            '4', '5', '6', '*', 'DEL',
            '1', '2', '3', '-', 'PI',
            '0', '.', '=', '+', 'E',
            '(', ')', '^', 'sqrt', 'sin',
            'cos', 'tan', 'ln', 'log'
        ]
        
        row = 0
        col = 0
        for button in buttons:
            action = lambda x=button: self.on_button_click(x)
            b = tk.Button(self.buttons_frame, text=button, font=("Arial", 18), command=action)
            b.grid(row=row, column=col, sticky="nsew")
            col += 1
            if col > 4:
                col = 0
                row += 1
        
        for i in range(5):
            self.buttons_frame.grid_columnconfigure(i, weight=1)
            self.buttons_frame.grid_rowconfigure(i, weight=1)
        
    def on_button_click(self, char):
        if self.expression == "Error":
            self.expression = ""
        
        if char == "C":
            self.expression = ""
        elif char == "DEL":
            self.expression = self.expression[:-1]
        elif char == "=":
            try:
                if self.expression and self.expression[-1] in "+-*/^":
                    self.expression = self.expression[:-1]
                self.expression = self.expression.replace('^', '**')
                result = eval(self.expression)
                if isinstance(result, float):
                    result = round(result, 10)
                self.history.append(f"{self.expression.replace('**', '^')} = {result}")
                self.update_history()
                self.expression = str(result)
            except Exception as e:
                self.expression = "Error"
        elif char == "PI":
            self.expression += str(math.pi)
        elif char == "E":
            self.expression += str(math.e)
        elif char == "sqrt":
            self.expression += "math.sqrt("
        elif char == "sin":
            self.expression += "math.sin("
        elif char == "cos":
            self.expression += "math.cos("
        elif char == "tan":
            self.expression += "math.tan("
        elif char == "ln":
            self.expression += "math.log("
        elif char == "log":
            self.expression += "math.log10("
        else:
            if char in "+-*/^" and self.expression and self.expression[-1] in "+-*/^":
                self.expression = self.expression[:-1] + char
            else:
                self.expression += str(char)
        self.update_display()
        
    def key_press(self, event):
        if event.char.isdigit() or event.char in "+-*/().":
            self.on_button_click(event.char)
        elif event.keysym == "Return":
            self.on_button_click("=")
        elif event.keysym == "BackSpace":
            self.on_button_click("DEL")
        
    def update_display(self):
        self.display.delete(0, tk.END)
        self.display.insert(0, self.expression)
        
    def update_history(self):
        self.history_display.config(state=tk.NORMAL)
        self.history_display.delete(1.0, tk.END)
        for line in self.history[-5:]:
            self.history_display.insert(tk.END, line + "\n")
        self.history_display.config(state=tk.DISABLED)

if __name__ == "__main__":
    root = tk.Tk()
    calc = Calculator(root)
    root.mainloop()