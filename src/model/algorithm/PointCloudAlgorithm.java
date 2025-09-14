package model.algorithm;

import java.util.List;

/**
 * Interfície per a tots els algoritmes de núvol de punts que operen sobre col·leccions de punts.
 * <p>
 * Aquesta interfície defineix el contracte que han de complir tots els algoritmes implementats
 * per al processament de núvols de punts, tant en 2D com en 3D. Utilitza genèrics per permetre
 * diferents tipus de punts.
 * </p>
 * 
 * @param <T> El tipus de punts (Point2D o Point3D)
 * @author Point Cloud Analyzer
 * @version 1.0
 */
public interface PointCloudAlgorithm<T> {
    
    /**
     * Executa l'algoritme sobre la col·lecció de punts donada.
     * 
     * @param points Els punts a processar
     * @return El resultat de l'algoritme
     */
    AlgorithmResult<T> execute(List<T> points);
    
    /**
     * Obté el tipus d'aquest algoritme.
     * 
     * @return El tipus d'algoritme
     */
    AlgorithmType getType();
    
    /**
     * Obté la dimensió en què opera aquest algoritme.
     * 
     * @return La dimensió (2D o 3D)
     */
    Dimension getDimension();
}
