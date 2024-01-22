package com.example.studiu;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class server {
    // Lista de handleri pentru clienți conectați
    private static final List<ClientHandler> clients = new ArrayList<>();

    // Textul partajat între toți clienții
    private static final StringBuilder sharedText = new StringBuilder();

    public static void main(String[] args) {
        try {
            // Se deschide un ServerSocket pentru a asculta conexiuni de la clienți
            ServerSocket serverSocket = new ServerSocket(12345);

            while (true) {
                // Se așteaptă conectarea unui nou client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nou client conectat de la IP: " + clientSocket);

                // Se creează un stream de ieșire pentru a trimite date clientului
                ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

                // Se adaugă un nou handler pentru client în lista de clienți
                clients.add(new ClientHandler(clientSocket, outputStream));

                // Se trimite textul partajat clientului nou conectat
                outputStream.writeObject(sharedText.toString());
                outputStream.flush();

                // Se pornește un fir de execuție pentru handler-ul clientului
                Thread clientThread = new Thread(clients.get(clients.size() - 1));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clasa internă pentru gestionarea fiecărui client într-un fir de execuție separat
    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ObjectOutputStream outputStream;

        public ClientHandler(Socket socket, ObjectOutputStream outputStream) {
            this.socket = socket;
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                while (true) {
                    try {
                        String updatedText = (String) inputStream.readObject();

                        // Actualizați sharedText când un client trimite text nou
                        synchronized (sharedText) {
                            sharedText.setLength(0);  // Ștergeți conținutul existent
                            sharedText.append(updatedText);
                        }

                        broadcastUpdate(updatedText);
                    } catch (EOFException eof) {
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    clients.remove(this);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcastUpdate(String updatedText) {
            for (ClientHandler client : clients) {
                try {
                    client.outputStream.writeObject(updatedText);
                    client.outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}