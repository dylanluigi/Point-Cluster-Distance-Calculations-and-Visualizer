package controller;

import javafx.concurrent.Task;
import model.Point2D;
import model.Point3D;
import model.PointGenerator;
import model.algorithm.AlgorithmFactory;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.Dimension;
import model.algorithm.PointCloudAlgorithm;
import model.prediction.ExecutionTimePredictor;
import notification.NotificationService;
import view.PredictionView;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador responsable de coordinar l'execució dels algoritmes i les interaccions de l'usuari.
 * <p>
 * Aquesta classe segueix el patró MVC i actua com a intermediari entre la vista 
 * i les classes del model, gestionant l'execució asíncrona dels algoritmes.
 * </p>
 * 
 * @author Point Cloud Analyzer
 * @version 1.0
 */
public class MainController {
    private final NotificationService notificationService;
    private final List<AlgorithmResult<?>> algorithmResults = new ArrayList<>();
    private final ExecutionTimePredictor executionTimePredictor = new ExecutionTimePredictor();

    /**
     * Constructor que injecta el servei de notificacions.
     * 
     * @param notificationService el servei encarregat de comunicar els canvis d'estat a la UI
     */
    public MainController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Gestiona l'execució d'un algoritme de núvol de punts basat en l'entrada de l'usuari.
     * Executa l'algoritme en un fil separat per no bloquejar la UI.
     * 
     * @param dimensionStr    La dimensió on operar (2D o 3D) en format String
     * @param distributionStr La distribució a utilitzar per a la generació de punts en format String
     * @param numPoints       El nombre de punts a generar
     * @param bound           El límit per a les coordenades dels punts
     * @param algorithmName   El nom de l'algoritme a executar
     */
    public void runAlgorithm(String dimensionStr, String distributionStr, int numPoints, int bound, String algorithmName) {
        // Converteix les cadenes a tipus enum
        Dimension dimension = Dimension.fromDisplayName(dimensionStr);
        PointGenerator.Distribution distribution = PointGenerator.Distribution.valueOf(distributionStr);
        AlgorithmType algorithmType = AlgorithmType.fromDisplayName(algorithmName);
        
        if (dimension == null || algorithmType == null) {
            notificationService.notifyComputationError("Dimensió o tipus d'algoritme invàlids");
            return;
        }
        
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // Notifica que s'ha iniciat la generació
                    notificationService.notifyPointGenerationStarted(numPoints, distributionStr);
                    
                    // Genera punts en funció de la dimensió
                    if (dimension == Dimension.TWO_D) {
                        List<Point2D> points = PointGenerator.generate2DPoints(numPoints, distribution, bound, bound);
                        
                        // Notifica que s'ha completat la generació de punts
                        notificationService.notifyPointGenerationCompleted();
                        
                        // Executa l'algoritme
                        notificationService.notifyAlgorithmStarted(algorithmType, numPoints);
                        PointCloudAlgorithm<Point2D> algorithm = AlgorithmFactory.createAlgorithm(algorithmType, dimension);
                        AlgorithmResult<Point2D> result = algorithm.execute(points);
                        // Desa el resultat per a anàlisi posterior
                        synchronized (algorithmResults) {
                            algorithmResults.add(result);
                        }
                        notificationService.notifyAlgorithmCompleted(result);
                        
                        return null;
                    } else {
                        List<Point3D> points = PointGenerator.generate3DPoints(numPoints, distribution, bound, bound, bound);
                        
                        // Notifica que s'ha completat la generació de punts
                        notificationService.notifyPointGenerationCompleted();
                        
                        // Executa l'algoritme
                        notificationService.notifyAlgorithmStarted(algorithmType, numPoints);
                        PointCloudAlgorithm<Point3D> algorithm = AlgorithmFactory.createAlgorithm(algorithmType, dimension);
                        AlgorithmResult<Point3D> result = algorithm.execute(points);
                        // Desa el resultat per a anàlisi posterior
                        synchronized (algorithmResults) {
                            algorithmResults.add(result);
                        }
                        notificationService.notifyAlgorithmCompleted(result);
                        
                        return null;
                    }
                } catch (Exception e) {
                    notificationService.notifyComputationError(e.getMessage());
                }
                return null;
            }
        };

        // Executa la tasca en un fil secundari
        new Thread(task).start();
    }
    
    /**
     * Obté punts per a la visualització en 2D.
     * 
     * @param distribution la distribució estadística a utilitzar
     * @param numPoints el nombre de punts a generar
     * @param bound el límit per a les coordenades
     * @return una llista de punts 2D
     */
    public List<Point2D> generatePointsFor2DVisualization(PointGenerator.Distribution distribution, int numPoints, int bound) {
        return PointGenerator.generate2DPoints(numPoints, distribution, bound, bound);
    }
    
    /**
     * Obté punts per a la visualització en 3D.
     * 
     * @param distribution la distribució estadística a utilitzar
     * @param numPoints el nombre de punts a generar
     * @param bound el límit per a les coordenades
     * @return una llista de punts 3D
     */
    public List<Point3D> generatePointsFor3DVisualization(PointGenerator.Distribution distribution, int numPoints, int bound) {
        return PointGenerator.generate3DPoints(numPoints, distribution, bound, bound, bound);
    }
    
    /**
     * Obre la finestra de predicció del temps d'execució.
     * <p>
     * Aquesta finestra permet a l'usuari visualitzar i calibrar models de predicció
     * per als diferents algoritmes implementats.
     * </p>
     */
    public void openPredictionView() {
        List<AlgorithmResult<?>> resultsCopy;
        synchronized (algorithmResults) {
            resultsCopy = new ArrayList<>(algorithmResults);
        }
        PredictionView predictionView = new PredictionView(executionTimePredictor, resultsCopy);
        predictionView.show();
    }
    
    /**
     * Prediu el temps d'execució per a un algoritme específic.
     * 
     * @param algorithmType tipus d'algoritme
     * @param numPoints nombre de punts
     * @return temps d'execució previst en mil·lisegons
     */
    public double predictExecutionTime(AlgorithmType algorithmType, int numPoints) {
        return executionTimePredictor.predictExecutionTime(algorithmType, numPoints);
    }
}