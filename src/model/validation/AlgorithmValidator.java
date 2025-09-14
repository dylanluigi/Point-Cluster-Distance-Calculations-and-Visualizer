package model.validation;

import model.Point2D;
import model.Point3D;
import model.PointGenerator;
import model.algorithm.AlgorithmFactory;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.Dimension;
import model.algorithm.PointCloudAlgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.DoubleSummaryStatistics;

/**
 * Classe per validar i comparar els diferents algoritmes de càlcul de distàncies.
 * <p>
 * Aquesta classe permet verificar que tots els algoritmes troben la mateixa distància
 * mínima (amb els mateixos punts) i comparar el rendiment dels algoritmes de diàmetre.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class AlgorithmValidator {
    
    // Constants per a la generació de punts
    private static final int DEFAULT_NUM_POINTS = 1000;
    private static final int MAX_COORDINATE = 1000;
    
    /**
     * Punt d'entrada principal per executar la validació i comparació.
     * 
     * @param args arguments de línia de comandes (no utilitzats)
     */
    public static void main(String[] args) {
        System.out.println("Validant els algoritmes de parell més proper en 2D...");
        validateClosestPairAlgorithms2D();
        
        System.out.println("\nValidant els algoritmes de parell més proper en 3D...");
        validateClosestPairAlgorithms3D();
        
        System.out.println("\nValidant els algoritmes de diàmetre en 2D...");
        validateDiameterAlgorithms2D();
        
        System.out.println("\nValidant els algoritmes de diàmetre en 3D...");
        validateDiameterAlgorithms3D();
        
        System.out.println("\nComparant rendiment dels algoritmes de parell més proper en 2D...");
        compareClosestPairPerformance2D();
        
        System.out.println("\nComparant rendiment dels algoritmes de parell més proper en 3D...");
        compareClosestPairPerformance3D();
        
        System.out.println("\nComparant rendiment dels algoritmes de diàmetre en 2D...");
        compareDiameterPerformance2D();
        
        System.out.println("\nComparant rendiment dels algoritmes de diàmetre en 3D...");
        compareDiameterPerformance3D();
    }
    
    /**
     * Valida que tots els algoritmes de parell més proper en 2D trobin la mateixa distància mínima.
     */
    public static void validateClosestPairAlgorithms2D() {
        // Genera un conjunt de punts 2D aleatoris uniforme
        List<Point2D> points = PointGenerator.generate2DPoints(
            DEFAULT_NUM_POINTS, 
            PointGenerator.Distribution.UNIFORM, 
            MAX_COORDINATE, 
            MAX_COORDINATE
        );
        
        // Llista d'algoritmes de parell més proper en 2D
        List<AlgorithmType> closestPairAlgorithms = Arrays.asList(
            AlgorithmType.CLOSEST_PAIR_NAIVE,
            AlgorithmType.CLOSEST_PAIR_EFFICIENT,
            AlgorithmType.CLOSEST_PAIR_KDTREE
        );
        
        // Executar cada algoritme amb el mateix conjunt de punts
        List<AlgorithmResult<Point2D>> results = new ArrayList<>();
        for (AlgorithmType type : closestPairAlgorithms) {
            PointCloudAlgorithm<Point2D> algorithm = AlgorithmFactory.createAlgorithm(type, Dimension.TWO_D);
            AlgorithmResult<Point2D> result = algorithm.execute(points);
            results.add(result);
            
            System.out.printf("%s: Distància mínima = %.6f entre punts %s i %s%n", 
                type.getDisplayName(), 
                result.getDistance(),
                result.getPoint1(),
                result.getPoint2()
            );
        }
        
        // Verificar que tots els resultats tenen la mateixa distància (amb tolerància)
        validateDistances(results);
    }
    
    /**
     * Valida que tots els algoritmes de parell més proper en 3D trobin la mateixa distància mínima.
     */
    public static void validateClosestPairAlgorithms3D() {
        // Genera un conjunt de punts 3D aleatoris
        List<Point3D> points = PointGenerator.generate3DPoints(
            DEFAULT_NUM_POINTS, 
            PointGenerator.Distribution.UNIFORM, 
            MAX_COORDINATE, 
            MAX_COORDINATE,
            MAX_COORDINATE
        );
        
        // Llista d'algoritmes de parell més proper en 3D
        List<AlgorithmType> closestPairAlgorithms = Arrays.asList(
            AlgorithmType.CLOSEST_PAIR_NAIVE,
            AlgorithmType.CLOSEST_PAIR_EFFICIENT,
            AlgorithmType.CLOSEST_PAIR_KDTREE
        );
        
        // Executar cada algoritme amb el mateix conjunt de punts
        List<AlgorithmResult<Point3D>> results = new ArrayList<>();
        for (AlgorithmType type : closestPairAlgorithms) {
            PointCloudAlgorithm<Point3D> algorithm = AlgorithmFactory.createAlgorithm(type, Dimension.THREE_D);
            AlgorithmResult<Point3D> result = algorithm.execute(points);
            results.add(result);
            
            System.out.printf("%s: Distància mínima = %.6f entre punts %s i %s%n", 
                type.getDisplayName(), 
                result.getDistance(),
                result.getPoint1(),
                result.getPoint2()
            );
        }
        
        // Verificar que tots els resultats tenen la mateixa distància (amb tolerància)
        validateDistances(results);
    }
    
    /**
     * Valida que tots els algoritmes de diàmetre en 2D trobin la mateixa distància màxima.
     */
    public static void validateDiameterAlgorithms2D() {
        // Genera un conjunt de punts 2D aleatoris
        List<Point2D> points = PointGenerator.generate2DPoints(
            DEFAULT_NUM_POINTS, 
            PointGenerator.Distribution.UNIFORM, 
            MAX_COORDINATE, 
            MAX_COORDINATE
        );
        
        // Llista d'algoritmes de diàmetre en 2D
        List<AlgorithmType> diameterAlgorithms = Arrays.asList(
            AlgorithmType.DIAMETER_NAIVE,
            AlgorithmType.DIAMETER_CONCURRENT,
            AlgorithmType.DIAMETER_QUICKHULL
        );
        
        // Executar cada algoritme amb el mateix conjunt de punts
        List<AlgorithmResult<Point2D>> results = new ArrayList<>();
        for (AlgorithmType type : diameterAlgorithms) {
            PointCloudAlgorithm<Point2D> algorithm = AlgorithmFactory.createAlgorithm(type, Dimension.TWO_D);
            AlgorithmResult<Point2D> result = algorithm.execute(points);
            results.add(result);
            
            System.out.printf("%s: Diàmetre = %.6f entre punts %s i %s%n", 
                type.getDisplayName(), 
                result.getDistance(),
                result.getPoint1(),
                result.getPoint2()
            );
        }
        
        // Verificar que tots els resultats tenen la mateixa distància (amb tolerància)
        validateDistances(results);
    }
    
    /**
     * Valida que tots els algoritmes de diàmetre en 3D trobin la mateixa distància màxima.
     */
    public static void validateDiameterAlgorithms3D() {
        // Genera un conjunt de punts 3D aleatoris
        List<Point3D> points = PointGenerator.generate3DPoints(
            DEFAULT_NUM_POINTS, 
            PointGenerator.Distribution.UNIFORM, 
            MAX_COORDINATE, 
            MAX_COORDINATE,
            MAX_COORDINATE
        );
        
        // Llista d'algoritmes de diàmetre en 3D
        List<AlgorithmType> diameterAlgorithms = Arrays.asList(
            AlgorithmType.DIAMETER_NAIVE,
            AlgorithmType.DIAMETER_CONCURRENT,
            AlgorithmType.DIAMETER_QUICKHULL
        );
        
        // Executar cada algoritme amb el mateix conjunt de punts
        List<AlgorithmResult<Point3D>> results = new ArrayList<>();
        for (AlgorithmType type : diameterAlgorithms) {
            PointCloudAlgorithm<Point3D> algorithm = AlgorithmFactory.createAlgorithm(type, Dimension.THREE_D);
            AlgorithmResult<Point3D> result = algorithm.execute(points);
            results.add(result);
            
            System.out.printf("%s: Diàmetre = %.6f entre punts %s i %s%n", 
                type.getDisplayName(), 
                result.getDistance(),
                result.getPoint1(),
                result.getPoint2()
            );
        }
        
        // Verificar que tots els resultats tenen la mateixa distància (amb tolerància)
        validateDistances(results);
    }
    
    /**
     * Compara el rendiment dels algoritmes de parell més proper en 2D.
     */
    private static void compareClosestPairPerformance2D() {
        // Genera diferents conjunts de punts 2D amb diferents mides i distribucions
        Map<String, Map<AlgorithmType, List<Long>>> performanceData = new HashMap<>();
        
        int[] pointSizes = {1000, 2000, 5000, 10000};
        PointGenerator.Distribution[] distributions = {
            PointGenerator.Distribution.UNIFORM,
            PointGenerator.Distribution.GAUSSIAN,
            PointGenerator.Distribution.EXPONENTIAL
        };
        
        // Algoritmes a comparar
        List<AlgorithmType> closestPairAlgorithms = Arrays.asList(
            AlgorithmType.CLOSEST_PAIR_NAIVE,
            AlgorithmType.CLOSEST_PAIR_EFFICIENT,
            AlgorithmType.CLOSEST_PAIR_KDTREE
        );
        
        // Executa els algoritmes en cada conjunt de punts
        for (int size : pointSizes) {
            for (PointGenerator.Distribution dist : distributions) {
                String key = String.format("%d punts, %s", size, dist.name());
                performanceData.put(key, new HashMap<>());
                
                // Genera els punts per a aquesta iteració
                List<Point2D> points = PointGenerator.generate2DPoints(
                    size, dist, MAX_COORDINATE, MAX_COORDINATE
                );
                
                // Executa cada algoritme 3 vegades per obtenir mitjanes
                for (AlgorithmType type : closestPairAlgorithms) {
                    List<Long> times = new ArrayList<>();
                    performanceData.get(key).put(type, times);
                    
                    for (int i = 0; i < 3; i++) {
                        PointCloudAlgorithm<Point2D> algorithm = AlgorithmFactory.createAlgorithm(type, Dimension.TWO_D);
                        AlgorithmResult<Point2D> result = algorithm.execute(points);
                        times.add(result.getExecutionTimeMs());
                    }
                }
            }
        }
        
        // Mostra els resultats de rendiment
        printPerformanceResults(performanceData);
    }
    
    /**
     * Compara el rendiment dels algoritmes de parell més proper en 3D.
     */
    private static void compareClosestPairPerformance3D() {
        // Genera diferents conjunts de punts 3D amb diferents mides i distribucions
        Map<String, Map<AlgorithmType, List<Long>>> performanceData = new HashMap<>();
        
        int[] pointSizes = {1000, 2000, 5000, 10000};
        PointGenerator.Distribution[] distributions = {
            PointGenerator.Distribution.UNIFORM,
            PointGenerator.Distribution.GAUSSIAN,
            PointGenerator.Distribution.EXPONENTIAL
        };
        
        // Algoritmes a comparar
        List<AlgorithmType> closestPairAlgorithms = Arrays.asList(
            AlgorithmType.CLOSEST_PAIR_NAIVE,
            AlgorithmType.CLOSEST_PAIR_EFFICIENT,
            AlgorithmType.CLOSEST_PAIR_KDTREE
        );
        
        // Executa els algoritmes en cada conjunt de punts
        for (int size : pointSizes) {
            for (PointGenerator.Distribution dist : distributions) {
                String key = String.format("%d punts, %s", size, dist.name());
                performanceData.put(key, new HashMap<>());
                
                // Genera els punts per a aquesta iteració
                List<Point3D> points = PointGenerator.generate3DPoints(
                    size, dist, MAX_COORDINATE, MAX_COORDINATE, MAX_COORDINATE
                );
                
                // Executa cada algoritme 3 vegades per obtenir mitjanes
                for (AlgorithmType type : closestPairAlgorithms) {
                    List<Long> times = new ArrayList<>();
                    performanceData.get(key).put(type, times);
                    
                    for (int i = 0; i < 3; i++) {
                        PointCloudAlgorithm<Point3D> algorithm = AlgorithmFactory.createAlgorithm(type, Dimension.THREE_D);
                        AlgorithmResult<Point3D> result = algorithm.execute(points);
                        times.add(result.getExecutionTimeMs());
                    }
                }
            }
        }
        
        // Mostra els resultats de rendiment
        printPerformanceResults(performanceData);
    }
    
    /**
     * Compara el rendiment dels algoritmes de diàmetre en 2D.
     */
    private static void compareDiameterPerformance2D() {
        // Genera diferents conjunts de punts 2D amb diferents mides i distribucions
        Map<String, Map<AlgorithmType, List<Long>>> performanceData = new HashMap<>();
        
        int[] pointSizes = {1000, 2000, 5000, 10000};
        PointGenerator.Distribution[] distributions = {
            PointGenerator.Distribution.UNIFORM,
            PointGenerator.Distribution.GAUSSIAN,
            PointGenerator.Distribution.EXPONENTIAL
        };
        
        // Algoritmes a comparar
        List<AlgorithmType> diameterAlgorithms = Arrays.asList(
            AlgorithmType.DIAMETER_NAIVE,
            AlgorithmType.DIAMETER_CONCURRENT,
            AlgorithmType.DIAMETER_QUICKHULL
        );
        
        // Executa els algoritmes en cada conjunt de punts
        for (int size : pointSizes) {
            for (PointGenerator.Distribution dist : distributions) {
                String key = String.format("%d punts, %s", size, dist.name());
                performanceData.put(key, new HashMap<>());
                
                // Genera els punts per a aquesta iteració
                List<Point2D> points = PointGenerator.generate2DPoints(
                    size, dist, MAX_COORDINATE, MAX_COORDINATE
                );
                
                // Executa cada algoritme 3 vegades per obtenir mitjanes
                for (AlgorithmType type : diameterAlgorithms) {
                    List<Long> times = new ArrayList<>();
                    performanceData.get(key).put(type, times);
                    
                    for (int i = 0; i < 3; i++) {
                        PointCloudAlgorithm<Point2D> algorithm = AlgorithmFactory.createAlgorithm(type, Dimension.TWO_D);
                        AlgorithmResult<Point2D> result = algorithm.execute(points);
                        times.add(result.getExecutionTimeMs());
                    }
                }
            }
        }
        
        // Mostra els resultats de rendiment
        printPerformanceResults(performanceData);
    }
    
    /**
     * Compara el rendiment dels algoritmes de diàmetre en 3D.
     */
    private static void compareDiameterPerformance3D() {
        // Genera diferents conjunts de punts 3D amb diferents mides i distribucions
        Map<String, Map<AlgorithmType, List<Long>>> performanceData = new HashMap<>();
        
        int[] pointSizes = {1000, 2000, 5000, 10000};
        PointGenerator.Distribution[] distributions = {
            PointGenerator.Distribution.UNIFORM,
            PointGenerator.Distribution.GAUSSIAN,
            PointGenerator.Distribution.EXPONENTIAL
        };
        
        // Algoritmes a comparar
        List<AlgorithmType> diameterAlgorithms = Arrays.asList(
            AlgorithmType.DIAMETER_NAIVE,
            AlgorithmType.DIAMETER_CONCURRENT,
            AlgorithmType.DIAMETER_QUICKHULL
        );
        
        // Executa els algoritmes en cada conjunt de punts
        for (int size : pointSizes) {
            for (PointGenerator.Distribution dist : distributions) {
                String key = String.format("%d punts, %s", size, dist.name());
                performanceData.put(key, new HashMap<>());
                
                // Genera els punts per a aquesta iteració
                List<Point3D> points = PointGenerator.generate3DPoints(
                    size, dist, MAX_COORDINATE, MAX_COORDINATE, MAX_COORDINATE
                );
                
                // Executa cada algoritme 3 vegades per obtenir mitjanes
                for (AlgorithmType type : diameterAlgorithms) {
                    List<Long> times = new ArrayList<>();
                    performanceData.get(key).put(type, times);
                    
                    for (int i = 0; i < 3; i++) {
                        PointCloudAlgorithm<Point3D> algorithm = AlgorithmFactory.createAlgorithm(type, Dimension.THREE_D);
                        AlgorithmResult<Point3D> result = algorithm.execute(points);
                        times.add(result.getExecutionTimeMs());
                    }
                }
            }
        }
        
        // Mostra els resultats de rendiment
        printPerformanceResults(performanceData);
    }
    
    /**
     * Imprimeix els resultats de rendiment i els guarda en un fitxer CSV.
     * 
     * @param performanceData les dades de rendiment a mostrar i desar
     */
    private static void printPerformanceResults(Map<String, Map<AlgorithmType, List<Long>>> performanceData) {
        // Imprimeix els resultats a la consola
        for (Map.Entry<String, Map<AlgorithmType, List<Long>>> entry : performanceData.entrySet()) {
            System.out.println("\n" + entry.getKey() + ":");
            System.out.println("-".repeat(entry.getKey().length() + 1));
            
            for (Map.Entry<AlgorithmType, List<Long>> algorithmEntry : entry.getValue().entrySet()) {
                List<Long> times = algorithmEntry.getValue();
                double avgTime = times.stream().mapToLong(Long::longValue).average().orElse(0);
                
                System.out.printf("%-40s: Mitjana=%.2f ms (Execucions: %s)%n", 
                    algorithmEntry.getKey().getDisplayName(),
                    avgTime,
                    times.stream().map(String::valueOf).collect(Collectors.joining(", "))
                );
            }
        }
        
        // Desa els resultats en un fitxer CSV
        try {
            java.io.FileWriter writer = new java.io.FileWriter("benchmark_results.csv");
            
            // Escriu la capçalera del CSV
            writer.write("DataSet,Size,Distribution,Algorithm,ExecutionTime(ms)\n");
            
            // Escriu les dades
            for (Map.Entry<String, Map<AlgorithmType, List<Long>>> entry : performanceData.entrySet()) {
                String key = entry.getKey();
                String[] parts = key.split(",");
                String sizeStr = parts[0].trim();
                String distribution = parts.length > 1 ? parts[1].trim() : "UNKNOWN";
                
                int size = Integer.parseInt(sizeStr.split(" ")[0]);
                
                for (Map.Entry<AlgorithmType, List<Long>> algorithmEntry : entry.getValue().entrySet()) {
                    AlgorithmType type = algorithmEntry.getKey();
                    String algorithm = type.name();
                    
                    for (Long time : algorithmEntry.getValue()) {
                        writer.write(String.format("%s,%d,%s,%s,%d\n", 
                            key.replace(",", " -"), 
                            size,
                            distribution,
                            algorithm,
                            time
                        ));
                    }
                }
            }
            
            writer.close();
            System.out.println("\nResultats desats a 'benchmark_results.csv'");
            
        } catch (java.io.IOException e) {
            System.err.println("Error en desar els resultats al fitxer CSV: " + e.getMessage());
        }
    }
    
    /**
     * Valida que tots els resultats tenen la mateixa distància (amb tolerància).
     * 
     * @param results la llista de resultats a validar
     * @param <T> el tipus de punts (Point2D o Point3D)
     */
    private static <T> void validateDistances(List<AlgorithmResult<T>> results) {
        if (results.isEmpty()) {
            System.out.println("No hi ha resultats per validar.");
            return;
        }
        
        // Tolerància per a comparacions de nombres en coma flotant
        final double EPSILON = 1e-6;
        
        // Obtén la primera distància com a referència
        double referenceDistance = results.get(0).getDistance();
        boolean allEqual = true;
        
        for (int i = 1; i < results.size(); i++) {
            double currentDistance = results.get(i).getDistance();
            if (Math.abs(currentDistance - referenceDistance) > EPSILON) {
                allEqual = false;
                System.out.printf("\nDIFERÈNCIA DETECTADA: %s (%.6f) vs %s (%.6f)%n",
                    results.get(0).getAlgorithmType().getDisplayName(), referenceDistance,
                    results.get(i).getAlgorithmType().getDisplayName(), currentDistance
                );
            }
        }
        
        if (allEqual) {
            System.out.println("\nVALIDACIÓ COMPLETADA: Tots els algoritmes han trobat la mateixa distància.");
        } else {
            System.out.println("\nVALIDACIÓ FALLIDA: S'han detectat diferències entre les distàncies trobades.");
        }
        
        // Estadístiques de temps d'execució
        DoubleSummaryStatistics stats = results.stream()
            .mapToDouble(result -> result.getExecutionTimeMs())
            .summaryStatistics();
        
        System.out.printf("\nEstadístiques de temps d'execució: min=%.2f ms, max=%.2f ms, mitjana=%.2f ms%n",
            stats.getMin(), stats.getMax(), stats.getAverage());
    }
}