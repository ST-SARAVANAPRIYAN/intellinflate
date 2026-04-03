import cv2
import imutils
import numpy as np
import easyocr
import os
import json
import re

YOLO_MODEL_PATH = os.getenv('INTELLINFLATE_YOLO_MODEL', '').strip()
OCR_ENGINE = os.getenv('INTELLINFLATE_OCR_ENGINE', 'paddle').strip().lower()
_YOLO_MODEL = None
_PADDLE_OCR = None
MIN_OCR_CONFIDENCE = float(os.getenv('INTELLINFLATE_MIN_OCR_CONFIDENCE', '0.35'))
PLATE_REGEX = re.compile(r'^[A-Z]{2}\d{1,2}[A-Z]{1,3}\d{3,4}$')
PLATE_DISPLAY_REGEX = re.compile(r'^[A-Z]{2}\s\d{1,2}\s[A-Z]{1,3}\s\d{3,4}$')


def _load_yolo_model():
    global _YOLO_MODEL
    if _YOLO_MODEL is not None:
        return _YOLO_MODEL
    if not YOLO_MODEL_PATH or not os.path.exists(YOLO_MODEL_PATH):
        return None
    try:
        from ultralytics import YOLO  # Optional dependency for pretrained plate detector
        _YOLO_MODEL = YOLO(YOLO_MODEL_PATH)
        return _YOLO_MODEL
    except Exception:
        return None


def _find_plate_bbox_with_yolo(img):
    model = _load_yolo_model()
    if model is None:
        return None
    try:
        results = model.predict(img, verbose=False)
        if not results:
            return None
        boxes = results[0].boxes
        if boxes is None or len(boxes) == 0:
            return None
        best = max(boxes, key=lambda b: float(b.conf[0]))
        x1, y1, x2, y2 = best.xyxy[0].tolist()
        return int(x1), int(y1), int(x2), int(y2), float(best.conf[0])
    except Exception:
        return None


def _expand_bbox(x1, y1, x2, y2, width, height, padding=0.08):
    box_width = max(1, x2 - x1)
    box_height = max(1, y2 - y1)
    pad_x = int(box_width * padding)
    pad_y = int(box_height * padding)
    return (
        max(0, x1 - pad_x),
        max(0, y1 - pad_y),
        min(width - 1, x2 + pad_x),
        min(height - 1, y2 + pad_y),
    )


def _dedupe_boxes(boxes):
    deduped = []
    seen = set()
    for box in boxes:
        key = tuple(int(round(value / 8.0)) for value in box[:4])
        if key in seen:
            continue
        seen.add(key)
        deduped.append(box)
    return deduped


def _find_plate_bbox_candidates(img):
    candidates = []
    height, width = img.shape[:2]

    yolo_bbox = _find_plate_bbox_with_yolo(img)
    if yolo_bbox is not None:
        x1, y1, x2, y2, conf = yolo_bbox
        if conf >= 0.45:
            candidates.append((*_expand_bbox(int(x1), int(y1), int(x2), int(y2), width, height, padding=0.10), 'yolo', float(conf)))

    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    enhanced = cv2.equalizeHist(gray)
    variants = [
        cv2.bilateralFilter(enhanced, 11, 17, 17),
        cv2.GaussianBlur(enhanced, (5, 5), 0),
        cv2.Canny(enhanced, 40, 180),
        cv2.Canny(cv2.bilateralFilter(enhanced, 9, 75, 75), 30, 150),
    ]

    image_area = float(width * height)
    for variant in variants:
        work = variant.copy()
        if len(work.shape) == 2 and np.max(work) != 255:
            work = cv2.Canny(work, 30, 150)

        contours = cv2.findContours(work.copy(), cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)
        contours = imutils.grab_contours(contours)
        contours = sorted(contours, key=cv2.contourArea, reverse=True)[:20]

        for contour in contours:
            area = cv2.contourArea(contour)
            if area < image_area * 0.003:
                continue

            perimeter = cv2.arcLength(contour, True)
            approx = cv2.approxPolyDP(contour, 0.02 * perimeter, True)
            x, y, w, h = cv2.boundingRect(approx)
            if w < 40 or h < 16:
                continue

            aspect_ratio = w / float(h + 1e-5)
            if aspect_ratio < 1.2 or aspect_ratio > 7.5:
                continue

            fill_ratio = area / float(w * h + 1e-5)
            if fill_ratio < 0.25:
                continue

            x1, y1, x2, y2 = _expand_bbox(x, y, x + w - 1, y + h - 1, width, height, padding=0.06)
            detector_score = min(0.99, 0.35 + min(0.5, area / (image_area * 0.05)) + min(0.14, fill_ratio * 0.2))
            candidates.append((x1, y1, x2, y2, 'contour', detector_score))

    candidates.sort(key=lambda item: (item[5], (item[2] - item[0]) * (item[3] - item[1])), reverse=True)
    return _dedupe_boxes(candidates[:8])


def _plate_preprocess_variants(cropped_image):
    if cropped_image.ndim == 3:
        base_gray = cv2.cvtColor(cropped_image, cv2.COLOR_BGR2GRAY)
    else:
        base_gray = cropped_image.copy()

    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    enhanced = clahe.apply(base_gray)
    sharpen_kernel = np.array([[0, -1, 0], [-1, 5, -1], [0, -1, 0]], dtype=np.float32)
    sharpened = cv2.filter2D(enhanced, -1, sharpen_kernel)
    thresholded = cv2.adaptiveThreshold(
        sharpened,
        255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY,
        31,
        11,
    )

    return [base_gray, enhanced, sharpened, cv2.bitwise_not(thresholded)]


def _evaluate_plate_candidates(img, candidate_boxes):
    scored = []
    for x1, y1, x2, y2, source, detector_conf in candidate_boxes:
        cropped_image = img[y1:y2 + 1, x1:x2 + 1]
        for variant_index, variant in enumerate(_plate_preprocess_variants(cropped_image)):
            text_candidates, fallback_reason = _extract_text_candidates(variant)
            for text_raw, conf, engine in text_candidates:
                canonical = _canonicalize_plate(text_raw)
                if not canonical:
                    continue

                compact = re.sub(r'[^A-Z0-9]', '', canonical)
                structured = canonical != compact and re.match(r'^[A-Z]{2}\s\d{1,2}\s[A-Z]{1,3}\s\d{3,4}$', canonical)
                structure_bonus = 0.24 if structured else -0.08
                variant_bonus = 0.02 if variant_index == 0 else 0.0
                candidate_bonus = min(0.18, detector_conf * 0.18)
                score = conf + structure_bonus + variant_bonus + candidate_bonus + min(len(compact), 10) * 0.008
                scored.append({
                    'text': canonical,
                    'confidence': float(conf),
                    'engine': engine,
                    'fallbackReason': fallback_reason,
                    'score': float(score),
                    'bbox': (x1, y1, x2, y2),
                    'source': source,
                    'detectorConfidence': float(detector_conf),
                })

    scored.sort(key=lambda item: item['score'], reverse=True)
    return scored

# Initialize OCR once to avoid reloading it for every request
reader = easyocr.Reader(['en'], gpu=False, verbose=False)


def _load_paddle_ocr():
    global _PADDLE_OCR
    if _PADDLE_OCR is not None:
        return _PADDLE_OCR
    try:
        from paddleocr import PaddleOCR  # Optional stronger OCR backend
        _PADDLE_OCR = PaddleOCR(use_angle_cls=True, lang='en', show_log=False)
        return _PADDLE_OCR
    except Exception:
        return None


def _letters_only_with_corrections(segment):
    digit_to_letter = {'0': 'O', '1': 'I', '2': 'Z', '5': 'S', '6': 'G', '8': 'B'}
    out = []
    replacements = 0
    for ch in segment:
        if 'A' <= ch <= 'Z':
            out.append(ch)
        elif ch in digit_to_letter:
            out.append(digit_to_letter[ch])
            replacements += 1
        else:
            return None, 999
    return ''.join(out), replacements


def _digits_only_with_corrections(segment):
    letter_to_digit = {'O': '0', 'Q': '0', 'D': '0', 'I': '1', 'L': '1', 'Z': '2', 'S': '5', 'B': '8', 'G': '6'}
    out = []
    replacements = 0
    for ch in segment:
        if '0' <= ch <= '9':
            out.append(ch)
        elif ch in letter_to_digit:
            out.append(letter_to_digit[ch])
            replacements += 1
        else:
            return None, 999
    return ''.join(out), replacements


def _regex_correct_plate(raw_text):
    compact = re.sub(r'[^A-Z0-9]', '', (raw_text or '').upper())
    if len(compact) < 8 or len(compact) > 10:
        return None

    best = None
    best_score = 10**9
    # Indian registration plate pattern: AA(LL) 0-99 AA(A) 000-9999
    for rto_len in (2, 1):
        for series_len in (1, 2, 3):
            for number_len in (4, 3):
                if 2 + rto_len + series_len + number_len != len(compact):
                    continue

                s_state = compact[0:2]
                s_rto = compact[2:2 + rto_len]
                s_series = compact[2 + rto_len:2 + rto_len + series_len]
                s_number = compact[-number_len:]

                state, c1 = _letters_only_with_corrections(s_state)
                rto, c2 = _digits_only_with_corrections(s_rto)
                series, c3 = _letters_only_with_corrections(s_series)
                number, c4 = _digits_only_with_corrections(s_number)
                if None in (state, rto, series, number):
                    continue

                compact_candidate = f"{state}{rto}{series}{number}"
                if not PLATE_REGEX.match(compact_candidate):
                    continue

                display_candidate = f"{state} {rto} {series} {number}"
                score = c1 + c2 + c3 + c4
                if score < best_score:
                    best_score = score
                    best = display_candidate

    return best


def _canonicalize_plate(raw_text):
    corrected = _regex_correct_plate(raw_text)
    if corrected:
        return corrected

    compact = re.sub(r'[^A-Z0-9]', '', (raw_text or '').upper())
    if len(compact) < 6:
        return None

    # Fallback: return compact text grouped for readability
    return compact


def _extract_text_candidates(cropped_image):
    candidates = []
    fallback_reason = None

    if OCR_ENGINE != 'easyocr':
        paddle = _load_paddle_ocr()
        if paddle is not None:
            try:
                paddle_out = paddle.ocr(cropped_image, cls=True) or []
                for line in paddle_out:
                    for item in line or []:
                        text = (item[1][0] or '').strip().upper()
                        conf = float(item[1][1] or 0.0)
                        if text:
                            candidates.append((text, conf, 'paddleocr'))
            except Exception:
                fallback_reason = 'paddleocr_runtime_error'
        else:
            fallback_reason = 'paddleocr_unavailable'

        # Use EasyOCR only when PaddleOCR produced no usable output.
        if candidates:
            return candidates, None
        if fallback_reason is None:
            fallback_reason = 'paddleocr_no_text'

    # EasyOCR fallback when PaddleOCR is unavailable or returns empty output.
    easy_results = reader.readtext(cropped_image, allowlist='ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -')
    for item in easy_results:
        text = (item[1] or '').strip().upper()
        conf = float(item[2] or 0.0)
        if text:
            candidates.append((text, conf, 'easyocr'))

    if OCR_ENGINE == 'easyocr':
        return candidates, None
    return candidates, fallback_reason or 'paddleocr_fallback'

def detect_number_plate(image_path_or_frame):
    if isinstance(image_path_or_frame, str):
        if not os.path.exists(image_path_or_frame):
            return {"error": f"Image at {image_path_or_frame} not found."}
        img = cv2.imread(image_path_or_frame)
    else:
        img = image_path_or_frame # assume it's a numpy array (cv2 image frame)
        
    if img is None:
        return {"error": "Could not read image."}
    candidate_boxes = _find_plate_bbox_candidates(img)
    if not candidate_boxes:
        return {"error": "No number plate contour detected.", "image": img}

    scored_candidates = _evaluate_plate_candidates(img, candidate_boxes)
    if not scored_candidates:
        return {"error": "Could not read text from the detected plate.", "image": img}

    best_candidate = scored_candidates[0]
    if best_candidate['confidence'] < MIN_OCR_CONFIDENCE:
        strong_candidates = [item for item in scored_candidates if item['confidence'] >= MIN_OCR_CONFIDENCE]
        if strong_candidates:
            best_candidate = max(strong_candidates, key=lambda item: item['score'])
        else:
            best_candidate = max(scored_candidates[:5], key=lambda item: (item['confidence'], item['score']))

    text = best_candidate['text']
    confidence = max(0.0, min(0.99, best_candidate['confidence']))

    x_min, y_min, x_max, y_max = best_candidate['bbox']
    bbox = {
        "x": x_min,
        "y": y_min,
        "w": int(max(0, x_max - x_min)),
        "h": int(max(0, y_max - y_min))
    }

    font = cv2.FONT_HERSHEY_SIMPLEX
    res = cv2.rectangle(img, (x_min, y_min), (x_max, y_max), (0, 255, 0), 3)
    res = cv2.putText(
        res,
        text=text,
        org=(x_min, max(0, y_min - 12)),
        fontFace=font,
        fontScale=0.9,
        color=(0, 255, 0),
        thickness=2,
        lineType=cv2.LINE_AA,
    )
    
    return {
        "text": text,
        "confidence": confidence,
        "ocrEngine": best_candidate['engine'],
        "fallbackReason": best_candidate.get('fallbackReason'),
        "bbox": bbox,
        "imageSize": {
            "width": int(img.shape[1]),
            "height": int(img.shape[0])
        },
        "image": res,
        "modelSource": best_candidate['source'],
        "detectorConfidence": round(float(best_candidate['detectorConfidence']), 3)
    }

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Number Plate Detection Module")
    parser.add_argument("-i", "--image", required=True, help="Path to input vehicle image")
    args = parser.parse_args()
    res = detect_number_plate(args.image)
    if "error" in res:
        print(json.dumps({"success": False, "error": res["error"]}))
    else:
        print(json.dumps({
            "success": True,
            "text": res["text"],
            "confidence": round(float(res["confidence"]), 2),
            "ocrEngine": res.get("ocrEngine", "easyocr"),
            "fallbackReason": res.get("fallbackReason"),
            "bbox": res.get("bbox"),
            "imageSize": res.get("imageSize"),
            "modelSource": res.get("modelSource"),
            "detectorConfidence": res.get("detectorConfidence")
        }))
