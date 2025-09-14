package model.algorithm.diameter;

import model.Point2D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.Dimension;

import java.util.List;

/**
 * Classe base per a algoritmes de càlcul del diàmetre en 2D.
 */
public abstract class DiameterBase2D implements DiameterAlgorithmStrategy<Point2D> {

    @Override
    public Dimension getDimension() {
        return Dimension.TWO_D;
    }

    /**
     * Calcula la distància entre dos punts en 2D.
     */
    protected double distance(Point2D p, Point2D q) {
        double dx = p.x() - q.x();
        double dy = p.y() - q.y();
        return Math.hypot(dx, dy);
    }
}
