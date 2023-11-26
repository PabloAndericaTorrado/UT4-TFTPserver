import java.net.*;
import java.io.*;
import java.util.*;

class TFTPserverWRQ extends Thread {

    protected DatagramSocket Socket;
    protected InetAddress host;
    protected int puerto;
    protected FileOutputStream outFile;
    protected TFTPpacket req;
    protected int timeoutLimit = 5;
    protected File saveFile;
    protected String fileName;

    // Inicializar solicitud de escritura
    public TFTPserverWRQ(TFTPwrite solicitud) throws TftpException {
        try {
            req = solicitud;
            Socket = new DatagramSocket(); // Nuevo puerto para la transferencia
            Socket.setSoTimeout(1000);

            host = solicitud.getAddress();
            puerto = solicitud.getPort();
            fileName = solicitud.nombreArchivo();
            // Crear objeto de archivo en la carpeta principal
            saveFile = new File("../" + fileName);

            if (!saveFile.exists()) {
                outFile = new FileOutputStream(saveFile);
                TFTPack a = new TFTPack(0);
                a.send(host, puerto, Socket); // Enviar ACK 0 al principio, listo para recibir
                this.start();
            } else
                throw new TftpException("El archivo ya existe");

        } catch (Exception e) {
            TFTPerror ePak = new TFTPerror(1, e.getMessage()); // código de error 1
            try {
                ePak.send(host, puerto, Socket);
            } catch (Exception f) {
            }

            System.out.println("Inicio fallido del cliente: " + e.getMessage());
        }
    }

    public void run() {
        // Manejar solicitud de escritura
        if (req instanceof TFTPwrite) {
            try {
                for (int numBloque = 1, bytesOut = 512; bytesOut == 512; numBloque++) {
                    while (timeoutLimit != 0) {
                        try {
                            TFTPpacket inPak = TFTPpacket.receive(Socket);
                            // Verificar tipo de paquete
                            if (inPak instanceof TFTPerror) {
                                TFTPerror p = (TFTPerror) inPak;
                                throw new TftpException(p.mensaje());
                            } else if (inPak instanceof TFTPdata) {
                                TFTPdata p = (TFTPdata) inPak;
                                // Verificar número de bloque
                                if (p.numeroBloque() != numBloque) { // Se espera que sea el mismo
                                    throw new SocketTimeoutException();
                                }
                                // Escribir en el archivo y enviar ACK
                                bytesOut = p.write(outFile);
                                TFTPack a = new TFTPack(numBloque);
                                a.send(host, puerto, Socket);
                                break;
                            }
                        } catch (SocketTimeoutException t2) {
                            System.out.println("Tiempo de espera agotado, reenviar ACK");
                            TFTPack a = new TFTPack(numBloque - 1);
                            a.send(host, puerto, Socket);
                            timeoutLimit--;
                        }
                    }
                    if (timeoutLimit == 0) {
                        throw new Exception("Fallo de conexión");
                    }
                }
                System.out.println("Transferencia completada. (Cliente " + host + ")");
            } catch (Exception e) {
                TFTPerror ePak = new TFTPerror(1, e.getMessage());
                try {
                    ePak.send(host, puerto, Socket);
                } catch (Exception f) {
                }

                System.out.println("Fallo del cliente:  " + e.getMessage());
                saveFile.delete();
            }
        }
    }
}
