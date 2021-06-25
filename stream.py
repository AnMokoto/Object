from absl import app, flags, logging
import tensorflow as tf
import numpy as np
import cv2
import os


@tf.function
def detect_fn(image):
    pass


def main(_):
    model = tf.saved_model.load("exported-models/ssd_mobilenet_v2_320x320_coco17_tpu-8")
    pass


if __name__ == '__main__':
    os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
    tf.get_logger().setLevel('ERROR')  # Suppress TensorFlow logging (2)
    app.run(main)
