#
#    sample pakage code for maintest
#    written by chlgideh, 2025-02-03
#

def take_screenshot(shared_data):
    with lock:
        screenshot = pyautogui.screenshot()
        print("...")
        shared_data['screenshot'] = screenshot
        print("screenshot saved...")
        screenshot_taken.set()
