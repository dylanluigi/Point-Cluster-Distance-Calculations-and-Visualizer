package model;

/**
 * Representa un punt en l'espai bidimensional (2D).
 * <p>
 * Aquest registre immutable emmagatzema les coordenades x i y d'un punt en l'espai 2D.
 * Les coordenades han de ser no negatives.
 * </p>
 * 
 * @author Point Cloud Analyzer
 * @version 1.0
 */
public record Point2D(double x, double y) {
    /**
     * Constructor compacte que valida les coordenades del punt.
     * 
     * @throws IllegalArgumentException si alguna coordenada és negativa
     */
    public Point2D {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Les coordenades han de ser no negatives");
        }
    }

    /**
     * Retorna una representació en format text del punt.
     * 
     * @return String amb format "Point2D(x, y)"
     */
    @Override
    public String toString() {
        return String.format("Point2D(%.2f, %.2f)", x, y);
    }
}
