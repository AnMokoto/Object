//
// Created by android-1 on 2021/6/9.
//
//#include "exif.h"

#ifndef APP_CALCULATOR_H
#define APP_CALCULATOR_H

#include <cmath>

#include "point3.h"
#include <array>

static double SENSOR_DIAGONAL_35MM = std::sqrt(1872.0);

class Calculator {
private:
    double standInMeter = 10;
    double focal_length_in_pixel = 0;

    inline double computeDepth(
            const std::pair<int, int> &p0,
            const std::pair<int, int> &p1
    );

    inline float computeLandmarkDepth(
            const Point3 &l0,
            const Point3 &l1,
            const std::pair<int, int> &size
    );

    float computeDiameter(
            const std::array<Point3 *, 4> &marks,
            const std::pair<int, int> &size
    );
//
//    float calculateDepth(
//            const Point3 &center,
//            float focal_length,
//            float diameter,
//            const std::pair<int, int> &size
//    );

public:

    Calculator();

    Calculator(double inMeter):standInMeter(inMeter){}

    double getStandInMeter() const;

    void setStandInMeter(double standInMeter);

    double getFocalLengthInPixel() const;

    void setFocalLengthInPixel(double focalLengthInPixel);

    float calculateDepth(
            const std::pair<int, int> &size,
            const std::array<Point3*, 5> &marks
    );

    void calculateFocalLengthInPixels(
            const std::pair<int, int> &size,
            float focal_length_35mm,
            float focal_length_mm
    );
};


#endif //APP_CALCULATOR_H
