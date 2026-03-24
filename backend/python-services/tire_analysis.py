import cv2
import numpy as np
import os
import argparse
import json
import sys

def analyze_cracks_side_view(image):
    """
    Specialized analysis for Side View images to detect sidewall cracks.
    """
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    # Enhance contrast for crack detection
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
    enhanced = clahe.apply(gray)
    
    # Use Canny with specific thresholds for fine cracks
    blurred = cv2.GaussianBlur(enhanced, (3, 3), 0)
    edges = cv2.Canny(blurred, 40, 120)
    
    # Use Morphological operations to close small gaps in potential cracks
    kernel = np.ones((3,3), np.uint8)
    dilated = cv2.dilate(edges, kernel, iterations=1)
    
    contours, _ = cv2.findContours(dilated, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    cracks = []
    for cnt in contours:
        length = cv2.arcLength(cnt, True)
        if length > 50:
            x, y, w, h = cv2.boundingRect(cnt)
            rect = cv2.minAreaRect(cnt)
            (width, height) = rect[1]
            aspect_ratio = max(width, height) / (min(width, height) + 1e-5)
            
            if aspect_ratio > 5: # Highly elongated = likely crack
                cracks.append({
                    "x": int(x), "y": int(y), "w": int(w), "h": int(h),
                    "length": round(length, 2),
                    "severity": "High" if length > 150 else "Medium"
                })

    status = "Healthy"
    level = 0
    if len(cracks) > 0:
        status = "Cracks Detected"
        level = 2 if any(c['severity'] == "High" for c in cracks) else 1
        
    return {
        "status": status,
        "level": level,
        "count": len(cracks),
        "details": cracks,
        "message": f"Detected {len(cracks)} potential sidewall cracks."
    }

def analyze_tread_and_alignment_front(image):
    """
    Specialized analysis for Front View images to detect tread depth and misalignment angle.
    """
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    height, width = gray.shape
    
    # 1. Tread Depth Analysis (Edge density in the center area)
    roi_h = height // 2
    roi_w = width // 2
    roi = gray[height//4:height//4*3, width//4:width//4*3]
    edges = cv2.Canny(roi, 50, 150)
    edge_density = np.sum(edges > 0) / (edges.shape[0] * edges.shape[1])
    
    tread_depth = round(1.6 + (edge_density * 40), 1) # Estimated mm
    tread_status = "Good"
    tread_level = 0
    if tread_depth < 3.0:
        tread_status = "Worn"
        tread_level = 1
    if tread_depth < 1.6:
        tread_status = "Critical (Illegal)"
        tread_level = 2

    # 2. Misalignment Analysis (Wear gradient / Angle)
    # Split the tread into 4 vertical strips and compare intensities
    strip_w = width // 4
    strips = [gray[:, i*strip_w : (i+1)*strip_w] for i in range(4)]
    strip_means = [np.mean(s) for s in strips]
    
    # Calculate wear gradient (slope)
    # If one side is significantly brighter/darker, it indicates uneven wear
    x_axis = np.array([0, 1, 2, 3])
    y_axis = np.array(strip_means)
    slope, _ = np.polyfit(x_axis, y_axis, 1)
    
    # Map slope to an angle (simulation of misalignment angle)
    alignment_angle = round(abs(slope) / 5, 2)
    alignment_status = "Balanced"
    alignment_level = 0
    if alignment_angle > 1.5:
        alignment_status = "Minor Misalignment"
        alignment_level = 1
    if alignment_angle > 3.0:
        alignment_status = "Severe Misalignment"
        alignment_level = 2

    return {
        "tread": {
            "status": tread_status,
            "value": f"{tread_depth} mm",
            "level": tread_level
        },
        "alignment": {
            "status": alignment_status,
            "value": f"{alignment_angle}°",
            "level": alignment_level
        },
        "overall_score": max(0, 100 - (tread_level * 30) - (alignment_level * 20))
    }

def process_analysis(image_path, mode):
    img = cv2.imread(image_path)
    if img is None:
        return {"success": False, "error": "Could not read image"}
        
    if mode == "SIDE":
        results = analyze_cracks_side_view(img)
        return {"success": True, "mode": "SIDE", "cracks": results}
    elif mode == "FRONT":
        results = analyze_tread_and_alignment_front(img)
        return {"success": True, "mode": "FRONT", **results}
    else:
        return {"success": False, "error": f"Invalid mode: {mode}"}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-i", "--image", required=True)
    parser.add_argument("-m", "--mode", required=True, choices=["SIDE", "FRONT"])
    args = parser.parse_args()
    
    result = process_analysis(args.image, args.mode)
    print(json.dumps(result))
