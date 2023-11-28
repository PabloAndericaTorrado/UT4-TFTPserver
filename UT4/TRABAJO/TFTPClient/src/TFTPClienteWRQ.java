

import java.net.*;
import java.io.*;
import java.util.*;

class TFTPClienteWRQ {
	protected InetAddress servidor;
	protected String nombreArchivo;
	protected String modoDatos;

	public TFTPClienteWRQ(InetAddress ip, String nombre, String modo) {
		servidor = ip;
		nombreArchivo = nombre;
		modoDatos = modo;

		try {

			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(2000);
			int limiteTiempo = 5;

			FileInputStream fuente = new FileInputStream("../" + nombreArchivo);


			EscrituraTFTP paqueteSolicitud = new EscrituraTFTP(nombreArchivo, modoDatos);
			paqueteSolicitud.enviar(servidor, 6973, socket);

			PaqueteTFTP respuestaEnvio = PaqueteTFTP.recibir(socket);

			int puerto = respuestaEnvio.obtenerPuerto();


			if (respuestaEnvio instanceof PaqueteTFTP) {
				PaqueteTFTP respuesta = (PaqueteTFTP) respuestaEnvio;
				System.out.println("--Servidor listo--\nSubiendo");
			} else if (respuestaEnvio instanceof ErrorTFTP) {
				ErrorTFTP respuesta = (ErrorTFTP) respuestaEnvio;
				fuente.close();
				System.err.println("ERROR");
				System.exit(1);
			}

			int bytesLeidos = PaqueteTFTP.longitudMaximaPaqueteTftp;



			for (int numBloque = 1; bytesLeidos == PaqueteTFTP.longitudMaximaPaqueteTftp; numBloque++) {
				DatosTFTP paqueteSalida = new DatosTFTP(numBloque, fuente);
				bytesLeidos = paqueteSalida.obtenerLongitud();
				paqueteSalida.enviar(servidor, puerto, socket);


				if (numBloque % 500 == 0) {
					System.out.print("\b.>");
				}
				if (numBloque % 15000 == 0) {
					System.out.println("\b.");
				}

				while (limiteTiempo != 0) {
					try {
						PaqueteTFTP ack = PaqueteTFTP.recibir(socket);
						if (!(ack instanceof PaqueteTFTP)) {
							break;
						}

						PaqueteTFTP a = (PaqueteTFTP) ack;


						if (puerto != a.obtenerPuerto()) {
							continue;
						}

						break;
					} catch (SocketTimeoutException t0) {
						System.out.println("Reenviar blk " + numBloque);
						paqueteSalida.enviar(servidor, puerto, socket);
						limiteTiempo--;
					}
				}

				if (limiteTiempo == 0) {
					System.err.println("ERROR de conexion");
					System.exit(1);
				}
			}

			fuente.close();
			socket.close();

			System.out.println("\n¡Subida finalizada!\nNombre de archivo: " + nombreArchivo);

		} catch (SocketTimeoutException t) {
			System.out.println("No hay respuesta del servidor, por favor intente nuevamente");
		} catch (IOException e) {
			System.out.println("Error de E/S, transferencia abortada");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}
