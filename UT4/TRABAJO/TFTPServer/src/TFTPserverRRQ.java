import java.net.*;
import java.security.MessageDigest;
import java.io.*;
import java.util.*;

class TFTPserverRRQ extends Thread {

    protected DatagramSocket Socket;
    protected InetAddress host;
    protected int port;
    protected FileInputStream source;
    protected TFTPpacket req;
    protected int timeoutLimit = 5;
    protected String nombreArchivo;

    // Inicializar solicitud de lectura
    public TFTPserverRRQ(TFTPread solicitud) throws TftpException {
        try {
            req = solicitud;
            // Abrir nuevo socket con número de puerto aleatorio para transferencia
            Socket = new DatagramSocket();
            Socket.setSoTimeout(1000);
            nombreArchivo = solicitud.nombreArchivo();

            host = solicitud.getAddress();
            port = solicitud.getPort();
            

            File archivoFuente = new File("../" + nombreArchivo);


            if (archivoFuente.exists() && archivoFuente.isFile() && archivoFuente.canRead()) {
                source = new FileInputStream(archivoFuente);
                this.start(); 
            } else
                throw new TftpException("violación de acceso");

        } catch (Exception e) {
            TFTPerror ePak = new TFTPerror(1, e.getMessage()); 
            try {
                ePak.send(host, port, Socket);
            } catch (Exception f) {
            }

            System.out.println("Fallo de inicio del cliente: " + e.getMessage());
        }
    }


    public void run() {
        int bytesRead = TFTPpacket.LongitudMaximaDePaquete;

        if (req instanceof TFTPread) {
            try {
                for (int numBloque = 1; bytesRead == TFTPpacket.LongitudMaximaDePaquete; numBloque++) {
                    TFTPdata outPak = new TFTPdata(numBloque, source);
                    bytesRead = outPak.getLength();
                    outPak.send(host, port, Socket);
                    
               
                    while (timeoutLimit != 0) {
                        try {
							TFTPpacket ack = TFTPpacket.receive(Socket);
                            if (!(ack instanceof TFTPack)) {
                                throw new Exception("Fallo del cliente");
                            }
                            TFTPack a = (TFTPack) ack;
                            
                            if (a.numeroBloque() != numBloque) { 
                                throw new SocketTimeoutException("Paquete perdido, reenviar");
                            }
                            break;
                        } catch (SocketTimeoutException t) { 
                            System.out.println("Reenviar bloque " + numBloque);
                            timeoutLimit--;
                            outPak.send(host, port, Socket);
                        }
                    } 
                    if (timeoutLimit == 0) {
                        throw new Exception("falla de conexión");
                    }
                }
                System.out.println("Transferencia completada. (Cliente " + host + ")");
            } catch (Exception e) {
                TFTPerror ePak = new TFTPerror(1, e.getMessage());

                try {
                    ePak.send(host, port, Socket);
                } catch (Exception f) {
                }

                System.out.println("Fallo del cliente: " + e.getMessage());
            }
        }
    }
}
