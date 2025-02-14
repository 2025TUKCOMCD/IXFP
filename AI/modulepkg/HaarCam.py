#
#  Haar Cascade processing (for camera)
#  2025-01-07, chlgideh
#

import cv2
from datetime import datetime

def HaarCam():
    haar_classifier = cv2.CascadeClassifier("D:/github_local/JZP/AI/opencv-4.x/data/haarcascades/haarcascade_frontalface_default.xml")

    capture = cv2.VideoCapture(0)
    capture.set(cv2.CAP_PROP_FRAME_WIDTH,1280)
    capture.set(cv2.CAP_PROP_FRAME_HEIGHT,720)

    count = 0           # detected frame counter
    detection = 1       # flag for consecutive detection frame
    frame_counter = 0   # counter for camera (30 frame per 1 sec)

    # image processing & show loop
    while True: 
        frame_counter+=1

        #image processing
        ret, frame = capture.read()
        # cf. frame = cv2.flip(frame, -1)
        frame_origin = frame                             # original frame copy for clean save (will be draw grid on frame)
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        
        faces = haar_classifier.detectMultiScale(
            gray,                   # image for detection
            scaleFactor = 1.3,      # scale up factor (n>=1), if use image resize, not necessary.
            minNeighbors = 6,       # gap between detected grid
            minSize=(320,180)       # minimun size of capture frame. ignore if small than this pixel.
        )
        
        # detection count
        if len(faces)>0:
            detection +=1

        # draw grid on detected area (face)
        # if consecutive detected
        if frame_counter/detection == frame_counter/2:  # evasion division by zero error
            for (x,y,w,h) in faces:
                now = datetime.now()
                current_time_str=now.strftime("%Y-%m-%d_(%H-%M-%S)")
                cv2.imwrite("D:/github_local/JZP/AI/capture_data/"+current_time_str+'_'+str(count/60+1)+".jpg",frame_origin[y-50:y+h+50, x-25:x+w+25])
                cv2.rectangle(frame,(x,y),(x+w,y+h),(255,0,0),2) # rectangle(image, start_point, end_point, color, thickness)
                count += 1
                detection=1
        # else, not consecutive just detected        
        for (x,y,w,h) in faces:
            count += 1
            if count%15==0:
                now = datetime.now()
                current_time_str=now.strftime("%Y-%m-%d_(%H-%M-%S)")
                cv2.imwrite("D:/github_local/JZP/AI/capture_data/"+current_time_str+'_'+str(count/60+1)+".jpg",frame_origin[y-50:y+h+50, x-25:x+w+25])
            cv2.rectangle(frame,(x,y),(x+w,y+h),(255,0,0),2)
        
        cv2.imshow('image',frame)
        
        if cv2.waitKey(1) > 0 : break

    #
    # 로직 문제로 사진 자체에 rectangle이 200장 중 하나 꼴로 포함된다.
    #

    print("\n Exiting Program and cleanup ...\n")

    capture.release() # release memory
    cv2.destroyAllWindows() #destroy windows