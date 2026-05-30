# Bundled TensorFlow Lite OCR Models

This folder contains the TensorFlow Lite OCR example models referenced by the official TensorFlow Lite OCR documentation:

- `east_text_detector.tflite`
- `keras_ocr_recognizer.tflite`

Source references:

- https://www.tensorflow.org/lite/examples/optical_character_recognition/overview
- https://tfhub.dev/sayakpaul/lite-model/east-text-detector/fp16/1
- https://tfhub.dev/tulasiram58827/lite-model/keras-ocr/float16/2

The detector finds likely text regions. The recognizer reads cropped text regions. A complete OCR pipeline must connect both steps with decoding, non-max suppression, perspective/crop processing, and output decoding.
