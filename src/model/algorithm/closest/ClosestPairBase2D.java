package model.algorithm.closest;

import model.Point2D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.Dimension;

import java.util.List;

/**
 * Classe base per a totes les implementacions d'algoritmes de parell més proper en 2D.
 * <p>
 * Proporciona funcionalitat comuna per a tots els algoritmes que calculen el parell més proper
 * entre punts bidimensionals, incloent el càlcul de distàncies i un mètode de força bruta per a
 * conjunts petits de punts.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public abstract class ClosestPairBase2D implements ClosestPairAlgorithmStrategy<Point2D> {

    /**
     * Retorna la dimensió en què opera aquest algoritme (sempre 2D).
     * 
     * @return la dimensió bidimensional
     */
    @Override
    public Dimension getDimension() {
        return Dimension.TWO_D;
    }

    /**
     * Calcula la distància euclidiana entre dos punts 2D.
     * <p>
     * Utilitza la fórmula d'hipotenusa: sqrt(dx² + dy²).
     * </p>
     * 
     * @param p el primer punt
     * @param q el segon punt
     * @return la distància euclidiana entre els punts
     */
    protected double distance(Point2D p, Point2D q) {
        double dx = p.x() - q.x();
        double dy = p.y() - q.y();
        return Math.hypot(dx, dy);
    }
    
    /**
     * Algoritme de força bruta per trobar el parell més proper entre un conjunt petit de punts.
     * <p>
     * Aquest mètode compara totes les parelles possibles de punts en el rang especificat.
     * Té una complexitat temporal de O((right-left)²), per això només s'utilitza per a
     * petits subconjunts de punts o com a cas base en algoritmes de dividir i conquerir.
     * </p>
     * 
     * @param points    l'array de punts a analitzar
     * @param left      l'índex del primer punt a considerar
     * @param right     l'índex de l'últim punt a considerar
     * @param startTime el temps d'inici de l'execució en nanosegons
     * @return un objecte AlgorithmResult amb el parell més proper trobat
     */
    protected AlgorithmResult<Point2D> bruteForce(Point2D[] points, int left, int right, long startTime) {
        double minDist = Double.POSITIVE_INFINITY;
        Point2D p1 = null, p2 = null;
        
        for (int i = left; i <= right; i++) {
            for (int j = i + 1; j <= right; j++) {
                double d = distance(points[i], points[j]);
                if (d < minDist) {
                    minDist = d;
                    p1 = points[i];
                    p2 = points[j];
                }
            }
        }
        
        long executionTime = System.nanoTime() - startTime;
        return new AlgorithmResult<>(p1, p2, minDist, getType(), executionTime / 1_000_000);
    }
}
