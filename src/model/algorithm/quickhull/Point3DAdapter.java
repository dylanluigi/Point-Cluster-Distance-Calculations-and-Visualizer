package model.algorithm.quickhull;

import model.Point3D;

/**
 * Adapter for Point3D to be used with the QuickHull3D algorithm.
 * This allows us to use the QuickHull algorithm with our existing Point3D class.
 */
public class Point3DAdapter {
    private final Point3D point;

    public Point3DAdapter(Point3D point) {
        this.point = point;
    }

    public Point3D getPoint() {
        return point;
    }

    public double getX() {
        return point.x();
    }

    public double getY() {
        return point.y();
    }

    public double getZ() {
        return point.z();
    }

    /**
     * Compute the Euclidean distance between this point and another point.
     */
    public double distanceTo(Point3DAdapter other) {
        double dx = this.getX() - other.getX();
        double dy = this.getY() - other.getY();
        double dz = this.getZ() - other.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Compute the cross product of vectors (this - ref) and (p - ref).
     * @return The cross product vector
     */
    public double[] crossProduct(Point3DAdapter ref, Point3DAdapter p) {
        double[] result = new double[3];
        
        double ax = getX() - ref.getX();
        double ay = getY() - ref.getY();
        double az = getZ() - ref.getZ();
        
        double bx = p.getX() - ref.getX();
        double by = p.getY() - ref.getY();
        double bz = p.getZ() - ref.getZ();
        
        result[0] = ay * bz - az * by;
        result[1] = az * bx - ax * bz;
        result[2] = ax * by - ay * bx;
        
        return result;
    }

    /**
     * Calculate the dot product of two vectors.
     */
    public double dotProduct(double[] v1, double[] v2) {
        return v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2];
    }

    @Override
    public String toString() {
        return point.toString();
    }
}