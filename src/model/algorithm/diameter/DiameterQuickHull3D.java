package model.algorithm.diameter;

import model.Point3D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.quickhull.QuickHull3D;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Implementació de l'algoritme de diàmetre per a punts 3D utilitzant l'algoritme QuickHull.
 * <p>
 * Aquest algoritme aprofita el fet que el diàmetre d'un conjunt de punts és el mateix que el diàmetre
 * del seu envolupant convex, i el diàmetre d'un poliedre convex és la distància màxima
 * entre qualsevol dels seus vèrtexs.
 * 
 * Les principals característiques són:
 * <ul>
 *   <li>Construeix l'envolupant convex utilitzant l'algoritme QuickHull</li>
 *   <li>Calcula el diàmetre només entre els vèrtexs de l'envolupant, no entre tots els punts</li>
 *   <li>Utilitza processament paral·lel amb ForkJoinPool per accelerar els càlculs</li>
 *   <li>Exposa l'objecte QuickHull a la UI per mantenir una visualització consistent</li>
 * </ul>
 * 
 * La complexitat temporal és O(n log n) per a la construcció de l'envolupant i O(h²) 
 * per calcular el diàmetre, on h és el nombre de vèrtexs de l'envolupant.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class DiameterQuickHull3D extends DiameterBase3D {

    // Per cridar el envolupant desde la UI.
    public static QuickHull3D lastUsedQuickHull;
    
    /**
     * Executa l'algoritme de càlcul del diàmetre utilitzant QuickHull.
     * <p>
     * El procés consta de les següents etapes:
     * <ol>
     *   <li>Construcció de l'envolupant convex dels punts d'entrada</li>
     *   <li>Càlcul del diàmetre utilitzant només els vèrtexs de l'envolupant</li>
     *   <li>Paral·lelització del càlcul per millorar el rendiment</li>
     * </ol>
     * </p>
     * 
     * @param points la llista de punts 3D a processar
     * @return el resultat del càlcul del diàmetre amb els punts més llunyans i la distància
     */
    @Override
    public AlgorithmResult<Point3D> execute(List<Point3D> points) {
        long startTime = System.nanoTime();

        // Construim el envolupant convex utilitzant l'algoritme QuickHull.
        QuickHull3D quickHull = new QuickHull3D(true);
        List<Point3D> hullPoints = quickHull.buildHull(points);

        // Guardam l'instància de QuickHull per a la UI
        lastUsedQuickHull = quickHull;

        // Troba
        // Find the diameter using the rotating calipers algorithm or brute force
        // For simplicity, we'll use a concurrent brute force approach here

        // Trobam el diàmetre utilitzant l'algorisme de "Calibre Giratorio" o la força bruta
        int numThreads = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(numThreads);
        
        DiameterTask task = new DiameterTask(hullPoints, 0, hullPoints.size());
        DiameterResult result = pool.invoke(task);
        pool.shutdown();
        
        long executionTime = System.nanoTime() - startTime;
        return new AlgorithmResult<>(
            result.p1, result.p2, result.maxDist, getType(), executionTime / 1_000_000
        );
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.DIAMETER_QUICKHULL;
    }
    
    /**
     * Classe auxiliar per emmagatzemar els resultats del càlcul del diàmetre.
     * <p>
     * Encapsula els dos punts que defineixen el diàmetre i la distància entre ells.
     * També proporciona un mètode per combinar resultats de diferents subtasques.
     * </p>
     */
    private static class DiameterResult {
        final Point3D p1;
        final Point3D p2;
        final double maxDist;
        
        public DiameterResult(Point3D p1, Point3D p2, double maxDist) {
            this.p1 = p1;
            this.p2 = p2;
            this.maxDist = maxDist;
        }
        
        public DiameterResult merge(DiameterResult other) {
            if (other.maxDist > this.maxDist) {
                return other;
            }
            return this;
        }
    }
    
    /**
     * Tasca recursiva per al càlcul paral·lel del diàmetre.
     * <p>
     * Implementa l'enfocament de dividir i conquerir per trobar el diàmetre del conjunt de punts.
     * Utilitza Fork/Join per paral·lelitzar el càlcul i un llindar per canviar a computació directa
     * quan el subconjunt és prou petit.
     * </p>
     */
    private class DiameterTask extends RecursiveTask<DiameterResult> {
        private static final int THRESHOLD = 500;
        private final List<Point3D> points;
        private final int start;
        private final int end;
        
        public DiameterTask(List<Point3D> points, int start, int end) {
            this.points = points;
            this.start = start;
            this.end = end;
        }
        
        @Override
        protected DiameterResult compute() {
            if (end - start <= THRESHOLD) {
                return computeDirectly();
            }
            
            int mid = start + (end - start) / 2;
            DiameterTask leftTask = new DiameterTask(points, start, mid);
            DiameterTask rightTask = new DiameterTask(points, mid, end);
            
            leftTask.fork();
            DiameterResult rightResult = rightTask.compute();
            DiameterResult leftResult = leftTask.join();
            
           // Per assegurar que els punts de l'envolupant convex es considera
           // el calcula creuat dels dos subconjunts
            DiameterResult crossResult = computeCrossDistances(start, mid, mid, end);
            
            return leftResult.merge(rightResult).merge(crossResult);
        }
        
        private DiameterResult computeDirectly() {
            double maxDist = 0.0;
            Point3D p1 = null, p2 = null;

            for (int i = start; i < end; i++) {
                for (int j = i + 1; j < end; j++) {
                    double dist = distance(points.get(i), points.get(j));
                    if (dist > maxDist) {
                        maxDist = dist;
                        p1 = points.get(i);
                        p2 = points.get(j);
                    }
                }
            }
            
            return new DiameterResult(p1, p2, maxDist);
        }
        
        private DiameterResult computeCrossDistances(int startA, int endA, int startB, int endB) {
            double maxDist = 0.0;
            Point3D p1 = null, p2 = null;

            // Calculam distàncies entre punts de diferents particions
            // les dues particions ja són subconjunts dels vèrtexs del envolupant.

            for (int i = startA; i < endA; i++) {
                for (int j = startB; j < endB; j++) {
                    double dist = distance(points.get(i), points.get(j));
                    if (dist > maxDist) {
                        maxDist = dist;
                        p1 = points.get(i);
                        p2 = points.get(j);
                    }
                }
            }
            
            return new DiameterResult(p1, p2, maxDist);
        }
    }
}