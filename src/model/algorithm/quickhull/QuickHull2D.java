package model.algorithm.quickhull;

import model.Point2D;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * Implementació de l'algorisme QuickHull per trobar l'envolupant convexa d'un conjunt de punts 2D.
 * Aquest és un algorisme de dividir i vèncer que opera en temps O(n log n) de mitjana.
 */
public class QuickHull2D {
    private List<Point2DAdapter> hullPoints;
    private boolean concurrent;
    private ForkJoinPool forkJoinPool;
    
    /**
     * Crea una nova instància de QuickHull2D
     * @param concurrent Si s'ha d'utilitzar execució concurrent
     */
    public QuickHull2D(boolean concurrent) {
        this.concurrent = concurrent;
        if (concurrent) {
            this.forkJoinPool = new ForkJoinPool();
        }
    }
    
    /**
     * Construeix l'envolupant convexa per a un conjunt de punts
     * @param points Els punts a partir dels quals construir l'envolupant
     * @return La llista de punts que formen l'envolupant convexa
     */
    public List<Point2D> buildHull(List<Point2D> points) {
        // Adaptem els punts al nostre format intern
        List<Point2DAdapter> adaptedPoints = points.stream()
                .map(Point2DAdapter::new)
                .collect(Collectors.toList());
        
        // Identifiquem els punts més a l'esquerra i més a la dreta
        Point2DAdapter leftmost = adaptedPoints.get(0);
        Point2DAdapter rightmost = adaptedPoints.get(0);
        
        for (Point2DAdapter p : adaptedPoints) {
            if (p.getX() < leftmost.getX()) {
                leftmost = p;
            }
            if (p.getX() > rightmost.getX()) {
                rightmost = p;
            }
        }
        
        // Inicialitzem els punts de l'envolupant amb els més a l'esquerra i més a la dreta
        hullPoints = new ArrayList<>();
        hullPoints.add(leftmost);
        hullPoints.add(rightmost);
        
        // Partim els punts en dos conjunts - per sobre i per sota de la línia
        List<Point2DAdapter> aboveLine = new ArrayList<>();
        List<Point2DAdapter> belowLine = new ArrayList<>();
        
        for (Point2DAdapter p : adaptedPoints) {
            if (p == leftmost || p == rightmost) continue;
            
            double side = p.whichSide(leftmost, rightmost);
            if (side > 0) {
                aboveLine.add(p);
            } else if (side < 0) {
                belowLine.add(p);
            }
            // Els punts sobre la línia (side == 0) s'exclouen ja que no poden formar part de l'envolupant
        }
        
        if (concurrent && forkJoinPool != null) {
            // Implementació concurrent
            FindHullTask aboveTask = new FindHullTask(leftmost, rightmost, aboveLine, true);
            FindHullTask belowTask = new FindHullTask(leftmost, rightmost, belowLine, false);
            
            aboveTask.fork();
            List<Point2DAdapter> belowHull = belowTask.compute();
            List<Point2DAdapter> aboveHull = aboveTask.join();
            
            hullPoints.addAll(aboveHull);
            hullPoints.addAll(belowHull);
        } else {
            // Implementació seqüencial
            findHull(leftmost, rightmost, aboveLine, true);
            findHull(leftmost, rightmost, belowLine, false);
        }
        
        // Ordenem els punts de l'envolupant en sentit contrari a les agulles del rellotge
        sortHullPointsCounterClockwise();
        
        // Convertim de tornada al format Point2D original
        return hullPoints.stream()
                .map(Point2DAdapter::getPoint)
                .collect(Collectors.toList());
    }
    
    /**
     * Mètode recursiu per trobar els punts de l'envolupant en un costat d'una línia
     * @param p1 Primer extrem de la línia
     * @param p2 Segon extrem de la línia
     * @param points Punts a considerar
     * @param above Si estem processant punts per sobre o per sota de la línia
     */
    private void findHull(Point2DAdapter p1, Point2DAdapter p2, List<Point2DAdapter> points, boolean above) {
        if (points.isEmpty()) return;
        
        // Trobem el punt amb la distància màxima des de la línia
        Point2DAdapter farthest = null;
        double maxDistance = 0;
        
        for (Point2DAdapter p : points) {
            double distance = p.distanceToLine(p1, p2);
            if (distance > maxDistance) {
                maxDistance = distance;
                farthest = p;
            }
        }
        
        if (farthest == null) return;
        
        // Afegim el punt més llunyà a l'envolupant
        hullPoints.add(farthest);
        
        // Partim els punts restants en dos conjunts
        List<Point2DAdapter> set1 = new ArrayList<>();
        List<Point2DAdapter> set2 = new ArrayList<>();
        
        for (Point2DAdapter p : points) {
            if (p == farthest) continue;
            
            double side1 = p.whichSide(p1, farthest);
            double side2 = p.whichSide(farthest, p2);
            
            if ((above && side1 > 0) || (!above && side1 < 0)) {
                set1.add(p);
            } else if ((above && side2 > 0) || (!above && side2 < 0)) {
                set2.add(p);
            }
        }
        
        // Trobem recursivament els punts de l'envolupant en ambdós conjunts
        findHull(p1, farthest, set1, above);
        findHull(farthest, p2, set2, above);
    }
    
    /**
     * Ordena els punts de l'envolupant en sentit contrari a les agulles del rellotge
     */
    private void sortHullPointsCounterClockwise() {
        if (hullPoints.size() <= 2) return;
        
        // Trobem el centroide dels punts de l'envolupant
        double cx = 0, cy = 0;
        for (Point2DAdapter p : hullPoints) {
            cx += p.getX();
            cy += p.getY();
        }
        cx /= hullPoints.size();
        cy /= hullPoints.size();
        
        // Creem una referència final al centroide per utilitzar-la en la lambda
        final double finalCx = cx;
        final double finalCy = cy;
        
        // Ordenem els punts per angle des del centroide
        hullPoints.sort((p1, p2) -> {
            double angle1 = Math.atan2(p1.getY() - finalCy, p1.getX() - finalCx);
            double angle2 = Math.atan2(p2.getY() - finalCy, p2.getX() - finalCx);
            return Double.compare(angle1, angle2);
        });
    }
    
    /**
     * Implementació de RecursiveTask per a QuickHull concurrent
     */
    private class FindHullTask extends RecursiveTask<List<Point2DAdapter>> {
        private final Point2DAdapter p1;
        private final Point2DAdapter p2;
        private final List<Point2DAdapter> points;
        private final boolean above;
        
        public FindHullTask(Point2DAdapter p1, Point2DAdapter p2, 
                           List<Point2DAdapter> points, boolean above) {
            this.p1 = p1;
            this.p2 = p2;
            this.points = points;
            this.above = above;
        }
        
        @Override
        protected List<Point2DAdapter> compute() {
            List<Point2DAdapter> result = new ArrayList<>();
            if (points.isEmpty()) return result;
            
            // Trobem el punt amb la distància màxima des de la línia
            Point2DAdapter farthest = null;
            double maxDistance = 0;
            
            for (Point2DAdapter p : points) {
                double distance = p.distanceToLine(p1, p2);
                if (distance > maxDistance) {
                    maxDistance = distance;
                    farthest = p;
                }
            }
            
            if (farthest == null) return result;
            
            // Afegim el punt més llunyà al resultat
            result.add(farthest);
            
            // Partim els punts restants en dos conjunts
            List<Point2DAdapter> set1 = new ArrayList<>();
            List<Point2DAdapter> set2 = new ArrayList<>();
            
            for (Point2DAdapter p : points) {
                if (p == farthest) continue;
                
                double side1 = p.whichSide(p1, farthest);
                double side2 = p.whichSide(farthest, p2);
                
                if ((above && side1 > 0) || (!above && side1 < 0)) {
                    set1.add(p);
                } else if ((above && side2 > 0) || (!above && side2 < 0)) {
                    set2.add(p);
                }
            }
            
            // Creem subtasques per a ambdós conjunts
            FindHullTask task1 = new FindHullTask(p1, farthest, set1, above);
            FindHullTask task2 = new FindHullTask(farthest, p2, set2, above);
            
            task1.fork();
            List<Point2DAdapter> result2 = task2.compute();
            List<Point2DAdapter> result1 = task1.join();
            
            // Combinem els resultats
            result.addAll(result1);
            result.addAll(result2);
            
            return result;
        }
    }
}