import sys
import os
import ffmpeg
import cv2

if __name__ == "__main__":
  path = sys.argv[1]
  out_path = sys.argv[2]

  # get video fps using cv2
  cap = cv2.VideoCapture(path)
  fps = str(cap.get(cv2.CAP_PROP_FPS)).split('.')[0]

  print(path + " -r " + fps + "frame%d.png")

  # extract frames
  (
  ffmpeg.input(path)
  .output(out_path + "frame%03d.png", vf=f"fps={fps}")
  .run()
  )        
