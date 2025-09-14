package controller;

import javafx.application.Application;
import javafx.stage.Stage;
import notification.NotificationService;
import notification.NotificationServiceImpl;
import view.MainView;

/**
 * Classe principal de l'aplicació que configura l'arquitectura MVC i la injecció de dependències.
 */
public class PointCloudApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Crea la vista principal
        MainView mainView = new MainView();

        // Crea el servei de notificacions amb la vista com a gestor d'interfície d'usuari
        NotificationService notificationService = new NotificationServiceImpl(mainView);

        // Crea el controlador amb les dependències injectades
        MainController controller = new MainController(notificationService);

        // Assigna el controlador a la vista
        mainView.setController(controller);

        // Inicialitza i mostra la vista
        mainView.initialize(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
