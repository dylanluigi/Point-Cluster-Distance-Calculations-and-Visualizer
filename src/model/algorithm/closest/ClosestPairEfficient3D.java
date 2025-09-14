package model.algorithm.closest;

import model.Point3D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Implementació eficient O(n log^2 n) de l'algoritme del parell més proper per a punts 3D.
 * <p>
 * Aquest algoritme utilitza un enfocament de dividir i conquerir en tres dimensions amb:
 * <ul>
 *   <li>Ordenació dels punts per coordenades x, y i z</li>
 *   <li>Divisió recursiva del conjunt de punts</li>
 *   <li>Processament concurrent amb "work stealing" dinàmic via ForkJoinPool</li>
 *   <li>Optimització específica per a l'espai tridimensional</li>
 * </ul>
 * </p>
 * <p>
 * La complexitat temporal és O(n log^2 n) degut a les operacions addicionals necessàries
 * per processar la tercera dimensió.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class ClosestPairEfficient3D extends ClosestPairBase3D {
    /**
     * Llindar per determinar quan canviar a computació directa.
     * Per sota d'aquesta quantitat de punts, s'utilitza l'algoritme de força bruta.
     */
    private static final int THRESHOLD = 50;

    /**
     * Executa l'algoritme del parell més proper en 3D utilitzant una estratègia eficient.
     * <p>
     * El procés d'execució inclou:
     * <ol>
     *   <li>Ordenar els punts per coordenades x, y i z</li>
     *   <li>Utilitzar un ForkJoinPool per paral·lelitzar el càlcul</li>
     *   <li>Aplicar l'algoritme de dividir i conquerir recursivament</li>
     *   <li>Considerar les tres dimensions per trobar el parell més proper</li>
     * </ol>
     * </p>
     * 
     * @param points la llista de punts 3D a processar
     * @return el resultat del càlcul del parell més proper amb els dos punts més propers i la seva distància
     */
    @Override
    public AlgorithmResult<Point3D> execute(List<Point3D> points) {
        long startTime = System.nanoTime();
        int n = points.size();
        Point3D[] pts = points.toArray(new Point3D[0]);
        
        // Ordena els punts per coordenada x
        Point3D[] Px = pts.clone();
        Arrays.sort(Px, Comparator.comparingDouble(Point3D::x));
        
        // Ordena els punts per coordenada y
        Point3D[] Py = pts.clone();
        Arrays.sort(Py, Comparator.comparingDouble(Point3D::y));
        
        // Ordena els punts per coordenada z
        Point3D[] Pz = pts.clone();
        Arrays.sort(Pz, Comparator.comparingDouble(Point3D::z));
        
        // Utilitza un ForkJoinPool per execució paral·lela
        int parallelism = Runtime.getRuntime().availableProcessors();
        ForkJoinPool pool = new ForkJoinPool(parallelism);
        // Crea la tasca principal amb tots els punts ordenats
        ClosestPairTask task = new ClosestPairTask(Px, Py, Pz);
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
     * Conté els dos punts més propers trobats i la seva distància en l'espai 3D.
     * També proporciona un mètode per comparar i seleccionar el resultat amb menor distància.
     * </p>
     */
    private static class ClosestPairResult {
        final Point3D p1;
        final Point3D p2;
        final double distance;
        
        /**
         * Constructor per crear un nou resultat.
         * 
         * @param p1 el primer punt del parell
         * @param p2 el segon punt del parell
         * @param distance la distància entre els dos punts
         */
        public ClosestPairResult(Point3D p1, Point3D p2, double distance) {
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
     * utilitzant el framework Fork/Join. Manté arrays de punts ordenats per les tres
     * coordenades (x, y, z) per optimitzar la cerca.
     * </p>
     */
    private class ClosestPairTask extends RecursiveTask<ClosestPairResult> {
        private final Point3D[] Px; // Punts ordenats per coordenada x
        private final Point3D[] Py; // Punts ordenats per coordenada y
        private final Point3D[] Pz; // Punts ordenats per coordenada z

        /**
         * Constructor per crear una nova tasca de càlcul.
         * 
         * @param Px array de punts ordenats per coordenada x
         * @param Py array de punts ordenats per coordenada y
         * @param Pz array de punts ordenats per coordenada z
         */
        public ClosestPairTask(Point3D[] Px, Point3D[] Py, Point3D[] Pz) {
            this.Px = Px;
            this.Py = Py;
            this.Pz = Pz;
        }

        /**
         * Implementa l'algoritme de divisió i conquesta per trobar el parell més proper en 3D.
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
            Point3D midPoint = Px[mid];
            
            // Dividim els punts ordenats per x en dos subconjunts
            Point3D[] Qx = Arrays.copyOfRange(Px, 0, mid);
            Point3D[] Rx = Arrays.copyOfRange(Px, mid, n);
            
            // Dividim els punts ordenats per y i z en dos subconjunts
            // corresponents als punts de l'esquerra i dreta
            List<Point3D> QyList = new ArrayList<>();
            List<Point3D> RyList = new ArrayList<>();
            List<Point3D> QzList = new ArrayList<>();
            List<Point3D> RzList = new ArrayList<>();
            
            // Assignem cada punt ordenat per y a la llista corresponent
            for (Point3D p : Py) {
                // Si el punt està a l'esquerra del punt mitjà o és el mateix punt però amb y menor
                if (p.x() < midPoint.x() || (p.x() == midPoint.x() && p.y() < midPoint.y())) {
                    QyList.add(p);
                } else {
                    RyList.add(p);
                }
            }
            
            // Assignem cada punt ordenat per z a la llista corresponent
            for (Point3D p : Pz) {
                // Si el punt està a l'esquerra del punt mitjà o és el mateix punt però amb z menor
                if (p.x() < midPoint.x() || (p.x() == midPoint.x() && p.z() < midPoint.z())) {
                    QzList.add(p);
                } else {
                    RzList.add(p);
                }
            }
            
            // Convertim les llistes a arrays
            Point3D[] Qy = QyList.toArray(new Point3D[0]);
            Point3D[] Ry = RyList.toArray(new Point3D[0]);
            Point3D[] Qz = QzList.toArray(new Point3D[0]);
            Point3D[] Rz = RzList.toArray(new Point3D[0]);
            
            // Executem la subtasca esquerra en un fil separat (fork)
            ClosestPairTask leftTask = new ClosestPairTask(Qx, Qy, Qz);
            leftTask.fork();
            
            // Executem la subtasca dreta en el fil actual (compute)
            ClosestPairResult rightResult = new ClosestPairTask(Rx, Ry, Rz).compute();
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
        private ClosestPairResult bruteForceClosestPair(Point3D[] points) {
            double minDist = Double.POSITIVE_INFINITY;
            Point3D p1 = null, p2 = null;
            
            // Comparem tots els parells possibles
            for (int i = 0; i < points.length; i++) {
                for (int j = i + 1; j < points.length; j++) {
                    // Calculem la distància euclidiana en 3D
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
         * En 3D, la tècnica de la franja és més complexa que en 2D, ja que cal considerar 
         * les tres dimensions. Només punts dins d'un volum limitat al voltant del pla divisori
         * són candidats a formar el parell més proper.
         * </p>
         * 
         * @param Py array de punts ordenats per coordenada y
         * @param midX coordenada x de la línia divisòria
         * @param delta distància mínima trobada fins ara
         * @return el resultat amb els punts més propers trobats a la franja, o null si no hi ha cap parell més proper
         */
        private ClosestPairResult findClosestInStrip(Point3D[] Py, double midX, double delta) {
            // Creem una franja de punts a distància màxima delta del pla divisori
            List<Point3D> strip = new ArrayList<>();
            for (Point3D p : Py) {
                // Només considerem punts a distància horitzontal menor que delta
                if (Math.abs(p.x() - midX) < delta) {
                    strip.add(p);
                }
            }
            
            // Cerquem el parell més proper dins la franja
            double minDist = delta;
            Point3D p1 = null, p2 = null;
            int size = strip.size();
            
            for (int i = 0; i < size; i++) {
                // En 3D, cal comprovar més punts que en 2D
                // El límit de 15 és una fita superior conservadora per a l'espai 3D
                for (int j = i + 1; j < size && j <= i + 15; j++) {
                    Point3D a = strip.get(i);
                    Point3D b = strip.get(j);
                    
                    // Optimització: si les distàncies en y o z ja són majors que delta, saltem
                    if (Math.abs(b.y() - a.y()) >= delta || Math.abs(b.z() - a.z()) >= delta) {
                        continue;
                    }
                    
                    // Calculem la distància euclidiana en 3D
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