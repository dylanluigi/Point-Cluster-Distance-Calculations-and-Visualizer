package view;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.Dimension;
import model.prediction.ExecutionTimePredictor;

import java.util.ArrayList;
import java.util.List;

/**
 * Finestra de visualització per a la predicció del temps d'execució.
 * <p>
 * Aquesta vista mostra un gràfic amb les prediccions de temps d'execució
 * per a diferents algoritmes i permet a l'usuari estimar el temps per a
 * diferents tamanys de dades.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class PredictionView {
    
    private final Stage stage;
    private final ExecutionTimePredictor predictor;
    private final List<AlgorithmResult<?>> historicalResults;
    
    private LineChart<Number, Number> chart;
    private ComboBox<AlgorithmType> algorithmCombo;
    private ComboBox<Dimension> dimensionCombo;
    private TextField pointsField;
    private Label predictionResultLabel;
    
    /**
     * Constructor per a la finestra de predicció.
     * 
     * @param predictor el predictor de temps d'execució a utilitzar
     * @param historicalResults resultats històrics per calibrar el model
     */
    public PredictionView(ExecutionTimePredictor predictor, List<AlgorithmResult<?>> historicalResults) {
        this.predictor = predictor;
        this.historicalResults = historicalResults != null ? historicalResults : new ArrayList<>();
        this.stage = new Stage();
        
        setupUI();
        stage.setTitle("Predicció del Temps d'Execució");
        stage.setWidth(800);
        stage.setHeight(600);
    }
    
    /**
     * Configura la interfície d'usuari de la finestra.
     */
    private void setupUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        GridPane controls = new GridPane();
        controls.setHgap(10);
        controls.setVgap(5);
        controls.setPadding(new Insets(0, 0, 10, 0));

        Label algorithmLabel = new Label("Algoritme:");
        algorithmCombo = new ComboBox<>();
        algorithmCombo.getItems().addAll(AlgorithmType.values());
        if (!algorithmCombo.getItems().isEmpty()) {
            algorithmCombo.setValue(algorithmCombo.getItems().get(0));
        }

        Label dimensionLabel = new Label("Dimensió:");
        dimensionCombo = new ComboBox<>();
        dimensionCombo.getItems().addAll(Dimension.values());
        if (!dimensionCombo.getItems().isEmpty()) {
            dimensionCombo.setValue(dimensionCombo.getItems().get(0));
        }

        Label pointsLabel = new Label("Nombre de punts:");
        pointsField = new TextField("10000");

        Button predictButton = new Button("Predir Temps");
        predictButton.setOnAction(e -> updatePrediction());

        predictionResultLabel = new Label("Temps predit: (pendent)");

        controls.add(algorithmLabel, 0, 0);
        controls.add(algorithmCombo, 1, 0);
        controls.add(dimensionLabel, 2, 0);
        controls.add(dimensionCombo, 3, 0);
        controls.add(pointsLabel, 0, 1);
        controls.add(pointsField, 1, 1);
        controls.add(predictButton, 2, 1);

        HBox resultBox = new HBox(10, predictionResultLabel);
        resultBox.setPadding(new Insets(5, 0, 10, 0));

        setupChart();

        Button generateCurveButton = new Button("Generar Corba de Predicció");
        generateCurveButton.setOnAction(e -> generateCurve());

        VBox topSection = new VBox(10, controls, resultBox, generateCurveButton);
        root.setTop(topSection);
        root.setCenter(chart);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }
    
    /**
     * Configura el gràfic de línia per visualitzar les prediccions.
     */
    private void setupChart() {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Nombre de Punts");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Temps d'Execució (ms)");

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Predicció del Temps d'Execució");
        chart.setAnimated(false);
    }
    
    /**
     * Actualitza la predicció basant-se en les seleccions de l'usuari.
     */
    private void updatePrediction() {
        try {
            AlgorithmType algorithm = algorithmCombo.getValue();
            int numPoints = Integer.parseInt(pointsField.getText().trim());
            

            double predictedTime = predictor.predictExecutionTime(algorithm, numPoints);
            

            String complexityModel = predictor.getModelType(algorithm).toString();
            predictionResultLabel.setText(String.format(
                    "Temps predit per a %d punts amb %s: %.2f ms (Model: %s)",
                    numPoints, algorithm.getDisplayName(), predictedTime, complexityModel
            ));
            

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("Predicció per a " + algorithm.getDisplayName());
            series.getData().add(new XYChart.Data<>(numPoints, predictedTime));
            
            chart.getData().clear();
            chart.getData().add(series);
            
        } catch (NumberFormatException e) {
            predictionResultLabel.setText("Error: Introdueix un nombre vàlid de punts");
        } catch (Exception e) {
            predictionResultLabel.setText("Error: " + e.getMessage());
        }
    }
    
    /**
     * Genera una corba completa de predicció per a diferents tamanys de dades.
     */
    private void generateCurve() {
        try {
            AlgorithmType algorithm = algorithmCombo.getValue();

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("Corba de predicció per a " + algorithm.getDisplayName());

            int maxPoints = Integer.parseInt(pointsField.getText().trim());
            int step = Math.max(1, maxPoints / 50);
            
            for (int n = step; n <= maxPoints; n += step) {
                double predictedTime = predictor.predictExecutionTime(algorithm, n);
                series.getData().add(new XYChart.Data<>(n, predictedTime));
            }

            chart.getData().clear();
            chart.getData().add(series);
            
            // També mostrem el punt final
            double finalPrediction = predictor.predictExecutionTime(algorithm, maxPoints);
            predictionResultLabel.setText(String.format(
                    "Temps predit per a %d punts amb %s: %.2f ms",
                    maxPoints, algorithm.getDisplayName(), finalPrediction
            ));
            
        } catch (NumberFormatException e) {
            predictionResultLabel.setText("Error: Introdueix un nombre vàlid de punts");
        } catch (Exception e) {
            predictionResultLabel.setText("Error: " + e.getMessage());
        }
    }
    
    /**
     * Mostra la finestra de predicció.
     */
    public void show() {
        stage.show();
    }
}