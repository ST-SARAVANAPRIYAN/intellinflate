import cv2
import imutils
import numpy as np
import easyocr
import os

# Initialize reader once to avoid reloading it for every request
print("Initializing EasyOCR reader...")
reader = easyocr.Reader(['en'], gpu=False)
print("EasyOCR initialized.")

def detect_number_plate(image_path_or_frame):
    if isinstance(image_path_or_frame, str):
        if not os.path.exists(image_path_or_frame):
            return {"error": f"Image at {image_path_or_frame} not found."}
        img = cv2.imread(image_path_or_frame)
    else:
        img = image_path_or_frame # assume it's a numpy array (cv2 image frame)
        
    if img is None:
        return {"error": "Could not read image."}

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    # Noise reduction
    bfilter = cv2.bilateralFilter(gray, 11, 17, 17) 
    
    # Edge detection
    edged = cv2.Canny(bfilter, 30, 200) 
    
    # Find contours
    keypoints = cv2.findContours(edged.copy(), cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
    contours = imutils.grab_contours(keypoints)
    contours = sorted(contours, key=cv2.contourArea, reverse=True)[:10]
    
    location = None
    for contour in contours:
        approx = cv2.approxPolyDP(contour, 10, True)
        if len(approx) == 4:
            location = approx
            break
            
    if location is None:
         return {"error": "No number plate contour detected.", "image": img}
        
    mask = np.zeros(gray.shape, np.uint8)
    new_image = cv2.drawContours(mask, [location], 0, 255, -1)
    new_image = cv2.bitwise_and(img, img, mask=mask)
    
    (x, y) = np.where(mask == 255)
    (x1, y1) = (np.min(x), np.min(y))
    (x2, y2) = (np.max(x), np.max(y))
    cropped_image = gray[x1:x2+1, y1:y2+1]
    
    # Use allowlist to focus the model on uppercase letters and numbers
    # This prevents the AI from mistaking numbers for lowercase letters or random symbols
    result = reader.readtext(cropped_image, allowlist='ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -')
    
    if not result:
        return {"error": "Could not read text from the detected plate.", "image": img}
        
    # Combine text results in case the plate was broken into multiple fragments
    text_fragments = []
    total_conf = 0
    valid_fragments = 0
    
    for r in result:
        # Filter out random single characters with low confidence
        if len(r[-2].replace(' ', '')) > 2 or r[-1] > 0.5:
            text_fragments.append(r[-2])
            total_conf += r[-1]
            valid_fragments += 1
            
    if valid_fragments == 0:
        # Fallback to the first detection if everything was filtered out
        text_fragments.append(result[0][-2])
        total_conf += result[0][-1]
        valid_fragments = 1
        
    text = " ".join(text_fragments)
    confidence = total_conf / valid_fragments
    
    font = cv2.FONT_HERSHEY_SIMPLEX
    res = cv2.putText(img, text=text, org=(location[0][0][0], location[1][0][1]+60), fontFace=font, fontScale=1, color=(0,255,0), thickness=2, lineType=cv2.LINE_AA)
    res = cv2.rectangle(img, tuple(location[0][0]), tuple(location[2][0]), (0,255,0), 3)
    
    return {"text": text, "confidence": confidence, "image": res}

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Number Plate Detection Module")
    parser.add_argument("-i", "--image", required=True, help="Path to input vehicle image")
    args = parser.parse_args()
    res = detect_number_plate(args.image)
    if "error" in res:
        print(res["error"])
    else:
        print(f"Detected: {res['text']} (Confidence: {res['confidence']:.2f})")
