package com.example.studiu;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.List;

public class client extends Application {
    private ObjectOutputStream outputStream;
    private String fontSize = "12px";
    private String textColor = "#000000";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Se realizează conexiunea către server
            Socket socket = new Socket("localhost", 12345);
            outputStream = new ObjectOutputStream(socket.getOutputStream());

            // Se obțin informații despre sistemul de operare și utilizator
            String osName = System.getProperty("os.name");
            String userId = System.getProperty("user.name");
            String clientIdentifier = osName + "-" + userId + " Port: " + socket.getLocalPort();

            // Se configurează fereastra principală
            primaryStage.setTitle("Client " + clientIdentifier + " EDITOR TEXT");

            // Se creează structura de bază a interfeței grafice
            BorderPane root = new BorderPane();
            root.setCenter(createTextAreaContainer(socket));

            Scene scene = new Scene(root, 800, 600);
            primaryStage.setScene(scene);

            // Adăugăm un gestionar pentru închiderea ferestrei
            primaryStage.setOnCloseRequest(event -> {
                // Închidem resursele corespunzătoare
                try {
                    outputStream.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Ieșim din aplicație
                Platform.exit();
                System.exit(0);
            });

            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metoda pentru crearea containerului cu TextArea și controale asociate
    private VBox createTextAreaContainer(Socket socket) {
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-size: " + fontSize + "; -fx-text-fill: " + textColor + ";");
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);

        // Adăugăm un ascultător pentru modificările din TextArea și trimitem textul la server
        textArea.textProperty().addListener((observable, oldValue, newValue) -> sendTextToServer(newValue));

        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setOnAction(event -> {
            textColor = "#" + colorPicker.getValue().toString().substring(2, 8);
            textArea.setStyle("-fx-font-size: " + fontSize + "; -fx-text-fill: " + textColor + ";");
        });

        Button fontSizeButton = getButton(textArea);

        Button exportButton = getExportButton(textArea);

        Button selectFilesButton = getSelectFilesButton(textArea);

        VBox controlsVBox = new VBox(10, colorPicker, fontSizeButton, exportButton, selectFilesButton);
        controlsVBox.setAlignment(Pos.CENTER);

        VBox textAreaVBox = new VBox(10, textArea);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        HBox rootHBox = new HBox(10, controlsVBox, textAreaVBox);
        rootHBox.setAlignment(Pos.CENTER);

        VBox textAreaContainer = new VBox(rootHBox);
        VBox.setVgrow(rootHBox, Priority.ALWAYS);

        // Pornim un fir de execuție pentru gestionarea actualizărilor primite de la server
        Thread clientThread = new Thread(new ClientHandler(socket, textArea));
        clientThread.start();

        return textAreaContainer;
    }

    // Metoda pentru crearea butonului de selectare a fișierelor
    private Button getSelectFilesButton(TextArea textArea) {
        Button selectFilesButton = new Button("Selectează documente");
        selectFilesButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            List<File> selectedFiles = fileChooser.showOpenMultipleDialog(null);

            if (selectedFiles != null) {
                // Procesăm fiecare fișier selectat
                for (File file : selectedFiles) {
                    editFile(file, textArea);
                }
            }
        });
        return selectFilesButton;
    }

    // Metoda pentru editarea conținutului TextArea cu textul dintr-un fișier
    private void editFile(File file, TextArea textArea) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            textArea.setText(content);
            sendTextToServer(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metoda pentru crearea butonului de export
    private Button getExportButton(TextArea textArea) {
        Button exportButton = new Button("Exportează în document .txt");
        exportButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
            File file = fileChooser.showSaveDialog(null);

            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(textArea.getText());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return exportButton;
    }

    // Metoda pentru crearea butonului de schimbare a dimensiunii fontului
    private Button getButton(TextArea textArea) {
        Button fontSizeButton = new Button("Dimensiunea fontului");
        fontSizeButton.setOnAction(event -> {
            String newSize = FontSizeInputDialog.showDialog();
            if (newSize != null) {
                fontSize = newSize;
                textArea.setStyle("-fx-font-size: " + fontSize + "; -fx-text-fill: " + textColor + ";");
            }
        });
        return fontSizeButton;
    }

    // Metoda pentru trimiterea textului la server
    private void sendTextToServer(String newText) {
        try {
            outputStream.writeObject(newText);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clasa internă pentru gestionarea actualizărilor primite de la server
    private static class FontSizeInputDialog {
        public static String showDialog() {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Seteaza dimensiunea fontului");

            Label label = new Label("Introduceti dimensiunea fontului:");
            TextField fontSizeField = new TextField();

            // Configurăm aspectul dialogului și adăugăm acțiunile pentru butoane
            VBox layout = getVBox(dialog, fontSizeField, label);

            dialog.getDialogPane().setContent(layout);

            // Focus pe câmpul de text
            Platform.runLater(fontSizeField::requestFocus);

            dialog.showAndWait();

            return dialog.getResult();
        }

        // Metoda pentru crearea aspectului dialogului și butoanelor
        private static VBox getVBox(Dialog<String> dialog, TextField fontSizeField, Label label) {
            Button okButton = new Button("OK");
            okButton.setOnAction(event -> {
                dialog.setResult(fontSizeField.getText());
                dialog.close();
            });

            Button cancelButton = new Button("Refuza");
            cancelButton.setOnAction(event -> dialog.close());

            HBox buttons = new HBox(10, okButton, cancelButton);
            buttons.setAlignment(Pos.CENTER);

            return new VBox(10, label, fontSizeField, buttons);
        }
    }

    // Clasa internă pentru gestionarea actualizărilor primite de la server
    private record ClientHandler(Socket socket, TextArea textArea) implements Runnable {
        @Override
        public void run() {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    // Primim textul actualizat de la server
                    String updatedText = (String) inputStream.readObject();
                    int caretPosition = textArea.getCaretPosition();

                    int anchor = textArea.getSelection().getEnd();

                    // Actualizăm textul din TextArea
                    textArea.setText(updatedText);

                    // Restabilim poziția cursorului și selecția
                    textArea.positionCaret(Math.min(caretPosition, updatedText.length()));
                    if (anchor >= 0) {
                        textArea.selectRange(anchor, caretPosition);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
