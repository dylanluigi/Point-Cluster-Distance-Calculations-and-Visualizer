package model.algorithm.closest;

import model.Point3D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;

import java.util.List;

/**
 * Implementació naïf O(n²) de l'algoritme del parell més proper per a punts 3D.
 * <p>
 * Aquesta implementació utilitza l'enfocament de força bruta, comparant tots els 
 * parells possibles de punts en l'espai tridimensional per trobar els dos més propers.
 * Té una complexitat temporal de O(n²) i és adequada per a conjunts petits de punts.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class ClosestPairNaive3D extends ClosestPairBase3D {

    /**
     * Executa l'algoritme del parell més proper mitjançant un enfocament de força bruta en 3D.
     * 
     * @param points la llista de punts 3D a processar
     * @return el resultat amb el parell de punts més propers i la seva distància
     */
    @Override
    public AlgorithmResult<Point3D> execute(List<Point3D> points) {
        long startTime = System.nanoTime();  // Registra el temps d'inici
        int n = points.size();
        Point3D[] pointsArray = points.toArray(new Point3D[0]);
        
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
