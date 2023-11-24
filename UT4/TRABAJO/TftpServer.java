package UT4.TRABAJO;

import java.io.*;
import java.net.*;

public class TftpRequestHandler implements Runnable {
    private DatagramPacket requestPacket;
    private String directory;

    public TftpRequestHandler(DatagramPacket requestPacket, String directory) {
        this.requestPacket = requestPacket;
        this.directory = directory;
    }

    @Override
    public void run() {
        try {
            InetAddress clientAddress = requestPacket.getAddress();
            int clientPort = requestPacket.getPort();

            byte[] requestData = requestPacket.getData();
            ByteArrayInputStream byteStream = new ByteArrayInputStream(requestData);
            DataInputStream dataInput = new DataInputStream(byteStream);

            // Leer el código de operación de la solicitud TFTP
            short opcode = dataInput.readShort();

            if (opcode == 1) { // Si es una solicitud de lectura (RRQ)
                handleReadRequest(clientAddress, clientPort, dataInput);
            } else if (opcode == 2) { // Si es una solicitud de escritura (WRQ)
                handleWriteRequest(clientAddress, clientPort, dataInput);
            } else {
                // Manejar otro tipo de solicitudes (ACK, ERROR, etc.) si es necesario
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


   private void handleReadRequest(InetAddress clientAddress, int clientPort, DataInputStream dataInput) throws IOException {
    // Leer el nombre del archivo solicitado del paquete TFTP
    StringBuilder fileNameBuilder = new StringBuilder();
    byte[] buffer = new byte[1];

    while (dataInput.read(buffer) != -1 && buffer[0] != 0) {
        fileNameBuilder.append(new String(buffer));
    }

    String fileName = fileNameBuilder.toString();

    // Comprobar si el archivo solicitado existe en el directorio
    File file = new File(directory + fileName);

    if (!file.exists()) {
        sendErrorPacket(1, "File not found", clientAddress, clientPort);
        return;
    }

    // Lógica para enviar el archivo al cliente
    FileInputStream fileInputStream = new FileInputStream(file);
    DatagramSocket sendSocket = new DatagramSocket();

    byte[] fileData = new byte[512];
    int bytesRead;

    while ((bytesRead = fileInputStream.read(fileData)) != -1) {
        DatagramPacket sendPacket = new DatagramPacket(fileData, bytesRead, clientAddress, clientPort);
        sendSocket.send(sendPacket);
    }

    fileInputStream.close();
    sendSocket.close();

}
private void handleWriteRequest(InetAddress clientAddress, int clientPort, DataInputStream dataInput) throws IOException {
    // Leer el nombre del archivo a escribir del paquete TFTP
    StringBuilder fileNameBuilder = new StringBuilder();
    byte[] buffer = new byte[1];

    while (dataInput.read(buffer) != -1 && buffer[0] != 0) {
        fileNameBuilder.append(new String(buffer));
    }

    String fileName = fileNameBuilder.toString();

    // Lógica para recibir el archivo del cliente
    FileOutputStream fileOutputStream = new FileOutputStream(directory + fileName);
    DatagramSocket receiveSocket = new DatagramSocket(clientPort);

    byte[] receiveData = new byte[516];
    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

    while (true) {
        receiveSocket.receive(receivePacket);
        byte[] data = receivePacket.getData();

        if (data[1] == 3) { // Verificar si es un paquete de datos (Data Packet)
            fileOutputStream.write(data, 4, receivePacket.getLength() - 4);

            // Enviar ACK (Acknowledgment) al cliente
            sendAckPacket(receivePacket.getData()[2], receivePacket.getData()[3], clientAddress, clientPort);
            
            // Si es el último paquete, salir del ciclo
            if (receivePacket.getLength() < 516) {
                break;
            }
        } else {
            // Manejar otro tipo de paquetes si es necesario (ERROR, etc.)
        }
    }

    fileOutputStream.close();
    receiveSocket.close();
}

private void sendAckPacket(byte blockNumberHigh, byte blockNumberLow, InetAddress clientAddress, int clientPort) throws IOException {
    byte[] ackPacket = new byte[4];
    ackPacket[0] = 0;
    ackPacket[1] = 4; // Código de operación ACK
    ackPacket[2] = blockNumberHigh;
    ackPacket[3] = blockNumberLow;

    DatagramPacket sendPacket = new DatagramPacket(ackPacket, ackPacket.length, clientAddress, clientPort);
    DatagramSocket sendSocket = new DatagramSocket();
    sendSocket.send(sendPacket);
    sendSocket.close();
}

private void sendErrorPacket(int errorCode, String errorMessage, InetAddress clientAddress, int clientPort) throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    DataOutputStream dataOutput = new DataOutputStream(byteStream);

    dataOutput.writeShort(5); // Código de operación ERROR
    dataOutput.writeShort(errorCode);
    dataOutput.write(errorMessage.getBytes());
    dataOutput.writeByte(0); // Agregar byte nulo al final del mensaje

    byte[] errorData = byteStream.toByteArray();
    DatagramPacket errorPacket = new DatagramPacket(errorData, errorData.length, clientAddress, clientPort);
    DatagramSocket sendSocket = new DatagramSocket();
    sendSocket.send(errorPacket);
    sendSocket.close();

}

public static void main(String[] args) {
    // Puerto en el que el servidor TFTP escuchará las solicitudes
    int serverPort = 51324;

    // Directorio raíz donde se encuentran los archivos del servidor TFTP
    String directory = "C:\\Users\\dam2\\Desktop\\directorioserver";

    try {
        DatagramSocket serverSocket = new DatagramSocket(serverPort);

        System.out.println("Servidor TFTP iniciado. Esperando solicitudes...");

        while (true) {
            byte[] receiveData = new byte[516]; // Tamaño máximo del paquete
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            serverSocket.receive(receivePacket);

            // Crear un hilo para manejar la solicitud entrante
            TftpRequestHandler requestHandler = new TftpRequestHandler(receivePacket, directory);
            Thread thread = new Thread(requestHandler);
            thread.start();
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}


}
