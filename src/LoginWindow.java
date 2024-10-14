import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class LoginWindow extends Application {
    static Socket socket;
    static DataOutputStream out;
    static DataInputStream in;

    String receiver = " ";
    String text = " ";
    int trials = 0;
    Client chat;
    static String serverIP;
    static int port;

    boolean access = false;

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;

    /// ComboBox requires the data type of its child items

    @FXML
    private Button btnLogin;

    Stage stage;

    public String Username, password;

    ArrayList<String> userData = new ArrayList<String>();

    public LoginWindow() throws IOException {
    }

    public static void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LoginWindow.fxml"));
        loader.setController(this);
        Parent root = loader.load();

        Scene scene = new Scene(root, 300, 280);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Zip Chat");
        // primaryStage.initModality(Modality.APPLICATION_MODAL);
        // primaryStage.setResizable(false);
        primaryStage.show();
        // primaryStage.setOnCloseRequest(event -> {
        // System.exit(0);
        // });
        primaryStage.setOnCloseRequest(event ->{
            System.exit(0);
        });
    }

    @FXML
    protected void initialize() {
        btnLogin.setOnMouseClicked(event -> {
            Username = txtUsername.getText();
            password = txtPassword.getText();
            boolean authorized = false;
            try {
                authorized = userAuthorized();
                if(authorized){
                    chat = new Client(Username, in, out, socket, serverIP, port);
                    if(chat.isCancelled == false){
                        print("username: %s\n", chat.username);
                    }else{
                        print("Signed Out");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            stage.close();
        });

        // if(access == true){
        //     Client chat = new Client();
        // }
    }

    public Boolean userAuthorized(DataInputStream in) throws IOException {

        String str = "";
        str = receiveString(in);
        return !str.equals("User not authorized");
    }

    public void sendString(String string, DataOutputStream out) throws IOException {
        int len = string.length();
        out.writeInt(len);
        out.write(string.getBytes(), 0, len);
        out.flush();
    }

    public String receiveString(DataInputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        String str = "";
        int len = in.readInt();
        while (len > 0) {
            int l = in.read(buffer, 0, Math.min(len, buffer.length));
            str += new String(buffer, 0, l);
            len -= l;
        }
        return str;
    }

    public boolean userAuthorized() throws IOException {
        try {
            trials += 1;
            sendString(Username, out);
            sendString(password, out);
            access = userAuthorized(in);
            txtPassword.clear();
            txtUsername.clear();
            if (access == false) {
                print("Login failed");
            }
            if (!(trials < 3)) {
                print("Cannot Login, too many times attempted");
                System.exit(0);
            }
            if (trials < 3 & access == true) {
                print("success");
                return access;
            }

        } catch (IOException ex) {
            print("User not authorized\n");
        }
        return access;
    }

    public static void main(String[] args) throws UnknownHostException, IOException {

        serverIP = args[0];
        port = Integer.parseInt(args[1]);

        socket = new Socket(serverIP, port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        launch(args);
    }

}
