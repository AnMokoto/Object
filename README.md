# [FEATURE] Tensorflow Object Detection 

---


## LEARNING

* [tensorflow2](https://tf.wiki/index.html)
* [tensorflow-object-detection](https://tensorflow-object-detection-api-tutorial.readthedocs.io)


## Content
```markdown
.----android                     # ANDROID
.--------android_mediapipe       # [mediapipe](https://github.com/google/mediapipe)
.--------app                     # TESTING
.--------pipe                    # OBJECT DISTANCE POWER
·----annotations                 # MAKE XML AND TF RECORDS
·----convert                     # SCRIPT TO ANNOTATIONS
·----exported-models             # EXPORT MODELS
·----fruits-360                  # TRAIN AND TEST DATA
·----models                      # GENERATED TRAIN DATA 
·----pre-models                  # PRE MODELS
```

## DOING

### DOWNLOAD [tensorflow/models](https://github.com/tensorflow/models)
```shell
 tensorflow/models
```

### BUILD [#](https://tensorflow-object-detection-api-tutorial.readthedocs.io/en/latest/install.html#tensorflow-object-detection-api-installation)
```shell
# From within TensorFlow/models/research/
protoc object_detection/protos/*.proto --python_out=.
# From within TensorFlow/models/research/
cp object_detection/packages/tf2/setup.py .
python -m pip install --use-feature=2020-resolver .
```



### TRAIN MODEL [#](./models)
```shell
python tensorflow/models/research/object_detection/model_main_tf2.py \
--model_dir=models/ssd_mobilenet_v2_320x320_coco17_tpu-8/ \
--pipeline_config_path=models/ssd_mobilenet_v2_320x320_coco17_tpu-8/ssd_mobilenet_v2_320x320_coco17_tpu-8.config \
--logtostderr
```


### EXPORT MODEL [#](./exported-models) 
```shell
python tensorflow/models/research/object_detection/exporter_main_v2.py --input_type=image_tensor \
--pipeline_config_path=models/ssd_mobilenet_v2_320x320_coco17_tpu-8/ssd_mobilenet_v2_320x320_coco17_tpu-8.config \
--trained_checkpoint_dir=models/ssd_mobilenet_v2_320x320_coco17_tpu-8/ \
--output_directory=exported-models/ssd_mobilenet_v2_320x320_coco17_tpu-8
```