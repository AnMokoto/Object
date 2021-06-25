"""
Usage:
  # From tensorflow/pre-models/
  # Create train data:
  python generate_tfrecord.py --csv_input=data/train_labels.csv  --output_path=train.record
  # Create test data:
  python generate_tfrecord.py --csv_input=data/test_labels.csv  --output_path=testypes.record
"""
import io
import os.path

import pandas as pd
import tensorflow as tf
from PIL import Image


def class_text_to_int(row_label):
    if row_label == 'apple':
        return 1
    else:
        return -1


# The following functions can be used to convert a value to a type compatible
# with annotations.Example.

def _bytes_feature(value):
    """Returns a bytes_list from a string / byte."""
    if isinstance(value, type(tf.constant(0))):
        value = value.numpy()  # BytesList won't unpack a string from an EagerTensor.
    return tf.train.Feature(bytes_list=tf.train.BytesList(value=[value]))


def _float_feature(value):
    """Returns a float_list from a float / double."""
    return tf.train.Feature(float_list=tf.train.FloatList(value=[value]))


def _int64_feature(value):
    """Returns an int64_list from a bool / enum / int / uint."""
    return tf.train.Feature(int64_list=tf.train.Int64List(value=[value]))


print(tf.__version__)


def conver2tfrecords(group, input_csv_file, output_tfrecord_file):
    # 将 iris.csv 保存成TFRecord文件
    iris_frame = pd.read_csv(input_csv_file, header=0)
    print(iris_frame)
    # label,sepal_length,sepal_width,petal_length,petal_width
    print("values shape: ", iris_frame.shape)
    row_count = iris_frame.shape[0]
    col_count = iris_frame.shape[1]
    with tf.io.TFRecordWriter(output_tfrecord_file) as writer:
        for i in range(1, row_count):
            filename = iris_frame.iloc[i, 0]
            image = tf.io.read_file("fruits-360/%s/%s" % (group, filename))
            width = iris_frame.iloc[i, 1]
            height = iris_frame.iloc[i, 2]
            example = tf.train.Example(
                features=tf.train.Features(
                    # ['filename', 'width', 'height', 'class', 'xmin', 'ymin', 'xmax', 'ymax']
                    feature={
                        'image/filename': _bytes_feature(filename.encode('utf8')),
                        'image/source_id': _bytes_feature(filename.encode('utf8')),
                        'image/width': _int64_feature(width),
                        'image/height': _int64_feature(height),
                        'image/object/bbox/xmin': _float_feature(iris_frame.iloc[i, 4] / width),
                        'image/object/bbox/xmax': _float_feature(iris_frame.iloc[i, 5] / width),
                        'image/object/bbox/ymin': _float_feature(iris_frame.iloc[i, 6] / height),
                        'image/object/bbox/ymax': _float_feature(iris_frame.iloc[i, 7] / height),
                        'image/object/class/text': _bytes_feature(iris_frame.iloc[i, 3].encode('utf8')),
                        'image/object/class/label': _int64_feature(class_text_to_int(iris_frame.iloc[i, 3])),
                        "image/format": _bytes_feature(b'jpg'),
                        'image/encoded': _bytes_feature(image)

                    }
                )
            )
            writer.write(record=example.SerializeToString())
        writer.close()


def main():
    conver2tfrecords("Training", "annotations/train.csv", "annotations/train.tfrecord")
    conver2tfrecords("Test", "annotations/testing.csv", "annotations/testing.tfrecord")
