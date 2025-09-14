package model.algorithm.diameter;

import model.algorithm.PointCloudAlgorithm;

/**
 * Interfície per a tots els algoritmes de càlcul del diàmetre.
 * <p>
 * Aquesta interfície serveix com a marcador per a tots els algoritmes que 
 * implementen el càlcul del diàmetre (la distància màxima entre dos punts) en un conjunt de punts.
 * Estén la interfície PointCloudAlgorithm i pot ser ampliada amb mètodes 
 * específics per a la cerca del diàmetre en el futur.
 * </p>
 * 
 * @param <T> El tipus de punts (Point2D o Point3D)
 * @author Dylan Canning
 * @version 1.0
 */
public interface DiameterAlgorithmStrategy<T> extends PointCloudAlgorithm<T> {

}
