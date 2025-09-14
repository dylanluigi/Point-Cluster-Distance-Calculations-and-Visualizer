package model.algorithm.diameter;

import model.Point2D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Implementació concurrent de l'algoritme del diàmetre per a punts 2D.
 * <p>
 * Aquesta implementació utilitza tècniques de paral·lelització mitjançant 
 * el framework Fork/Join de Java per distribuir la càrrega de càlcul entre
 * múltiples fils d'execució. Segueix un enfocament de dividir i conquerir
 * per calcular el diàmetre de manera paral·lela.
 * 
 * Tot i que la complexitat algorítmica segueix sent O(n²) en el pitjor cas,
 * l'execució paral·lela permet un rendiment significativament millor en 
 * processadors multi-nucli.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class DiameterConcurrent2D extends DiameterBase2D {

    /**
     * Executa l'algoritme del diàmetre de manera concurrent.
     * <p>
     * Utilitza ForkJoinPool per paral·lelitzar el càlcul del diàmetre,
     * adaptant-se al nombre de processadors disponibles en el sistema.
     * </p>
     * 
     * @param points la llista de punts 2D a processar
     * @return el resultat amb el parell de punts més llunyans i la seva distància
     */
    @Override
    public AlgorithmResult<Point2D> execute(List<Point2D> points) {
        long startTime = System.nanoTime();  // Registra el temps d'inici
        
        // Configura el pool de threads segons el nombre de processadors disponibles
        int numThreads = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(numThreads);
        
        // Crea i executa la tasca paral·lela
        DiameterTask task = new DiameterTask(points, 0, points.size());
        AlgorithmResult<Point2D> result = pool.invoke(task);
        pool.shutdown();
        
        long executionTime = System.nanoTime() - startTime;
        return new AlgorithmResult<>(
                result.getPoint1(), 
                result.getPoint2(), 
                result.getDistance(), 
                getType(), 
                executionTime / 1_000_000
        );
    }

    /**
     * Retorna el tipus d'aquest algoritme.
     * 
     * @return el tipus d'algoritme (DIAMETER_CONCURRENT)
     */
    @Override
    public AlgorithmType getType() {
        return AlgorithmType.DIAMETER_CONCURRENT;
    }
    
    /**
     * Classe interna per a la tasca recursiva en l'algoritme Fork/Join.
     * <p>
     * Implementa la divisió del problema en subtasques que s'executen en paral·lel,
     * amb un llindar per a la mida de les subtasques per sota del qual es fa un
     * càlcul directe.
     * </p>
     */
    private class DiameterTask extends RecursiveTask<AlgorithmResult<Point2D>> {
        private static final int THRESHOLD = 10000;
        private final List<Point2D> points;
        private final int start;
        private final int end;
        
        public DiameterTask(List<Point2D> points, int start, int end) {
            this.points = points;
            this.start = start;
            this.end = end;
        }
        
//        @Override
//        protected AlgorithmResult<Point2D> compute() {
//            if (end - start <= THRESHOLD) {
//                return computeDirectly();
//            }
//
//            int mid = start + (end - start) / 2;
//            DiameterTask leftTask = new DiameterTask(points, start, mid);
//            leftTask.fork();
//
//            DiameterTask rightTask = new DiameterTask(points, mid, end);
//            AlgorithmResult<Point2D> rightResult = rightTask.compute();
//            AlgorithmResult<Point2D> leftResult = leftTask.join();
//
//            return merge(leftResult, rightResult);
//        }

        /**
         * Mètode principal de divisió i conquesta per a la tasca paral·lela.
         * 
         * @return el resultat amb el parell de punts que maximitza la distància
         */
        @Override
        protected AlgorithmResult<Point2D> compute() {
            // Cas base: si la mida de la subtasca és menor que el llindar, calcula directament
            if (end - start <= THRESHOLD) {
                return computeDirectly();
            }

            // Divideix la tasca en dues subtasques
            int mid = start + (end - start) / 2;
            DiameterTask leftTask = new DiameterTask(points, start, mid);
            DiameterTask rightTask = new DiameterTask(points, mid, end);

            // Executa la subtasca esquerra asíncronament (en un altre fil)
            leftTask.fork();
            
            // Executa la subtasca dreta en el fil actual
            AlgorithmResult<Point2D> rightResult = rightTask.compute();
            
            // Espera que la subtasca esquerra finalitzi i obté el resultat
            AlgorithmResult<Point2D> leftResult = leftTask.join();

            // Calcula les distàncies entre punts que creuen les dues particions
            AlgorithmResult<Point2D> crossResult = computeCrossPairs(start, mid, mid, end);

            // Combina els resultats per obtenir el màxim global
            return merge(merge(leftResult, rightResult), crossResult);
        }

        /**
         * Calcula les distàncies entre punts de dues particions diferents.
         * 
         * @param leftStart índex d'inici de la partició esquerra
         * @param leftEnd índex final de la partició esquerra
         * @param rightStart índex d'inici de la partició dreta
         * @param rightEnd índex final de la partició dreta
         * @return el resultat amb el parell de punts que maximitza la distància entre les particions
         */
        private AlgorithmResult<Point2D> computeCrossPairs(int leftStart, int leftEnd, int rightStart, int rightEnd) {
            double maxDist = 0.0;
            Point2D p1 = null, p2 = null;

            // Compara tots els punts de la partició esquerra amb els de la partició dreta
            for (int i = leftStart; i < leftEnd; i++) {
                for (int j = rightStart; j < rightEnd; j++) {
                    double dist = distance(points.get(i), points.get(j));
                    if (dist > maxDist) {
                        maxDist = dist;
                        p1 = points.get(i);
                        p2 = points.get(j);
                    }
                }
            }

            return new AlgorithmResult<>(p1, p2, maxDist, getType(), 0);
        }

        /**
         * Calcula el diàmetre dins d'un segment de punts específic.
         * S'utilitza quan la mida de la tasca és menor que el llindar.
         * 
         * @return el resultat amb el parell de punts que maximitza la distància en el segment
         */
        private AlgorithmResult<Point2D> computeDirectly() {
            double maxDist = 0.0;
            Point2D p1 = null, p2 = null;
            
            // Per cada punt en el rang assignat, calcula la distància amb tots els altres punts
            for (int i = start; i < end; i++) {
                for (int j = 0; j < points.size(); j++) {
                    if (i != j) {  // Evita comparar un punt amb si mateix
                        double dist = distance(points.get(i), points.get(j));
                        if (dist > maxDist) {
                            maxDist = dist;
                            p1 = points.get(i);
                            p2 = points.get(j);
                        }
                    }
                }
            }
            
            return new AlgorithmResult<>(p1, p2, maxDist, getType(), 0);
        }
        
//        private AlgorithmResult<Point2D> merge(
//                AlgorithmResult<Point2D> left,
//                AlgorithmResult<Point2D> right) {
//            if (left.getDistance() >= right.getDistance()) {
//                return left;
//            } else {
//                return right;
//            }
//        }
/**
 * Combina dos resultats seleccionant el que té la distància màxima.
 * 
 * @param a primer resultat a comparar
 * @param b segon resultat a comparar
 * @return el resultat amb la distància màxima
 */
private AlgorithmResult<Point2D> merge(AlgorithmResult<Point2D> a, AlgorithmResult<Point2D> b) {
    // Selecciona el resultat amb la distància més gran
    return (a.getDistance() >= b.getDistance()) ? a : b;
}
    }
}
