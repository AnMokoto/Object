#include <jni.h>
#include <string>
#include <calculator.h>
#include <array>

using namespace std;

extern "C" {

static Calculator *calculator;

JNIEXPORT jint JNICALL
JNI_OnLoad(
        JavaVM *vm,
        void *reserved
) {

    JNIEnv *env;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) != JNI_OK) {
        return -1;
    }

    calculator = new Calculator();

    return JNI_VERSION_1_4;
}


JNIEXPORT jfloat JNICALL
Java_com_mokoto_pipe_Pipe_computeFocalLengthInPixels(
        JNIEnv *env,
        jobject thiz,
        jint width,
        jint height,
        jfloat focal_length_in_35mm,
        jfloat focal_length_mm) {

    assert(calculator != nullptr);

    calculator->calculateFocalLengthInPixels(
            std::make_pair(width, height),
            focal_length_in_35mm,
            focal_length_mm
    );

    return 1;
}


JNIEXPORT jfloat JNICALL
Java_com_mokoto_pipe_Pipe_calculateDepth(
        JNIEnv *env,
        jobject thiz,
        jint width,
        jint height,
        jobjectArray marks) {

    assert(calculator != nullptr);

    auto length = env->GetArrayLength(marks);
    if (length != 5) {
        return -2;
    }

    std::array<Point3 *, 5> ps{};
    for (int i = 0; i < length; ++i) {
        jobject object = env->GetObjectArrayElement(marks, i);
        jclass p3 = env->GetObjectClass(object);
        jfieldID x = env->GetFieldID(p3, "x", "F");
        jfieldID y = env->GetFieldID(p3, "y", "F");
        jfieldID z = env->GetFieldID(p3, "z", "F");
        auto *point3 = new Point3(
                env->GetFloatField(object, x),
                env->GetFloatField(object, y),
                env->GetFloatField(object, z)
        );
        ps[i] = point3;
        env->DeleteLocalRef(object);
    }

    return calculator->calculateDepth(
            std::make_pair(width, height),
            ps
    );

}


JNIEXPORT jint JNICALL
Java_com_mokoto_pipe_Pipe_initialization(
        JNIEnv *env,
        jobject thiz,
        jfloat focalLengthInPixel,
        jfloat in_meter
) {

//    calculator = new Calculator(in_meter);
    // initialization more ....
    calculator->setStandInMeter(in_meter);
    calculator->setFocalLengthInPixel(focalLengthInPixel);
    return 1;
}

}