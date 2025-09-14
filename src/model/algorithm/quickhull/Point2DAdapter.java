package model.algorithm.quickhull;

import model.Point2D;

/**
 * Adapter for Point2D to be used with the QuickHull algorithm.
 * This allows us to use the QuickHull algorithm with our existing Point2D class.
 */
public class Point2DAdapter {
    private final Point2D point;

    public Point2DAdapter(Point2D point) {
        this.point = point;
    }

    public Point2D getPoint() {
        return point;
    }

    public double getX() {
        return point.x();
    }

    public double getY() {
        return point.y();
    }

    /**
     * Compute the distance from this point to the line passing through p1 and p2.
     */
    public double distanceToLine(Point2DAdapter p1, Point2DAdapter p2) {
        double x0 = getX();
        double y0 = getY();
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();

        // Line equation: ax + by + c = 0
        double a = y2 - y1;
        double b = x1 - x2;
        double c = x2 * y1 - x1 * y2;

        // Distance from point to line
        return Math.abs(a * x0 + b * y0 + c) / Math.sqrt(a * a + b * b);
    }

    /**
     * Compute the side of the line from p1 to p2 that this point lies on.
     * @return Positive if on left side, negative if on right side, 0 if on the line.
     */
    public double whichSide(Point2DAdapter p1, Point2DAdapter p2) {
        double x0 = getX();
        double y0 = getY();
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();

        return (x2 - x1) * (y0 - y1) - (y2 - y1) * (x0 - x1);
    }

    /**
     * Calculate Euclidean distance between two points
     */
    public double distanceTo(Point2DAdapter other) {
        double dx = this.getX() - other.getX();
        double dy = this.getY() - other.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return point.toString();
    }
}