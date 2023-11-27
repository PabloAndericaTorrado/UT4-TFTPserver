
import java.net.*;
import java.io.*;
import java.util.*;


//////////////////////////////////////////////////////////////////////////////
// Paquete GENERAL: define la estructura del paquete, miembros y métodos   //
// necesarios del paquete TFTP. Debe ser extendido por otros paquetes      //
// específicos (lectura, escritura, etc.).                                  //
//////////////////////////////////////////////////////////////////////////////
public class PaqueteTFTP {

    // Constantes TFTP
    public static int puertoTftp = 69;
    public static int longitudMaximaPaqueteTftp = 516;
    public static int datosMaximosTftp = 512;

    // Códigos de operación TFTP
    protected static final short tftpRRQ = 1;
    protected static final short tftpWRQ = 2;
    protected static final short tftpDATA = 3;
    protected static final short tftpACK = 4;
    protected static final short tftpERROR = 5;

    // Desplazamientos en el paquete
    protected static final int desplazamientoOperacion = 0;

    protected static final int desplazamientoArchivo = 2;

    protected static final int desplazamientoBloque = 2;
    protected static final int desplazamientoDatos = 4;

    protected static final int desplazamientoNumero = 2;
    protected static final int desplazamientoMensaje = 4;

    // El paquete real para la transferencia UDP
    protected byte[] mensaje;
    protected int longitud;

    // Información de dirección (necesaria para respuestas)
    protected InetAddress host;
    protected int puerto;

    // Constructor
    public PaqueteTFTP() {
        mensaje = new byte[longitudMaximaPaqueteTftp];
        longitud = longitudMaximaPaqueteTftp;
    }

    // Métodos para recibir el paquete y convertirlo al tipo correcto (datos/ack/lectura/...)
    public static PaqueteTFTP recibir(DatagramSocket sock) throws IOException {
        PaqueteTFTP in = new PaqueteTFTP(), retPak = new PaqueteTFTP();
        // recibir datos y colocarlos en in.message
        DatagramPacket inPak = new DatagramPacket(in.mensaje, in.longitud);
        sock.receive(inPak);

        // Verificar el código de operación en el mensaje y luego convertir el mensaje en el tipo correspondiente
        switch (in.get(0)) {
            case tftpRRQ:
                retPak = new LecturaTFTP();
                break;
            case tftpWRQ:
                retPak = new EscrituraTFTP();
                break;
            case tftpDATA:
                retPak = new DatosTFTP();
                break;
            case tftpACK:
                retPak = new ACKTFTP();
                break;
            case tftpERROR:
                retPak = new ErrorTFTP();
                break;
        }
        retPak.mensaje = in.mensaje;
        retPak.longitud = inPak.getLength();
        retPak.host = inPak.getAddress();
        retPak.puerto = inPak.getPort();

        return retPak;
    }

    // Método para enviar el paquete
    public void enviar(InetAddress ip, int puerto, DatagramSocket s) throws IOException {
        s.send(new DatagramPacket(mensaje, longitud, ip, puerto));
    }

    // Métodos similares a DatagramPacket
    public InetAddress obtenerDireccion() {
        return host;
    }

    public int obtenerPuerto() {
        return puerto;
    }

    public int obtenerLongitud() {
        return longitud;
    }

    // Métodos para colocar el código de operación, número de bloque, código de error en el array de bytes 'mensaje'
    protected void colocar(int en, short valor) {
        mensaje[en++] = (byte) (valor >>> 8); // primer byte
        mensaje[en] = (byte) (valor % 256); // último byte
    }

    @SuppressWarnings("deprecation")
    // Coloca el nombre de archivo y el modo en 'mensaje' en 'en' seguido por el byte "del"
    protected void colocar(int en, String valor, byte del) {
        valor.getBytes(0, valor.length(), mensaje, en);
        mensaje[en + valor.length()] = del;
    }

    protected int get(int en) {
        return (mensaje[en] & 0xff) << 8 | mensaje[en + 1] & 0xff;
    }

    protected String get(int en, byte del) {
        StringBuffer resultado = new StringBuffer();
        while (mensaje[en] != del)
            resultado.append((char) mensaje[en++]);
        return resultado.toString();
    }
}

////////////////////////////////////////////////////////
// Paquete DATOS: coloca el código correcto en el     // 
// mensaje; lee el archivo para enviar; escribe el    //
// archivo después de recibir                         //
////////////////////////////////////////////////////////
final class DatosTFTP extends PaqueteTFTP {

    // Constructores
    protected DatosTFTP() {
    }

    public DatosTFTP(int numeroBloque, FileInputStream in) throws IOException {
        this.mensaje = new byte[longitudMaximaPaqueteTftp];
        // manipular mensaje
        this.colocar(desplazamientoOperacion, tftpDATA);
        this.colocar(desplazamientoBloque, (short) numeroBloque);
        // leer el archivo en el paquete y calcular la longitud total
        longitud = in.read(mensaje, desplazamientoDatos, datosMaximosTftp) + 4;
    }

    // Accesores

    public int numeroBloque() {
        return this.get(desplazamientoBloque);
    }

    // Salida de archivo
    public int escribir(FileOutputStream out) throws IOException {
        out.write(mensaje, desplazamientoDatos, longitud - 4);

        return (longitud - 4);
    }
}

/////////////////////////////////////////////////////////
// Paquete ERROR: coloca los códigos y mensajes de error// 
// correctos en el 'mensaje'                            //
/////////////////////////////////////////////////////////
class ErrorTFTP extends PaqueteTFTP {

    // Constructores
    protected ErrorTFTP() {
    }

    // Genera paquete de error
    public ErrorTFTP(int numero, String mensaje) {
        longitud = 4 + mensaje.length() + 1;
        this.mensaje = new byte[longitud];
        colocar(desplazamientoOperacion, tftpERROR);
        colocar(desplazamientoNumero, (short) numero);
        colocar(desplazamientoMensaje, mensaje, (byte) 0);
    }

    // Accesores
    public int numero() {
        return this.get(desplazamientoNumero);
    }

    public String mensaje() {
        return this.get(desplazamientoMensaje, (byte) 0);
    }
}

/////////////////////////////////////////////////////////
// Paquete ACK: coloca el código de operación correcto y// 
// el número de bloque en el 'mensaje'                  //
/////////////////////////////////////////////////////////
final class ACKTFTP extends PaqueteTFTP {

    // Constructores
    protected ACKTFTP() {
    }

    // Genera paquete ACK
    public ACKTFTP(int numeroBloque) {
        longitud = 4;
        this.mensaje = new byte[longitud];
        colocar(desplazamientoOperacion, tftpACK);
        colocar(desplazamientoBloque, (short) numeroBloque);
    }

    // Accesores
    public int numeroBloque() {
        return this.get(desplazamientoBloque);
    }
}

/////////////////////////////////////////////////////////
// Paquete LECTURA: coloca el código de operación y el  // 
// nombre de archivo, modo en el 'mensaje'             //
/////////////////////////////////////////////////////////
final class LecturaTFTP extends PaqueteTFTP {

    // Constructores
    protected LecturaTFTP() {
    }

    // Especifica el nombre de archivo y el modo de transferencia
    public LecturaTFTP(String nombreArchivo, String modoDatos) {
        longitud = 2 + nombreArchivo.length() + 1 + modoDatos.length() + 1;
        mensaje = new byte[longitud];

        colocar(desplazamientoOperacion, tftpRRQ);
        colocar(desplazamientoArchivo, nombreArchivo, (byte) 0);
        colocar(desplazamientoArchivo + nombreArchivo.length() + 1, modoDatos, (byte) 0);
    }

    // Accesores

    public String nombreArchivo() {
        return this.get(desplazamientoArchivo, (byte) 0);
    }

    public String tipoSolicitud() {
        String nombreArchivo = nombreArchivo();
        return this.get(desplazamientoArchivo + nombreArchivo.length() + 1, (byte) 0);
    }
}

/////////////////////////////////////////////////////////
// Paquete ESCRITURA: coloca el código de operación y el// 
// nombre de archivo, modo en el 'mensaje'             //
/////////////////////////////////////////////////////////
final class EscrituraTFTP extends PaqueteTFTP {

    // Constructores

    protected EscrituraTFTP() {
    }

    public EscrituraTFTP(String nombreArchivo, String modoDatos) {
        longitud = 2 + nombreArchivo.length() + 1 + modoDatos.length() + 1;
        mensaje = new byte[longitud];

        colocar(desplazamientoOperacion, tftpWRQ);
        colocar(desplazamientoArchivo, nombreArchivo, (byte) 0);
        colocar(desplazamientoArchivo + nombreArchivo.length() + 1, modoDatos, (byte) 0);
    }

    // Accesores

    public String nombreArchivo() {
        return this.get(desplazamientoArchivo, (byte) 0);
    }

    public String tipoSolicitud() {
        String nombreArchivo = nombreArchivo();
        return this.get(desplazamientoArchivo + nombreArchivo.length() + 1, (byte) 0);
    }
}
