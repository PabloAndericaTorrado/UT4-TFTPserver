
import java.net.*;
import java.io.*;
import java.util.*;



//Paquete: define la estructura del paquete y métodos necesarios del paquete TFTP.
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
    protected static final int desplazamientoNumero = 2;
    protected static final int desplazamientoDatos = 4;
    protected static final int desplazamientoMensaje = 4;
    protected static final int desplazamientoArchivo = 2;
    protected static final int desplazamientoBloque = 2;

    protected byte[] mensaje;
    protected int longitud;

    protected InetAddress host;
    protected int puerto;

    public PaqueteTFTP() {
        mensaje = new byte[longitudMaximaPaqueteTftp];
        longitud = longitudMaximaPaqueteTftp;
    }

    // Métodos para recibir el paquete y convertirlo datos,ack,lectura
    public static PaqueteTFTP recibir(DatagramSocket sock) throws IOException {
        PaqueteTFTP in = new PaqueteTFTP(), retPak = new PaqueteTFTP();
        DatagramPacket inPak = new DatagramPacket(in.mensaje, in.longitud);
        sock.receive(inPak);

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


    public void enviar(InetAddress ip, int puerto, DatagramSocket s) throws IOException {
        s.send(new DatagramPacket(mensaje, longitud, ip, puerto));
    }


    public InetAddress obtenerDireccion() {
        return host;
    }

    public int obtenerPuerto() {
        return puerto;
    }

    public int obtenerLongitud() {
        return longitud;
    }


    protected void colocar(int en, short valor) {
        mensaje[en++] = (byte) (valor >>> 8); // primer byte
        mensaje[en] = (byte) (valor % 256); // último byte
    }

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


//Coloca el código correcto en el mensaje; lee para enviar; escribe después de recibir

final class DatosTFTP extends PaqueteTFTP {


    protected DatosTFTP() {
    }

    public DatosTFTP(int numeroBloque, FileInputStream in) throws IOException {
        this.mensaje = new byte[longitudMaximaPaqueteTftp];

        this.colocar(desplazamientoOperacion, tftpDATA);
        this.colocar(desplazamientoBloque, (short) numeroBloque);

        longitud = in.read(mensaje, desplazamientoDatos, datosMaximosTftp) + 4;
    }



    public int numeroBloque() {
        return this.get(desplazamientoBloque);
    }

    public int escribir(FileOutputStream out) throws IOException {
        out.write(mensaje, desplazamientoDatos, longitud - 4);

        return (longitud - 4);
    }
}

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


    public int numero() {
        return this.get(desplazamientoNumero);
    }

    public String mensaje() {
        return this.get(desplazamientoMensaje, (byte) 0);
    }
}


// Paquete ACK: coloca el código de operación correcto
final class ACKTFTP extends PaqueteTFTP {

    protected ACKTFTP() {
    }
    public ACKTFTP(int numeroBloque) {
        longitud = 4;
        this.mensaje = new byte[longitud];
        colocar(desplazamientoOperacion, tftpACK);
        colocar(desplazamientoBloque, (short) numeroBloque);
    }

    public int numeroBloque() {
        return this.get(desplazamientoBloque);
    }
}


// Paquete LECTURA: coloca el código de operación y el nombre de archivo

final class LecturaTFTP extends PaqueteTFTP {


    protected LecturaTFTP() {
    }

    public LecturaTFTP(String nombreArchivo, String modoDatos) {
        longitud = 2 + nombreArchivo.length() + 1 + modoDatos.length() + 1;
        mensaje = new byte[longitud];

        colocar(desplazamientoOperacion, tftpRRQ);
        colocar(desplazamientoArchivo, nombreArchivo, (byte) 0);
        colocar(desplazamientoArchivo + nombreArchivo.length() + 1, modoDatos, (byte) 0);
    }


    public String nombreArchivo() {
        return this.get(desplazamientoArchivo, (byte) 0);
    }

    public String tipoSolicitud() {
        String nombreArchivo = nombreArchivo();
        return this.get(desplazamientoArchivo + nombreArchivo.length() + 1, (byte) 0);
    }
}


// Paquete ESCRITURA: coloca el código de operación y el nombre de archivo

final class EscrituraTFTP extends PaqueteTFTP {



    protected EscrituraTFTP() {
    }

    public EscrituraTFTP(String nombreArchivo, String modoDatos) {
        longitud = 2 + nombreArchivo.length() + 1 + modoDatos.length() + 1;
        mensaje = new byte[longitud];

        colocar(desplazamientoOperacion, tftpWRQ);
        colocar(desplazamientoArchivo, nombreArchivo, (byte) 0);
        colocar(desplazamientoArchivo + nombreArchivo.length() + 1, modoDatos, (byte) 0);
    }



    public String nombreArchivo() {
        return this.get(desplazamientoArchivo, (byte) 0);
    }

    public String tipoSolicitud() {
        String nombreArchivo = nombreArchivo();
        return this.get(desplazamientoArchivo + nombreArchivo.length() + 1, (byte) 0);
    }
}
