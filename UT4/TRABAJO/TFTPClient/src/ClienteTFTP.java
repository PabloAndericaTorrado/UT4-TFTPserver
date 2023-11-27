
import java.net.InetAddress;
import java.net.UnknownHostException;


public class ClienteTFTP {
	public static void main(String argv[]){
		String host = "";
		String nombreArchivo = "";
		String modo = "octeto"; // modo predeterminado
		String tipo = "";
		try {
			// Procesar línea de comandos
			if (argv.length == 0)
				System.err.println("--Uso--:  ClienteTFTP [host] [Tipo(L/E?)] [nombreArchivo]");
			// usar modo predeterminado (octeto)
			if (argv.length == 3) {
				host = argv[0];
				tipo = argv[argv.length - 2];
				nombreArchivo = argv[argv.length - 1];
			}
			// usar otros modos
			else if (argv.length == 4) {
				host = argv[0];
				modo = argv[argv.length - 1];
				tipo = argv[argv.length - 3];
				nombreArchivo = argv[argv.length - 2];
			} else
				System.err.println("comando incorrecto. \n--Uso-- \nmodo:  ClienteTFTP [host] [Tipo(L/E?)] [nombreArchivo]");

			InetAddress servidor = InetAddress.getByName(host);

			// procesar solicitud de lectura
			if (tipo.matches("L")) {
				TFTPClienteRRQ r = new TFTPClienteRRQ(servidor, nombreArchivo, modo);
			}
			// procesar solicitud de escritura
			else if (tipo.matches("E")) {
				TFTPClienteWRQ w = new TFTPClienteWRQ(servidor, nombreArchivo, modo);
			} else {
				System.err.println("comando incorrecto. \n--Uso-- \nmodo octeto:  ClienteTFTP [host] [Tipo(L/E?)] [nombreArchivo] \notro modo:  ClienteTFTP [host] [Tipo(L/E?)] [nombreArchivo] [modo]");
			}

		} catch (UnknownHostException e) {
			System.out.println("Host desconocido " + host);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
