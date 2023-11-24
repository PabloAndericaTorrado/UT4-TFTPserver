import java.io.*;
import java.net.*;

public class TftpClient {

    public static void main(String[] args) {
        // Dirección IP y puerto del servidor TFTP
        String serverAddress = "127.0.0.1"; // Cambiar a la dirección IP del servidor
        int serverPort = 51324;

        // Directorio del archivo a descargar/subir
        String fileName = "archivo_prueba.txt"; // Nombre del archivo

        // Modo de operación (lectura o escritura)
        String mode = "octet"; // Modo binario para transferencia de archivos

        try {
            InetAddress serverIP = InetAddress.getByName(serverAddress);
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(10000); // Timeout en milisegundos

            // Preparar solicitud de lectura (RRQ) o escritura (WRQ)
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            DataOutputStream dataOutput = new DataOutputStream(byteStream);

            // Construir el paquete de solicitud RRQ o WRQ
            dataOutput.writeShort(1); // Código de operación 1 para RRQ o 2 para WRQ
            dataOutput.write(fileName.getBytes());
            dataOutput.writeByte(0);
            dataOutput.write(mode.getBytes());
            dataOutput.writeByte(0);

            // Crear el paquete de solicitud
            byte[] requestData = byteStream.toByteArray();
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, serverIP, serverPort);

            // Enviar la solicitud al servidor
            socket.send(requestPacket);

            if (mode.equals("octet")) {
                // Si el modo es 'octet' (binario), es una descarga de archivo
                receiveFile(socket, fileName);
            } else {
                // De lo contrario, es un modo de escritura
                sendFile(socket, fileName);
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void receiveFile(DatagramSocket socket, String fileName) throws IOException {
        // Preparar archivo local para escribir los datos recibidos del servidor
        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
    
        int blockNumber = 1;
        boolean lastBlock = false;
    
        while (!lastBlock) {
            byte[] receiveData = new byte[516];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    
            socket.receive(receivePacket);
            byte[] data = receivePacket.getData();
    
            // Verificar si es un paquete de datos (opcode 3)
            if (data[1] == 3) {
                // Verificar el número de bloque
                int receivedBlockNumber = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
    
                // Verificar si es el último paquete
                if (receivePacket.getLength() < 516) {
                    lastBlock = true;
                }
    
                // Si el número de bloque recibido es el esperado
                if (receivedBlockNumber == blockNumber) {
                    fileOutputStream.write(data, 4, receivePacket.getLength() - 4);
    
                    // Enviar ACK al servidor por el bloque recibido
                    sendAckPacket(socket, (byte) (blockNumber >> 8), (byte) blockNumber, receivePacket.getAddress(), receivePacket.getPort());
    
                    // Incrementar el número de bloque
                    blockNumber++;
                }
            }
        }
    
        fileOutputStream.close();
    }
    
    private static void sendFile(DatagramSocket socket, String fileName) throws IOException {
        File fileToSend = new File(fileName);
    
        if (!fileToSend.exists()) {
            System.out.println("El archivo no existe.");
            return;
        }
    
        FileInputStream fileInputStream = new FileInputStream(fileToSend);
    
        int blockNumber = 1;
        boolean lastBlock = false;
    
        while (!lastBlock) {
            byte[] fileData = new byte[512];
            int bytesRead = fileInputStream.read(fileData);
    
            if (bytesRead == -1) {
                lastBlock = true;
            }
    
            byte[] sendData = new byte[bytesRead + 4];
            sendData[0] = 0;
            sendData[1] = 3; // Opcode 3 para paquete de datos
            sendData[2] = (byte) (blockNumber >> 8);
            sendData[3] = (byte) blockNumber;
    
            System.arraycopy(fileData, 0, sendData, 4, bytesRead);
    
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, socket.getInetAddress(), socket.getPort());
            socket.send(sendPacket);
    
            // Esperar ACK del servidor
            waitForAck(socket, blockNumber);
    
            blockNumber++;
        }
    
        fileInputStream.close();
    }
    
    private static void sendAckPacket(DatagramSocket socket, byte blockNumberHigh, byte blockNumberLow, InetAddress clientAddress, int clientPort) throws IOException {
        byte[] ackPacket = new byte[4];
        ackPacket[0] = 0;
        ackPacket[1] = 4; // Opcode 4 para ACK
        ackPacket[2] = blockNumberHigh;
        ackPacket[3] = blockNumberLow;
    
        DatagramPacket sendPacket = new DatagramPacket(ackPacket, ackPacket.length, clientAddress, clientPort);
        socket.send(sendPacket);
    }
    
    private static void waitForAck(DatagramSocket socket, int expectedBlockNumber) throws IOException {
        byte[] receiveData = new byte[4];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    
        boolean ackReceived = false;
    
        while (!ackReceived) {
            try {
                socket.receive(receivePacket);
                byte[] data = receivePacket.getData();
    
                if (data[1] == 4) { // Opcode 4 para ACK
                    int receivedBlockNumber = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
    
                    if (receivedBlockNumber == expectedBlockNumber) {
                        ackReceived = true;
                    }
                }
            } catch (SocketTimeoutException e) {
                // Reenviar el paquete en caso de timeout
                socket.send(new DatagramPacket(receiveData, receiveData.length, receivePacket.getAddress(), receivePacket.getPort()));
            }
        }
    }
    
}
