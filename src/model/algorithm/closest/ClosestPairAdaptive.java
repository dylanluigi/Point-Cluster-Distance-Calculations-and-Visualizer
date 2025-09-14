package model.algorithm.closest;

import model.Point2D;
import model.Point3D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.Dimension;
import model.algorithm.PointCloudAlgorithm;
import model.algorithm.kdtree.KDTree;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * Implementació adaptativa de l'algoritme del parell més proper que selecciona l'estratègia
 * més adequada basant-se en les característiques del conjunt de punts.
 * <p>
 * Aquest algoritme analitza la distribució dels punts i dinàmicament escull entre
 * diverses estratègies:
 * <ul>
 *   <li>KD-Tree per a distribucions uniformes o gaussianes</li>
 *   <li>Grid/Bucket per a distribucions uniformes amb molts punts</li>
 *   <li>Divide-and-conquer per a distribucions irregulars</li>
 * </ul>
 * D'aquesta manera s'aconsegueix un bon rendiment en diferents escenaris.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class ClosestPairAdaptive {
    
    private static final int GRID_THRESHOLD = 10000; // Llindar per utilitzar Grid/Bucket
    private static final int PARALLEL_THRESHOLD = 5000; // Llindar per paral·lelitzar
    
    /**
     * Tipus d'algoritme específic d'aquesta implementació.
     */
    private static final AlgorithmType ALGORITHM_TYPE = AlgorithmType.CLOSEST_PAIR_ADAPTIVE;
    
    /**
     * Enum per classificar diferents tipus de distribucions.
     */
    private enum DistributionType {
        UNIFORM,
        CLUSTERED,
        IRREGULAR
    }
    
    /**
     * Factory method que crea i retorna la implementació adaptativa adequada
     * segons la dimensió.
     * 
     * @param dimension la dimensió (2D o 3D)
     * @return la implementació adaptativa adequada
     */
    @SuppressWarnings("unchecked")
    public static <T> PointCloudAlgorithm<T> createAdaptiveStrategy(Dimension dimension) {
        if (dimension == Dimension.TWO_D) {
            return (PointCloudAlgorithm<T>) new ClosestPairAdaptive2D();
        } else if (dimension == Dimension.THREE_D) {
            return (PointCloudAlgorithm<T>) new ClosestPairAdaptive3D();
        } else {
            throw new IllegalArgumentException("Dimensió no suportada: " + dimension);
        }
    }
    
    /**
     * Implementació adaptativa per a punts 2D.
     */
    public static class ClosestPairAdaptive2D extends ClosestPairBase2D {
        @Override
        public AlgorithmResult<Point2D> execute(List<Point2D> points) {
            long startTime = System.nanoTime();
            
            if (points.size() < 2) {
                throw new IllegalArgumentException("Es necessiten almenys 2 punts per trobar el parell més proper");
            }
            
            // Analitzem la distribució
            DistributionType distType = analyzeDistribution(points);
            
            // Escollim l'estratègia més adequada
            AlgorithmResult<Point2D> result;
            
            switch (distType) {
                case UNIFORM:
                    if (points.size() > GRID_THRESHOLD) {
                        result = executeGridBased(points);
                    } else {
                        result = executeKDTree(points);
                    }
                    break;
                case CLUSTERED:
                    result = executeKDTree(points);
                    break;
                case IRREGULAR:
                default:
                    if (points.size() > PARALLEL_THRESHOLD) {
                        result = executeParallelDivideAndConquer(points);
                    } else {
                        result = executeSequentialDivideAndConquer(points);
                    }
                    break;
            }
            
            // Actualitzem el temps total
            long totalTime = (System.nanoTime() - startTime) / 1_000_000;
            return new AlgorithmResult<>(
                    result.getPoint1(),
                    result.getPoint2(),
                    result.getDistance(),
                    ALGORITHM_TYPE,
                    totalTime
            );
        }
        
        /**
         * Analitza la distribució dels punts per determinar l'estratègia més adequada.
         * 
         * @param points llista de punts a analitzar
         * @return el tipus de distribució detectat
         */
        private DistributionType analyzeDistribution(List<Point2D> points) {
            // Càlcul ràpid de l'entropía espacial i agrupament dels punts
            
            // 1. Trobar els límits
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            
            for (Point2D p : points) {
                minX = Math.min(minX, p.x());
                minY = Math.min(minY, p.y());
                maxX = Math.max(maxX, p.x());
                maxY = Math.max(maxY, p.y());
            }
            
            // 2. Crear una quadrícula bàsica per analitzar la distribució
            int gridSize = (int) Math.sqrt(points.size()) / 4;
            int[][] grid = new int[gridSize][gridSize];
            
            double cellWidth = (maxX - minX) / gridSize;
            double cellHeight = (maxY - minY) / gridSize;
            
            for (Point2D p : points) {
                int cellX = Math.min(gridSize - 1, Math.max(0, (int) ((p.x() - minX) / cellWidth)));
                int cellY = Math.min(gridSize - 1, Math.max(0, (int) ((p.y() - minY) / cellHeight)));
                grid[cellX][cellY]++;
            }
            
            // 3. Anàlisi estadístic de la distribució
            int emptyCount = 0;
            int highDensityCount = 0;
            double meanCount = (double) points.size() / (gridSize * gridSize);
            double variance = 0;
            
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    int count = grid[i][j];
                    if (count == 0) emptyCount++;
                    if (count > meanCount * 2) highDensityCount++;
                    variance += Math.pow(count - meanCount, 2);
                }
            }
            variance /= (gridSize * gridSize);
            double stdDev = Math.sqrt(variance);
            double coeffOfVariation = stdDev / meanCount;
            
            // 4. Determinació del tipus de distribució
            if (coeffOfVariation < 0.5 && emptyCount < gridSize * gridSize * 0.1) {
                return DistributionType.UNIFORM;
            } else if (highDensityCount > gridSize * gridSize * 0.05) {
                return DistributionType.CLUSTERED;
            } else {
                return DistributionType.IRREGULAR;
            }
        }
        
        /**
         * Executa l'algoritme de parell més proper utilitzant KD-Tree.
         * 
         * @param points llista de punts
         * @return el resultat amb el parell més proper
         */
        private AlgorithmResult<Point2D> executeKDTree(List<Point2D> points) {
            long startTime = System.nanoTime();
            KDTree<Point2D> kdTree = KDTree.create2DTree(new ArrayList<>(points));
            
            double minDistance = Double.POSITIVE_INFINITY;
            Point2D closestP1 = null;
            Point2D closestP2 = null;
            
            for (Point2D point : points) {
                List<Point2D> neighbors = kdTree.findKNearest(point, 2);
                if (neighbors.size() > 1) {
                    Point2D nearest = neighbors.get(1);
                    double distance = distance(point, nearest);
                    
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestP1 = point;
                        closestP2 = nearest;
                    }
                }
            }
            
            long executionTime = (System.nanoTime() - startTime) / 1_000_000;
            return new AlgorithmResult<>(closestP1, closestP2, minDistance, ALGORITHM_TYPE, executionTime);
        }
        
        /**
         * Executa l'algoritme de parell més proper utilitzant Grid/Bucket.
         * 
         * @param points llista de punts
         * @return el resultat amb el parell més proper
         */
        private AlgorithmResult<Point2D> executeGridBased(List<Point2D> points) {
            long startTime = System.nanoTime();
            
            // 1. Trobar els límits
            double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            
            for (Point2D p : points) {
                minX = Math.min(minX, p.x());
                minY = Math.min(minY, p.y());
                maxX = Math.max(maxX, p.x());
                maxY = Math.max(maxY, p.y());
            }
            
            // Inicialitzem amb una cerca naïf d'una mostra petita per estimar distància
            int sampleSize = Math.min(100, points.size());
            double minDistance = Double.POSITIVE_INFINITY;
            Point2D closestP1 = null, closestP2 = null;
            
            for (int i = 0; i < sampleSize; i++) {
                for (int j = i + 1; j < sampleSize; j++) {
                    double dist = distance(points.get(i), points.get(j));
                    if (dist < minDistance) {
                        minDistance = dist;
                        closestP1 = points.get(i);
                        closestP2 = points.get(j);
                    }
                }
            }
            
            // 2. Crear una quadrícula amb dimensions basades en la distància mínima estimada
            double cellSize = minDistance;
            int numCellsX = (int) Math.ceil((maxX - minX) / cellSize) + 1;
            int numCellsY = (int) Math.ceil((maxY - minY) / cellSize) + 1;
            
            // Estructura per emmagatzemar punts per cel·la
            List<Point2D>[][] grid = new ArrayList[numCellsX][numCellsY];
            for (int i = 0; i < numCellsX; i++) {
                for (int j = 0; j < numCellsY; j++) {
                    grid[i][j] = new ArrayList<>();
                }
            }
            
            // 3. Assignar punts a cel·les
            for (Point2D point : points) {
                int cellX = (int) ((point.x() - minX) / cellSize);
                int cellY = (int) ((point.y() - minY) / cellSize);
                grid[cellX][cellY].add(point);
            }
            
            // 4. Per cada punt, buscar-lo amb punts de la mateixa cel·la i cel·les adjacents
            minDistance = Double.POSITIVE_INFINITY;
            closestP1 = null;
            closestP2 = null;
            
            for (int i = 0; i < numCellsX; i++) {
                for (int j = 0; j < numCellsY; j++) {
                    List<Point2D> cellPoints = grid[i][j];
                    
                    // Buscar parells dins la mateixa cel·la
                    for (int k = 0; k < cellPoints.size(); k++) {
                        for (int l = k + 1; l < cellPoints.size(); l++) {
                            double dist = distance(cellPoints.get(k), cellPoints.get(l));
                            if (dist < minDistance) {
                                minDistance = dist;
                                closestP1 = cellPoints.get(k);
                                closestP2 = cellPoints.get(l);
                            }
                        }
                    }
                    
                    // Buscar parells amb cel·les adjacents
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            if (dx == 0 && dy == 0) continue;
                            
                            int ni = i + dx;
                            int nj = j + dy;
                            
                            if (ni >= 0 && ni < numCellsX && nj >= 0 && nj < numCellsY) {
                                List<Point2D> neighborCellPoints = grid[ni][nj];
                                
                                for (Point2D p1 : cellPoints) {
                                    for (Point2D p2 : neighborCellPoints) {
                                        double dist = distance(p1, p2);
                                        if (dist < minDistance) {
                                            minDistance = dist;
                                            closestP1 = p1;
                                            closestP2 = p2;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            long executionTime = (System.nanoTime() - startTime) / 1_000_000;
            return new AlgorithmResult<>(closestP1, closestP2, minDistance, ALGORITHM_TYPE, executionTime);
        }
        
        /**
         * Executa l'algoritme de parell més proper utilitzant Divide-and-Conquer seqüencial.
         * 
         * @param points llista de punts
         * @return el resultat amb el parell més proper
         */
        private AlgorithmResult<Point2D> executeSequentialDivideAndConquer(List<Point2D> points) {
            // Delegat a la implementació eficient existent
            ClosestPairEfficient2D efficientAlgo = new ClosestPairEfficient2D();
            return efficientAlgo.execute(points);
        }
        
        /**
         * Executa l'algoritme de parell més proper utilitzant Divide-and-Conquer paral·lel.
         * 
         * @param points llista de punts
         * @return el resultat amb el parell més proper
         */
        private AlgorithmResult<Point2D> executeParallelDivideAndConquer(List<Point2D> points) {
            // Similar a ClosestPairEfficient2D però utilitzant paral·lelisme
            // Per simplicitat, reutilitzem l'implementació existent
            ClosestPairEfficient2D efficientAlgo = new ClosestPairEfficient2D();
            return efficientAlgo.execute(points);
        }
        
        @Override
        public AlgorithmType getType() {
            return ALGORITHM_TYPE;
        }
    }
    
    /**
     * Implementació adaptativa per a punts 3D.
     */
    public static class ClosestPairAdaptive3D extends ClosestPairBase3D {
        @Override
        public AlgorithmResult<Point3D> execute(List<Point3D> points) {
            long startTime = System.nanoTime();
            
            if (points.size() < 2) {
                throw new IllegalArgumentException("Es necessiten almenys 2 punts per trobar el parell més proper");
            }
            
            // Analitzem la distribució (simplificat per a 3D)
            DistributionType distType = DistributionType.UNIFORM;  // Per defecte usem KD-Tree
            
            // Escollim l'estratègia més adequada
            AlgorithmResult<Point3D> result;
            
            switch (distType) {
                case UNIFORM:
                case CLUSTERED:
                    result = executeKDTree(points);
                    break;
                case IRREGULAR:
                default:
                    if (points.size() > PARALLEL_THRESHOLD) {
                        result = executeParallelDivideAndConquer(points);
                    } else {
                        result = executeSequentialDivideAndConquer(points);
                    }
                    break;
            }
            
            // Actualitzem el temps total
            long totalTime = (System.nanoTime() - startTime) / 1_000_000;
            return new AlgorithmResult<>(
                    result.getPoint1(),
                    result.getPoint2(),
                    result.getDistance(),
                    ALGORITHM_TYPE,
                    totalTime
            );
        }
        
        /**
         * Executa l'algoritme de parell més proper utilitzant KD-Tree per a punts 3D.
         * 
         * @param points llista de punts
         * @return el resultat amb el parell més proper
         */
        private AlgorithmResult<Point3D> executeKDTree(List<Point3D> points) {
            long startTime = System.nanoTime();
            KDTree<Point3D> kdTree = KDTree.create3DTree(new ArrayList<>(points));
            
            double minDistance = Double.POSITIVE_INFINITY;
            Point3D closestP1 = null;
            Point3D closestP2 = null;
            
            for (Point3D point : points) {
                List<Point3D> neighbors = kdTree.findKNearest(point, 2);
                if (neighbors.size() > 1) {
                    Point3D nearest = neighbors.get(1);
                    double distance = distance(point, nearest);
                    
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestP1 = point;
                        closestP2 = nearest;
                    }
                }
            }
            
            long executionTime = (System.nanoTime() - startTime) / 1_000_000;
            return new AlgorithmResult<>(closestP1, closestP2, minDistance, ALGORITHM_TYPE, executionTime);
        }
        
        /**
         * Executa l'algoritme de parell més proper utilitzant Divide-and-Conquer seqüencial.
         * 
         * @param points llista de punts
         * @return el resultat amb el parell més proper
         */
        private AlgorithmResult<Point3D> executeSequentialDivideAndConquer(List<Point3D> points) {
            // Delegat a la implementació eficient existent
            ClosestPairEfficient3D efficientAlgo = new ClosestPairEfficient3D();
            return efficientAlgo.execute(points);
        }
        
        /**
         * Executa l'algoritme de parell més proper utilitzant Divide-and-Conquer paral·lel.
         * 
         * @param points llista de punts
         * @return el resultat amb el parell més proper
         */
        private AlgorithmResult<Point3D> executeParallelDivideAndConquer(List<Point3D> points) {
            // Similar a ClosestPairEfficient3D però paral·lelitzat
            // Per simplicitat, reutilitzem l'implementació existent
            ClosestPairEfficient3D efficientAlgo = new ClosestPairEfficient3D();
            return efficientAlgo.execute(points);
        }
        
        @Override
        public AlgorithmType getType() {
            return ALGORITHM_TYPE;
        }
    }
}