package model.algorithm.diameter;

import model.Point3D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Implementació concurrent de l'algoritme de diàmetre per a punts 3D.
 * <p>
 * Aquesta classe implementa una versió paral·lela de l'algoritme per trobar el diàmetre
 * d'un núvol de punts en 3D utilitzant el framework Fork/Join de Java. L'algoritme
 * divideix el conjunt de punts i processa cada subconjunt en paral·lel, aprofitant
 * els múltiples nuclis del processador.
 * </p>
 * <p>
 * La complexitat temporal és O(n²), però amb un factor constant més baix que la
 * versió seqüencial gràcies a la paral·lelització.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class DiameterConcurrent3D extends DiameterBase3D {

    @Override
    public AlgorithmResult<Point3D> execute(List<Point3D> points) {
        long startTime = System.nanoTime();
        // Determina el nombre de fils disponibles en el sistema
        int numThreads = Runtime.getRuntime().availableProcessors();
        // Crea un pool amb el nombre òptim de fils per al hardware actual
        ForkJoinPool pool = new ForkJoinPool(numThreads);
        // Crea la tasca principal que processarà tots els punts
        DiameterTask task = new DiameterTask(points, 0, points.size());
        // Inicia l'execució de la tasca en el pool i espera el resultat
        AlgorithmResult<Point3D> result = pool.invoke(task);
        // Allibera els recursos del pool
        pool.shutdown();
        
        // Calcula el temps d'execució en mil·lisegons
        long executionTime = System.nanoTime() - startTime;
        return new AlgorithmResult<>(
                result.getPoint1(), 
                result.getPoint2(), 
                result.getDistance(), 
                getType(), 
                executionTime / 1_000_000
        );
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.DIAMETER_CONCURRENT;
    }
    
    /**
     * Classe interna per implementar tasques recursives amb Fork/Join.
     * <p>
     * Aquesta classe hereta de RecursiveTask per permetre dividir el problema
     * en subtasques que s'executen en paral·lel i combinar els resultats.
     * </p>
     */
    private class DiameterTask extends RecursiveTask<AlgorithmResult<Point3D>> {
        /**
         * Llindar que determina quan es processa directament un subconjunt en comptes de dividir-lo.
         * Un valor més gran redueix l'overhead de creació de tasques, però pot limitar el paral·lelisme.
         */
        private static final int THRESHOLD = 10000;
        private final List<Point3D> points;
        private final int start;
        private final int end;
        
        /**
         * Constructor per a la tasca de càlcul de diàmetre.
         * 
         * @param points llista de punts 3D a processar
         * @param start índex d'inici del rang de punts a processar
         * @param end índex final del rang de punts a processar (exclusiu)
         */
        public DiameterTask(List<Point3D> points, int start, int end) {
            this.points = points;
            this.start = start;
            this.end = end;
        }
        
        /**
         * Mètode principal que implementa l'algoritme de divisió i conquesta.
         * <p>
         * Si el nombre de punts és menor que el llindar, es calcula directament.
         * En cas contrari, es divideix la tasca en dues subtasques que s'executen
         * en paral·lel i després es combinen els resultats.
         * </p>
         * 
         * @return el resultat amb el parell de punts més distants
         */
        @Override
        protected AlgorithmResult<Point3D> compute() {
            // Cas base: si la mida és menor que el llindar, calcula directament
            if (end - start <= THRESHOLD) {
                return computeDirectly();
            }
            
            // Divideix la tasca en dues parts aproximadament iguals
            int mid = start + (end - start) / 2;
            // Crea una subtasca per a la primera meitat
            DiameterTask leftTask = new DiameterTask(points, start, mid);
            // Executa la subtasca esquerra asíncronament en un altre fil
            leftTask.fork();
            
            // Crea una subtasca per a la segona meitat
            DiameterTask rightTask = new DiameterTask(points, mid, end);
            // Executa la subtasca dreta en el fil actual
            AlgorithmResult<Point3D> rightResult = rightTask.compute();
            // Espera que finalitzi la tasca esquerra i obté el seu resultat
            AlgorithmResult<Point3D> leftResult = leftTask.join();
            
            // Combina els resultats per obtenir el parell amb màxima distància
            return merge(leftResult, rightResult);
        }
        
        /**
         * Calcula directament el diàmetre per a un subconjunt de punts.
         * <p>
         * Aquest mètode s'utilitza quan el nombre de punts és menor que el llindar
         * i implementa l'algoritme de força bruta que compara tots els parells possibles.
         * </p>
         * 
         * @return el resultat amb el parell de punts més distants trobats
         */
        private AlgorithmResult<Point3D> computeDirectly() {
            double maxDist = 0.0;
            Point3D p1 = null, p2 = null;
            
            // Compara cada punt del subconjunt assignat amb tots els punts
            for (int i = start; i < end; i++) {
                for (int j = 0; j < points.size(); j++) {
                    // Evita comparar un punt amb si mateix
                    if (i != j) {
                        // Calcula la distància euclidiana entre els punts
                        double dist = distance(points.get(i), points.get(j));
                        // Actualitza el màxim si trobem una distància més gran
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
        
        /**
         * Combina els resultats de dues subtasques.
         * <p>
         * Selecciona el resultat amb la distància més gran entre els dos resultats
         * proporcionats per les subtasques.
         * </p>
         * 
         * @param left resultat de la subtasca esquerra
         * @param right resultat de la subtasca dreta
         * @return el resultat amb la distància màxima
         */
        private AlgorithmResult<Point3D> merge(
                AlgorithmResult<Point3D> left, 
                AlgorithmResult<Point3D> right) {
            // Retorna el resultat amb la distància més gran
            if (left.getDistance() >= right.getDistance()) {
                return left;
            } else {
                return right;
            }
        }
    }
}
