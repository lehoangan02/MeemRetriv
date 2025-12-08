import sys
import cv2
import numpy as np
import easyocr

reader = easyocr.Reader(['en'], gpu=False)

input_path = sys.argv[1]
output_mask = sys.argv[2]

img = cv2.imread(input_path)
results = reader.readtext(img)

mask = np.zeros(img.shape[:2], dtype=np.uint8)

for (bbox, text, prob) in results:
    pts = np.array([bbox], dtype=np.int32)
    cv2.fillPoly(mask, pts, 255)

kernel = np.ones((9, 9), np.uint8)
mask = cv2.dilate(mask, kernel, iterations=2)

cv2.imwrite(output_mask, mask)
