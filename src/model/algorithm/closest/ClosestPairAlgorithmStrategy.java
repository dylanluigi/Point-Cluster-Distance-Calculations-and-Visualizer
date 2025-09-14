package model.algorithm.closest;

import model.algorithm.PointCloudAlgorithm;

/**
 * Interfície per a tots els algoritmes de cerca del parell més proper.
 * <p>
 * Aquesta interfície serveix com a marcador per a tots els algoritmes que 
 * implementen el càlcul del parell més proper entre un conjunt de punts.
 * Estén la interfície PointCloudAlgorithm i pot ser ampliada amb mètodes 
 * específics per a la cerca de parells més propers en el futur.
 * </p>
 * 
 * @param <T> El tipus de punts (Point2D o Point3D)
 * @author Dylan Canning
 * @version 1.0
 */
public interface ClosestPairAlgorithmStrategy<T> extends PointCloudAlgorithm<T> {

}
