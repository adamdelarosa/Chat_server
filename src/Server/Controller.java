package Server;

import Server.DataText.DataAddHistoryChat;
import Server.HealthCheck.ConnectionStatus;
import Server.DataText.DataTextConnection;
import Server.SoundPlayer.WavPlayer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class Controller implements Runnable {

    @FXML
    public Label connectToClientText;
    public Label setSteamsText;
    public Label textLabelGetFromClient;
    public Label connectionStatusActive;
    public TextArea serverChatArea, serverLogArea;
    public TextField serverChatField;
    public Button connectionStatusStart;
    public Button connectionStatusStop;
    boolean tofConnectionStatus;

    private DataOutputStream sendToClient;
    private DataInputStream getFromClient;
    private ServerSocket serverSocketState;
    public Socket socketState;
    private int port = 6789;
    private int numberOfConnetions = 100;
    private Thread iThread;
    private boolean getFromClientSwitch = false;
    public String messageStringfromClient;
    public String messageStringFromServer;

    private DataTextConnection dataTextConnection = new DataTextConnection();
    private DataAddHistoryChat dataAddHistoryChat = new DataAddHistoryChat(this);
    private WavPlayer wavPlayer = new WavPlayer();
    private ConnectionStatus connectionStatus = new ConnectionStatus(true,this,this,this,this);


    public void connectToClient() {
        getFromClientSwitch = true;
        serverLogArea.appendText("*** Server Started ***");

        //Add history to chat window from Mysql
        dataAddHistoryChat.getTextHistoryMysql();

        Thread runAndConnectToClient = new Thread(() -> {
            try {

                while (true)
                    try {
                        serverSocketState();
                        waitingForConnection();
                        setSteams();
                        getMessage();
                    } catch (EOFException eofexception) {
                        closeConnection();
                    }
            } catch (BindException bindexception) {
                bindexception.printStackTrace();
            } catch (IOException ioexception) {
                ioexception.printStackTrace();
            }
        });
        runAndConnectToClient.start();
        connectToClientText.setText("ONLINE");
        connectToClientText.setTextFill(javafx.scene.paint.Color.web("#00FF00"));
        wavPlayer.SoundClipConnectionOnline();
    }

    public void serverSocketState() {
        if (serverSocketState == null) {
            try {
                serverSocketState = new ServerSocket(port, numberOfConnetions);
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverLogArea.appendText("\n" + String.valueOf(serverSocketState));
        } else {
            serverLogArea.appendText("\n" + String.valueOf(serverSocketState));
        }
    }

    public void closeConnection() {
        try {
            serverLogArea.appendText("\nClosing connection.");
        }catch (Exception ignored){

        }
        getFromClientSwitch = false; //<--- Kill Controller Thread
        wavPlayer.SoundClipConnectionOffline();
    }

    private void waitingForConnection() throws IOException {
        socketState = serverSocketState.accept();
        serverLogArea.appendText("\nWaiting for connection...");
    }

    @FXML
    public void connectionStatusStart() {
        connectionStatus.startConnecionStatusCheck();
        wavPlayer.SoundClipConnectionStatusStart();
    }

    @FXML
    public void connectionStatusStop() {
        connectionStatus.killConnecionStatusCheck();
        wavPlayer.SoundClipConnectionStatusEnd();
    }

    public void testSwitch() {
        tofConnectionStatus = !tofConnectionStatus;
        System.out.println("STATE: " + tofConnectionStatus);
        wavPlayer.SoundClipSwitch();

    }

    private void setSteams() throws IOException {
        sendToClient = new DataOutputStream(socketState.getOutputStream());
        sendToClient.flush();
        getFromClient = new DataInputStream(socketState.getInputStream());
        Platform.runLater(() -> {
            serverLogArea.appendText("\nClient connected.");
            setSteamsText.setText("ONLINE");
            setSteamsText.setTextFill(javafx.scene.paint.Color.web("#00FF00"));
        });
    }

    public void sendMessage() {
        messageStringFromServer = serverChatField.getText();
        try {
            sendToClient.writeUTF(messageStringFromServer);
            sendToClient.flush();
            serverChatField.setText("");
            Platform.runLater(() -> serverChatArea.appendText("\n" + messageStringFromServer));
            wavPlayer.SoundClipMessageOut();
        } catch (IOException e) {
            serverLogArea.appendText("\nMessage was not sent.");
            e.printStackTrace();
        }
        //DataBase writer:
        dataTextConnection.mysqlConnection(messageStringFromServer);
    }

    private void getMessage() {
        getFromClientSwitch = true;
        iThread = new Thread(this);
        iThread.run();
    }

    //Get message:

    public void run() {
        serverLogArea.appendText("\nMessages - ONLINE.");


        while (getFromClientSwitch) {
            try {
                messageStringfromClient = getFromClient.readUTF();
                serverChatArea.appendText("\n" + "\t" + messageStringfromClient);

                //DataBase writer:
                dataTextConnection.mysqlConnection(messageStringfromClient);

                //Sound pop while get message:
                wavPlayer.SoundClipMessageIn();

                Platform.runLater(() -> {
                    textLabelGetFromClient.setText("ONLINE");
                    textLabelGetFromClient.setTextFill(javafx.scene.paint.Color.web("#00FF00"));
                });

            } catch (EOFException eofexception) {
                getFromClientSwitch = false;
                serverLogArea.appendText("\nEOFException: getFromClient - STOPPED.");
            } catch (IOException eofexceptionGetMessage) {
                getFromClientSwitch = false;
                serverLogArea.appendText("\nIOException: getFromClient - STOPPED.");
                eofexceptionGetMessage.printStackTrace();
            }

        }
        Platform.runLater(() -> {
            textLabelGetFromClient.setText("OFFLINE");
            textLabelGetFromClient.setTextFill(javafx.scene.paint.Color.web("#ff0000"));
        });
        serverLogArea.appendText("\nMessages - OFFLINE.");
    }

/*    private void mysqlAddDB(){
        DataAddMysqlTable dataAddMysqlTable = new DataAddMysqlTable();
        dataAddMysqlTable.mysqlConnectionAddTable();
    }*/
}