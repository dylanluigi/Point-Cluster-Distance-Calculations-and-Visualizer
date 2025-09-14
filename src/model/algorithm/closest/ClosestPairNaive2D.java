package model.algorithm.closest;

import model.Point2D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;

import java.util.List;

/**
 * Implementació naïf O(n²) de l'algoritme del parell més proper per a punts 2D.
 * <p>
 * Aquesta implementació utilitza l'enfocament de força bruta, comparant tots els 
 * parells possibles de punts per trobar els dos més propers. Té una complexitat
 * temporal de O(n²) i és adequada per a conjunts petits de punts.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class ClosestPairNaive2D extends ClosestPairBase2D {

    /**
     * Executa l'algoritme del parell més proper mitjançant un enfocament de força bruta.
     * 
     * @param points la llista de punts 2D a processar
     * @return el resultat amb el parell de punts més propers i la seva distància
     */
    @Override
    public AlgorithmResult<Point2D> execute(List<Point2D> points) {
        long startTime = System.nanoTime();  // Registra el temps d'inici
        int n = points.size();
        Point2D[] pointsArray = points.toArray(new Point2D[0]);
        
        // Utilitza el mètode de força bruta definit a la classe base
        return bruteForce(pointsArray, 0, n - 1, startTime);
    }

    /**
     * Retorna el tipus d'aquest algoritme.
     * 
     * @return el tipus d'algoritme (CLOSEST_PAIR_NAIVE)
     */
    @Override
    public AlgorithmType getType() {
        return AlgorithmType.CLOSEST_PAIR_NAIVE;
    }
}
