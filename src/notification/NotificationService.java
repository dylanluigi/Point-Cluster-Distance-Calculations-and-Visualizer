package notification;

import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;

/**
 * Interfície de servei per gestionar notificacions sobre l'execució i l'estat dels algorismes.
 */
public interface NotificationService {

    /**
     * Notifica que s'ha iniciat la generació de punts
     * @param numPoints Nombre de punts que s'estan generant
     * @param distribution Nom de la distribució
     */
    void notifyPointGenerationStarted(int numPoints, String distribution);

    /**
     * Notifica que s'ha completat la generació de punts
     */
    void notifyPointGenerationCompleted();

    /**
     * Notifica que s'ha iniciat l'execució de l'algorisme
     * @param type Algorisme que s'està executant
     * @param numPoints Nombre de punts que s'estan processant
     */
    void notifyAlgorithmStarted(AlgorithmType type, int numPoints);

    /**
     * Notifica el progrés de l'algorisme
     * @param type Algorisme que s'està executant
     * @param progress Percentatge de progrés (0.0 - 1.0)
     */
    void notifyAlgorithmProgress(AlgorithmType type, double progress);

    /**
     * Notifica que s'ha completat l'execució de l'algorisme
     * @param result Resultat de l'algorisme
     */
    void notifyAlgorithmCompleted(AlgorithmResult<?> result);

    /**
     * Notifica que s'ha produït un error durant el càlcul
     * @param errorMessage Missatge d'error
     */
    void notifyComputationError(String errorMessage);
}
