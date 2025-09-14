package model.algorithm.diameter;

import model.Point2D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;

import java.util.List;

/**
 * Implementació naïf O(n²) de l'algoritme del diàmetre per a punts 2D.
 * <p>
 * Aquesta implementació utilitza un enfocament de força bruta, calculant la distància
 * entre tots els parells possibles de punts i retornant el parell amb la màxima distància.
 * Té una complexitat temporal de O(n²).
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class DiameterNaive2D extends DiameterBase2D {

    /**
     * Executa l'algoritme del diàmetre mitjançant un enfocament de força bruta en 2D.
     * 
     * @param points la llista de punts 2D a processar
     * @return el resultat amb el parell de punts més llunyans i la seva distància
     */
    @Override
    public AlgorithmResult<Point2D> execute(List<Point2D> points) {
        long startTime = System.nanoTime();  // Registra el temps d'inici
        double maxDist = 0.0;
        Point2D p1 = null, p2 = null;
        
        // Compara totes les parelles possibles de punts
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                // Calcula la distància entre els punts i i j
                double dist = distance(points.get(i), points.get(j));
                
                // Actualitza el màxim si trobem una distància més gran
                if (dist > maxDist) {
                    maxDist = dist;
                    p1 = points.get(i);
                    p2 = points.get(j);
                }
            }
        }
        
        long executionTime = System.nanoTime() - startTime;
        return new AlgorithmResult<>(p1, p2, maxDist, getType(), executionTime / 1_000_000);
    }

    /**
     * Retorna el tipus d'aquest algoritme.
     * 
     * @return el tipus d'algoritme (DIAMETER_NAIVE)
     */
    @Override
    public AlgorithmType getType() {
        return AlgorithmType.DIAMETER_NAIVE;
    }
}
