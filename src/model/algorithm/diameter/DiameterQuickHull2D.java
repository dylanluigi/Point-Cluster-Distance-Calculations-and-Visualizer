package model.algorithm.diameter;

import model.Point2D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.quickhull.QuickHull2D;

import java.util.List;

/**
 * Implementation of the diameter algorithm for 2D points using QuickHull algorithm.
 * This algorithm uses the fact that the diameter of a point set is the same as the diameter
 * of its convex hull, and the diameter of a convex polygon is the maximum distance
 * between any two vertices.
 */
public class DiameterQuickHull2D extends DiameterBase2D {

    @Override
    public AlgorithmResult<Point2D> execute(List<Point2D> points) {
        long startTime = System.nanoTime();
        
        // Build the convex hull using QuickHull algorithm
        QuickHull2D quickHull = new QuickHull2D(true); // Use concurrent implementation
        List<Point2D> hullPoints = quickHull.buildHull(points);
        
        // Find the diameter by checking all pairs of hull vertices
        double maxDist = 0.0;
        Point2D p1 = null, p2 = null;
        
        for (int i = 0; i < hullPoints.size(); i++) {
            for (int j = i + 1; j < hullPoints.size(); j++) {
                double dist = distance(hullPoints.get(i), hullPoints.get(j));
                if (dist > maxDist) {
                    maxDist = dist;
                    p1 = hullPoints.get(i);
                    p2 = hullPoints.get(j);
                }
            }
        }
        
        long executionTime = System.nanoTime() - startTime;
        return new AlgorithmResult<>(p1, p2, maxDist, getType(), executionTime / 1_000_000);
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.DIAMETER_QUICKHULL;
    }
}