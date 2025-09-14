package model.algorithm.kdtree;

import model.Point2D;
import model.Point3D;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Generic implementation of a KDTree (K–dimensional tree) for efficient nearest neighbor search.
 * <p>
 * The KDTree organizes points in a k–dimensional space. It is useful for range searches
 * and nearest neighbor queries with an average search complexity of O(log n).
 * </p>
 *
 * @param <T> the type of point (e.g., Point2D or Point3D)
 * @author
 * @version 1.0
 */
public class KDTree<T> {
    private KDNode root;
    private final int dimensions;
    private final Function<T, Double>[] coordinateExtractors;

    /**
     * Inner class representing a node in the KDTree.
     * <p>
     * Each node contains a point, pointers to left and right children,
     * and the depth at which the node resides (to determine the splitting axis).
     * </p>
     */
    private class KDNode {
        private T point;
        private KDNode left, right;
        private int depth;

        public KDNode(T point, int depth) {
            this.point = point;
            this.depth = depth;
            this.left = null;
            this.right = null;
        }
    }

    /**
     * Creates a 2D KDTree for Point2D objects.
     *
     * @param points the list of 2D points to be organized in the tree
     * @return an optimized KDTree for 2D points
     */
    @SuppressWarnings("unchecked")
    public static KDTree<Point2D> create2DTree(List<Point2D> points) {
        Function<Point2D, Double>[] extractors = new Function[2];
        extractors[0] = Point2D::x;  // Extract x–coordinate
        extractors[1] = Point2D::y;  // Extract y–coordinate
        return new KDTree<>(points, 2, extractors);
    }

    /**
     * Creates a 3D KDTree for Point3D objects.
     *
     * @param points the list of 3D points to be organized in the tree
     * @return an optimized KDTree for 3D points
     */
    @SuppressWarnings("unchecked")
    public static KDTree<Point3D> create3DTree(List<Point3D> points) {
        Function<Point3D, Double>[] extractors = new Function[3];
        extractors[0] = Point3D::x;  // Extract x–coordinate
        extractors[1] = Point3D::y;  // Extract y–coordinate
        extractors[2] = Point3D::z;  // Extract z–coordinate
        return new KDTree<>(points, 3, extractors);
    }

    /**
     * General KDTree constructor.
     *
     * @param points               list of points to be inserted into the tree
     * @param dimensions           number of dimensions of the space
     * @param coordinateExtractors functions to extract each coordinate from a point
     */
    public KDTree(List<T> points, int dimensions, Function<T, Double>[] coordinateExtractors) {
        this.dimensions = dimensions;
        this.coordinateExtractors = coordinateExtractors;
        if (points != null && !points.isEmpty()) {
            List<T> pointsCopy = new ArrayList<>(points);
            this.root = buildTree(pointsCopy, 0);
        }
    }

    /**
     * Recursively builds the KDTree.
     * <p>
     * The strategy is to split the points at each level based on the median of the current dimension.
     * </p>
     *
     * @param points list of points to process
     * @param depth  current depth in the tree (determines the splitting axis)
     * @return the root of the subtree created
     */
    private KDNode buildTree(List<T> points, int depth) {
        if (points == null || points.isEmpty()) {
            return null;
        }

        int axis = depth % dimensions;

        // Sort points by the coordinate corresponding to the current axis
        points.sort((p1, p2) -> Double.compare(coordinateExtractors[axis].apply(p1),
                coordinateExtractors[axis].apply(p2)));

        int medianIndex = points.size() / 2;
        KDNode node = new KDNode(points.get(medianIndex), depth);

        // Recursively build the left and right subtrees if there are points available
        if (medianIndex > 0) {
            node.left = buildTree(new ArrayList<>(points.subList(0, medianIndex)), depth + 1);
        }
        if (medianIndex < points.size() - 1) {
            node.right = buildTree(new ArrayList<>(points.subList(medianIndex + 1, points.size())), depth + 1);
        }

        return node;
    }

    /**
     * Finds the nearest neighbor to the target point.
     *
     * @param target the target point to search for
     * @return the nearest neighbor point to the target
     */
    public T findNearest(T target) {
        NearestInfo nearest = new NearestInfo(null, Double.POSITIVE_INFINITY);
        findNearest(root, target, nearest);
        return nearest.point;
    }

    /**
     * Helper class to store the best (nearest) point information.
     */
    private class NearestInfo {
        T point;
        double distance; // squared distance

        NearestInfo(T point, double distance) {
            this.point = point;
            this.distance = distance;
        }
    }

    /**
     * Recursively searches for the nearest neighbor.
     * <p>
     * The algorithm updates the best candidate and uses squared distances for comparison.
     * </p>
     *
     * @param node    current node to explore
     * @param target  target point of the search
     * @param nearest current best candidate info
     */
    private void findNearest(KDNode node, T target, NearestInfo nearest) {
        if (node == null) {
            return;
        }

        double d = calculateDistanceSquared(node.point, target);
        if (d < nearest.distance) {
            nearest.point = node.point;
            nearest.distance = d;
        }

        int axis = node.depth % dimensions;
        double nodeCoord = coordinateExtractors[axis].apply(node.point);
        double targetCoord = coordinateExtractors[axis].apply(target);

        KDNode first, second;
        if (targetCoord < nodeCoord) {
            first = node.left;
            second = node.right;
        } else {
            first = node.right;
            second = node.left;
        }

        // Explore the most promising subtree first.
        findNearest(first, target, nearest);

        // Only explore the opposite subtree if the squared difference
        // in the current dimension is less than the best squared distance found.
        double axisDiff = targetCoord - nodeCoord;
        if ((axisDiff * axisDiff) < nearest.distance) {
            findNearest(second, target, nearest);
        }
    }

    /**
     * Finds the k nearest neighbors to the target point.
     *
     * @param target the target point to search for
     * @param k      the number of nearest neighbors to return
     * @return list of k nearest neighbors ordered by increasing distance
     */
    public List<T> findKNearest(T target, int k) {
        BoundedPriorityQueue<T> queue = new BoundedPriorityQueue<>(k);
        findKNearest(root, target, queue);
        List<T> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            result.add(queue.poll());
        }
        return result;
    }

    /**
     * Recursively searches for the k nearest neighbors.
     *
     * @param node    current node to explore
     * @param target  target point of the search
     * @param queue   bounded priority queue storing the best candidates
     */
    private void findKNearest(KDNode node, T target, BoundedPriorityQueue<T> queue) {
        if (node == null) {
            return;
        }

        double d = calculateDistanceSquared(node.point, target);
        queue.add(node.point, d);

        int axis = node.depth % dimensions;
        double nodeCoord = coordinateExtractors[axis].apply(node.point);
        double targetCoord = coordinateExtractors[axis].apply(target);

        KDNode first, second;
        if (targetCoord < nodeCoord) {
            first = node.left;
            second = node.right;
        } else {
            first = node.right;
            second = node.left;
        }

        findKNearest(first, target, queue);

        double axisDiff = targetCoord - nodeCoord;
        if ((axisDiff * axisDiff) < queue.getLargestDistance()) {
            findKNearest(second, target, queue);
        }
    }

    /**
     * Calculates the squared Euclidean distance between two points.
     * <p>
     * Using the squared distance avoids the cost of computing square roots while preserving order.
     * </p>
     *
     * @param p1 first point
     * @param p2 second point
     * @return the squared Euclidean distance between p1 and p2
     */
    private double calculateDistanceSquared(T p1, T p2) {
        double sum = 0.0;
        for (int i = 0; i < dimensions; i++) {
            double diff = coordinateExtractors[i].apply(p1) - coordinateExtractors[i].apply(p2);
            sum += diff * diff;
        }
        return sum;
    }

    /**
     * Inner class implementing a bounded priority queue to store up to k nearest neighbors.
     * <p>
     * The queue maintains elements in sorted order by their squared distance values.
     * </p>
     *
     * @param <E> type of element stored in the queue
     */
    private class BoundedPriorityQueue<E> {
        private final int maxSize;
        private final List<E> elements;
        private final List<Double> distances;

        public BoundedPriorityQueue(int maxSize) {
            this.maxSize = maxSize;
            this.elements = new ArrayList<>(maxSize);
            this.distances = new ArrayList<>(maxSize);
        }

        /**
         * Inserts an element with its associated squared distance.
         * <p>
         * If the queue exceeds maxSize, the element with the largest (worst)
         * distance is removed.
         * </p>
         *
         * @param element  the element to add
         * @param distance its squared distance from the target
         */
        public void add(E element, double distance) {
            int pos = 0;
            // Find the correct insertion position in the sorted order.
            while (pos < elements.size() && distances.get(pos) < distance) {
                pos++;
            }
            // Only insert if there's room or the new element is closer than one already in the queue.
            if (elements.size() < maxSize || pos < elements.size()) {
                elements.add(pos, element);
                distances.add(pos, distance);
                // If size exceeds maxSize, remove the farthest neighbor (last element).
                if (elements.size() > maxSize) {
                    elements.remove(maxSize);
                    distances.remove(maxSize);
                }
            }
        }

        /**
         * Checks if the queue is empty.
         *
         * @return true if there are no elements in the queue; false otherwise.
         */
        public boolean isEmpty() {
            return elements.isEmpty();
        }

        /**
         * Retrieves and removes the closest element (with the smallest distance).
         *
         * @return the closest neighbor
         */
        public E poll() {
            if (elements.isEmpty()) {
                return null;
            }
            E result = elements.remove(0);
            distances.remove(0);
            return result;
        }

        /**
         * Gets the largest (worst) squared distance currently in the queue.
         * <p>
         * If the queue is not full, returns positive infinity, ensuring further nodes
         * will be considered.
         * </p>
         *
         * @return the largest squared distance or infinity if the queue is not full
         */
        public double getLargestDistance() {
            if (elements.size() < maxSize) {
                return Double.POSITIVE_INFINITY;
            }
            return distances.get(elements.size() - 1);
        }
    }
}
