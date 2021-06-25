//
// Created by android-1 on 2021/6/9.
//
#ifndef APP_POINT2_H
#define APP_POINT2_H


#include "../../../../../../../env/Android/Sdk/ndk/21.0.6113669/toolchains/llvm/prebuilt/windows-x86_64/sysroot/usr/include/c++/v1/utility"

class Point2 {
private:
    double x, y;
public:
    Point2(const std::pair<int, int> &size) : x(size.first), y(size.second) {

    }

    Point2(double x, double y) : x(x), y(y) {}

    double getX() const {
        return x;
    }

    void setX(double x) {
        Point2::x = x;
    }

    double getY() const {
        return y;
    }

    void setY(double y) {
        Point2::y = y;
    }
};

#endif