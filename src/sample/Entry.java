package sample;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;


@SuppressWarnings("restriction")
public class Entry extends Application {

    //create window, add checkerboard, start game loop
    public void start(Stage stage) {
        CheckerBoard board = CheckerBoard.createCheckerBoard();
        Scene scene = new Scene(board, 500, 500);
        board.bindToParent();
        stage.setScene(scene);
        board.setGridLinesVisible(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
