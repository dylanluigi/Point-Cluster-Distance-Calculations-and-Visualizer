package notification;

import javafx.application.Platform;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;

/**
 * Implementació de NotificationService per a la interacció amb la interfície gràfica.
 */
public class NotificationServiceImpl implements NotificationService {
    private final UINotificationHandler uiHandler;

    /**
     * Interfície per gestionar notificacions a la interfície d'usuari
     */
    public interface UINotificationHandler {
        void onPointGenerationStarted(int numPoints, String distribution);
        void onPointGenerationCompleted();
        void onAlgorithmStarted(AlgorithmType type, int numPoints);
        void onAlgorithmProgress(AlgorithmType type, double progress);
        void onAlgorithmCompleted(AlgorithmResult<?> result);
        void onComputationError(String errorMessage);
    }

    public NotificationServiceImpl(UINotificationHandler uiHandler) {
        this.uiHandler = uiHandler;
    }

    @Override
    public void notifyPointGenerationStarted(int numPoints, String distribution) {
        Platform.runLater(() -> uiHandler.onPointGenerationStarted(numPoints, distribution));
    }

    @Override
    public void notifyPointGenerationCompleted() {
        Platform.runLater(() -> uiHandler.onPointGenerationCompleted());
    }

    @Override
    public void notifyAlgorithmStarted(AlgorithmType type, int numPoints) {
        Platform.runLater(() -> uiHandler.onAlgorithmStarted(type, numPoints));
    }

    @Override
    public void notifyAlgorithmProgress(AlgorithmType type, double progress) {
        Platform.runLater(() -> uiHandler.onAlgorithmProgress(type, progress));
    }

    @Override
    public void notifyAlgorithmCompleted(AlgorithmResult<?> result) {
        Platform.runLater(() -> uiHandler.onAlgorithmCompleted(result));
    }

    @Override
    public void notifyComputationError(String errorMessage) {
        Platform.runLater(() -> uiHandler.onComputationError(errorMessage));
    }
}
