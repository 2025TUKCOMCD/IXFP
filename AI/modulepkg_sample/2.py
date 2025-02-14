#
#    sample package code for maintest
#    written by chlgideh, 2025-02-03
#

def use_screenshot(shared_data):
    screenshot_taken.wait()
    with lock:
        print("modeule 2 awake")
        screenshot = shared_data.get('screenshot')
        if screenshot:
            screenshot.show()
