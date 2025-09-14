package model.algorithm.diameter;

import model.Point3D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.Dimension;

import java.util.List;

/**
 * Classe base per a algoritmes de càlcul del diàmetre en 3D.
 */
public abstract class DiameterBase3D implements DiameterAlgorithmStrategy<Point3D> {

    @Override
    public Dimension getDimension() {
        return Dimension.THREE_D;
    }

    /**
     * Calcula la distància entre dos punts en 3D.
     */
    protected double distance(Point3D p, Point3D q) {
        double dx = p.x() - q.x();
        double dy = p.y() - q.y();
        double dz = p.z() - q.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
