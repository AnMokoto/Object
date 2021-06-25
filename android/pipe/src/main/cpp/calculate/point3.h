//
// Created by android-1 on 2021/6/9.
//

#ifndef APP_POINT3_H
#define APP_POINT3_H

class Point3 {
private:
    double x, y, z;
public:
    Point3(double x, double y, double z) : x(x), y(y), z(z) {}

    double getX() const {
        return x;
    }

    void setX(double x) {
        Point3::x = x;
    }

    double getY() const {
        return y;
    }

    void setY(double y) {
        Point3::y = y;
    }

    double getZ() const {
        return z;
    }

    void setZ(double z) {
        Point3::z = z;
    }
};

#endif