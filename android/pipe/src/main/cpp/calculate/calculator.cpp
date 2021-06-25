//
// Created by android-1 on 2021/6/9.
//

#include "calculator.h"

inline double Calculator::computeDepth(
        const std::pair<int, int> &p0,
        const std::pair<int, int> &p1
) {
    return std::sqrt(std::pow(p0.first - p1.first, 2) + std::pow(p0.second - p1.second, 2));
}

inline float
Calculator::computeLandmarkDepth(
        const Point3 &l0,
        const Point3 &l1,
        const std::pair<int, int> &size
) {
    auto p0 = std::make_pair(l0.getX() * size.first, l0.getY() * size.second);
    auto p1 = std::make_pair(l1.getX() * size.first, l1.getY() * size.second);
    return this->computeDepth(p0, p1);
}

float Calculator::computeDiameter(
        const std::array<Point3 *, 4> &marks,
        const std::pair<int, int> &size
) {
    const auto dist_vert = this->computeLandmarkDepth(*marks.at(0), *marks.at(1), size);
    const auto dist_hori = this->computeLandmarkDepth(*marks.at(2), *marks.at(3), size);
    return (dist_vert + dist_hori) / 2;
}


float
Calculator::calculateDepth(
        const std::pair<int, int> &size,
        const std::array<Point3 *, 5> &marks
) {

    auto center = *marks.at(0);
    std::array<Point3 *, 4> ps{};
    std::copy(marks.begin() + 1, marks.end(), ps.begin());
    auto diameter = computeDiameter(ps, size);
    auto origin = std::make_pair(size.first / 2., size.second / 2.);
    auto c = std::make_pair(center.getX() * size.first, center.getY() * size.second);
    auto y = this->computeDepth(origin, c);
    auto x = std::sqrt(std::pow(this->focal_length_in_pixel, 2) + std::pow(y, 2));
    return this->standInMeter * x / diameter;
}


//float Calculator::calculateDepth(
//        const Point3 &center,
//        float focal_length,
//        float diameter,
//        const std::pair<int, int> &size
//) {
//    auto origin = std::make_pair(size.first / 2., size.second / 2.);
//    auto c = std::make_pair(center.getX(), center.getY());
//    auto y = this->computeDepth(origin, c);
//    auto x = std::sqrt(std::pow(focal_length, 2) + std::pow(y, 2));
//    return this->standInMeter * x / diameter;
//}

// Derived from
// https://en.wikipedia.org/wiki/35_mm_equivalent_focal_length#Calculation.
/// Using focal_length_35mm = focal_length_mm * SENSOR_DIAGONAL_35MM /
/// sensor_diagonal_mm, we can calculate the diagonal length of the sensor in
/// millimeters i.e. sensor_diagonal_mm.
void Calculator::calculateFocalLengthInPixels(
        const std::pair<int, int> &size,
        float focal_length_35mm,
        float focal_length_mm
) {

    auto sensor_diagonal_mm = SENSOR_DIAGONAL_35MM / focal_length_35mm * focal_length_mm;
    float width = size.first;
    float height = size.second;
    auto inv_aspect_ratio = width / height;
    if (height > width) {
        inv_aspect_ratio = height / width;
    }

    auto sensor_length = std::sqrt(
            std::pow(sensor_diagonal_mm, 2) / (1.0 + std::pow(inv_aspect_ratio, 2)));
    this->focal_length_in_pixel = width * focal_length_mm / sensor_length;
}

double Calculator::getStandInMeter() const {
    return standInMeter;
}

void Calculator::setStandInMeter(double standInMeter) {
    Calculator::standInMeter = standInMeter;
}

double Calculator::getFocalLengthInPixel() const {
    return focal_length_in_pixel;
}

void Calculator::setFocalLengthInPixel(double focalLengthInPixel) {
    Calculator::focal_length_in_pixel = focalLengthInPixel;
}

Calculator::Calculator() = default;

