#
#   Tensorflow-gpu. gpu grep test code
#   edited by chlgideh, 2025-01-19
#   if this program run correctly, you can see 'device 0(or 1. whatever) added' message
#
#   If you using tensorflow <= 2.10, might be some different. Recent Tensorflow is combined with tensorflow-cpu & tensorflow-gpu.
#   If you using tensorflow <= 2.*, keras lib and tensorflow lib structure is different. tensorflow is combined with keras after verson 2.x
#

from tensorflow.python.client import device_lib

print(device_lib.list_local_devices())