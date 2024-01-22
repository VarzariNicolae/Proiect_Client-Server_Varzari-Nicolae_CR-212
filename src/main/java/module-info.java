module com.example.studiu {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.example.studiu to javafx.fxml;
    exports com.example.studiu;

}