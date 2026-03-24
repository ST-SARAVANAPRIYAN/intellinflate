import os
import cv2
import numpy as np
import base64
from flask import Flask, render_template, request, jsonify
from number_plate_detection import detect_number_plate

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = 'uploads'
os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "No file part"})
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "No selected file"})
    
    if file:
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], file.filename)
        file.save(filepath)
        
        # Process the image
        result = detect_number_plate(filepath)
        
        if "error" in result:
             return jsonify({"error": result["error"]})
        
        # Convert output image to base64 to send it back
        _, buffer = cv2.imencode('.jpg', result["image"])
        img_base64 = base64.b64encode(buffer).decode('utf-8')
        
        return jsonify({
            "success": True,
            "text": result["text"],
            "confidence": f"{result['confidence']:.2f}",
            "image": img_base64
        })

@app.route('/detect_frame', methods=['POST'])
def detect_frame():
    try:
        data = request.json
        image_data = data['image'].split(',')[1]
        nparr = np.frombuffer(base64.b64decode(image_data), np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
        result = detect_number_plate(img)
        if "error" in result:
             return jsonify({"error": result["error"]})
        
        _, buffer = cv2.imencode('.jpg', result["image"])
        img_base64 = base64.b64encode(buffer).decode('utf-8')
        
        return jsonify({
            "success": True,
            "text": result["text"],
            "confidence": f"{result['confidence']:.2f}",
            "image": img_base64
        })
    except Exception as e:
        return jsonify({"error": str(e)})

if __name__ == '__main__':
    app.run(debug=True, port=5000)
