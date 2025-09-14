package model.algorithm.quickhull;

import model.Point3D;
import java.util.List;

/**
 * Implementation of the QuickHull algorithm for finding the convex hull of a set of 3D points.
 * This version delegates to the RobustQuickHull3D implementation, which wraps the external
 * QuickHull3D implementation from the quickhall3d package.
 */
public class QuickHull3D {
    private RobustQuickHull3D delegate;
    
    /**
     * Create a new QuickHull3D instance
     * @param concurrent Whether to use concurrent execution
     */
    public QuickHull3D(boolean concurrent) {
        this.delegate = new RobustQuickHull3D(concurrent);
    }
    
    /**
     * Build the convex hull for a set of points
     * @param points The points to build the hull from
     * @return The list of points that form the convex hull
     */
    public List<Point3D> buildHull(List<Point3D> points) {
        return delegate.buildHull(points);
    }
    
    /**
     * Get the faces of the convex hull as triangles
     * @return Array of integer arrays, each containing 3 indices into the hull vertices
     */
    public int[][] getFaces() {
        return delegate.getFaces();
    }
    
    /**
     * Compute the diameter of the hull (maximum distance between any two vertices)
     * @return The maximum distance between any two vertices of the hull
     */
    public double computeDiameter() {
        return delegate.computeDiameter();
    }
}