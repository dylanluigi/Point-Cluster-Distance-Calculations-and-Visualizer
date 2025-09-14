package model.algorithm.closest;

import model.Point2D;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.kdtree.KDTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementació de l'algoritme del parell més proper utilitzant un arbre KD per a punts 2D.
 * <p>
 * Aquesta implementació utilitza una estructura d'arbre KD per optimitzar la cerca
 * del parell més proper. Té una complexitat temporal mitjana de O(n log n) per a la
 * construcció de l'arbre i O(n log n) per a la cerca del parell més proper.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class ClosestPairKDTree2D extends ClosestPairBase2D {

    /**
     * Tipus d'algoritme específic per a aquesta implementació.
     */
    private static final AlgorithmType ALGORITHM_TYPE = AlgorithmType.CLOSEST_PAIR_KDTREE;

    /**
     * Executa l'algoritme del parell més proper utilitzant un arbre KD.
     * <p>
     * L'estratègia consisteix en:
     * <ol>
     *   <li>Construir un arbre KD amb tots els punts</li>
     *   <li>Per cada punt, cercar el seu veí més proper utilitzant l'arbre KD</li>
     *   <li>Seleccionar el parell amb la distància mínima</li>
     * </ol>
     * </p>
     * 
     * @param points la llista de punts 2D a processar
     * @return el resultat amb els dos punts més propers i la seva distància
     */
    @Override
    public AlgorithmResult<Point2D> execute(List<Point2D> points) {
        long startTime = System.nanoTime();
        
        if (points.size() < 2) {
            throw new IllegalArgumentException("Es necessiten almenys 2 punts per trobar el parell més proper");
        }
        
        // Construïm l'arbre KD només una vegada amb tots els punts
        KDTree<Point2D> kdTree = KDTree.create2DTree(new ArrayList<>(points));
        
        // Inicialitzem les variables per al resultat
        double minDistance = Double.POSITIVE_INFINITY;
        Point2D closestP1 = null;
        Point2D closestP2 = null;
        
        // Enfocament eficient: per a cada punt, busquem els seus 2 veïns més propers
        // (un d'ells sempre serà el mateix punt)
        for (Point2D point : points) {
            // Demanem els 2 punts més propers 
            List<Point2D> neighbors = kdTree.findKNearest(point, 2);
            
            // Comprovem que hi hagi prou veïns (sempre hauria d'haver-n'hi almenys 1, el mateix punt)
            if (neighbors.size() > 1) {
                // El segon punt més proper no és el mateix punt
                Point2D nearest = neighbors.get(1);
                
                // Calculem la distància
                double distance = distance(point, nearest);
                
                // Evitem duplicació de parells (si ja hem considerat B->A, no cal considerar A->B)
                // I ho fem verificant si encara no hem trobat cap parell, o si aquest parell és més proper
                if (closestP1 == null || distance < minDistance) {
                    minDistance = distance;
                    closestP1 = point;
                    closestP2 = nearest;
                }
            }
        }
        
        // Calculem el temps d'execució en mil·lisegons
        long executionTime = (System.nanoTime() - startTime) / 1_000_000;
        
        return new AlgorithmResult<>(closestP1, closestP2, minDistance, ALGORITHM_TYPE, executionTime);
    }

    @Override
    public AlgorithmType getType() {
        return ALGORITHM_TYPE;
    }
}