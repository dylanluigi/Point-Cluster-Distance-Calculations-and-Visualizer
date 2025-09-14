package model;

/**
 * Representa un punt en l'espai tridimensional (3D).
 * <p>
 * Aquest registre immutable emmagatzema les coordenades x, y i z d'un punt en l'espai 3D.
 * A diferència de Point2D, aquest registre permet coordenades negatives per permetre més
 * flexibilitat en certes visualitzacions i algoritmes.
 * </p>
 * 
 * @author Point Cloud Analyzer
 * @version 1.0
 */
public record Point3D(double x, double y, double z) {
    /**
     * Retorna una representació en format text del punt.
     * 
     * @return String amb format "Point3D(x, y, z)"
     */
    @Override
    public String toString() {
        return String.format("Point3D(%.2f, %.2f, %.2f)", x, y, z);
    }
}
