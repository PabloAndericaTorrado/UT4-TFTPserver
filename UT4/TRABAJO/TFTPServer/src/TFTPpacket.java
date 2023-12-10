import java.net.*;
import java.io.*;
import java.util.*;

class TftpException extends Exception {
    public TftpException() {
        super();
    }

    public TftpException(String s) {
        super(s);
    }
}


public class TFTPpacket {

    // Constantes TFTP
    public static int Puerto = 69;
    public static int LongitudMaximaDePaquete = 516;
    public static int DatosMaximosTFTP = 512;

    // Códigos de operación TFTP
    protected static final short tftpRRQ = 1;
    protected static final short tftpWRQ = 2;
    protected static final short tftpDATA = 3;
    protected static final short tftpACK = 4;
    protected static final short tftpERROR = 5;

    // Desplazamientos en el paquete
    protected static final int opOffset = 0;
    protected static final int fileOffset = 2;
    protected static final int blkOffset = 2;
    protected static final int dataOffset = 4;
    protected static final int numOffset = 2;
    protected static final int msgOffset = 4;

    // El paquete real para la transferencia UDP
    protected byte[] message;
    protected int length;

    // Información de dirección (necesaria para respuestas)
    protected InetAddress host;
    protected int port;

    // Constructor
    public TFTPpacket() {
        message = new byte[LongitudMaximaDePaquete];
        length = LongitudMaximaDePaquete;
    }

    // Métodos para recibir el paquete y convertirlo al tipo correcto (datos/ACK/lectura/...)
    public static TFTPpacket receive(DatagramSocket Socket) throws IOException {
        TFTPpacket Entrada = new TFTPpacket(), PaqueteDevuelto = new TFTPpacket();
        // Recibir datos y ponerlos en in.message
        DatagramPacket PaqueteEntrada = new DatagramPacket(Entrada.message, Entrada.length);
        Socket.receive(PaqueteEntrada);


        switch (Entrada.get(0)) {
            case tftpRRQ:
                PaqueteDevuelto = new TFTPread();
                break;
            case tftpWRQ:
                PaqueteDevuelto = new TFTPwrite();
                break;
            case tftpDATA:
                PaqueteDevuelto = new TFTPdata();
                break;
            case tftpACK:
                PaqueteDevuelto = new TFTPack();
                break;
            case tftpERROR:
                PaqueteDevuelto = new TFTPerror();
                break;
        }
        PaqueteDevuelto.message = Entrada.message;
        PaqueteDevuelto.length = PaqueteEntrada.getLength();
        PaqueteDevuelto.host = PaqueteEntrada.getAddress();
        PaqueteDevuelto.port = PaqueteEntrada.getPort();

        return PaqueteDevuelto;
    }

    public void send(InetAddress ip, int puerto, DatagramSocket Socket) throws IOException {
        Socket.send(new DatagramPacket(message, length, ip, puerto));
    }


    public InetAddress getAddress() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getLength() {
        return length;
    }

    protected void put(int at, short value) {
        message[at++] = (byte) (value >>> 8);  
        message[at] = (byte) (value % 256);    
    }

    @SuppressWarnings("deprecation")
    protected void put(int at, String valor, byte del) {
        valor.getBytes(0, valor.length(), message, at);
        message[at + valor.length()]=del;
    }

    protected int get(int at) {
        return (message[at] & 0xff) << 8 | message[at + 1] & 0xff;
    }

    protected String get(int at, byte del) {
        StringBuffer result = new StringBuffer();
        while (message[at] != del) result.append((char) message[at++]);
        return result.toString();
    }
}


final class TFTPdata extends TFTPpacket {

    protected TFTPdata() {}

    public TFTPdata(int numeroBloque, FileInputStream in) throws IOException {
        this.message = new byte[LongitudMaximaDePaquete];
        this.put(opOffset, tftpDATA);
        this.put(blkOffset, (short) numeroBloque);

        length = in.read(message, dataOffset, DatosMaximosTFTP) + 4;
    }


    public int numeroBloque() {
        return this.get(blkOffset);
    }


    public int write(FileOutputStream out) throws IOException {
        out.write(message, dataOffset, length - 4);
        return (length - 4);
    }
}


class TFTPerror extends TFTPpacket {

    protected TFTPerror() {}
    public TFTPerror(int numero, String mensaje) {
        length = 4 + mensaje.length() + 1;
        this.message = new byte[length];
        put(opOffset, tftpERROR);
        put(numOffset, (short) numero);
        put(msgOffset, mensaje, (byte) 0);
    }

    public int numero() {
        return this.get(numOffset);
    }

    public String mensaje() {
        return this.get(msgOffset, (byte) 0);
    }
}


final class TFTPack extends TFTPpacket {

    protected TFTPack() {}
    public TFTPack(int numeroBloque) {
        length = 4;
        this.message = new byte[length];
        put(opOffset, tftpACK);
        put(blkOffset, (short) numeroBloque);
    }


    public int numeroBloque() {
        return this.get(blkOffset);
    }
}


final class TFTPread extends TFTPpacket {

    protected TFTPread() {}

    public TFTPread(String nombreArchivo, String modoDatos) {
        length = 2 + nombreArchivo.length() + 1 + modoDatos.length() + 1;
        message = new byte[length];
        put(opOffset, tftpRRQ);
        put(fileOffset, nombreArchivo, (byte) 0);
        put(fileOffset + nombreArchivo.length() + 1, modoDatos, (byte) 0);
    }

    public String nombreArchivo() {
        return this.get(fileOffset, (byte) 0);
    }

    public String tipoSolicitud() {
        String nombre = nombreArchivo();
        return this.get(fileOffset + nombre.length() + 1, (byte) 0);
    }
}

final class TFTPwrite extends TFTPpacket {
    // Constructores
    protected TFTPwrite() {}

    public TFTPwrite(String nombreArchivo, String modoDatos) {
        length = 2 + nombreArchivo.length() + 1 + modoDatos.length() + 1;
        message = new byte[length];
        put(opOffset, tftpWRQ);
        put(fileOffset, nombreArchivo, (byte) 0);
        put(fileOffset + nombreArchivo.length() + 1, modoDatos, (byte) 0);
    }


    public String nombreArchivo() {
        return this.get(fileOffset, (byte) 0);
    }

    public String tipoSolicitud() {
        String nombre = nombreArchivo();
        return this.get(fileOffset + nombre.length() + 1, (byte) 0);
    }
}
