#
#    main.py testing code (removable)
#    written by chlgideh 2025-02-03
#

import threading
import time
import pyautogui

lock = threading.Lock()

screenshot_taken = threading.Event()

def take_screenshot(shared_data):
    with lock:
        screenshot = pyautogui.screenshot()
        print("...")
        shared_data['screenshot'] = screenshot
        print("screenshot saved...")
        screenshot_taken.set()

def take_screenshot(shared_data):
    with lock:
        screenshot = pyautogui.screenshot()
        print("...")
        shared_data['screenshot'] = screenshot
        print("screenshot saved...")
        screenshot_taken.set()
        
shared_data = {}

thread1 = threading.Thread(target=take_screenshot, args=(shared_data,))
thread2 = threading.Thread(target=use_screenshot, args=(shared_data,))

thread1.start()
thread2.start()

thread1.join()
thread2.join()
