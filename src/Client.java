import java.io.*;
import java.net.Socket;
import java.util.Scanner;

import javax.sound.midi.Receiver;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Box;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;

public class Client {
    Socket socket;
    DataOutputStream out;
    DataInputStream in;
    // public static String[] args;

    ObservableList<Node> children;
    int msgIndex = 0;

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private ScrollPane scrollUsersPane;
    @FXML
    private TextField txtInput;
    @FXML
    private TextField gNameText;
    @FXML
    private TextField gMembersText;
    @FXML
    private Button gCreate;
    @FXML
    private VBox messagePane;
    @FXML
    private VBox usersPanel;
    @FXML
    private Label receiverName;
    @FXML
    private Label userName;
    @FXML
    private Button fileChoose;

    @FXML
    private Button sendBtn;

    @FXML
    private Button emojiBtn;

    @FXML private Button SignOut;

    Stage stage;

    Label usersList[];

    String receiver = " ";
    String username; // change later hard coded for now
    String random = "okayyyy";
    String text = " ";
    String serverIP;
    int port;
    File selectedFile = new File("");
    Boolean isCancelled = false;
    
    ArrayList<String> userData = new ArrayList<String>();

    public Client(String user, DataInputStream inLogin, DataOutputStream outLogin, Socket socketLogin, String IP,
            int Port)
            throws IOException {
        serverIP = IP;
        port = Port;
        in = inLogin;
        out = outLogin;
        socket = socketLogin;
        username = user;

        stage = new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ChatWindow.fxml"));
        loader.setController(this);
        Parent root = loader.load();
        stage.setScene(new Scene(root,600,400));
        stage.setTitle("ZipChat");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.show();

        addToUserPanel();

        stage.setOnCloseRequest(event ->{
            System.exit(0);
        });
    }

    @FXML
    protected void initialize() throws FileNotFoundException {
        userName.setText("Logged in as " + username);

        children = messagePane.getChildren();

        SignOut.setOnMouseClicked(event->{
            isCancelled = true;
            stage.close();
        });

        fileChoose.setOnAction(event -> {
            FileChooser f = new FileChooser();
            selectedFile = f.showOpenDialog(null);
            try {
                sendFileMsg(username, receiver, selectedFile, out);
                children.add(messageNode("File Sent!", true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        messagePane.heightProperty().addListener(event -> {
            scrollPane.setVvalue(1);
        });

        usersPanel.heightProperty().addListener(event -> {
            scrollUsersPane.setVvalue(1);
        });

        txtInput.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("ENTER"))
                displayMessage();
        });

        sendBtn.setOnMouseClicked(event -> displayMessage());

        emojiBtn.setOnMouseClicked(event -> displayEmoji());

        gCreate.setOnMouseClicked(event -> createGroup());

        usersPanel.setOnMouseClicked(event -> {
            if (usersList != null) {
                for (Label each : usersList) {
                    each.setOnMouseClicked(eventTwo -> {
                        receiver = (String) each.getUserData();
                        receiverName.setText("To: " + receiver);
                        children.clear();
                        String history = "";
                        boolean groupMsg = false;
                        try {
                            BufferedReader readGroupFile = new BufferedReader(new FileReader("groupList.txt"));
                            String eachGroup;
                            while ((eachGroup = readGroupFile.readLine()) != null) {
                                String[] groupName = eachGroup.split(":");
                                if (groupName[0].equals(receiver))
                                    groupMsg = true;
                            }
                            readGroupFile.close();
                            if (groupMsg) {
                                history = readGroupText(receiver);
                                if (!history.equals("")) {
                                    String eachMessage[] = history.split("\n");
                                    for (String message : eachMessage) {
                                        String[] detail = message.split(":");
                                        if (detail[0].equals(username)) {
                                            children.add(messageNode(detail[1], true));
                                        } else {
                                            children.add(messageNode(detail[1], false));
                                        }
                                    }
                                }

                            } else {
                                history = readText(username, receiver);
                                if (!history.equals("")){
                                    String eachMessage[] = history.split("\n");
                                    for (String message : eachMessage) {
                                        String[] detail = message.split(":",2);
                                        if (detail[0].equals(username)) {
                                            if(detail[1].contains(".jpg") || detail[1].contains(".jpeg") || detail[1].contains(".png"))
                                                children.add(imageNode(detail[1], true));
                                            else
                                                children.add(messageNode(detail[1], true));
                                        } else if (detail[0].equals(receiver)) {
                                            if(detail[1].contains(".jpg") || detail[1].contains(".jpeg") || detail[1].contains(".png"))
                                                children.add(imageNode(detail[1], false));
                                            else if (!detail[1].contains("C:")) {
                                                children.add(messageNode(detail[1], false));
                                            } else {
                                                children.add(messageNode("File Sent!", false));
                                            }
                                        }
                                    }
                                }

                            }


                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    });
                }
                ;
            }
        });

        try {
            Clientt(serverIP, port);
            System.out.println(out);
        } catch (IOException e) {
        }


    }

    private void displayEmoji() {
        Platform.runLater(() -> {
            try {
                File file = new File("emoji.png");
                children.add(imageNode("emoji.png", true));
                System.out.println(out);
                sendFileMsg(username, receiver, file, out);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private void displayMessage() {
        Platform.runLater(() -> {
            text = txtInput.getText();
            txtInput.clear();
            children.add(messageNode(text, true));
            boolean groupMsg = false;
            try {
                BufferedReader readGroupFile = new BufferedReader(new FileReader("groupList.txt"));
                String eachGroup;
                while ((eachGroup = readGroupFile.readLine()) != null) {
                    String[] groupName = eachGroup.split(":");
                    if (groupName[0].equals(receiver))
                        groupMsg = true;
                }
                readGroupFile.close();
                if (groupMsg) {
                    sendGroupMsg(username,receiver,text,out);
                    writeGroupText(username,receiver,text);
                }

                else {
                    sendTextMsg(username, receiver, text, out);
                    writeText(username, receiver, text);
                }



            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private Node downloadNode(String sender, String text, boolean alignToRight) {
        HBox box = new HBox();
        box.paddingProperty().setValue(new Insets(10, 10, 10, 10));

        if (alignToRight)
            box.setAlignment(Pos.BASELINE_RIGHT);
        javafx.scene.control.Button downloadFile = new Button(text);
        downloadFile.setWrapText(true);
        downloadFile.setOnAction(event -> {
            try {
                String splited[] = text.split("-");
                clickDownload(sender, username, splited[1], out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        box.getChildren().add(downloadFile);
        return box;
    }

    public void addToUserPanel() {
        try {
            userData.clear();
            BufferedReader readUserFile = new BufferedReader(new FileReader("userList.txt"));
            BufferedReader readGroupFile = new BufferedReader(new FileReader("groupList.txt"));
            String eachUser;
            while ((eachUser = readUserFile.readLine()) != null) {
                if (!eachUser.equals(username))
                    userData.add(eachUser);
            }
            String eachGroup;
            while ((eachGroup = readGroupFile.readLine()) != null) {
                String[] groupName = eachGroup.split(":");
                userData.add(groupName[0]);
            }
            readUserFile.close();
            readGroupFile.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        usersList = new Label[userData.size()];

        for (int i = 0; i < userData.size(); i++) {
            usersList[i] = new Label(userData.get(i));
            usersList[i].setUserData(userData.get(i));
            usersList[i].setMinHeight(50);
            usersList[i].setMinWidth(90);
            usersList[i].setAlignment(Pos.CENTER);
            usersList[i].setStyle("-fx-border-color: black;");
            HBox box = new HBox();
            box.paddingProperty().setValue(new Insets(5, 5, 5, 5));
            usersList[i].setWrapText(true);
            box.getChildren().add(usersList[i]);
            usersPanel.getChildren().add(box);
        }
    }


    private Node messageNode(String text, boolean alignToRight) {
        HBox box = new HBox();
        box.paddingProperty().setValue(new Insets(10, 10, 10, 10));

        if (alignToRight)
            box.setAlignment(Pos.BASELINE_RIGHT);
        javafx.scene.control.Label label = new Label(text);
        label.setWrapText(true);
        box.getChildren().add(label);
        return box;
    }

    private Node imageNode(String imagePath, boolean alignToRight) {
        try {
            HBox box = new HBox();
            box.paddingProperty().setValue(new Insets(10, 10, 10, 10));

            if (alignToRight)
                box.setAlignment(Pos.BASELINE_RIGHT);
            FileInputStream in = new FileInputStream(imagePath);
            ImageView imageView = new ImageView(new Image(in));
            imageView.setFitWidth(100);
            imageView.setPreserveRatio(true);
            box.getChildren().add(imageView);
            return box;
        } catch (IOException ex) {
            ex.printStackTrace();
            return messageNode("!!! Fail to display an image !!!", alignToRight);
        }
    }

    private void createGroup() {
        Platform.runLater(() -> {
            String groupName = "";
            groupName = gNameText.getText();
            gNameText.clear();
            String groupMembers = "";
            groupMembers = gMembersText.getText();
            gMembersText.clear();
            if (!groupName.equals("") && !groupMembers.equals("")) {
                try {
                    createGroup(username,groupName,groupMembers,out);
                    String groupInfo = groupName + ":" + groupMembers;
                    addToGroupList(groupInfo);
                    usersPanel.getChildren().clear();
                    addToUserPanel();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void print(String str, Object... o) {
        System.out.printf(str, o);
    }

    public static void print(String str) {
        System.out.print(str);
    }

    public void setname(String name) {
        username = name;
    }

    public void Clientt(String serverIP, int port) throws IOException {

        print("Connecting to %s:%d\n", serverIP, port);

        Thread t1 = new Thread(() -> {
            while (true) {
                try {
                    int msgType = in.readInt();
                    if (msgType == 1) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        String incoming = receiveIndividualMsg(in);
                        String[] incomingMsg = incoming.split(":");
                        //todo
                        String sender = incomingMsg[0];
                        writeText(sender,username,incomingMsg[1]);
                        if (receiver.equals(sender)) {
                            Platform.runLater(() -> {
                            children.clear();
                                try {
                                    String history = "";
                                    history = readText(username, receiver);
                                    String eachMessage[] = history.split("\n");
                                    for (String message : eachMessage) {
                                        String[] detail = message.split(":",2);
                                        if (!detail[1].equals("")) {

                                            if (detail[0].equals(username)) {
                                                if (detail[1].contains("Download-")) {
                                                    children.add(downloadNode(sender, detail[1], true));
                                                } else if (detail[1].contains(".jpg")||detail[1].contains(".jpeg") || detail[1].contains(".png")) {
                                                    children.add(imageNode(detail[1], true));
                                                } else {
                                                    if (!detail[1].contains("\\")) {
                                                        children.add(messageNode(detail[1], true));
                                                    } else {
                                                        children.add(messageNode("File Sent!", true));
                                                    }

                                                }


                                            } else if (detail[0].equals(receiver)) {
                                                if (detail[1].contains("Download-")) {
                                                    children.add(downloadNode(sender, detail[1], false));
                                                }
                                                else if (detail[1].contains(".jpg")||detail[1].contains(".jpeg") || detail[1].contains(".png")) {
                                                    children.add(imageNode(detail[1],false));
                                                } else {
                                                    if (!detail[1].contains("C:")) {
                                                        children.add(messageNode(detail[1], false));
                                                    } else {
                                                        children.add(messageNode("File Sent!", false));
                                                    }
                                                }
                                            }

                                        }
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }

                            });
                        }

                    } else if (msgType == 2) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        String incoming = receiveGroupMsg(in);
                        String[] incomingMsg = incoming.split(":");
                        String sender = incomingMsg[0];
                        String group = incomingMsg[1];
                        writeGroupText(sender,group,incomingMsg[2]);
                        if (receiver.equals(group)) {
                            Platform.runLater(() -> {
                                children.clear();
                                try {
                                    String history = "";
                                    history = readGroupText(group);
                                    System.out.println(history);
                                    String eachMessage[] = history.split("\n");
                                    for (String message : eachMessage) {
                                        String[] detail = message.split(":");
                                        if (!detail[1].equals("")) {
                                            if (detail[0].equals(username)) {
                                                if (detail[1].contains("Download-"))
                                                    children.add(downloadNode(sender, detail[1], true));
                                                else
                                                    children.add(messageNode(detail[1], true));
                                            } else if (!detail[0].equals(username)) {
                                                if (detail[1].contains("Download-"))
                                                    children.add(downloadNode(sender, detail[1], false));
                                                else
                                                    children.add(messageNode(detail[1], false));
                                            }

                                        }
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }

                            });
                        }
                    } else if (msgType == 3) {
                        String targetReceiver = receiver;
                        String sender = receiveString(in);
                        String receiver = receiveString(in);
                        String filename = receiveString(in);
                        receiveFile(sender, receiver, filename, in);
                        writeText(sender,receiver,filename);
                        if (sender.equals(targetReceiver)) {
                            if(filename.contains(".jpg") || filename.contains(".jpeg") || filename.contains(".png")) {
                                Platform.runLater(() -> {
                                    children.add(imageNode(filename, false));
                                });
                            }
                        }
                    } else if (msgType == 4) {
                        String groupInfo = receiveString(in);
                        addToGroupList(groupInfo);
                        Platform.runLater(() -> {
                        usersPanel.getChildren().clear();
                        addToUserPanel();
                        });
//                        usersPanel.getChildren().clear();
//                        addToUserPanel();
                    }
                } catch (IOException ex) {
                    print("Connection drop!");
                }
            }
        });

        t1.start();

    }

    // for sending username info
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

    // for sending conversational msg
    public void sendTextMsg(String sender, String receiver, String msg, DataOutputStream out) throws IOException {
        out.writeInt(1);
        sendString(sender, out);
        sendString(receiver, out);
        sendString(msg, out);
    }

    public String receiveIndividualMsg(DataInputStream in) throws IOException {
        String sender = receiveString(in);
        String receiver = receiveString(in);
        String msg = receiveString(in);
        return sender + ":" + msg;

    }

    // send group message
    public void sendGroupMsg(String sender, String group, String msg, DataOutputStream out) throws IOException {
        out.writeInt(2);
        sendString(sender,out);
        sendString(group, out);
        sendString(msg, out);
    }

    public String receiveGroupMsg(DataInputStream in) throws IOException {
        String sender = receiveString(in);
        String group = receiveString(in);
        String msg = receiveString(in);
        return sender + ":" + group + ":" + msg;
    }

    // create group
    public void createGroup(String sender, String group, String members, DataOutputStream out) throws IOException {
        out.writeInt(4);
        sendString(sender, out);
        sendString(group, out);
        sendString(members, out);
    }

    public void clickDownload(String sender, String uname, String filename, DataOutputStream out) throws IOException {
        out.writeInt(5);
        sendString(sender, out);
        sendString(uname, out);
        sendString(filename, out);
    }

    // for sending files (IMAGES, VIDEOS)
    public void sendFileMsg(String sender, String receiver, File file, DataOutputStream out) throws IOException {
        FileInputStream in = new FileInputStream(file);
        out.writeInt(3);
        sendString(sender, out);
        sendString(receiver, out);

        byte[] filename = file.getName().getBytes();
        out.writeInt(filename.length);
        out.write(filename, 0, filename.length);

        long size = file.length();
        out.writeLong(size);

        byte[] buffer = new byte[1024];
        while (size > 0) {
            int len = in.read(buffer, 0, (int) Math.min(size, buffer.length));
            out.write(buffer, 0, len);
            size -= len;
        }

        in.close();
        out.flush();
        if(file.getName().equals("emoji.png")){
            writeText(sender, receiver, file.getName());
        } else if(selectedFile.getName().contains(".jpg") || selectedFile.getName().contains(".jpeg") || selectedFile.getName().contains(".png")) {
            Platform.runLater(() -> {
            children.add(imageNode(selectedFile.getPath(), true));
         });
            writeText(sender, receiver, selectedFile.getPath());
        }
        else{
            writeText(sender, receiver, selectedFile.getPath());
        }
    }

    public void receiveFile(String sender, String receiver, String filename, DataInputStream in) throws IOException {
        byte[] buffer = new byte[1024];

        File file = new File(filename);
        FileOutputStream fout = new FileOutputStream(file);

        long size = in.readLong();

        while (size > 0) {
            int len = in.read(buffer, 0, (int) Math.min(size, buffer.length));
            fout.write(buffer, 0, len);
            size -= len;
        }

        print("Download completed\n");
        print(filename);
        fout.flush();
        fout.close();
//         if(filename.contains(".jpg") || filename.contains(".jpeg") || filename.contains(".png")) {
//            Platform.runLater(() -> {
//            children.add(imageNode(filename, false));
//         });
//        }

    }

    public void writeText(String sender, String receiver, String message) throws IOException {
        String msgToWrite = sender + ":" + message;
        msgToWrite += "\n";
        String filename = sender + "_and_" + receiver + ".txt";
        String filename2 = receiver + "_and_" + sender + ".txt";
        filename = filename.replace("\n", "").replace("\r", "");
        filename2 = filename2.replace("\n", "").replace("\r", "");
        File file = new File(filename);
        File file2 = new File(filename2);
        if (file.exists()) {
            FileOutputStream out = new FileOutputStream(file, true);
            out.write(msgToWrite.getBytes(), 0, msgToWrite.length());
            out.flush();
            out.close();
        } else if (file2.exists()) {
            FileOutputStream out = new FileOutputStream(file2, true);
            out.write(msgToWrite.getBytes(), 0, msgToWrite.length());
            out.flush();
            out.close();
        } else {
            FileOutputStream out = new FileOutputStream(file);
            out.write(msgToWrite.getBytes(), 0, msgToWrite.length());
            out.flush();
            out.close();
        }
    }
    public void writeGroupText(String sender, String group, String message) throws IOException {
        String msgToWrite = sender + ":" + message;
        msgToWrite += "\n";
        String filename = group + ".txt";
        filename = filename.replace("\n", "").replace("\r", "");
        File file = new File(filename);
        if (file.exists()) {
            FileOutputStream out = new FileOutputStream(file, true);
            out.write(msgToWrite.getBytes(), 0, msgToWrite.length());
            out.flush();
            out.close();
        } else {
            FileOutputStream out = new FileOutputStream(file);
            out.write(msgToWrite.getBytes(), 0, msgToWrite.length());
            out.flush();
            out.close();
        }
    }

    public String readText(String sender, String receiver) throws IOException {
        String fileName = sender + "_and_" + receiver + ".txt";
        String fileName2 = receiver + "_and_" + sender + ".txt";

        String str = "";
        byte[] buffer = new byte[1024];
        File file = new File(fileName);
        File file2 = new File(fileName2);

        if (file.exists()) {

            long size = file.length();
            FileInputStream in = new FileInputStream(file);
            while (size > 0) {
                int len = in.read(buffer, 0, buffer.length);
                size -= len;
                str = new String(buffer, 0, len);
            }
            in.close();
        } else if (file2.exists()) {

            long size = file2.length();
            FileInputStream in = new FileInputStream(file2);
            while (size > 0) {
                int len = in.read(buffer, 0, buffer.length);
                size -= len;
                str = new String(buffer, 0, len);
            }
            in.close();
        }

        return str;
    }

    public String readGroupText(String group) throws IOException {
        String fileName = group + ".txt";

        String str = "";
        byte[] buffer = new byte[1024];
        File file = new File(fileName);

        if (file.exists()) {

            long size = file.length();
            FileInputStream in = new FileInputStream(file);
            while (size > 0) {
                int len = in.read(buffer, 0, buffer.length);
                size -= len;
                str = new String(buffer, 0, len);
            }
            in.close();
        }

        return str;
    }

    public void addToGroupList(String groupInfo) throws IOException {
        groupInfo += "\n";
        String filename = "groupList.txt";
        filename = filename.replace("\n", "").replace("\r", "");
        File file = new File(filename);
        if (file.exists()) {
            FileOutputStream out = new FileOutputStream(file, true);
            out.write(groupInfo.getBytes(), 0, groupInfo.length());
            out.flush();
            out.close();
        } else {
            FileOutputStream out = new FileOutputStream(file);
            out.write(groupInfo.getBytes(), 0, groupInfo.length());
            out.flush();
            out.close();
        }
    }

}
