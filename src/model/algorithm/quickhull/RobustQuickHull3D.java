package model.algorithm.quickhull;

import model.Point3D;
import model.quickhall3d.Point3d;
import model.quickhall3d.QuickHull3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the QuickHull algorithm for finding the convex hull of a set of 3D points.
 * This version delegates to the robust QuickHull3D implementation from the quickhall3d package.
 */
public class RobustQuickHull3D {
    private boolean concurrent;
    private List<Point3D> hullVertices;
    
    // Store the reference to the hull for retrieving faces later
    private QuickHull3D lastBuiltHull;
    
    /**
     * Create a new RobustQuickHull3D instance
     * @param concurrent Whether to use concurrent execution (ignored in this implementation as
     *                   the underlying QuickHull3D doesn't support concurrency)
     */
    public RobustQuickHull3D(boolean concurrent) {
        this.concurrent = concurrent;
    }
    
    /**
     * Build the convex hull for a set of points
     * @param points The points to build the hull from
     * @return The list of points that form the convex hull
     */
    public List<Point3D> buildHull(List<Point3D> points) {
        if (points.size() < 4) {
            throw new IllegalArgumentException("Need at least 4 points to build a 3D convex hull");
        }
        
        // Convert input points to Point3d format
        Point3d[] inputPoints = new Point3d[points.size()];
        for (int i = 0; i < points.size(); i++) {
            Point3D p = points.get(i);
            inputPoints[i] = new Point3d(p.x(), p.y(), p.z());
        }
        
        // Create and build the hull using the robust implementation
        lastBuiltHull = new QuickHull3D();
        lastBuiltHull.build(inputPoints);
        
        // Get hull vertices
        Point3d[] hullPointsArray = lastBuiltHull.getVertices();
        
        // Convert back to our Point3D format
        hullVertices = new ArrayList<>(hullPointsArray.length);
        for (Point3d p : hullPointsArray) {
            hullVertices.add(new Point3D((int)p.x, (int)p.y, (int)p.z));
        }
        
        return hullVertices;
    }
    
    /**
     * Get the faces of the convex hull as triangles
     * @return Array of integer arrays, each containing 3 indices into the hull vertices
     */
    public int[][] getFaces() {
        if (hullVertices == null || lastBuiltHull == null) {
            throw new IllegalStateException("Hull has not been built yet");
        }
        
        // Return the faces from the robust implementation
        return lastBuiltHull.getFaces();
    }
    
    /**
     * Compute the diameter of the hull (maximum distance between any two vertices)
     * @return The maximum distance between any two vertices of the hull
     */
    public double computeDiameter() {
        if (hullVertices == null || hullVertices.size() < 2) {
            throw new IllegalStateException("Hull has not been built yet");
        }
        
        double maxDist = 0;
        Point3D p1 = null, p2 = null;
        
        for (int i = 0; i < hullVertices.size(); i++) {
            for (int j = i + 1; j < hullVertices.size(); j++) {
                Point3D pi = hullVertices.get(i);
                Point3D pj = hullVertices.get(j);
                
                double dx = pi.x() - pj.x();
                double dy = pi.y() - pj.y();
                double dz = pi.z() - pj.z();
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                
                if (dist > maxDist) {
                    maxDist = dist;
                    p1 = pi;
                    p2 = pj;
                }
            }
        }
        
        return maxDist;
    }
}