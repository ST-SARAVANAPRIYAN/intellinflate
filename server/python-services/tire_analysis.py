import cv2
import numpy as np
import os
import argparse
import json
import sys

CRACK_MODEL_PATH = os.getenv("INTELLINFLATE_CRACK_MODEL", "").strip()
TREAD_MODEL_PATH = os.getenv("INTELLINFLATE_TREAD_MODEL", "").strip()
CRACK_YOLO_MODEL_PATH = os.getenv("INTELLINFLATE_CRACK_YOLO_MODEL", "").strip()
WEAR_YOLO_MODEL_PATH = os.getenv("INTELLINFLATE_WEAR_YOLO_MODEL", "").strip()
_TF_MODEL_CACHE = {}
_YOLO_MODEL_CACHE = {}
PRETRAINED_CRACK_MIN_CONFIDENCE = float(os.getenv("INTELLINFLATE_CRACK_MIN_CONFIDENCE", "0.90"))
PRETRAINED_TREAD_MIN_CONFIDENCE = float(os.getenv("INTELLINFLATE_TREAD_MIN_CONFIDENCE", "0.82"))


def _estimate_image_quality(gray):
    # Blur via variance of Laplacian, exposure via mean intensity.
    blur_score = float(cv2.Laplacian(gray, cv2.CV_64F).var())
    mean_intensity = float(np.mean(gray))
    quality = "good"
    if blur_score < 80 or mean_intensity < 40 or mean_intensity > 220:
        quality = "poor"
    elif blur_score < 150:
        quality = "fair"
    return {
        "quality": quality,
        "blurScore": round(blur_score, 2),
        "meanIntensity": round(mean_intensity, 2)
    }


def _load_tf_model(model_path):
    if not model_path:
        return None
    if model_path in _TF_MODEL_CACHE:
        return _TF_MODEL_CACHE[model_path]
    if not os.path.exists(model_path):
        return None
    try:
        import tensorflow as tf  # Optional dependency for pretrained models
        model = tf.keras.models.load_model(model_path)
        _TF_MODEL_CACHE[model_path] = model
        return model
    except Exception:
        return None


def _load_yolo_model(model_path):
    if not model_path:
        return None
    if model_path in _YOLO_MODEL_CACHE:
        return _YOLO_MODEL_CACHE[model_path]
    if not os.path.exists(model_path):
        return None
    try:
        from ultralytics import YOLO  # Optional dependency for stronger object detection
        model = YOLO(model_path)
        _YOLO_MODEL_CACHE[model_path] = model
        return model
    except Exception:
        return None


def _predict_with_pretrained_model(image, model_path, labels):
    model = _load_tf_model(model_path)
    if model is None:
        return None

    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    resized = cv2.resize(rgb, (224, 224)).astype(np.float32) / 255.0
    batch = np.expand_dims(resized, axis=0)

    preds = model.predict(batch, verbose=0)
    if preds.ndim == 2 and preds.shape[1] == 1:
        score = float(preds[0][0])
        idx = 1 if score >= 0.5 else 0
        conf = score if idx == 1 else (1.0 - score)
    else:
        flat = preds[0]
        idx = int(np.argmax(flat))
        conf = float(flat[idx])

    label = labels[idx] if idx < len(labels) else str(idx)
    return {"label": label, "confidence": round(conf, 4)}


def _predict_with_yolo_model(image, model_path, labels=None, min_confidence=0.25):
    model = _load_yolo_model(model_path)
    if model is None:
        return []

    try:
        results = model.predict(source=image, conf=min_confidence, verbose=False)
    except Exception:
        return []

    if not results:
        return []

    result = results[0]
    names = getattr(result, "names", None) or getattr(model, "names", {})
    boxes = getattr(result, "boxes", None)
    if boxes is None:
        return []

    detections = []
    for box in boxes:
        try:
            confidence = float(box.conf[0]) if hasattr(box.conf, "__len__") else float(box.conf)
            class_index = int(box.cls[0]) if hasattr(box.cls, "__len__") else int(box.cls)
            if isinstance(names, dict):
                label = names.get(class_index, str(class_index))
            elif isinstance(names, (list, tuple)) and class_index < len(names):
                label = names[class_index]
            else:
                label = str(class_index)

            if labels and label not in labels:
                continue

            xyxy = box.xyxy[0].tolist()
            x1, y1, x2, y2 = [float(value) for value in xyxy]
            detections.append({
                "x": int(max(0, round(x1))),
                "y": int(max(0, round(y1))),
                "w": int(max(1, round(x2 - x1))),
                "h": int(max(1, round(y2 - y1))),
                "label": label,
                "confidence": round(confidence, 4)
            })
        except Exception:
            continue

    return detections


def _detect_cracks_with_heuristics(gray):
    quality = _estimate_image_quality(gray)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    enhanced = clahe.apply(gray)
    denoised = cv2.bilateralFilter(enhanced, 7, 45, 45)

    blackhat = cv2.morphologyEx(denoised, cv2.MORPH_BLACKHAT, np.ones((9, 9), np.uint8))
    ridge = cv2.morphologyEx(denoised, cv2.MORPH_GRADIENT, np.ones((3, 3), np.uint8))
    blurred = cv2.GaussianBlur(blackhat, (3, 3), 0)
    edges_a = cv2.Canny(blurred, 20, 90)
    edges_b = cv2.Canny(ridge, 25, 100)
    edges = cv2.bitwise_or(edges_a, edges_b)

    line_mask = np.zeros_like(edges)
    lines = cv2.HoughLinesP(edges, 1, np.pi / 180, threshold=30, minLineLength=max(30, gray.shape[1] // 18), maxLineGap=18)
    if lines is not None:
        for line in lines[:, 0]:
            x1, y1, x2, y2 = [int(value) for value in line]
            cv2.line(line_mask, (x1, y1), (x2, y2), 255, 2)

    kernel = np.ones((3, 3), np.uint8)
    dilated = cv2.dilate(cv2.bitwise_or(edges, line_mask), kernel, iterations=1)

    contours, _ = cv2.findContours(dilated, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    cracks = []
    for cnt in contours:
        length = cv2.arcLength(cnt, True)
        area = cv2.contourArea(cnt)
        if length <= 30 or area <= 12:
            continue

        x, y, w, h = cv2.boundingRect(cnt)
        rect = cv2.minAreaRect(cnt)
        width, height = rect[1]
        aspect_ratio = max(width, height) / (min(width, height) + 1e-5)
        solidity = area / (w * h + 1e-5)
        linearity = min(1.0, length / max(1.0, float(max(w, h)) * 4.0))
        crack_score = (aspect_ratio * 0.42) + ((1.0 - solidity) * 0.45) + (linearity * 0.4)

        if aspect_ratio > 2.8 and solidity < 0.72 and crack_score > 1.0:
            if crack_score > 2.15:
                severity = "High"
            elif crack_score > 1.45:
                severity = "Medium"
            else:
                severity = "Low"

            cracks.append({
                "x": int(x),
                "y": int(y),
                "w": int(w),
                "h": int(h),
                "length": round(length, 2),
                "severity": severity,
                "confidence": round(min(0.99, max(0.58, 0.58 + crack_score * 0.18)), 4),
                "crackType": "longitudinal" if aspect_ratio >= 5 else "surface"
            })

    yolo_cracks = _predict_with_yolo_model(
        image= cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR),
        model_path=CRACK_YOLO_MODEL_PATH,
        labels=["crack", "damage", "sidewall_crack"],
        min_confidence=0.25
    )
    for detection in yolo_cracks:
        cracks.append({
            "x": int(detection["x"]),
            "y": int(detection["y"]),
            "w": int(detection["w"]),
            "h": int(detection["h"]),
            "length": round(float(max(detection["w"], detection["h"])), 2),
            "severity": "High" if detection["confidence"] >= 0.8 else "Medium",
            "confidence": float(detection["confidence"]),
            "crackType": detection.get("label", "crack")
        })

    unique_cracks = []
    seen_boxes = set()
    for crack in cracks:
        key = (crack["x"] // 8, crack["y"] // 8, crack["w"] // 8, crack["h"] // 8)
        if key in seen_boxes:
            continue
        seen_boxes.add(key)
        unique_cracks.append(crack)

    cracks = unique_cracks

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
        "message": f"Detected {len(cracks)} potential sidewall cracks with local markers.",
        "imageQuality": quality,
        "modelSource": "hybrid-crack-detector",
        "imageSize": {"width": int(gray.shape[1]), "height": int(gray.shape[0])},
        "visualDetections": {
            "crackRegions": [
                {
                    "x": int(c["x"]),
                    "y": int(c["y"]),
                    "w": int(c["w"]),
                    "h": int(c["h"]),
                    "severity": c.get("severity", "Medium"),
                    "confidence": float(c.get("confidence", 0.6)),
                    "label": c.get("crackType", "crack")
                }
                for c in cracks
            ],
            "defectMarkers": [
                {
                    "kind": "crack",
                    "x": int(c["x"]),
                    "y": int(c["y"]),
                    "w": int(c["w"]),
                    "h": int(c["h"]),
                    "severity": c.get("severity", "Medium"),
                    "confidence": float(c.get("confidence", 0.6))
                }
                for c in cracks
            ]
        }
    }


def _detect_tread_alignment_heuristics(gray, quality, pretrained_tread=None):
    height, width = gray.shape

    roi_x = width // 4
    roi_y = height // 4
    roi_w = width // 2
    roi_h = height // 2
    roi = gray[roi_y:roi_y + roi_h, roi_x:roi_x + roi_w]
    sobel_x = cv2.Sobel(roi, cv2.CV_32F, 1, 0, ksize=3)
    sobel_y = cv2.Sobel(roi, cv2.CV_32F, 0, 1, ksize=3)
    laplacian = cv2.Laplacian(roi, cv2.CV_32F)
    grad_mag = cv2.magnitude(sobel_x, sobel_y)
    norm_grad = cv2.normalize(grad_mag, None, 0, 1.0, cv2.NORM_MINMAX)
    texture_energy = float(np.mean(norm_grad))
    lap_energy = float(np.mean(np.abs(laplacian))) / 255.0
    contrast_energy = float(np.std(roi)) / 128.0
    combined_energy = (texture_energy * 0.55) + (lap_energy * 0.25) + (contrast_energy * 0.20)

    tread_depth = round(1.8 + (combined_energy * 8.5), 1)
    tread_status = "Good"
    tread_level = 0
    tread_source = "opencv-heuristics"

    if pretrained_tread is not None:
        mapping = {
            "healthy": ("Healthy", 0, 7.2),
            "moderate_wear": ("Moderate Wear", 1, 3.5),
            "replace_soon": ("Replace Soon", 2, 1.8)
        }
        mapped = mapping.get(pretrained_tread["label"], ("Moderate Wear", 1, 3.0))
        tread_status, tread_level, mapped_depth = mapped
        tread_depth = mapped_depth
        tread_source = "pretrained-transfer-learning"
    else:
        if tread_depth < 3.0:
            tread_status = "Worn"
            tread_level = 1
        if tread_depth < 1.6:
            tread_status = "Critical (Illegal)"
            tread_level = 2

    strip_w = width // 4
    strips = [gray[:, i * strip_w: (i + 1) * strip_w] for i in range(4)]
    strip_energies = []
    strip_regions = []
    for s in strips:
        gx = cv2.Sobel(s, cv2.CV_32F, 1, 0, ksize=3)
        gy = cv2.Sobel(s, cv2.CV_32F, 0, 1, ksize=3)
        strip_energies.append(float(np.mean(cv2.magnitude(gx, gy))))

    energy_array = np.array(strip_energies, dtype=np.float32)
    energy_mean = float(np.mean(energy_array)) if len(energy_array) > 0 else 0.0
    energy_std = float(np.std(energy_array)) if len(energy_array) > 0 else 0.0
    lowest_energy = float(np.min(energy_array)) if len(energy_array) > 0 else 0.0
    wear_deviation = 0.0 if energy_mean <= 1e-6 else max(0.0, (energy_mean - lowest_energy) / energy_mean)

    wear_regions = []
    for i in range(4):
        x0 = i * strip_w
        w0 = width - x0 if i == 3 else strip_w
        strip_regions.append({
            "index": i,
            "x": int(x0),
            "y": 0,
            "w": int(w0),
            "h": int(height),
            "textureEnergy": round(strip_energies[i], 3),
            "wearScore": round(max(0.0, wear_deviation * 100 - (strip_energies[i] - lowest_energy)), 2)
        })
        relative_wear = 0.0 if energy_mean <= 1e-6 else max(0.0, (energy_mean - strip_energies[i]) / energy_mean)
        if relative_wear > 0.08:
            wear_regions.append({
                "index": i,
                "x": int(x0),
                "y": 0,
                "w": int(w0),
                "h": int(height),
                "severity": "High" if relative_wear > 0.22 else "Medium",
                "confidence": round(min(0.98, 0.62 + relative_wear * 1.4), 4),
                "wearLevel": round(relative_wear, 4)
            })

    yolo_wear = _predict_with_yolo_model(
        image=cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR),
        model_path=WEAR_YOLO_MODEL_PATH,
        labels=["wear", "tread_wear", "abrasion"],
        min_confidence=0.25
    )
    for detection in yolo_wear:
        wear_regions.append({
            "index": -1,
            "x": int(detection["x"]),
            "y": int(detection["y"]),
            "w": int(detection["w"]),
            "h": int(detection["h"]),
            "severity": "High" if detection["confidence"] >= 0.8 else "Medium",
            "confidence": float(detection["confidence"]),
            "wearLevel": float(detection["confidence"])
        })

    x_axis = np.array([0, 1, 2, 3])
    y_axis = np.array(strip_energies)
    slope, _ = np.polyfit(x_axis, y_axis, 1)

    alignment_angle = round(abs(slope) / max(8.0, energy_mean + 1e-5), 2)
    alignment_status = "Balanced"
    alignment_level = 0
    if alignment_angle > 0.55:
        alignment_status = "Minor Misalignment"
        alignment_level = 1
    if alignment_angle > 1.15:
        alignment_status = "Severe Misalignment"
        alignment_level = 2

    wear_level = 0
    wear_status = "Healthy"
    wear_depth = round(7.4 - (wear_deviation * 9.2) - (energy_std * 0.15), 1)
    if wear_deviation > 0.11:
        wear_status = "Moderate Wear"
        wear_level = 1
    if wear_deviation > 0.22:
        wear_status = "Severe Wear"
        wear_level = 2

    wear_confidence = round(min(0.98, max(0.58, 0.66 + wear_deviation * 1.2 + (energy_std / max(1.0, energy_mean + 1e-5)) * 0.08)), 4)

    return {
        "tread": {
            "status": tread_status,
            "value": f"{tread_depth} mm",
            "level": tread_level,
            "modelSource": tread_source
        },
        "wear": {
            "status": wear_status,
            "value": f"{wear_depth} mm",
            "level": wear_level,
            "confidence": wear_confidence,
            "modelSource": "hybrid-wear-detector"
        },
        "alignment": {
            "status": alignment_status,
            "value": f"{alignment_angle}°",
            "level": alignment_level,
            "modelSource": "opencv-rule-based"
        },
        "imageQuality": quality,
        "overall_score": max(0, 100 - (tread_level * 26) - (wear_level * 22) - (alignment_level * 18) - (15 if quality["quality"] == "poor" else 0)),
        "imageSize": {"width": int(width), "height": int(height)},
        "visualDetections": {
            "treadRegion": {"x": int(roi_x), "y": int(roi_y), "w": int(roi_w), "h": int(roi_h)},
            "alignmentRegions": strip_regions,
            "wearRegions": wear_regions,
            "dominantStripIndex": int(np.argmax(np.abs(np.array(strip_energies) - float(np.mean(strip_energies))))) if len(strip_energies) > 0 else 0,
            "textureSpread": round(energy_std, 3)
        }
    }

def analyze_cracks_side_view(image):
    """
    Specialized analysis for Side View images to detect sidewall cracks.
    """
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    img_h, img_w = gray.shape

    quality = _estimate_image_quality(gray)

    pretrained = _predict_with_pretrained_model(
        image,
        CRACK_MODEL_PATH,
        ["no_crack", "crack"]
    )
    heuristic = _detect_cracks_with_heuristics(gray)

    if pretrained is not None:
        is_crack = pretrained["label"] == "crack"
        confidence = pretrained["confidence"]
        if confidence >= PRETRAINED_CRACK_MIN_CONFIDENCE:
            if is_crack and heuristic["count"] == 0 and confidence < 0.98:
                return {
                    **heuristic,
                    "message": f"Pretrained crack model reported a crack at {confidence * 100:.1f}%, but no localized crack pattern was confirmed.",
                    "modelSource": "opencv-heuristics",
                    "fallbackReason": "Pretrained crack model was not corroborated by heuristics"
                }

            severity = "High" if is_crack and confidence >= 0.85 else "Medium"
            return {
                "status": "Cracks Detected" if is_crack else "Healthy",
                "level": 2 if severity == "High" else (1 if is_crack else 0),
                "count": 1 if is_crack else 0,
                "details": ([{"severity": severity, "confidence": confidence}] if is_crack else []),
                "message": f"Pretrained crack classifier result: {pretrained['label']} ({confidence * 100:.1f}%).",
                "modelSource": "pretrained-transfer-learning",
                "imageSize": {"width": int(img_w), "height": int(img_h)},
                "visualDetections": {
                    "crackRegions": [],
                    "note": "Pretrained classifier used; localized crack boxes unavailable for this model output."
                }
            }

        if not is_crack and heuristic["count"] == 0:
            return {
                **heuristic,
                "message": f"Pretrained model leaned toward {pretrained['label']} ({confidence * 100:.1f}%), but heuristics did not find a crack pattern.",
                "modelSource": "opencv-heuristics",
                "fallbackReason": "Pretrained model confidence below threshold"
            }

        if is_crack and heuristic["count"] > 0:
            return {
                **heuristic,
                "message": f"Heuristic crack regions found; pretrained confidence was only {confidence * 100:.1f}%.",
                "modelSource": "opencv-heuristics",
                "fallbackReason": "Pretrained crack model confidence below threshold"
            }

    return heuristic

def analyze_tread_and_alignment_front(image):
    """
    Specialized analysis for Front View images to detect tread depth and misalignment angle.
    """
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    quality = _estimate_image_quality(gray)

    pretrained_tread = _predict_with_pretrained_model(
        image,
        TREAD_MODEL_PATH,
        ["healthy", "moderate_wear", "replace_soon"]
    )
    heuristic = _detect_tread_alignment_heuristics(gray, quality, pretrained_tread=None)

    if pretrained_tread is not None and pretrained_tread["confidence"] >= PRETRAINED_TREAD_MIN_CONFIDENCE:
        heuristic["tread"]["status"] = {
            "healthy": "Healthy",
            "moderate_wear": "Moderate Wear",
            "replace_soon": "Replace Soon"
        }.get(pretrained_tread["label"], "Moderate Wear")
        heuristic["tread"]["level"] = {
            "healthy": 0,
            "moderate_wear": 1,
            "replace_soon": 2
        }.get(pretrained_tread["label"], 1)
        heuristic["tread"]["value"] = {
            "healthy": "7.2 mm",
            "moderate_wear": "3.5 mm",
            "replace_soon": "1.8 mm"
        }.get(pretrained_tread["label"], "3.0 mm")
        heuristic["tread"]["modelSource"] = "pretrained-transfer-learning"
        heuristic["message"] = f"Pretrained tread classifier result: {pretrained_tread['label']} ({pretrained_tread['confidence'] * 100:.1f}%)."
        return heuristic

    if pretrained_tread is not None:
        heuristic["message"] = f"Pretrained tread model confidence was only {pretrained_tread['confidence'] * 100:.1f}%; using OpenCV heuristics instead."
        heuristic["fallbackReason"] = "Pretrained tread model confidence below threshold"

    return heuristic

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
