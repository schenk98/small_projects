from pynput import keyboard, mouse
from pynput.keyboard import Key, Controller as KeyboardController
from pynput.mouse import Button, Controller as MouseController
import threading
import time

running = False
exit_flag = False
counter = 0

keyboard_controller = KeyboardController()
mouse_controller = MouseController()

def release_all():
    # Release all potentially pressed keys
    keyboard_controller.release(Key.alt_l)
    keyboard_controller.release('w')
    keyboard_controller.release('a')
    keyboard_controller.release('s')
    keyboard_controller.release('d')
    mouse_controller.release(Button.right)

def press_combo():
    global counter, running, exit_flag
    while not exit_flag:
        if running:
            counter += 1
            keyboard_controller.press(Key.alt_l)
            mouse_controller.press(Button.right)

            if counter % 4 == 0:
                keyboard_controller.press('w')
                time.sleep(0.05)
                keyboard_controller.release('w')
            elif counter % 4 == 1:
                keyboard_controller.press('a')
                time.sleep(0.05)
                keyboard_controller.release('a')
            elif counter % 4 == 2:
                keyboard_controller.press('s')
                time.sleep(0.05)
                keyboard_controller.release('s')
            elif counter % 4 == 3:
                keyboard_controller.press('d')
                time.sleep(0.05)
                keyboard_controller.release('d')

            time.sleep(0.10)
            mouse_controller.release(Button.right)
            keyboard_controller.release(Key.alt_l)
            time.sleep(0.10)
        else:
            time.sleep(0.1)

def on_press(key):
    global running, exit_flag, counter
    try:
        if key.char == ';':
            running = not running
            print("Running:", running)
            release_all()
            counter = 0
        elif key.char == 'q':
            print("Exiting...")
            release_all()
            exit_flag = True
            return False
    except AttributeError:
        pass

if __name__ == "__main__":
    thread = threading.Thread(target=press_combo)
    thread.daemon = True
    thread.start()

    with keyboard.Listener(on_press=on_press) as listener:
        listener.join()
