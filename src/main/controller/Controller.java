package main.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import main.model.ClientSocket;
import main.model.Nodo;
import main.model.Server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class Controller implements Observer {
    ServerSocket serverSocket = null;
    private final int PORT = 3001;
    private ArrayList<Nodo> poolSocket = new ArrayList<>();

    @FXML
    private Button btnOpenServer;

    @FXML
    private Button btnSalir;

    @FXML
    private ListView<String> listClient;

    @FXML
    private Circle circleLed;

    @FXML
    void OpenServerOnMouseClicked(MouseEvent event) {
        byte[] ipBytes = {(byte)192,(byte)168,(byte)5, (byte)230 };
        InetAddress ip = null;
        try {
            ip = InetAddress.getByAddress(ipBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            serverSocket = new ServerSocket(PORT,100,ip);
            listClient.getItems().add("Server abierto: " + serverSocket.getInetAddress().getHostName());
            circleLed.setFill(Color.GREEN);

           Server server = new Server(serverSocket);
           server.addObserver(this);
           new Thread(server).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @FXML
    void SalirOnMouseClicked(MouseEvent event) {
        System.exit(1);
    }

    @Override
    public void update(Observable o, Object arg) {

        if (o instanceof Server) {
            Socket socket = (Socket)arg;
            poolSocket.add(new Nodo(socket.hashCode(),"User "+poolSocket.size(),socket));
            // Broadcast a todos los sockets conectados para actualizar la lista de conexiones
            broadCast();
            // Crear un hilo que reciba mensajes entrantes de ese nuevo socket creado
            ClientSocket clientSocket = new ClientSocket(socket);
            clientSocket.addObserver(this);
            new Thread(clientSocket).start();
            Platform.runLater(() -> listClient.getItems().add(socket.getInetAddress().getHostName()));
        }
        if (o instanceof ClientSocket){
            String mensaje = (String)arg;
            String[] datagrama;
            datagrama = mensaje.split(":");
            System.out.println(datagrama);
            if (datagrama[0].equals("3")) {
                System.out.println("Recibio 3");
                sendMessage(datagrama[1],datagrama[2],datagrama[3]);
            }
            System.out.println("Recibio 1");
            Platform.runLater(() -> listClient.getItems().add(mensaje));
        }

    }

    private void broadCast(){
        DataOutputStream bufferDeSalida = null;
        Nodo ultimaConexion = poolSocket.get(poolSocket.size()-1);
        for (Nodo nodo: poolSocket) {
            try {
                bufferDeSalida = new DataOutputStream(nodo.getSocket().getOutputStream());
                bufferDeSalida.flush();
                listaClientes();
                bufferDeSalida.writeUTF("1:Servidor:"+nodo.getName()+":"+ultimaConexion.getName() + listaClientes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessage(String source, String destino, String mensaje) {
        DataOutputStream bufferDeSalida = null;
        for (Nodo nodo: poolSocket) {
            if(destino.equals(nodo.getName()))
            try {
                bufferDeSalida = new DataOutputStream(nodo.getSocket().getOutputStream());
                bufferDeSalida.flush();
                bufferDeSalida.writeUTF( source +": "+ mensaje);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private String listaClientes(){
        String lista = "";
        for (Nodo nodo: poolSocket) {
            lista = lista+":"+nodo.getName();
        }
        return lista;
    }
}



