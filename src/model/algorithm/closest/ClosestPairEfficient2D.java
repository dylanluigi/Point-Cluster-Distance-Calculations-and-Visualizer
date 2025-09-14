package model.algorithm.closest;

import model.Point2D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Implementació eficient O(n log n) de l'algoritme del parell més proper per a punts 2D.
 * <p>
 * Aquest algoritme utilitza un enfocament de dividir i conquerir amb les següents característiques:
 * <ul>
 *   <li>Ordena els punts per coordenades x i y</li>
 *   <li>Divideix recursivament el conjunt de punts</li>
 *   <li>Utilitza una tècnica d'escombratge del pla</li>
 *   <li>Implementa processament concurrent mitjançant ForkJoinPool</li>
 *   <li>Utilitza "work stealing" dinàmic per balancejar la càrrega entre fils</li>
 * </ul>
 * 
 * La complexitat temporal és O(n log n) en el cas mitjà i el pitjor cas.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class ClosestPairEfficient2D extends ClosestPairBase2D {
    /**
     * Llindar per determinar quan canviar a computació directa.
     * Per sota d'aquesta quantitat de punts, s'utilitza l'algoritme de força bruta.
     */
    private static final int THRESHOLD = 50;

    /**
     * Executa l'algoritme del parell més proper utilitzant una estratègia eficient.
     * <p>
     * El procés d'execució inclou:
     * <ol>
     *   <li>Ordenar els punts per coordenades x i y</li>
     *   <li>Utilitzar un ForkJoinPool per paral·lelitzar el càlcul</li>
     *   <li>Aplicar l'algoritme de dividir i conquerir recursivament</li>
     *   <li>Calcular la distància mínima entre punts en la franja central</li>
     * </ol>
     * 
     * NOTA: L'algoritme canvia a computació directa quan el nombre de punts és inferior al llindar (THRESHOLD).
     * </p>
     * 
     * @param points la llista de punts 2D a processar
     * @return el resultat del càlcul del parell més proper amb els dos punts més propers i la seva distància
     */
    @Override
    public AlgorithmResult<Point2D> execute(List<Point2D> points) {
        long startTime = System.nanoTime();
        int n = points.size();
        Point2D[] pts = points.toArray(new Point2D[0]);
        
        // Ordena els punts per coordenada x
        Point2D[] Px = pts.clone();
        Arrays.sort(Px, Comparator.comparingDouble(Point2D::x));
        
        // Ordena els punts per coordenada y
        Point2D[] Py = pts.clone();
        Arrays.sort(Py, Comparator.comparingDouble(Point2D::y));
        
        // Utilitza un ForkJoinPool per execució paral·lela
        int parallelism = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(parallelism);
        // Crea la tasca principal amb tots els punts ordenats
        ClosestPairTask task = new ClosestPairTask(Px, Py);
        // Executa la tasca i espera el resultat
        ClosestPairResult result = pool.invoke(task);
        // Allibera els recursos del pool quan ja no es necessiten
        pool.shutdown();
        
        // Calcula el temps d'execució en mil·lisegons
        long executionTime = System.nanoTime() - startTime;
        return new AlgorithmResult<>(
                result.p1, 
                result.p2, 
                result.distance, 
                getType(), 
                executionTime / 1_000_000
        );
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.CLOSEST_PAIR_EFFICIENT;
    }
    
    /**
     * Classe auxiliar per emmagatzemar el resultat del càlcul del parell més proper.
     * <p>
     * Conté els dos punts més propers trobats i la seva distància.
     * També proporciona un mètode per comparar i seleccionar el resultat amb menor distància.
     * </p>
     */
    private static class ClosestPairResult {
        final Point2D p1;
        final Point2D p2;
        final double distance;
        
        /**
         * Constructor per crear un nou resultat.
         * 
         * @param p1 el primer punt del parell
         * @param p2 el segon punt del parell
         * @param distance la distància entre els dos punts
         */
        public ClosestPairResult(Point2D p1, Point2D p2, double distance) {
            this.p1 = p1;
            this.p2 = p2;
            this.distance = distance;
        }
        
        /**
         * Retorna el resultat amb la distància mínima entre aquest i l'altre resultat.
         * 
         * @param other l'altre resultat a comparar (pot ser null)
         * @return el resultat amb la distància més petita
         */
        public ClosestPairResult min(ClosestPairResult other) {
            if (other == null) return this;
            return this.distance <= other.distance ? this : other;
        }
    }
    
    /**
     * Tasca recursiva per al càlcul del parell més proper utilitzant dividir i conquerir.
     * <p>
     * Aquesta classe implementa RecursiveTask per permetre l'execució paral·lela
     * utilitzant el framework Fork/Join. Manté arrays de punts ordenats per coordenades
     * x i y per facilitar la partició i el procés d'escombratge.
     * </p>
     */
    private class ClosestPairTask extends RecursiveTask<ClosestPairResult> {
        private final Point2D[] Px; // Punts ordenats per coordenada x
        private final Point2D[] Py; // Punts ordenats per coordenada y

        /**
         * Constructor per crear una nova tasca de càlcul.
         * 
         * @param Px array de punts ordenats per coordenada x
         * @param Py array de punts ordenats per coordenada y
         */
        public ClosestPairTask(Point2D[] Px, Point2D[] Py) {
            this.Px = Px;
            this.Py = Py;
        }

        /**
         * Implementa l'algoritme de divisió i conquesta per trobar el parell més proper.
         * 
         * @return el resultat amb el parell de punts més propers
         */
        @Override
        protected ClosestPairResult compute() {
            int n = Px.length;
            
            // Cas base: si hi ha pocs punts, calculem directament amb força bruta
            if (n <= THRESHOLD) {
                return bruteForceClosestPair(Px);
            }
            
            // Dividim el conjunt de punts per la meitat segons la coordenada x
            int mid = n / 2;
            Point2D midPoint = Px[mid];
            
            // Dividim els punts ordenats per x en dos subconjunts
            Point2D[] Qx = Arrays.copyOfRange(Px, 0, mid);
            Point2D[] Rx = Arrays.copyOfRange(Px, mid, n);
            
            // Dividim els punts ordenats per y en dos subconjunts
            // corresponents als punts de l'esquerra i dreta
            List<Point2D> QyList = new ArrayList<>();
            List<Point2D> RyList = new ArrayList<>();
            
            // Assignem cada punt a la llista corresponent
            for (Point2D p : Py) {
                // Si el punt està a l'esquerra del punt mitjà o és el mateix punt però amb y menor
                if (p.x() < midPoint.x() || (p.x() == midPoint.x() && p.y() < midPoint.y())) {
                    QyList.add(p);
                } else {
                    RyList.add(p);
                }
            }
            
            // Convertim les llistes a arrays
            Point2D[] Qy = QyList.toArray(new Point2D[0]);
            Point2D[] Ry = RyList.toArray(new Point2D[0]);
            
            // Executem la subtasca esquerra en un fil separat (fork)
            ClosestPairTask leftTask = new ClosestPairTask(Qx, Qy);
            leftTask.fork();
            
            // Executem la subtasca dreta en el fil actual (compute)
            ClosestPairResult rightResult = new ClosestPairTask(Rx, Ry).compute();
            // Esperem que la subtasca esquerra acabi i obtenim el seu resultat
            ClosestPairResult leftResult = leftTask.join();
            
            // Trobem la distància mínima entre els resultats esquerre i dret
            ClosestPairResult bestResult = leftResult.min(rightResult);
            double delta = bestResult.distance;
            
            // Comprovem si hi ha un parell més proper creuant la línia divisòria
            ClosestPairResult stripResult = findClosestInStrip(Py, midPoint.x(), delta);
            return bestResult.min(stripResult);
        }
        
        /**
         * Algoritme de força bruta per calcular el parell més proper en un conjunt petit de punts.
         * <p>
         * Aquest mètode compara totes les parelles possibles i té complexitat O(n²).
         * S'utilitza només quan el nombre de punts és menor que el llindar.
         * </p>
         * 
         * @param points array de punts a processar
         * @return el resultat amb els dos punts més propers i la seva distància
         */
        private ClosestPairResult bruteForceClosestPair(Point2D[] points) {
            double minDist = Double.POSITIVE_INFINITY;
            Point2D p1 = null, p2 = null;
            
            // Comparem tots els parells possibles
            for (int i = 0; i < points.length; i++) {
                for (int j = i + 1; j < points.length; j++) {
                    // Calculem la distància euclidiana
                    double d = distance(points[i], points[j]);
                    // Actualitzem si trobem una distància menor
                    if (d < minDist) {
                        minDist = d;
                        p1 = points[i];
                        p2 = points[j];
                    }
                }
            }
            
            return new ClosestPairResult(p1, p2, minDist);
        }
        
        /**
         * Cerca el parell més proper on els punts estan en costats oposats de la línia divisòria.
         * <p>
         * Aquest mètode implementa l'optimització clau de l'algoritme: només necessitem 
         * comprovar punts dins d'una franja estreta centrada a la línia divisòria, i per 
         * cada punt només necessitem comprovar un nombre limitat de veïns ordenats per y.
         * </p>
         * 
         * @param Py array de punts ordenats per coordenada y
         * @param midX coordenada x de la línia divisòria
         * @param delta distància mínima trobada fins ara
         * @return el resultat amb els punts més propers trobats a la franja, o null si no hi ha cap parell més proper
         */
        private ClosestPairResult findClosestInStrip(Point2D[] Py, double midX, double delta) {
            // Creem una franja de punts a distància màxima delta de la línia divisòria
            List<Point2D> strip = new ArrayList<>();
            for (Point2D p : Py) {
                // Només considerem punts a distància horitzontal menor que delta
                if (Math.abs(p.x() - midX) < delta) {
                    strip.add(p);
                }
            }
            
            // Cerquem el parell més proper dins la franja
            double minDist = delta;
            Point2D p1 = null, p2 = null;
            int size = strip.size();
            
            for (int i = 0; i < size; i++) {
                // Propietat matemàtica: només cal comprovar fins a 7 punts següents
                // (es pot demostrar que no hi pot haver més de 8 punts en un rectangle 2δ × δ)
                for (int j = i + 1; j < size && j <= i + 7; j++) {
                    Point2D a = strip.get(i);
                    Point2D b = strip.get(j);
                    
                    // Optimització: si la distància vertical ja és major que la mínima, saltem
                    if (b.y() - a.y() >= minDist) continue;
                    
                    // Calculem la distància euclidiana
                    double d = distance(a, b);
                    // Actualitzem si trobem una distància menor
                    if (d < minDist) {
                        minDist = d;
                        p1 = a;
                        p2 = b;
                    }
                }
            }
            
            // Només retornem un resultat si hem trobat una distància menor que delta
            return minDist < delta ? new ClosestPairResult(p1, p2, minDist) : null;
        }
    }
}