package view;

import controller.MainController;
import javafx.animation.RotateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.CullFace;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Point2D;
import model.Point3D;
import model.PointGenerator;
import model.algorithm.AlgorithmResult;
import model.algorithm.AlgorithmType;
import model.algorithm.quickhull.QuickHull2D;
import notification.NotificationServiceImpl.UINotificationHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Component principal de la vista responsable de la presentació de la UI i la interacció amb l'usuari.
 * <p>
 * Aquesta classe implementa la interfície gràfica de l'aplicació incloent:
 * <ul>
 *   <li>Visualització de núvols de punts en 2D i 3D</li>
 *   <li>Controls per seleccionar i executar algoritmes</li>
 *   <li>Visualització de resultats d'algoritmes</li>
 *   <li>Visualització d'envolupants convexes</li>
 *   <li>Interacció amb la visualització mitjançant zoom i rotació</li>
 * </ul>
 * 
 * La classe implementa el patró MVC actuant com a Vista i comunica amb 
 * el Controlador mitjançant el patró Observer.
 * </p>
 * 
 * @author Dylan Canning
 * @version 1.0
 */
public class MainView implements UINotificationHandler {
    private MainController controller;
    private ComboBox<String> dimensionCombo, distributionCombo, algorithmCombo;
    private TextField numPointsField, boundField;
    private Button runButton, focusButton;
    private CheckBox showHullCheckbox;
    private ProgressBar progressBar;
    private TextArea outputArea;
    private BorderPane root;
    private ScrollPane scrollPane;
    private Canvas canvas2D;
    private Group canvasGroup;
    private double scaleFactor = 1.0;

    // 3D components
    private Group world3D;
    private Group points3D;
    private SubScene subScene3D;
    private PerspectiveCamera camera3D;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Translate cameraTranslate = new Translate(0, 0, -500);
    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;

    // Animation control
    private RotateTransition cameraRotation;

    // Point data
    private List<Point2D> latest2DPoints;
    private List<Point3D> latest3DPoints;
    private Point2D highlight2DA, highlight2DB; // 2D algorithm result points
    private Point3D highlight3DA, highlight3DB; // 3D algorithm result points

    // Hull visualization
    private Group hull2DGroup = new Group();
    private Group hull3DGroup = new Group();
    private List<Point2D> hull2DPoints;
    private List<Point3D> hull3DPoints;
    private boolean shouldShowHull = false;
    private AlgorithmType currentAlgorithmType;

    /**
     * Estableix el controlador que gestionarà les accions de l'usuari.
     * <p>
     * Aquest mètode implementa la injecció de dependències per connectar
     * la vista amb el controlador seguint el patró MVC.
     * </p>
     * 
     * @param controller el controlador principal de l'aplicació
     */
    public void setController(MainController controller) {
        this.controller = controller;
    }

    /**
     * Inicialitza la vista amb l'escenari proporcionat.
     * <p>
     * Aquest mètode configura la interfície d'usuari, crea l'escena principal
     * i visualitza la finestra de l'aplicació.
     * </p>
     * 
     * @param primaryStage l'escenari principal de JavaFX per mostrar la interfície
     */
    public void initialize(Stage primaryStage) {
        setupUI(primaryStage);
        primaryStage.setTitle("Analitzador de Núvol de Punts");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.show();
    }

    private void setupUI(Stage stage) {
        root = new BorderPane();

        // Top Controls
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        controls.setAlignment(Pos.CENTER_LEFT);

        dimensionCombo = new ComboBox<>();
        dimensionCombo.getItems().addAll("2D", "3D");
        dimensionCombo.setValue("3D");
        dimensionCombo.setOnAction(e -> updateVisualization());

        distributionCombo = new ComboBox<>();
        distributionCombo.getItems().addAll(
                "UNIFORM", "GAUSSIAN", "EXPONENTIAL", "LOG_NORMAL", "RAYLEIGH",
                "PARETO", "CAUCHY", "WEIBULL", "BETA", "GAMMA", "TRIANGULAR"
        );
        distributionCombo.setValue("UNIFORM");

        algorithmCombo = new ComboBox<>();
        algorithmCombo.getItems().addAll(
                "Parell Més Proper (O(n²))", "Parell Més Proper (O(n log n))", "Parell Més Proper (KD-Tree)",
                "Diàmetre (Naïf)", "Diàmetre (Concurrent)", "Diàmetre (QuickHall)"
        );
        algorithmCombo.setValue("Parell Més Proper (O(n²))");
        algorithmCombo.setOnAction(e -> {
            // Update UI for QuickHull availability
            String selectedAlgo = algorithmCombo.getValue();
            boolean isQuickHull = selectedAlgo.contains("QuickHall");
            showHullCheckbox.setDisable(!isQuickHull);
        });

        numPointsField = new TextField("5000");
        boundField = new TextField("1000");

        runButton = new Button("Executar");
        runButton.setOnAction(e -> {
            int numPoints = Integer.parseInt(numPointsField.getText().trim());
            int bound = Integer.parseInt(boundField.getText().trim());
            currentAlgorithmType = AlgorithmType.fromDisplayName(algorithmCombo.getValue());

            // Call the controller's method to run the algorithm
            controller.runAlgorithm(
                    dimensionCombo.getValue(),
                    distributionCombo.getValue(),
                    numPoints,
                    bound,
                    algorithmCombo.getValue()
            );
        });

        // Focus button
        focusButton = new Button("Enfocar Resultat");
        focusButton.setOnAction(e -> {
            if (dimensionCombo.getValue().equals("2D")) {
                if (highlight2DA != null && highlight2DB != null) {
                    centerBetweenPoints(highlight2DA, highlight2DB);
                }
            } else {
                focusOn3DPoints();
            }
        });
        focusButton.setDisable(true);

        // QuickHull visualization checkbox
        showHullCheckbox = new CheckBox("Mostrar Envolupant Convexa");
        showHullCheckbox.setDisable(true);  // Initially disabled
        showHullCheckbox.setSelected(false);
        showHullCheckbox.setOnAction(e -> {
            shouldShowHull = showHullCheckbox.isSelected();
            updateHullVisualization();
        });
        
        // Botó per obrir la finestra de predicció
        Button predictionButton = new Button("Predicció de Temps");
        predictionButton.setOnAction(e -> {
            controller.openPredictionView();
        });

        controls.getChildren().addAll(
                new Label("Dimensió:"), dimensionCombo,
                new Label("Distribució:"), distributionCombo,
                new Label("Algoritme:"), algorithmCombo,
                new Label("Punts:"), numPointsField,
                new Label("Límit:"), boundField,
                runButton,
                focusButton,
                showHullCheckbox,
                predictionButton
        );

        // Create split pane for visualization and output
        SplitPane splitPane = new SplitPane();

        // Create the visualization pane (will contain both 2D and 3D)
        BorderPane visualizationPane = new BorderPane();

        // Setup 2D visualization
        canvas2D = new Canvas(1200, 800);
        canvasGroup = new Group(canvas2D);
        scrollPane = new ScrollPane(canvasGroup);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: black;");

        // Setup 3D visualization
        setup3DScene();

        // Add visualization components to pane
        visualizationPane.setCenter(dimensionCombo.getValue().equals("2D") ? scrollPane : subScene3D);

        // Bottom Output
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(150);

        progressBar = new ProgressBar(0);
        VBox outputBox = new VBox(10, new Label("Progrés:"), progressBar, outputArea);
        outputBox.setPadding(new Insets(10));

        // Add components to split pane
        splitPane.getItems().addAll(visualizationPane, outputBox);
        splitPane.setDividerPositions(0.7);
        SplitPane.setResizableWithParent(outputBox, false);

        // Add to main layout
        root.setTop(controls);
        root.setCenter(splitPane);

        // Add interactivity
        addPointClickFunctionality();
        setupZoomHandler();
    }

    /**
     * Configura l'escena 3D per a la visualització de núvols de punts.
     * <p>
     * Aquesta funció crea i configura tots els components necessaris per a la visualització 3D:
     * <ul>
     *   <li>Grups de nodes per organitzar elements visuals</li>
     *   <li>Transformacions per a la rotació i navegació</li>
     *   <li>Càmera, il·luminació i subescena</li>
     *   <li>Gestors d'esdeveniments per a la interacció de l'usuari</li>
     * </ul>
     * </p>
     */
    private void setup3DScene() {
        int width = 1200;
        int height = 800;

        // Crea grups per als diferents elements
        world3D = new Group();
        points3D = new Group();
        hull3DGroup = new Group();

        // Inicialitza la transformació de rotació
        focusRotate = new Rotate(0, Rotate.Y_AXIS);
        rotationTimeline = null;

        // Configura la jerarquia de l'escena
        world3D.getChildren().addAll(points3D, hull3DGroup);
        world3D.getTransforms().addAll(rotateX, rotateY);

        // Afegeix els eixos de coordenades
        world3D.getChildren().addAll(createAxisLines());

        // Crea la càmera i l'escena
        subScene3D = new SubScene(world3D, width, height, true, SceneAntialiasing.BALANCED);
        camera3D = new PerspectiveCamera(true);
        camera3D.getTransforms().add(cameraTranslate);
        camera3D.setNearClip(0.1);
        camera3D.setFarClip(5000.0);
        subScene3D.setCamera(camera3D);
        subScene3D.setFill(Color.BLACK);

        // Crea una configuració d'il·luminació adequada
        // Utilitza només llum ambient per a una visualització plana i clara
        AmbientLight ambientLight = new AmbientLight(Color.WHITE); 
        world3D.getChildren().add(ambientLight);

        // Afegeix gestors d'esdeveniments
        setupMouse3DHandlers();
        setupKeyboard3DHandlers();

        // Afegeix funcionalitat de zoom
        subScene3D.setOnScroll(event -> {
            double zoomFactor = event.getDeltaY() * 5;
            cameraTranslate.setZ(cameraTranslate.getZ() + zoomFactor);
        });
    }

    /**
     * Configura els gestors d'esdeveniments del ratolí per a la navegació 3D.
     * <p>
     * Permet a l'usuari rotar la visualització 3D arrossegant el ratolí.
     * També atura les animacions de rotació automàtica quan l'usuari interactua.
     * </p>
     */
    private void setupMouse3DHandlers() {
        subScene3D.setOnMousePressed((MouseEvent event) -> {
            // Desa les coordenades inicials per calcular el desplaçament
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = rotateX.getAngle();
            anchorAngleY = rotateY.getAngle();

            // Atura qualsevol rotació en curs quan l'usuari interactua
            if (rotationTimeline != null && rotationTimeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
                rotationTimeline.stop();
                restoreNormalView();
            }
        });

        subScene3D.setOnMouseDragged((MouseEvent event) -> {
            // Actualitza els angles de rotació basant-se en el moviment del ratolí
            // La divisió per 2 redueix la sensibilitat per un control més suau
            rotateX.setAngle(anchorAngleX - (anchorY - event.getSceneY()) / 2);
            rotateY.setAngle(anchorAngleY + (anchorX - event.getSceneX()) / 2);
        });
    }

    /**
     * Configura els gestors d'esdeveniments del teclat per a la navegació 3D.
     * <p>
     * Permet a l'usuari moure la càmera utilitzant tecles:
     * <ul>
     *   <li>W/S: Apropa/allunya la càmera (eix Z)</li>
     *   <li>A/D: Mou la càmera esquerra/dreta (eix X)</li>
     *   <li>Q/E: Mou la càmera amunt/avall (eix Y)</li>
     * </ul>
     * </p>
     */
    private void setupKeyboard3DHandlers() {
        subScene3D.setOnKeyPressed((KeyEvent event) -> {
            switch (event.getCode()) {
                case W -> cameraTranslate.setZ(cameraTranslate.getZ() + 20); // Apropa
                case S -> cameraTranslate.setZ(cameraTranslate.getZ() - 20); // Allunya
                case A -> cameraTranslate.setX(cameraTranslate.getX() - 20); // Esquerra
                case D -> cameraTranslate.setX(cameraTranslate.getX() + 20); // Dreta
                case Q -> cameraTranslate.setY(cameraTranslate.getY() - 20); // Amunt
                case E -> cameraTranslate.setY(cameraTranslate.getY() + 20); // Avall
            }
        });
    }

    /**
     * Crea els eixos de coordenades per a la visualització 3D.
     * <p>
     * Genera tres línies que representen els eixos X, Y i Z utilitzant els colors estàndard:
     * <ul>
     *   <li>Eix X: Vermell</li>
     *   <li>Eix Y: Verd</li>
     *   <li>Eix Z: Blau</li>
     * </ul>
     * Aquests eixos serveixen com a referència visual per a l'orientació espacial de l'escena 3D.
     * </p>
     * 
     * @return array de nodes que representen els tres eixos de coordenades
     */
    private Node[] createAxisLines() {
        // Crea materials amb els colors estàndard per als eixos
        PhongMaterial xMaterial = new PhongMaterial(Color.RED);    // Vermell per a l'eix X
        PhongMaterial yMaterial = new PhongMaterial(Color.GREEN);  // Verd per a l'eix Y
        PhongMaterial zMaterial = new PhongMaterial(Color.BLUE);   // Blau per a l'eix Z

        // Crea formes allargades per representar cada eix
        Box xAxis = new Box(200, 1, 1);  // Eix X: allargat en la dimensió X
        xAxis.setMaterial(xMaterial);

        Box yAxis = new Box(1, 200, 1);  // Eix Y: allargat en la dimensió Y
        yAxis.setMaterial(yMaterial);

        Box zAxis = new Box(1, 1, 200);  // Eix Z: allargat en la dimensió Z
        zAxis.setMaterial(zMaterial);

        return new Node[]{xAxis, yAxis, zAxis};
    }

    /**
     * Restaura la vista 3D al mode de navegació normal després de l'animació d'enfocament.
     * <p>
     * Aquest mètode restableix les transformacions i els elements de l'escena 3D
     * per tornar a l'estat de visualització normal després de l'animació d'enfocament
     * en punts específics. Elimina les transformacions especials i reconstrueix
     * l'escena amb tots els elements originals.
     * </p>
     */
    private void restoreNormalView() {
        // Desa els fills actuals per si cal restaurar-los
        List<Node> children = new ArrayList<>();
        if (world3D.getChildren().size() > 0 && world3D.getChildren().get(0) instanceof Group g) {
            children.addAll(g.getChildren());
        }

        // Restableix les transformacions i la disposició del món
        world3D.getTransforms().clear();
        world3D.getTransforms().addAll(rotateX, rotateY);
        world3D.getChildren().clear();
        world3D.setTranslateX(0);
        world3D.setTranslateY(0);
        world3D.setTranslateZ(0);

        // Torna a afegir el contingut original
        world3D.getChildren().addAll(points3D, hull3DGroup);
        world3D.getChildren().addAll(createAxisLines());

        // Torna a dibuixar tot per assegurar un estat adequat
        if (latest3DPoints != null && !latest3DPoints.isEmpty()) {
            draw3DPoints();
        }
    }

    private void updateVisualization() {
        // Reset focus button and hull checkbox state
        focusButton.setDisable(true);
        boolean isQuickHull = algorithmCombo.getValue().contains("QuickHall");
        showHullCheckbox.setDisable(!isQuickHull);

        if (dimensionCombo.getValue().equals("2D")) {
            // Switch to 2D visualization
            if (root.getCenter() instanceof SplitPane splitPane) {
                if (splitPane.getItems().get(0) instanceof BorderPane visualizationPane) {
                    visualizationPane.setCenter(scrollPane);
                }
            }

            // Stop any 3D animation that might be running
            if (rotationTimeline != null && rotationTimeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
                rotationTimeline.stop();
                restoreNormalView();
            }

            // Redraw 2D points if available
            if (latest2DPoints != null && !latest2DPoints.isEmpty()) {
                draw2DPoints();
                // Re-enable focus button if we have result points
                focusButton.setDisable(highlight2DA == null || highlight2DB == null);
            }
        } else {
            // Switch to 3D visualization
            if (root.getCenter() instanceof SplitPane splitPane) {
                if (splitPane.getItems().get(0) instanceof BorderPane visualizationPane) {
                    visualizationPane.setCenter(subScene3D);
                }
            }

            // Reset camera and view
            restoreNormalView();
            rotateX.setAngle(0);
            rotateY.setAngle(0);
            cameraTranslate.setZ(-500);

            // Redraw 3D points if available
            if (latest3DPoints != null && !latest3DPoints.isEmpty()) {
                draw3DPoints();
                // Re-enable focus button if we have result points
                focusButton.setDisable(highlight3DA == null || highlight3DB == null);
            }
        }

        // Update hull visualization
        if (shouldShowHull && isQuickHull) {
            computeAndVisualizeHull();
        }
    }

    private void setupZoomHandler() {
        scrollPane.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                double zoomFactor = (e.getDeltaY() > 0) ? 1.1 : 0.9;
                double oldScale = canvasGroup.getScaleX();
                double newScale = oldScale * zoomFactor;
                newScale = Math.max(0.1, Math.min(10.0, newScale));

                double mouseX = e.getX();
                double mouseY = e.getY();

                double scrollH = scrollPane.getHvalue();
                double scrollV = scrollPane.getVvalue();

                double innerWidth = scrollPane.getContent().getBoundsInLocal().getWidth();
                double innerHeight = scrollPane.getContent().getBoundsInLocal().getHeight();
                double viewportWidth = scrollPane.getViewportBounds().getWidth();
                double viewportHeight = scrollPane.getViewportBounds().getHeight();

                double dx = (mouseX + scrollH * (innerWidth - viewportWidth)) / innerWidth;
                double dy = (mouseY + scrollV * (innerHeight - viewportHeight)) / innerHeight;

                canvasGroup.setScaleX(newScale);
                canvasGroup.setScaleY(newScale);

                // Schedule position update after scaling has been applied
                javafx.application.Platform.runLater(() -> {
                    double newInnerWidth = scrollPane.getContent().getBoundsInLocal().getWidth();
                    double newInnerHeight = scrollPane.getContent().getBoundsInLocal().getHeight();

                    scrollPane.setHvalue((dx * newInnerWidth - viewportWidth / 2) / (newInnerWidth - viewportWidth));
                    scrollPane.setVvalue((dy * newInnerHeight - viewportHeight / 2) / (newInnerHeight - viewportHeight));
                });

                e.consume();
            }
        });
    }

    private void addPointClickFunctionality() {
        canvas2D.setOnMouseClicked(e -> {
            if (latest2DPoints == null || latest2DPoints.isEmpty()) return;

            // Get bounds
            double maxX = latest2DPoints.stream().mapToDouble(Point2D::x).max().orElse(1);
            double maxY = latest2DPoints.stream().mapToDouble(Point2D::y).max().orElse(1);
            double minX = latest2DPoints.stream().mapToDouble(Point2D::x).min().orElse(0);
            double minY = latest2DPoints.stream().mapToDouble(Point2D::y).min().orElse(0);


            double canvasW = canvas2D.getWidth();
            double canvasH = canvas2D.getHeight();
            double scaleX = canvasW / (maxX - minX + 1);
            double scaleY = canvasH / (maxY - minY + 1);
            double scale = Math.min(scaleX, scaleY);

            double offsetX = (canvasW - (maxX - minX) * scale) / 2.0;
            double offsetY = (canvasH - (maxY - minY) * scale) / 2.0;

            // Convert click coordinates back to point space
            double clickX = (e.getX() - offsetX) / scale + minX;
            double clickY = (e.getY() - offsetY) / scale + minY;

            // Find closest point
            Point2D closestPoint = null;
            double minDistance = Double.MAX_VALUE;

            for (Point2D p : latest2DPoints) {
                double distance = Math.sqrt(Math.pow(p.x() - clickX, 2) + Math.pow(p.y() - clickY, 2));
                if (distance < minDistance) {
                    minDistance = distance;
                    closestPoint = p;
                }
            }

            if (closestPoint != null) {
                // Check if we clicked on one of the highlighted points
                if (highlight2DA != null && closestPoint.equals(highlight2DA)) {
                    // If clicking on highlight2DA, center on highlight2DB to see the other end of the pair
                    centerOnPoint(highlight2DB);
                    outputArea.appendText(String.format("Viewing other point in pair: %s\n", highlight2DB));
                } else if (highlight2DB != null && closestPoint.equals(highlight2DB)) {
                    // If clicking on highlight2DB, center on highlight2DA to see the other end of the pair
                    centerOnPoint(highlight2DA);
                    outputArea.appendText(String.format("Viewing other point in pair: %s\n", highlight2DA));
                } else {
                    // Normal case - just select and center on this point
                    centerOnPoint(closestPoint);
                    outputArea.appendText(String.format("Selected point: %s\n", closestPoint));
                }
                draw2DPoints();
            }
        });
    }

    private void centerOnPoint(Point2D point) {
        if (point == null || latest2DPoints == null || latest2DPoints.isEmpty()) return;

        // Get bounds
        double maxX = latest2DPoints.stream().mapToDouble(Point2D::x).max().orElse(1);
        double maxY = latest2DPoints.stream().mapToDouble(Point2D::y).max().orElse(1);
        double minX = latest2DPoints.stream().mapToDouble(Point2D::x).min().orElse(0);
        double minY = latest2DPoints.stream().mapToDouble(Point2D::y).min().orElse(0);

        double canvasW = canvas2D.getWidth();
        double canvasH = canvas2D.getHeight();
        double scaleX = canvasW / (maxX - minX + 1);
        double scaleY = canvasH / (maxY - minY + 1);
        double scale = Math.min(scaleX, scaleY);

        double offsetX = (canvasW - (maxX - minX) * scale) / 2.0;
        double offsetY = (canvasH - (maxY - minY) * scale) / 2.0;

        // Calculate point position on canvas
        double pointX = (point.x() - minX) * scale + offsetX;
        double pointY = (point.y() - minY) * scale + offsetY;

        // Calculate scroll position to center on this point
        double hValue = (pointX - scrollPane.getViewportBounds().getWidth() / 2) /
                (scrollPane.getContent().getBoundsInLocal().getWidth() - scrollPane.getViewportBounds().getWidth());
        double vValue = (pointY - scrollPane.getViewportBounds().getHeight() / 2) /
                (scrollPane.getContent().getBoundsInLocal().getHeight() - scrollPane.getViewportBounds().getHeight());

        // Clamp values between 0 and 1
        hValue = Math.max(0, Math.min(1, hValue));
        vValue = Math.max(0, Math.min(1, vValue));

        // Set scroll positions
        scrollPane.setHvalue(hValue);
        scrollPane.setVvalue(vValue);
    }

    private void centerBetweenPoints(Point2D pointA, Point2D pointB) {
        if (pointA == null || pointB == null || latest2DPoints == null || latest2DPoints.isEmpty()) return;

        Point2D midpoint = new Point2D((pointA.x() + pointB.x()) / 2, (pointA.y() + pointB.y()) / 2);

        double maxX = latest2DPoints.stream().mapToDouble(Point2D::x).max().orElse(1);
        double maxY = latest2DPoints.stream().mapToDouble(Point2D::y).max().orElse(1);
        double minX = latest2DPoints.stream().mapToDouble(Point2D::x).min().orElse(0);
        double minY = latest2DPoints.stream().mapToDouble(Point2D::y).min().orElse(0);

        double canvasW = canvas2D.getWidth();
        double canvasH = canvas2D.getHeight();
        double scaleX = canvasW / (maxX - minX + 1);
        double scaleY = canvasH / (maxY - minY + 1);
        double scale = Math.min(scaleX, scaleY);

        double offsetX = (canvasW - (maxX - minX) * scale) / 2.0;
        double offsetY = (canvasH - (maxY - minY) * scale) / 2.0;

        double midX = (midpoint.x() - minX) * scale + offsetX;
        double midY = (midpoint.y() - minY) * scale + offsetY;

        // Adjust zoom scaleFactor if needed (optional)
        scaleFactor = Math.max(0.3, Math.min(2.5, scaleFactor));
        canvasGroup.setScaleX(scaleFactor);
        canvasGroup.setScaleY(scaleFactor);

        double viewW = scrollPane.getViewportBounds().getWidth();
        double viewH = scrollPane.getViewportBounds().getHeight();
        double contentW = canvasGroup.getBoundsInLocal().getWidth();
        double contentH = canvasGroup.getBoundsInLocal().getHeight();

        double scrollX = (midX * scaleFactor - viewW / 2.0) / (contentW * scaleFactor - viewW);
        double scrollY = (midY * scaleFactor - viewH / 2.0) / (contentH * scaleFactor - viewH);

        javafx.application.Platform.runLater(() -> {
            scrollPane.setHvalue(Math.max(0, Math.min(1, scrollX)));
            scrollPane.setVvalue(Math.max(0, Math.min(1, scrollY)));
        });
    }

    /**
     * Dibuixa els punts 2D al canvas amb les marques corresponents als resultats.
     * <p>
     * Aquest mètode gestiona la visualització completa del núvol de punts en 2D, incloent:
     * <ul>
     *   <li>Escalat automàtic per mostrar tots els punts</li>
     *   <li>Dibuix dels eixos de coordenades</li>
     *   <li>Visualització dels punts del núvol</li>
     *   <li>Ressaltat dels punts resultat (parell més proper o diàmetre)</li>
     *   <li>Visualització de l'envolupant convexa si està activada</li>
     * </ul>
     * </p>
     */
    private void draw2DPoints() {
        if (latest2DPoints == null || latest2DPoints.isEmpty()) return;

        GraphicsContext gc = canvas2D.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas2D.getWidth(), canvas2D.getHeight());
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas2D.getWidth(), canvas2D.getHeight());

        // Obté els límits del núvol de punts
        double maxX = latest2DPoints.stream().mapToDouble(Point2D::x).max().orElse(1.0);
        double maxY = latest2DPoints.stream().mapToDouble(Point2D::y).max().orElse(1.0);
        double minX = latest2DPoints.stream().mapToDouble(Point2D::x).min().orElse(0.0);
        double minY = latest2DPoints.stream().mapToDouble(Point2D::y).min().orElse(0.0);

        // Afegeix marges fixos per assegurar una millor visibilitat
        int margin = 60; // 60px de marge a tots els costats
        
        double canvasW = canvas2D.getWidth();
        double canvasH = canvas2D.getHeight();
        double usableWidth = canvasW - 2 * margin;
        double usableHeight = canvasH - 2 * margin;
        
        // Calcula el factor d'escala amb marges
        double scaleX = usableWidth / (maxX - minX + 1);
        double scaleY = usableHeight / (maxY - minY + 1);
        double scale = Math.min(scaleX, scaleY);
        
        // Calcula els desplaçaments per centrar el dibuix amb marges
        double offsetX = margin + (usableWidth - (maxX - minX) * scale) / 2.0;
        double offsetY = margin + (usableHeight - (maxY - minY) * scale) / 2.0;

        // Dibuixa els eixos de coordenades
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(1);
        // Eix X
        gc.strokeLine(offsetX, canvasH - offsetY, canvasW - offsetX, canvasH - offsetY);
        // Eix Y
        gc.strokeLine(offsetX, offsetY, offsetX, canvasH - offsetY);

        // Dibuixa els punts
        gc.setFill(Color.CYAN);
        for (Point2D p : latest2DPoints) {
            double x = (p.x() - minX) * scale + offsetX;
            double y = (p.y() - minY) * scale + offsetY;
            gc.fillOval(x - 1.5, y - 1.5, 3, 3);
        }

        // Dibuixa l'envolupant convexa si està activada i disponible
        if (shouldShowHull && hull2DPoints != null && !hull2DPoints.isEmpty() &&
            currentAlgorithmType != null && currentAlgorithmType.toString().contains("QUICKHULL")) {

            gc.setStroke(Color.ORANGE);
            gc.setLineWidth(2);

            // Dibuixa l'envolupant com a polígon
            double[] xPoints = new double[hull2DPoints.size()];
            double[] yPoints = new double[hull2DPoints.size()];

            for (int i = 0; i < hull2DPoints.size(); i++) {
                Point2D p = hull2DPoints.get(i);
                xPoints[i] = (p.x() - minX) * scale + offsetX;
                yPoints[i] = (p.y() - minY) * scale + offsetY;
            }

            // Dibuixa el polígon de l'envolupant
            gc.strokePolygon(xPoints, yPoints, hull2DPoints.size());

            // Dibuixa els punts de l'envolupant
            gc.setFill(Color.ORANGE);
            for (Point2D p : hull2DPoints) {
                double x = (p.x() - minX) * scale + offsetX;
                double y = (p.y() - minY) * scale + offsetY;
                gc.fillOval(x - 3, y - 3, 6, 6);
            }
        }

        // Dibuixa els punts ressaltats (resultat de l'algoritme)
        if (highlight2DA != null && highlight2DB != null) {
            double x1 = (highlight2DA.x() - minX) * scale + offsetX;
            double y1 = (highlight2DA.y() - minY) * scale + offsetY;
            double x2 = (highlight2DB.x() - minX) * scale + offsetX;
            double y2 = (highlight2DB.y() - minY) * scale + offsetY;

            // Dibuixa la línia que connecta els punts resultat
            gc.setStroke(Color.RED);
            gc.setLineWidth(2);
            gc.strokeLine(x1, y1, x2, y2);

            // Dibuixa els punts resultat en vermell
            gc.setFill(Color.RED);
            gc.fillOval(x1 - 4, y1 - 4, 8, 8);
            gc.fillOval(x2 - 4, y2 - 4, 8, 8);

            // Dibuixa cercles al voltant dels punts per ressaltar-los més
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(1);
            gc.strokeOval(x1 - 6, y1 - 6, 12, 12);
            gc.strokeOval(x2 - 6, y2 - 6, 12, 12);
        }
    }

    /**
     * Dibuixa els punts 3D a l'escena amb les marques corresponents als resultats.
     * <p>
     * Aquest mètode gestiona la visualització completa del núvol de punts en 3D, incloent:
     * <ul>
     *   <li>Centrat automàtic del núvol de punts a l'origen</li>
     *   <li>Visualització dels punts del núvol amb esferes</li>
     *   <li>Ressaltat dels punts resultat (parell més proper o diàmetre)</li>
     *   <li>Connexió visual entre els punts resultat</li>
     *   <li>Visualització d'efectes especials per al parell més proper</li>
     * </ul>
     * </p>
     */
    private void draw3DPoints() {
        if (latest3DPoints == null || latest3DPoints.isEmpty()) return;

        // Comprova si estem en mode d'enfocament - en aquest cas, no redibuixem
        if (rotationTimeline != null && rotationTimeline.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            return;
        }

        // Neteja els punts anteriors
        points3D.getChildren().clear();

        // Calcula el centre dels punts per a una millor visualització
        double avgX = latest3DPoints.stream().mapToDouble(Point3D::x).sum() / latest3DPoints.size();
        double avgY = latest3DPoints.stream().mapToDouble(Point3D::y).sum() / latest3DPoints.size();
        double avgZ = latest3DPoints.stream().mapToDouble(Point3D::z).sum() / latest3DPoints.size();

        // Centra els punts al voltant de l'origen
        List<Point3D> centeredPoints = latest3DPoints.stream()
                .map(p -> new Point3D(p.x() - avgX, p.y() - avgY, p.z() - avgZ))
                .toList();

        // Dibuixa tots els punts
        for (Point3D p : centeredPoints) {
            Sphere sphere = new Sphere(2);
            sphere.setTranslateX(p.x());
            sphere.setTranslateY(p.y());
            sphere.setTranslateZ(p.z());
            // Actualitza el material per a una il·luminació plana
            PhongMaterial pointMaterial = new PhongMaterial(Color.CYAN);
            pointMaterial.setSpecularPower(0); // Desactiva l'especular
            sphere.setMaterial(pointMaterial);
            points3D.getChildren().add(sphere);
        }

        // Afegeix visualització del resultat si està disponible
        if (highlight3DA != null && highlight3DB != null) {
            Point3D c1 = centerPoint(highlight3DA, avgX, avgY, avgZ);
            Point3D c2 = centerPoint(highlight3DB, avgX, avgY, avgZ);

            // Crea esferes més grans i visibles per als punts resultat
            Sphere endpoint1 = new Sphere(4);
            PhongMaterial material1 = new PhongMaterial(Color.RED);
            material1.setSpecularColor(Color.TRANSPARENT); // Sense brillantors especulars
            material1.setSpecularPower(0); // Desactiva l'especular
            endpoint1.setMaterial(material1);
            endpoint1.setTranslateX(c1.x());
            endpoint1.setTranslateY(c1.y());
            endpoint1.setTranslateZ(c1.z());

            Sphere endpoint2 = new Sphere(4);
            PhongMaterial material2 = new PhongMaterial(Color.RED);
            material2.setSpecularColor(Color.TRANSPARENT); // Sense brillantors especulars
            material2.setSpecularPower(0); // Desactiva l'especular
            endpoint2.setMaterial(material2);
            endpoint2.setTranslateX(c2.x());
            endpoint2.setTranslateY(c2.y());
            endpoint2.setTranslateZ(c2.z());

            // Tria el color basat en el tipus d'algoritme
            Color lineColor = currentAlgorithmType.toString().contains("CLOSEST_PAIR") ?
                Color.LIME : Color.ORANGE;

            // Crea una línia de connexió amb millors propietats d'il·luminació
            Cylinder line = makeLineBetween(c1, c2, 1.0, lineColor);

            // Afegeix els elements a l'escena
            points3D.getChildren().addAll(line, endpoint1, endpoint2);

            // Per al parell més proper, afegeix un cilindre transparent
            if (currentAlgorithmType.toString().contains("CLOSEST_PAIR")) {
                addTransparentCylinderAroundPoints(points3D, c1, c2);
            }

            // Habilita el botó d'enfocament ja que tenim punts resultat
            focusButton.setDisable(false);
        }

        // Actualitza la visualització de l'envolupant si cal
        updateHullVisualization();
    }

    private void updateHullVisualization() {
        // Clear previous hull
        hull3DGroup.getChildren().clear();
        hull2DGroup.getChildren().clear();

        if (!shouldShowHull || currentAlgorithmType == null ||
            !currentAlgorithmType.toString().contains("QUICKHULL")) {
            return;
        }

        // 2D hull is drawn directly in the draw2DPoints method

        // 3D hull visualization
        if (dimensionCombo.getValue().equals("3D") && hull3DPoints != null && !hull3DPoints.isEmpty()) {
            // Calculate center of points for hull visualization
            double avgX = latest3DPoints.stream().mapToDouble(Point3D::x).sum() / latest3DPoints.size();
            double avgY = latest3DPoints.stream().mapToDouble(Point3D::y).sum() / latest3DPoints.size();
            double avgZ = latest3DPoints.stream().mapToDouble(Point3D::z).sum() / latest3DPoints.size();

            // Center hull points around origin for better visualization
            List<Point3D> centeredHullPoints = hull3DPoints.stream()
                    .map(p -> new Point3D(p.x() - avgX, p.y() - avgY, p.z() - avgZ))
                    .toList();

            // Create transparent material for the triangular faces with flat lighting
            PhongMaterial faceMaterial = new PhongMaterial();
            faceMaterial.setDiffuseColor(new Color(1.0, 0.5, 0.0, 0.25)); // Transparent orange
            faceMaterial.setSpecularColor(Color.TRANSPARENT); // No specular highlights
            faceMaterial.setSpecularPower(0); // Disable specular for flat appearance

            if (hullFaces != null && hullFaces.length > 0) {
                // Use the actual faces from the QuickHull3D implementation
                for (int[] face : hullFaces) {
                    // Ensure face has 3 vertices
                    if (face.length >= 3) {
                        Point3D p1 = centeredHullPoints.get(face[0]);
                        Point3D p2 = centeredHullPoints.get(face[1]);
                        Point3D p3 = centeredHullPoints.get(face[2]);

                        // Create triangular face mesh
                        TriangleMesh triangleMesh = new TriangleMesh();
                        triangleMesh.getPoints().addAll(
                            (float)p1.x(), (float)p1.y(), (float)p1.z(),
                            (float)p2.x(), (float)p2.y(), (float)p2.z(),
                            (float)p3.x(), (float)p3.y(), (float)p3.z()
                        );

                        // Add texture coordinates (not really used but required)
                        triangleMesh.getTexCoords().addAll(
                            0, 0,
                            1, 0,
                            0, 1
                        );

                        // Add face indices
                        triangleMesh.getFaces().addAll(
                            0, 0, 1, 1, 2, 2
                        );

                        // Create mesh view and add to hull group
                        MeshView meshView = new MeshView(triangleMesh);
                        meshView.setMaterial(faceMaterial);
                        meshView.setCullFace(CullFace.NONE); // Show both sides of the triangles
                        meshView.setDrawMode(DrawMode.FILL);
                        hull3DGroup.getChildren().add(meshView);

                        // Create lines for each edge of the face for wireframe visualization
                        Cylinder line1 = makeLineBetween(p1, p2, 0.5, Color.ORANGE);
                        Cylinder line2 = makeLineBetween(p2, p3, 0.5, Color.ORANGE);
                        Cylinder line3 = makeLineBetween(p3, p1, 0.5, Color.ORANGE);

                        hull3DGroup.getChildren().addAll(line1, line2, line3);
                    }
                }
            } else {
                // Fallback if no faces are available - use the generated hull faces
                List<Integer[]> generatedFaces = generateConvexHullFaces(centeredHullPoints);

                // Create triangular meshes for each face
                for (Integer[] face : generatedFaces) {
                    Point3D p1 = centeredHullPoints.get(face[0]);
                    Point3D p2 = centeredHullPoints.get(face[1]);
                    Point3D p3 = centeredHullPoints.get(face[2]);

                    // Create triangular face mesh
                    TriangleMesh triangleMesh = new TriangleMesh();
                    triangleMesh.getPoints().addAll(
                        (float)p1.x(), (float)p1.y(), (float)p1.z(),
                        (float)p2.x(), (float)p2.y(), (float)p2.z(),
                        (float)p3.x(), (float)p3.y(), (float)p3.z()
                    );

                    // Add texture coordinates (not really used but required)
                    triangleMesh.getTexCoords().addAll(
                        0, 0,
                        1, 0,
                        0, 1
                    );

                    // Add face indices
                    triangleMesh.getFaces().addAll(
                        0, 0, 1, 1, 2, 2
                    );

                    // Create mesh view and add to hull group
                    MeshView meshView = new MeshView(triangleMesh);
                    meshView.setMaterial(faceMaterial);
                    meshView.setCullFace(CullFace.NONE); // Show both sides of the triangles
                    meshView.setDrawMode(DrawMode.FILL);
                    hull3DGroup.getChildren().add(meshView);

                    // Create lines for each edge
                    Cylinder line1 = makeLineBetween(p1, p2, 0.5, Color.ORANGE);
                    Cylinder line2 = makeLineBetween(p2, p3, 0.5, Color.ORANGE);
                    Cylinder line3 = makeLineBetween(p3, p1, 0.5, Color.ORANGE);

                    hull3DGroup.getChildren().addAll(line1, line2, line3);
                }
            }

            // Create spheres for hull vertices
            for (int i = 0; i < centeredHullPoints.size(); i++) {
                Point3D p = centeredHullPoints.get(i);
                Sphere sphere = new Sphere(3);
                sphere.setTranslateX(p.x());
                sphere.setTranslateY(p.y());
                sphere.setTranslateZ(p.z());

                // Check if this hull vertex is one of our highlight points
                Point3D c1 = highlight3DA != null ? centerPoint(highlight3DA, avgX, avgY, avgZ) : null;
                Point3D c2 = highlight3DB != null ? centerPoint(highlight3DB, avgX, avgY, avgZ) : null;

                // Use value-based equals
                if ((c1 != null && p.x() == c1.x() && p.y() == c1.y() && p.z() == c1.z()) ||
                    (c2 != null && p.x() == c2.x() && p.y() == c2.y() && p.z() == c2.z())) {
                    // This is a result point that's also on the hull - make it purple to show both properties
                    PhongMaterial material = new PhongMaterial(Color.PURPLE);
                    material.setSpecularColor(Color.WHITE);
                    sphere.setMaterial(material);
                } else {
                    // Normal hull point
                    PhongMaterial material = new PhongMaterial(Color.ORANGE);
                    sphere.setMaterial(material);
                }
                hull3DGroup.getChildren().add(sphere);
            }
        }
    }

    /**
     * Generate triangular faces for a convex hull using incremental algorithm
     * @param points List of 3D points that form the convex hull
     * @return List of integer triplets representing triangle faces (indices into the points list)
     */
    private List<Integer[]> generateConvexHullFaces(List<Point3D> points) {
        if (points.size() < 4) {
            // Not enough points for a 3D convex hull
            return new ArrayList<>();
        }

        List<Integer[]> faces = new ArrayList<>();

        // If we have many points, use tetrahedron-based approach
        if (points.size() >= 4) {
            // Find 4 points that form a tetrahedron
            int[] tetPoints = findTetrahedron(points);
            if (tetPoints != null) {
                // Initial tetrahedron faces
                faces.add(new Integer[]{tetPoints[0], tetPoints[1], tetPoints[2]});
                faces.add(new Integer[]{tetPoints[0], tetPoints[1], tetPoints[3]});
                faces.add(new Integer[]{tetPoints[0], tetPoints[2], tetPoints[3]});
                faces.add(new Integer[]{tetPoints[1], tetPoints[2], tetPoints[3]});

                // For each remaining point, check if it's inside the hull
                // If not, remove visible faces and add new ones
                // For visualization purposes, we'll skip this complex process
                // and just render the basic tetrahedron for the hull
            }
        }

        // If we don't have a tetrahedron, use a different approach to generate faces
        if (faces.isEmpty()) {
            // Generate faces by connecting each point to the "center" of the hull
            computeTriangularFaces(points, faces);
        }

        return faces;
    }

    /**
     * Find four points that form a tetrahedron
     */
    private int[] findTetrahedron(List<Point3D> points) {
        if (points.size() < 4) return null;

        // Find extreme points for initial tetrahedron
        int xMinIdx = 0, xMaxIdx = 0;
        int yMinIdx = 0, yMaxIdx = 0;
        int zMinIdx = 0, zMaxIdx = 0;

        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).x() < points.get(xMinIdx).x()) xMinIdx = i;
            if (points.get(i).x() > points.get(xMaxIdx).x()) xMaxIdx = i;
            if (points.get(i).y() < points.get(yMinIdx).y()) yMinIdx = i;
            if (points.get(i).y() > points.get(yMaxIdx).y()) yMaxIdx = i;
            if (points.get(i).z() < points.get(zMinIdx).z()) zMinIdx = i;
            if (points.get(i).z() > points.get(zMaxIdx).z()) zMaxIdx = i;
        }

        // Find the pair with maximum distance
        int[] extremeIndices = {xMinIdx, xMaxIdx, yMinIdx, yMaxIdx, zMinIdx, zMaxIdx};
        int v1Idx = -1, v2Idx = -1;
        double maxDist = 0;

        for (int i = 0; i < extremeIndices.length; i++) {
            for (int j = i + 1; j < extremeIndices.length; j++) {
                Point3D p1 = points.get(extremeIndices[i]);
                Point3D p2 = points.get(extremeIndices[j]);

                double dx = p1.x() - p2.x();
                double dy = p1.y() - p2.y();
                double dz = p1.z() - p2.z();
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

                if (dist > maxDist) {
                    maxDist = dist;
                    v1Idx = extremeIndices[i];
                    v2Idx = extremeIndices[j];
                }
            }
        }

        if (v1Idx == -1 || v2Idx == -1) return null;

        // Find third point for triangle with largest area
        int v3Idx = -1;
        double maxArea = 0;

        for (int i = 0; i < points.size(); i++) {
            if (i == v1Idx || i == v2Idx) continue;

            double area = triangleArea(points.get(v1Idx), points.get(v2Idx), points.get(i));
            if (area > maxArea) {
                maxArea = area;
                v3Idx = i;
            }
        }

        if (v3Idx == -1) return null;

        // Find fourth point for tetrahedron with largest volume
        int v4Idx = -1;
        double maxVolume = 0;

        for (int i = 0; i < points.size(); i++) {
            if (i == v1Idx || i == v2Idx || i == v3Idx) continue;

            double volume = tetrahedronVolume(
                points.get(v1Idx), points.get(v2Idx),
                points.get(v3Idx), points.get(i)
            );
            if (Math.abs(volume) > maxVolume) {
                maxVolume = Math.abs(volume);
                v4Idx = i;
            }
        }

        if (v4Idx == -1) return null;

        return new int[]{v1Idx, v2Idx, v3Idx, v4Idx};
    }

    /**
     * Compute triangle area for 3D points
     */
    private double triangleArea(Point3D p1, Point3D p2, Point3D p3) {
        double ax = p2.x() - p1.x();
        double ay = p2.y() - p1.y();
        double az = p2.z() - p1.z();

        double bx = p3.x() - p1.x();
        double by = p3.y() - p1.y();
        double bz = p3.z() - p1.z();

        // Cross product for normal vector
        double nx = ay * bz - az * by;
        double ny = az * bx - ax * bz;
        double nz = ax * by - ay * bx;

        // Length of normal = 2 * area
        return Math.sqrt(nx*nx + ny*ny + nz*nz) / 2;
    }

    /**
     * Compute tetrahedron volume
     */
    private double tetrahedronVolume(Point3D p1, Point3D p2, Point3D p3, Point3D p4) {
        double[] ab = {p2.x() - p1.x(), p2.y() - p1.y(), p2.z() - p1.z()};
        double[] ac = {p3.x() - p1.x(), p3.y() - p1.y(), p3.z() - p1.z()};
        double[] ad = {p4.x() - p1.x(), p4.y() - p1.y(), p4.z() - p1.z()};

        // Triple scalar product
        double xa = ab[0];
        double ya = ab[1];
        double za = ab[2];
        double xb = ac[0];
        double yb = ac[1];
        double zb = ac[2];
        double xc = ad[0];
        double yc = ad[1];
        double zc = ad[2];

        return (xa * (yb * zc - zb * yc) + ya * (zb * xc - xb * zc) + za * (xb * yc - yb * xc)) / 6.0;
    }

    /**
     * Compute triangular faces for a set of points on a convex hull
     */
    private void computeTriangularFaces(List<Point3D> points, List<Integer[]> faces) {
        // For a small number of points or if initial tetrahedron failed,
        // use a sphere approximation - create triangles by connecting
        // each point to its nearest neighbors

        // Create a mapping of each point to its nearest neighbors
        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                for (int k = j + 1; k < points.size(); k++) {
                    Point3D p1 = points.get(i);
                    Point3D p2 = points.get(j);
                    Point3D p3 = points.get(k);

                    // Check if these three points form a valid face
                    // For simplicity, we'll use all combinations of three points
                    // This may create some interior faces, but they'll be hidden by the exterior ones
                    faces.add(new Integer[]{i, j, k});
                }
            }
        }
    }

    private double computeAverageDistance(List<Point3D> points) {
        if (points.size() < 2) return 0;

        double totalDist = 0;
        int count = 0;

        // Compute average of distances between some point pairs (sampling)
        for (int i = 0; i < Math.min(10, points.size()); i++) {
            for (int j = i + 1; j < Math.min(i + 5, points.size()); j++) {
                Point3D p1 = points.get(i);
                Point3D p2 = points.get(j);
                double dx = p1.x() - p2.x();
                double dy = p1.y() - p2.y();
                double dz = p1.z() - p2.z();
                totalDist += Math.sqrt(dx*dx + dy*dy + dz*dz);
                count++;
            }
        }

        return count > 0 ? totalDist / count : 0;
    }

    private Point3D centerPoint(Point3D p, double avgX, double avgY, double avgZ) {
        return new Point3D(p.x() - avgX, p.y() - avgY, p.z() - avgZ);
    }

    // Track the focus animation
    private javafx.animation.Timeline rotationTimeline;
    private Rotate focusRotate = new Rotate(0, Rotate.Y_AXIS);

    private void focusOn3DPoints() {
        if (highlight3DA == null || highlight3DB == null) return;

        // Stop any existing animation
        if (rotationTimeline != null) {
            rotationTimeline.stop();
        }

        // Calculate center of all points for the offset
        double avgX = latest3DPoints.stream().mapToDouble(Point3D::x).sum() / latest3DPoints.size();
        double avgY = latest3DPoints.stream().mapToDouble(Point3D::y).sum() / latest3DPoints.size();
        double avgZ = latest3DPoints.stream().mapToDouble(Point3D::z).sum() / latest3DPoints.size();

        // Get the centered points
        Point3D p1 = centerPoint(highlight3DA, avgX, avgY, avgZ);
        Point3D p2 = centerPoint(highlight3DB, avgX, avgY, avgZ);

        // Calculate midpoint between the two highlighted points
        double midX = (p1.x() + p2.x()) / 2.0;
        double midY = (p1.y() + p2.y()) / 2.0;
        double midZ = (p1.z() + p2.z()) / 2.0;

        // Calculate distance between the points to determine camera distance
        double dx = p2.x() - p1.x();
        double dy = p2.y() - p1.y();
        double dz = p2.z() - p1.z();
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Reset transformations
        world3D.getTransforms().clear();

        // Create a Group for our focused content that will be centered at the midpoint
        Group focusedContent = new Group();
        focusedContent.getTransforms().add(focusRotate);

        // Move all children from world3D to focusedContent
        focusedContent.getChildren().addAll(points3D, hull3DGroup);
        focusedContent.getChildren().addAll(createAxisLines());  // Add axis lines

        // Translate the focused content to center on midpoint
        Translate centerTranslate = new Translate(-midX, -midY, -midZ);
        focusedContent.getTransforms().add(centerTranslate);

        // Add the focused content to world3D
        world3D.getChildren().clear();
        world3D.getChildren().add(focusedContent);

        // Reset camera view and position
        rotateX.setAngle(0);
        rotateY.setAngle(0);
        focusRotate.setAngle(0);

        // Set camera distance based on point distance
        double viewDistance = Math.max(distance * 5, 500);
        cameraTranslate.setX(0);
        cameraTranslate.setY(0);
        cameraTranslate.setZ(-viewDistance);

        // Setup rotation animation using Timeline
        rotationTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                Duration.ZERO,
                new javafx.animation.KeyValue(focusRotate.angleProperty(), 0)
            ),
            new javafx.animation.KeyFrame(
                Duration.seconds(10),
                new javafx.animation.KeyValue(focusRotate.angleProperty(), 360)
            )
        );
        rotationTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        rotationTimeline.play();

        // Output info for debugging
        outputArea.appendText(String.format("Enfocant en els punts: %s i %s (distància: %.2f)\n",
            highlight3DA, highlight3DB, distance));
    }

    private void addTransparentCylinderAroundPoints(Group group, Point3D p1, Point3D p2) {
        // Calculate midpoint between the two points
        double midX = (p1.x() + p2.x()) / 2;
        double midY = (p1.y() + p2.y()) / 2;
        double midZ = (p1.z() + p2.z()) / 2;

        // Calculate distance between points
        double dx = p2.x() - p1.x();
        double dy = p2.y() - p1.y();
        double dz = p2.z() - p1.z();
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

        // Make cylinder slightly larger than the distance
        double radius = distance * 0.6;
        double height = distance * 1.5;

        Cylinder cylinder = new Cylinder(radius, height);

        // Create a transparent material with a slight green tint - simpler for flat rendering
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(new Color(0.2, 1.0, 0.2, 0.15));  // Transparent green
        material.setSpecularColor(Color.TRANSPARENT);  // No specular highlights
        material.setSpecularPower(0);  // Disable specular
        cylinder.setMaterial(material);

        // Position and rotate the cylinder to enclose both points
        // First, position at midpoint
        Translate moveToMid = new Translate(midX, midY, midZ);

        // Determine rotation to align with points
        javafx.geometry.Point3D yAxis = new javafx.geometry.Point3D(0, 1, 0);
        javafx.geometry.Point3D direction = new javafx.geometry.Point3D(dx, dy, dz).normalize();

        // Calculate rotation axis and angle
        javafx.geometry.Point3D rotationAxis = yAxis.crossProduct(direction);
        double angle = Math.toDegrees(Math.acos(yAxis.dotProduct(direction)));

        // Create rotation transform (handle parallel vectors case)
        Rotate alignWithLine;
        if (rotationAxis.magnitude() < 1e-10) {
            alignWithLine = new Rotate(direction.getY() < 0 ? 180 : 0, Rotate.X_AXIS);
        } else {
            alignWithLine = new Rotate(angle, rotationAxis);
        }

        cylinder.getTransforms().addAll(moveToMid, alignWithLine);

        // Add depth-test options for better rendering of transparent objects
        cylinder.setDepthTest(DepthTest.ENABLE);

        group.getChildren().add(cylinder);
    }

    public Cylinder makeLineBetween(Point3D start, Point3D end, double radius, Color color) {
        double dx = end.x() - start.x();
        double dy = end.y() - start.y();
        double dz = end.z() - start.z();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        Cylinder cylinder = new Cylinder(radius, length);
        PhongMaterial lineMaterial = new PhongMaterial(color);
        lineMaterial.setSpecularPower(0); // Disable specular for flat appearance
        cylinder.setMaterial(lineMaterial);

        // First, position the cylinder at the start point
        Translate moveToStart = new Translate(start.x(), start.y(), start.z());

        // The cylinder is initially aligned with the Y axis, so we need to rotate it to align with our line
        javafx.geometry.Point3D yAxis = new javafx.geometry.Point3D(0, 1, 0);
        javafx.geometry.Point3D direction = new javafx.geometry.Point3D(dx, dy, dz).normalize();

        // Calculate the rotation axis and angle
        javafx.geometry.Point3D rotationAxis = yAxis.crossProduct(direction);
        double angle = Math.toDegrees(Math.acos(yAxis.dotProduct(direction)));

        // Create the rotation transform (check for parallel vectors where cross product would be zero)
        Rotate alignWithLine;
        if (rotationAxis.magnitude() < 1e-10) {
            // If vectors are parallel or anti-parallel, use another axis for rotation
            alignWithLine = new Rotate(direction.getY() < 0 ? 180 : 0, Rotate.X_AXIS);
        } else {
            alignWithLine = new Rotate(angle, rotationAxis);
        }

        // Move the center of the cylinder to match the start point, with half its length extending along the direction vector
        Translate moveToCenter = new Translate(0, length/2, 0);

        // Apply transforms in the correct order
        cylinder.getTransforms().addAll(moveToStart, alignWithLine, moveToCenter);

        return cylinder;
    }

    /**
     * Update the points for 2D visualization
     */
    public void setLatest2DPoints(List<Point2D> points) {
        this.latest2DPoints = points;
        // Clear algorithm results
        highlight2DA = null;
        highlight2DB = null;
        // Redraw if needed
        draw2DPoints();
    }

    /**
     * Update the points for 3D visualization
     */
    public void setLatest3DPoints(List<Point3D> points) {
        this.latest3DPoints = points;
        // Clear algorithm results
        highlight3DA = null;
        highlight3DB = null;
        // Redraw if needed
        draw3DPoints();
    }

    /**
     * Compute and visualize the convex hull if QuickHull is selected
     */
    private void computeAndVisualizeHull() {
        if (!shouldShowHull || currentAlgorithmType == null ||
            !currentAlgorithmType.toString().contains("QUICKHULL")) {
            return;
        }

        if (dimensionCombo.getValue().equals("2D") && latest2DPoints != null && !latest2DPoints.isEmpty()) {
            // Compute the hull using QuickHull2D
            QuickHull2D quickHull = new QuickHull2D(false);
            hull2DPoints = quickHull.buildHull(new ArrayList<>(latest2DPoints));
        }
        else if (dimensionCombo.getValue().equals("3D") && latest3DPoints != null && !latest3DPoints.isEmpty()) {
            model.algorithm.quickhull.QuickHull3D quickHull;
            
            // For QuickHull diameter algorithm, reuse the existing hull to ensure diameter points are on hull
            if (currentAlgorithmType == AlgorithmType.DIAMETER_QUICKHULL && 
                model.algorithm.diameter.DiameterQuickHull3D.lastUsedQuickHull != null) {
                outputArea.appendText("Using same hull as diameter calculation to ensure consistency.\n");
                
                // Reuse the hull points from the algorithm result
                quickHull = model.algorithm.diameter.DiameterQuickHull3D.lastUsedQuickHull;
                hull3DPoints = quickHull.buildHull(latest3DPoints); // The hull is already built
            } else {
                // Use a new QuickHull3D implementation otherwise
                quickHull = new model.algorithm.quickhull.QuickHull3D(false);
                
                // Build the hull
                hull3DPoints = quickHull.buildHull(latest3DPoints);
            }
            
            // Get the faces for visualization
            hullFaces = quickHull.getFaces();
            
            // Verify face data for debugging
            if (hullFaces == null || hullFaces.length == 0) {
                outputArea.appendText("Warning: Could not retrieve hull faces.\n");
            } else {
                outputArea.appendText(String.format("Hull contains %d faces and %d vertices.\n", 
                                                  hullFaces.length, hull3DPoints.size()));
            }
        }

        // Update visualization
        updateHullVisualization();
    }

    // Store hull faces for 3D visualization
    private int[][] hullFaces;

    //-------------------------------------------------------------------------
    // Implementació de UINotificationHandler
    //-------------------------------------------------------------------------

    /**
     * Gestiona l'inici de la generació de punts.
     * <p>
     * Aquest mètode és cridat pel controlador quan comença la generació
     * de punts. Actualitza la interfície d'usuari i intenta generar
     * una visualització preliminar dels punts.
     * </p>
     * 
     * @param numPoints nombre de punts a generar
     * @param distribution tipus de distribució estadística
     */
    @Override
    public void onPointGenerationStarted(int numPoints, String distribution) {
        outputArea.appendText("Generant " + numPoints + " punts amb distribució " + distribution + "\n");
        progressBar.setProgress(0.1);

        if (dimensionCombo.getValue().equals("2D")) {
            // Per a 2D, sol·licita anticipadament punts per a la visualització
            try {
                PointGenerator.Distribution dist = PointGenerator.Distribution.valueOf(distribution);
                int bound = Integer.parseInt(boundField.getText().trim());
                latest2DPoints = controller.generatePointsFor2DVisualization(dist, numPoints, bound);
                draw2DPoints();
            } catch (Exception ex) {
                // Ignora, simplement no actualitza la visualització
            }
        } else {
            // Per a 3D, sol·licita anticipadament punts per a la visualització
            try {
                PointGenerator.Distribution dist = PointGenerator.Distribution.valueOf(distribution);
                int bound = Integer.parseInt(boundField.getText().trim());
                latest3DPoints = PointGenerator.generate3DPoints(numPoints, dist, bound, bound, bound);
                draw3DPoints();
            } catch (Exception ex) {
                // Ignora, simplement no actualitza la visualització
            }
        }
    }

    /**
     * Gestiona la finalització de la generació de punts.
     * <p>
     * Aquest mètode és cridat pel controlador quan s'ha completat la generació
     * de punts. Actualitza la barra de progrés per indicar-ho.
     * </p>
     */
    @Override
    public void onPointGenerationCompleted() {
        progressBar.setProgress(0.2);
    }

    /**
     * Gestiona l'inici de l'execució d'un algoritme.
     * <p>
     * Aquest mètode és cridat pel controlador quan comença l'execució d'un
     * algoritme. Actualitza la interfície d'usuari i prepara la visualització
     * per mostrar els resultats.
     * </p>
     * 
     * @param type tipus d'algoritme que s'executarà
     * @param numPoints nombre de punts que processarà l'algoritme
     */
    @Override
    public void onAlgorithmStarted(AlgorithmType type, int numPoints) {
        String typeName = type.getDisplayName();
        outputArea.appendText("Executant " + typeName + " amb " + numPoints + " punts\n");
        progressBar.setProgress(0.3);
        currentAlgorithmType = type;

        // Neteja les dades d'envolupant anteriors
        hull2DPoints = null;
        hull3DPoints = null;
    }

    /**
     * Gestiona les actualitzacions de progrés durant l'execució d'un algoritme.
     * <p>
     * Aquest mètode és cridat pel controlador amb actualitzacions periòdiques
     * del progrés de l'algoritme. Actualitza la barra de progrés per mostrar
     * l'estat actual.
     * </p>
     * 
     * @param type tipus d'algoritme en execució
     * @param progress valor del progrés entre 0.0 i 1.0
     */
    @Override
    public void onAlgorithmProgress(AlgorithmType type, double progress) {
        progressBar.setProgress(0.3 + progress * 0.6);
    }

    /**
     * Gestiona la finalització de l'execució d'un algoritme.
     * <p>
     * Aquest mètode és cridat pel controlador quan un algoritme ha completat
     * la seva execució. Mostra els resultats a la interfície d'usuari i
     * actualitza la visualització per mostrar els punts resultat.
     * </p>
     * 
     * @param result resultat de l'execució de l'algoritme
     */
    @Override
    public void onAlgorithmCompleted(AlgorithmResult<?> result) {
        progressBar.setProgress(1.0);
        String resultType = result.isClosestPair() ? "Parell més proper" : "Diàmetre";

        outputArea.appendText(String.format(
                "%s: %s - %s (Distància: %.2f) [%d ms]\n",
                resultType, result.getPoint1(), result.getPoint2(),
                result.getDistance(), result.getExecutionTimeMs()
        ));

        // Habilita el botó d'enfocament
        focusButton.setDisable(false);

        // Gestiona la visualització segons la dimensió
        if (dimensionCombo.getValue().equals("2D") && latest2DPoints != null) {
            if (result.getPoint1() instanceof Point2D p1 && result.getPoint2() instanceof Point2D p2) {
                highlight2DA = p1;
                highlight2DB = p2;
                draw2DPoints();
                centerBetweenPoints(p1, p2);
            }
        } else if (dimensionCombo.getValue().equals("3D") && latest3DPoints != null) {
            if (result.getPoint1() instanceof Point3D p1 && result.getPoint2() instanceof Point3D p2) {
                highlight3DA = p1;
                highlight3DB = p2;
                draw3DPoints();
            }
        }

        // Si és un algoritme QuickHull, calcula i visualitza l'envolupant
        if (currentAlgorithmType != null &&
            currentAlgorithmType.toString().contains("QUICKHULL") && shouldShowHull) {
            computeAndVisualizeHull();

            // Mostra informació addicional sobre l'envolupant i els punts resultat
            if (dimensionCombo.getValue().equals("3D") &&
                highlight3DA != null && highlight3DB != null &&
                hull3DPoints != null && !hull3DPoints.isEmpty()) {

                // Comprova si els punts resultat estan a l'envolupant - utilitza una igualtat aproximada amb un petit epsilon
                // per a la comparació de punt flotant per tenir en compte possibles diferències de precisió
                final double EPSILON = 0.0001;
                boolean p1OnHull = hull3DPoints.stream().anyMatch(p ->
                    Math.abs(p.x() - highlight3DA.x()) < EPSILON && 
                    Math.abs(p.y() - highlight3DA.y()) < EPSILON && 
                    Math.abs(p.z() - highlight3DA.z()) < EPSILON);
                boolean p2OnHull = hull3DPoints.stream().anyMatch(p ->
                    Math.abs(p.x() - highlight3DB.x()) < EPSILON && 
                    Math.abs(p.y() - highlight3DB.y()) < EPSILON && 
                    Math.abs(p.z() - highlight3DB.z()) < EPSILON);

                outputArea.appendText("\nPunts resultat en relació amb l'envolupant:\n");
                outputArea.appendText(String.format("- Punt 1 (%s): %s l'envolupant\n",
                                                   highlight3DA, p1OnHull ? "A" : "DINS DE"));
                outputArea.appendText(String.format("- Punt 2 (%s): %s l'envolupant\n",
                                                   highlight3DB, p2OnHull ? "A" : "DINS DE"));
                outputArea.appendText(String.format("- L'envolupant té %d vèrtexs\n", hull3DPoints.size()));

                // Si ambdós punts estan a l'envolupant, proporciona informació addicional
                if (result.isClosestPair() && (p1OnHull || p2OnHull)) {
                    outputArea.appendText("Nota: Un o ambdós punts del parell més proper són a l'envolupant.\n" +
                                         "Això és inusual però pot ocórrer en distribucions de punts específiques.\n");
                }
            }
        }
    }

    /**
     * Gestiona els errors durant l'execució d'un algoritme.
     * <p>
     * Aquest mètode és cridat pel controlador quan es produeix un error
     * durant l'execució d'un algoritme. Mostra el missatge d'error a
     * l'usuari i restableix la barra de progrés.
     * </p>
     * 
     * @param errorMessage missatge d'error a mostrar
     */
    @Override
    public void onComputationError(String errorMessage) {
        outputArea.appendText("Error: " + errorMessage + "\n");
        progressBar.setProgress(0);
    }
}