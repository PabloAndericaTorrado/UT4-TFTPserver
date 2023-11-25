import java.net.*;
import java.security.MessageDigest;
import java.io.*;
import java.util.*;
import java.util.zip.Checksum;

class TFTPClienteRRQ {
	protected InetAddress servidor;
	protected String nombreArchivo;
	protected String modoDatos;

	public TFTPClienteRRQ(InetAddress ip, String nombre, String modo) {
		servidor = ip;
		nombreArchivo = nombre;
		modoDatos = modo;

		try {// Crear socket y abrir archivo de salida
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(2000); // establecer tiempo de espera en 2s

			FileOutputStream archivoSalida = new FileOutputStream("../" + nombreArchivo); // carpeta principal
			// Enviar solicitud al servidor
			LecturaTFTP paqueteSolicitud = new LecturaTFTP(nombreArchivo, modoDatos);
			paqueteSolicitud.enviar(servidor, 6973, socket);

			ACKTFTP ack = null;
			InetAddress nuevaIP = servidor; // para transferencia
			int nuevoPuerto = 0; // para transferencia
			int limiteTiempo = 5;
			int pruebaPerdida = 1; // solo para pruebas

			// Procesar la transferencia
			System.out.println("Descargando");
			for (int numBloque = 1, bytesSalida = 512; bytesSalida == 512; numBloque++) {
				while (limiteTiempo != 0) {
					try {
						PaqueteTFTP paqueteEntrada = PaqueteTFTP.recibir(socket);
						// verificar tipo de paquete
						if (paqueteEntrada instanceof ErrorTFTP) {
							ErrorTFTP p = (ErrorTFTP) paqueteEntrada;
							throw new ExcepcionTftp(p.mensaje());
						} else if (paqueteEntrada instanceof DatosTFTP) {
							DatosTFTP p = (DatosTFTP) paqueteEntrada;

							// efecto visual para el usuario
							if (numBloque % 500 == 0) {
								System.out.print("\b.>");
							}
							if (numBloque % 15000 == 0) {
								System.out.println("\b.");
							}

							nuevaIP = p.obtenerDireccion();
							// verificar número de puerto
							if (nuevoPuerto != 0 && nuevoPuerto != p.obtenerPuerto()) { // puerto incorrecto
								continue; // ignorar este paquete
							}
							nuevoPuerto = p.obtenerPuerto();
							// verificar número de bloque

							if (/* pruebaPerdida==20|| */numBloque != p.numeroBloque()) { // datos antiguos
								// pruebaPerdida++;
								//System.out.println("@pruebaPerdida pérdida numBloque " + numBloque);
								throw new SocketTimeoutException();
							}
							// todo está bien, entonces escribir en el archivo
							bytesSalida = p.escribir(archivoSalida);
							// enviar ack al servidor
							ack = new ACKTFTP(numBloque);
							ack.enviar(nuevaIP, nuevoPuerto, socket);
							// pruebaPerdida++;
							break;
						} else
							throw new ExcepcionTftp("Respuesta inesperada del servidor");
					}
					// #######manejar tiempo de espera
					catch (SocketTimeoutException t) {
						// no hay respuesta para la solicitud de lectura, intentar de nuevo
						if (numBloque == 1) {
							System.out.println("No se pudo llegar al servidor");
							paqueteSolicitud.enviar(servidor, 6973, socket);
							limiteTiempo--;
						}
						// no hay respuesta para el último ack
						else {
							System.out.println("Tiempo de conexión agotado, reenviar último ack. límite de tiempo restante=" + limiteTiempo);
							ack = new ACKTFTP(numBloque - 1);
							ack.enviar(nuevaIP, nuevoPuerto, socket);
							limiteTiempo--;
						}
					}
				}
				if (limiteTiempo == 0) {
					throw new ExcepcionTftp("La conexión falló");
				}
			}
			System.out.println("\nDescarga finalizada.\nNombre de archivo: " + nombreArchivo);

			archivoSalida.close();
			socket.close();
		} catch (IOException e) {
			System.out.println("Error de E/S, transferencia abortada");
			File archivoIncorrecto = new File(nombreArchivo);
			archivoIncorrecto.delete();
		} catch (ExcepcionTftp e) {
			System.out.println(e.getMessage());
			File archivoIncorrecto = new File(nombreArchivo);
			archivoIncorrecto.delete();
		}
	}
}
