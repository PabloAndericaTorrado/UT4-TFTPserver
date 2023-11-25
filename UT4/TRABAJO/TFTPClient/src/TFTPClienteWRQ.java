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
			// Crear socket y abrir archivo de salida
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(2000);
			int limiteTiempo = 5;

			FileInputStream fuente = new FileInputStream("../" + nombreArchivo);

			// Enviar solicitud al servidor

			EscrituraTFTP paqueteSolicitud = new EscrituraTFTP(nombreArchivo, modoDatos);
			paqueteSolicitud.enviar(servidor, 6973, socket);

			PaqueteTFTP respuestaEnvio = PaqueteTFTP.recibir(socket);

			int puerto = respuestaEnvio.obtenerPuerto(); // nuevo puerto para la transferencia

			// Verificar el tipo de paquete
			if (respuestaEnvio instanceof PaqueteTFTP) {
				PaqueteTFTP respuesta = (PaqueteTFTP) respuestaEnvio;
				System.out.println("--Servidor listo--\nSubiendo");
			} else if (respuestaEnvio instanceof ErrorTFTP) {
				ErrorTFTP respuesta = (ErrorTFTP) respuestaEnvio;
				fuente.close();
				throw new ExcepcionTftp(respuesta.mensaje());
			}

			int bytesLeidos = PaqueteTFTP.longitudMaximaPaqueteTftp;

			// Procesar la transferencia

			for (int numBloque = 1; bytesLeidos == PaqueteTFTP.longitudMaximaPaqueteTftp; numBloque++) {
				DatosTFTP paqueteSalida = new DatosTFTP(numBloque, fuente);
				bytesLeidos = paqueteSalida.obtenerLongitud();
				paqueteSalida.enviar(servidor, puerto, socket); // enviar el paquete

				// Efecto visual para el usuario
				if (numBloque % 500 == 0) {
					System.out.print("\b.>");
				}
				if (numBloque % 15000 == 0) {
					System.out.println("\b.");
				}

				while (limiteTiempo != 0) { // esperar el ack correcto
					try {
						PaqueteTFTP ack = PaqueteTFTP.recibir(socket);
						if (!(ack instanceof PaqueteTFTP)) {
							break;
						}

						PaqueteTFTP a = (PaqueteTFTP) ack;

						// número de puerto incorrecto
						if (puerto != a.obtenerPuerto()) {
							continue; // ignorar este paquete
						}

						break;
					} catch (SocketTimeoutException t0) {
						System.out.println("Reenviar blk " + numBloque);
						paqueteSalida.enviar(servidor, puerto, socket); // reenviar el último paquete
						limiteTiempo--;
					}
				} // fin del bucle while

				if (limiteTiempo == 0) {
					throw new ExcepcionTftp("falló la conexión");
				}
			}

			fuente.close();
			socket.close();

			System.out.println("\n¡Subida finalizada!\nNombre de archivo: " + nombreArchivo);

		} catch (SocketTimeoutException t) {
			System.out.println("No hay respuesta del servidor, por favor intente nuevamente");
		} catch (IOException e) {
			System.out.println("Error de E/S, transferencia abortada");
		} catch (ExcepcionTftp e) {
			System.out.println(e.getMessage());
		}
	}
}
