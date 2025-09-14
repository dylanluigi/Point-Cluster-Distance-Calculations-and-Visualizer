package model.validation;

import model.Point2D;
import model.Point3D;
import model.PointGenerator;
import model.algorithm.AlgorithmFactory;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.Dimension;
import model.algorithm.PointCloudAlgorithm;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ComprehensiveBenchmark {

    private static final int MAX_COORDINATE = 1000;
    private static final int MAX_POINTS = 100000;
    private static final int NUM_RUNS = 3;

    private static final int[] POINT_SIZES = {
        1000, 2000, 5000, 
        10000, 20000, 30000, 40000, 50000, 
        60000, 70000, 80000, 90000, 100000
    };

    private static final PointGenerator.Distribution[] DISTRIBUTIONS = {
        PointGenerator.Distribution.UNIFORM,
        PointGenerator.Distribution.GAUSSIAN,
        PointGenerator.Distribution.EXPONENTIAL,
        PointGenerator.Distribution.LOG_NORMAL,
        PointGenerator.Distribution.RAYLEIGH
    };

    private static final AlgorithmType[] CLOSEST_PAIR_ALGORITHMS = {
        AlgorithmType.CLOSEST_PAIR_NAIVE,
        AlgorithmType.CLOSEST_PAIR_EFFICIENT,
        AlgorithmType.CLOSEST_PAIR_KDTREE,
        AlgorithmType.CLOSEST_PAIR_ADAPTIVE
    };

    private static final AlgorithmType[] DIAMETER_ALGORITHMS = {
        AlgorithmType.DIAMETER_NAIVE,
        AlgorithmType.DIAMETER_CONCURRENT,
        AlgorithmType.DIAMETER_QUICKHULL
    };
    
    public static void main(String[] args) {
        System.out.println("Starting comprehensive benchmark...");

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outputFile = "benchmark_comprehensive_" + timestamp + ".csv";

        AtomicInteger totalConfigurations = new AtomicInteger(0);
        AtomicInteger completedConfigurations = new AtomicInteger(0);

        for (int size : POINT_SIZES) {
            for (PointGenerator.Distribution dist : DISTRIBUTIONS) {
                totalConfigurations.addAndGet(
                    CLOSEST_PAIR_ALGORITHMS.length * 2 * NUM_RUNS + 
                    DIAMETER_ALGORITHMS.length * 2 * NUM_RUNS
                );
            }
        }
        
        System.out.println("Total configurations to benchmark: " + totalConfigurations.get());
        
        try (FileWriter writer = new FileWriter(outputFile)) {

            writer.write("Algorithm,Dimension,Size,Distribution,Run,ExecutionTime(ms),MemoryUsage(MB),IsCorrect\n");
            

            for (int size : POINT_SIZES) {
                System.out.println("\nBenchmarking with " + size + " points...");
                

                boolean skipNaiveForLargeSize = size > 50000;
                

                for (PointGenerator.Distribution dist : DISTRIBUTIONS) {
                    System.out.println("  Distribution: " + dist.name());
                    

                    List<Point2D> points2D = PointGenerator.generate2DPoints(
                        size, dist, MAX_COORDINATE, MAX_COORDINATE
                    );
                    
                    List<Point3D> points3D = PointGenerator.generate3DPoints(
                        size, dist, MAX_COORDINATE, MAX_COORDINATE, MAX_COORDINATE
                    );
                    

                    

                    double reference2D = -1;
                    double reference3D = -1;
                    
                    if (size <= 10000) {

                        PointCloudAlgorithm<Point2D> refAlgo2D = AlgorithmFactory.createAlgorithm(
                            AlgorithmType.CLOSEST_PAIR_NAIVE, Dimension.TWO_D
                        );
                        reference2D = refAlgo2D.execute(points2D).getDistance();
                        
                        PointCloudAlgorithm<Point3D> refAlgo3D = AlgorithmFactory.createAlgorithm(
                            AlgorithmType.CLOSEST_PAIR_NAIVE, Dimension.THREE_D
                        );
                        reference3D = refAlgo3D.execute(points3D).getDistance();
                    } else {

                        PointCloudAlgorithm<Point2D> refAlgo2D = AlgorithmFactory.createAlgorithm(
                            AlgorithmType.CLOSEST_PAIR_EFFICIENT, Dimension.TWO_D
                        );
                        reference2D = refAlgo2D.execute(points2D).getDistance();
                        
                        PointCloudAlgorithm<Point3D> refAlgo3D = AlgorithmFactory.createAlgorithm(
                            AlgorithmType.CLOSEST_PAIR_EFFICIENT, Dimension.THREE_D
                        );
                        reference3D = refAlgo3D.execute(points3D).getDistance();
                    }
                    

                    for (AlgorithmType algoType : CLOSEST_PAIR_ALGORITHMS) {

                        if (skipNaiveForLargeSize && algoType == AlgorithmType.CLOSEST_PAIR_NAIVE) {
                            System.out.println("    Skipping " + algoType.name() + " for large point set");
                            continue;
                        }
                        
                        System.out.println("    Testing " + algoType.name() + "...");
                        

                        for (int run = 1; run <= NUM_RUNS; run++) {
                            benchmarkAlgorithm(
                                writer, algoType, Dimension.TWO_D,
                                size, dist, run, points2D, reference2D,
                                completedConfigurations, totalConfigurations
                            );
                        }
                        

                        for (int run = 1; run <= NUM_RUNS; run++) {
                            benchmarkAlgorithm(
                                writer, algoType, Dimension.THREE_D,
                                size, dist, run, points3D, reference3D,
                                completedConfigurations, totalConfigurations
                            );
                        }
                    }

                    double referenceDiameter2D = -1;
                    double referenceDiameter3D = -1;
                    
                    if (size <= 10000) {

                        PointCloudAlgorithm<Point2D> refDiamAlgo2D = AlgorithmFactory.createAlgorithm(
                            AlgorithmType.DIAMETER_NAIVE, Dimension.TWO_D
                        );
                        referenceDiameter2D = refDiamAlgo2D.execute(points2D).getDistance();
                        
                        PointCloudAlgorithm<Point3D> refDiamAlgo3D = AlgorithmFactory.createAlgorithm(
                            AlgorithmType.DIAMETER_NAIVE, Dimension.THREE_D
                        );
                        referenceDiameter3D = refDiamAlgo3D.execute(points3D).getDistance();
                    } else {

                        PointCloudAlgorithm<Point2D> refDiamAlgo2D = AlgorithmFactory.createAlgorithm(
                            AlgorithmType.DIAMETER_QUICKHULL, Dimension.TWO_D
                        );
                        referenceDiameter2D = refDiamAlgo2D.execute(points2D).getDistance();
                        
                        PointCloudAlgorithm<Point3D> refDiamAlgo3D = AlgorithmFactory.createAlgorithm(
                            AlgorithmType.DIAMETER_QUICKHULL, Dimension.THREE_D
                        );
                        referenceDiameter3D = refDiamAlgo3D.execute(points3D).getDistance();
                    }
                    

                    for (AlgorithmType algoType : DIAMETER_ALGORITHMS) {

                        if (skipNaiveForLargeSize && algoType == AlgorithmType.DIAMETER_NAIVE) {
                            System.out.println("    Skipping " + algoType.name() + " for large point set");
                            continue;
                        }
                        
                        System.out.println("    Testing " + algoType.name() + "...");
                        

                        for (int run = 1; run <= NUM_RUNS; run++) {
                            benchmarkAlgorithm(
                                writer, algoType, Dimension.TWO_D,
                                size, dist, run, points2D, referenceDiameter2D,
                                completedConfigurations, totalConfigurations
                            );
                        }
                        

                        for (int run = 1; run <= NUM_RUNS; run++) {
                            benchmarkAlgorithm(
                                writer, algoType, Dimension.THREE_D,
                                size, dist, run, points3D, referenceDiameter3D,
                                completedConfigurations, totalConfigurations
                            );
                        }
                    }
                }
            }
            
            System.out.println("\nBenchmark complete! Results saved to: " + outputFile);
            
        } catch (IOException e) {
            System.err.println("Error writing to CSV file: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error during benchmark: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static <T> void benchmarkAlgorithm(
            FileWriter writer,
            AlgorithmType algoType,
            Dimension dimension,
            int size,
            PointGenerator.Distribution distribution,
            int run,
            List<T> points,
            double referenceResult,
            AtomicInteger completedConfigs,
            AtomicInteger totalConfigs) throws IOException {
        

        PointCloudAlgorithm<T> algorithm = AlgorithmFactory.createAlgorithm(algoType, dimension);
        

        System.gc();

        long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        AlgorithmResult<T> result = algorithm.execute(points);

        long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double memoryUsageMB = (afterMemory - beforeMemory) / (1024.0 * 1024.0);
        if (memoryUsageMB < 0) memoryUsageMB = 0;

        final double EPSILON = 1e-6;
        boolean isCorrect = Math.abs(result.getDistance() - referenceResult) < EPSILON;

        writer.write(String.format(
            "%s,%s,%d,%s,%d,%d,%.2f,%b\n",
            algoType.name(),
            dimension.name(),
            size,
            distribution.name(),
            run,
            result.getExecutionTimeMs(),
            memoryUsageMB,
            isCorrect
        ));

        writer.flush();

        int completed = completedConfigs.incrementAndGet();
        int total = totalConfigs.get();
        int percent = (int)((completed * 100.0) / total);
        
        if (completed % 10 == 0 || completed == total) {
            System.out.printf("    Progress: %d/%d (%d%%)%n", completed, total, percent);
        }
    }
}