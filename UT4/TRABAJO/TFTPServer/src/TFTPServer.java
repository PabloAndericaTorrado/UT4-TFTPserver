import java.net.*;
import java.io.*;
import java.util.*;

public class TFTPServer {

	public static void main(String argv[]) {
		try {
			// Usar puerto 6973
			DatagramSocket socket = new DatagramSocket(6973);
			System.out.println("Servidor listo. Puerto: " + socket.getLocalPort());

			// Escuchar solicitudes
			while (true) {
				TFTPpacket entrada = TFTPpacket.receive(socket);
				// Recibir solicitud de lectura
				if (entrada instanceof TFTPread) {
					System.out.println("Solicitud de lectura desde " + entrada.getAddress());
					TFTPserverRRQ r = new TFTPserverRRQ((TFTPread)entrada);
				}
				// Recibir solicitud de escritura
				else if (entrada instanceof TFTPwrite) {
					System.out.println("Solicitud de escritura desde " + entrada.getAddress());
					TFTPserverWRQ w = new TFTPserverWRQ((TFTPwrite) entrada);
				}
			}
		} catch (SocketException e) {
			System.out.println("Servidor terminado (SocketException): " + e.getMessage());
		} catch (TftpException e) {
			System.out.println("Servidor terminado (TftpException): " + e.getMessage());
		} catch (IOException e) {
			System.out.println("Servidor terminado (IOException): " + e.getMessage());
		}
	}
}
