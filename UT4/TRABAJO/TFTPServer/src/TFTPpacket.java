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

//////////////////////////////////////////////////////////////////////////////
// Paquete GENERAL: define la estructura del paquete, miembros y métodos    //
// necesarios del paquete TFTP. Para ser extendido por otros paquetes       //
// específicos (lectura, escritura, etc.)                                  //
//////////////////////////////////////////////////////////////////////////////
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

        // Comprobar el código de operación en el mensaje, luego convertir el mensaje al tipo correspondiente
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

    // Método para enviar el paquete
    public void send(InetAddress ip, int puerto, DatagramSocket Socket) throws IOException {
        Socket.send(new DatagramPacket(message, length, ip, puerto));
    }

    // Métodos similares a DatagramPacket
    public InetAddress getAddress() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getLength() {
        return length;
    }

    // Métodos para colocar el código de operación, número de bloque, código de error en el arreglo de bytes 'message'
    protected void put(int at, short value) {
        message[at++] = (byte) (value >>> 8);  // primer byte
        message[at] = (byte) (value % 256);    // último byte
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

////////////////////////////////////////////////////////
// Paquete DATA: coloca el código correcto en el     //
// mensaje; lee archivo para enviar; escribe archivo  //
// después de recibir                                //
////////////////////////////////////////////////////////
final class TFTPdata extends TFTPpacket {
    // Constructores
    protected TFTPdata() {}

    public TFTPdata(int numeroBloque, FileInputStream in) throws IOException {
        this.message = new byte[LongitudMaximaDePaquete];
        // manipular mensaje
        this.put(opOffset, tftpDATA);
        this.put(blkOffset, (short) numeroBloque);
        // leer el archivo en el paquete y calcular la longitud total
        length = in.read(message, dataOffset, DatosMaximosTFTP) + 4;
    }

    // Accesores
    public int numeroBloque() {
        return this.get(blkOffset);
    }

    // Salida de archivo
    public int write(FileOutputStream out) throws IOException {
        out.write(message, dataOffset, length - 4);
        return (length - 4);
    }
}

/////////////////////////////////////////////////////////
// Paquete ERROR: coloca los códigos correctos y       //
// mensajes de error en el 'message'                   //
/////////////////////////////////////////////////////////
class TFTPerror extends TFTPpacket {
    // Constructores
    protected TFTPerror() {}

    // Generar paquete de error
    public TFTPerror(int numero, String mensaje) {
        length = 4 + mensaje.length() + 1;
        this.message = new byte[length];
        put(opOffset, tftpERROR);
        put(numOffset, (short) numero);
        put(msgOffset, mensaje, (byte) 0);
    }

    // Accesores
    public int numero() {
        return this.get(numOffset);
    }

    public String mensaje() {
        return this.get(msgOffset, (byte) 0);
    }
}

/////////////////////////////////////////////////////////
// Paquete ACK: coloca el código de operación y número //
// de bloque correctos en el 'message'                 //
/////////////////////////////////////////////////////////
final class TFTPack extends TFTPpacket {
    // Constructores
    protected TFTPack() {}

    // Generar paquete de ACK
    public TFTPack(int numeroBloque) {
        length = 4;
        this.message = new byte[length];
        put(opOffset, tftpACK);
        put(blkOffset, (short) numeroBloque);
    }

    // Accesores
    public int numeroBloque() {
        return this.get(blkOffset);
    }
}

/////////////////////////////////////////////////////////
// Paquete LECTURA: coloca el código de operación y    //
// nombre de archivo, modo en el 'message'             //
/////////////////////////////////////////////////////////
final class TFTPread extends TFTPpacket {
    // Constructores
    protected TFTPread() {}

    // Especificar el nombre de archivo y modo de transferencia
    public TFTPread(String nombreArchivo, String modoDatos) {
        length = 2 + nombreArchivo.length() + 1 + modoDatos.length() + 1;
        message = new byte[length];
        put(opOffset, tftpRRQ);
        put(fileOffset, nombreArchivo, (byte) 0);
        put(fileOffset + nombreArchivo.length() + 1, modoDatos, (byte) 0);
    }

    // Accesores
    public String nombreArchivo() {
        return this.get(fileOffset, (byte) 0);
    }

    public String tipoSolicitud() {
        String nombre = nombreArchivo();
        return this.get(fileOffset + nombre.length() + 1, (byte) 0);
    }
}

/////////////////////////////////////////////////////////
// Paquete ESCRITURA: coloca el código de operación y  //
// nombre de archivo, modo en el 'message'             //
/////////////////////////////////////////////////////////
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

    // Accesores
    public String nombreArchivo() {
        return this.get(fileOffset, (byte) 0);
    }

    public String tipoSolicitud() {
        String nombre = nombreArchivo();
        return this.get(fileOffset + nombre.length() + 1, (byte) 0);
    }
}
