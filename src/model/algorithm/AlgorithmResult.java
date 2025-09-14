package model.algorithm;

import java.util.Objects;

/**
 * Classe genèrica que representa el resultat d'un càlcul d'algoritme.
 * <p>
 * Aquesta classe emmagatzema els resultats d'executar un algoritme, incloent:
 * <ul>
 *   <li>Els punts trobats (els més propers o els més llunyans)</li>
 *   <li>La distància entre aquests punts</li>
 *   <li>El tipus d'algoritme utilitzat</li>
 *   <li>El temps d'execució (en ms)</li>
 * </ul>
 * </p>
 * 
 * @param <T> El tipus de punts (Point2D o Point3D)
 * @author Point Cloud Analyzer
 * @version 1.0
 */
public class AlgorithmResult<T> {
    private final T point1;
    private final T point2;
    private final double distance;
    private final AlgorithmType algorithmType;
    private final long executionTimeMs;

    /**
     * Constructor per emmagatzemar el resultat complet d'un algoritme.
     * 
     * @param point1 El primer punt del resultat
     * @param point2 El segon punt del resultat
     * @param distance La distància entre els punts
     * @param algorithmType El tipus d'algoritme utilitzat
     * @param executionTimeMs El temps d'execució en mil·lisegons
     */
    public AlgorithmResult(T point1, T point2, double distance, AlgorithmType algorithmType, long executionTimeMs) {
        this.point1 = point1;
        this.point2 = point2;
        this.distance = distance;
        this.algorithmType = algorithmType;
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * Obté el primer punt del resultat.
     * 
     * @return El primer punt
     */
    public T getPoint1() {
        return point1;
    }

    /**
     * Obté el segon punt del resultat.
     * 
     * @return El segon punt
     */
    public T getPoint2() {
        return point2;
    }

    /**
     * Obté la distància entre els punts.
     * 
     * @return La distància calculada
     */
    public double getDistance() {
        return distance;
    }
    
    /**
     * Obté el tipus d'algoritme utilitzat.
     * 
     * @return El tipus d'algoritme
     */
    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }
    
    /**
     * Obté el temps d'execució.
     * 
     * @return El temps d'execució en mil·lisegons
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    /**
     * Determina si aquest resultat és d'un algoritme de parell més proper.
     * 
     * @return true si és un algoritme de parell més proper, false altrament
     */
    public boolean isClosestPair() {
        return algorithmType == AlgorithmType.CLOSEST_PAIR_NAIVE || 
               algorithmType == AlgorithmType.CLOSEST_PAIR_EFFICIENT ||
               algorithmType == AlgorithmType.CLOSEST_PAIR_KDTREE ||
               algorithmType == AlgorithmType.CLOSEST_PAIR_ADAPTIVE;
    }
    
    /**
     * Determina si aquest resultat és d'un algoritme de diàmetre.
     * 
     * @return true si és un algoritme de diàmetre, false altrament
     */
    public boolean isDiameter() {
        return !isClosestPair();
    }

    /**
     * Compara aquest resultat amb un altre objecte per igualtat.
     * 
     * @param o L'objecte a comparar
     * @return true si els objectes són iguals, false altrament
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlgorithmResult<?> that = (AlgorithmResult<?>) o;
        return Double.compare(that.distance, distance) == 0 &&
                executionTimeMs == that.executionTimeMs &&
                Objects.equals(point1, that.point1) &&
                Objects.equals(point2, that.point2) &&
                algorithmType == that.algorithmType;
    }

    /**
     * Calcula un codi hash per a aquest resultat.
     * 
     * @return El valor hash calculat
     */
    @Override
    public int hashCode() {
        return Objects.hash(point1, point2, distance, algorithmType, executionTimeMs);
    }
}
