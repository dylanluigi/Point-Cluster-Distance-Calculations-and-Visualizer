package model.algorithm;

import model.Point2D;
import model.Point3D;
import model.algorithm.closest.*;
import model.algorithm.diameter.*;

/**
 * Fabrica per a crear implementacions d'algoritmes segons el tipus i la dimensió.
 * <p>
 * Aquesta classe implementa el patró de disseny Factory Method per crear les instàncies
 * apropiades d'algoritmes de núvol de punts sense exposar la lògica de creació al client.
 * Permet seleccionar dinàmicament l'algoritme correcte basant-se en el tipus i la dimensió.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class AlgorithmFactory {
    
    /**
     * Crea la implementació d'algoritme apropiada segons el tipus i la dimensió.
     * <p>
     * Aquest mètode utilitza una tècnica de dispatching per seleccionar i instanciar
     * la classe concreta d'algoritme que correspon a la combinació de tipus i dimensió.
     * Utilitza generics per proporcionar tipat segur per als diferents tipus de punts.
     * </p>
     * 
     * @param <T> El tipus de punt (Point2D o Point3D) que gestionarà l'algoritme
     * @param type El tipus d'algoritme a crear (CLOSEST_PAIR, DIAMETER, etc.)
     * @param dimension La dimensió en què ha d'operar l'algoritme (2D o 3D)
     * @return La implementació d'algoritme apropiada
     * @throws IllegalArgumentException si la combinació no és suportada
     */
    @SuppressWarnings("unchecked")
    public static <T> PointCloudAlgorithm<T> createAlgorithm(AlgorithmType type, Dimension dimension) {
        if (dimension == Dimension.TWO_D) {
            return (PointCloudAlgorithm<T>) createAlgorithm2D(type);
        } else if (dimension == Dimension.THREE_D) {
            return (PointCloudAlgorithm<T>) createAlgorithm3D(type);
        } else {
            throw new IllegalArgumentException("Unsupported dimension: " + dimension);
        }
    }
    
    /**
     * Crea la implementació d'algoritme apropiada per a punts 2D.
     * <p>
     * Selecciona la implementació concreta basant-se en el tipus d'algoritme sol·licitat.
     * </p>
     * 
     * @param type el tipus d'algoritme a crear
     * @return la implementació d'algoritme 2D corresponent
     */
    private static PointCloudAlgorithm<Point2D> createAlgorithm2D(AlgorithmType type) {
        return switch (type) {
            case CLOSEST_PAIR_NAIVE -> new ClosestPairNaive2D();
            case CLOSEST_PAIR_EFFICIENT -> new ClosestPairEfficient2D();
            case CLOSEST_PAIR_KDTREE -> new ClosestPairKDTree2D();
            case CLOSEST_PAIR_ADAPTIVE -> ClosestPairAdaptive.createAdaptiveStrategy(Dimension.TWO_D);
            case DIAMETER_NAIVE -> new DiameterNaive2D();
            case DIAMETER_CONCURRENT -> new DiameterConcurrent2D();
            case DIAMETER_QUICKHULL -> new DiameterQuickHull2D();
        };
    }
    
    /**
     * Crea la implementació d'algoritme apropiada per a punts 3D.
     * <p>
     * Selecciona la implementació concreta basant-se en el tipus d'algoritme sol·licitat.
     * </p>
     * 
     * @param type el tipus d'algoritme a crear
     * @return la implementació d'algoritme 3D corresponent
     */
    private static PointCloudAlgorithm<Point3D> createAlgorithm3D(AlgorithmType type) {
        return switch (type) {
            case CLOSEST_PAIR_NAIVE -> new ClosestPairNaive3D();
            case CLOSEST_PAIR_EFFICIENT -> new ClosestPairEfficient3D();
            case CLOSEST_PAIR_KDTREE -> new ClosestPairKDTree3D();
            case CLOSEST_PAIR_ADAPTIVE -> ClosestPairAdaptive.createAdaptiveStrategy(Dimension.THREE_D);
            case DIAMETER_NAIVE -> new DiameterNaive3D();
            case DIAMETER_CONCURRENT -> new DiameterConcurrent3D();
            case DIAMETER_QUICKHULL -> new DiameterQuickHull3D();
        };
    }
}