
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

		try {
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(2000);

			FileOutputStream archivoSalida = new FileOutputStream("../" + nombreArchivo);

			LecturaTFTP paqueteSolicitud = new LecturaTFTP(nombreArchivo, modoDatos);
			paqueteSolicitud.enviar(servidor, 6973, socket);

			ACKTFTP ack = null;
			InetAddress nuevaIP = servidor;
			int nuevoPuerto = 0;
			int limiteTiempo = 5;
			int pruebaPerdida = 1;


			System.out.println("Descargando");
			for (int numBloque = 1, bytesSalida = 512; bytesSalida == 512; numBloque++) {
				while (limiteTiempo != 0) {
					try {
						PaqueteTFTP paqueteEntrada = PaqueteTFTP.recibir(socket);

						if (paqueteEntrada instanceof ErrorTFTP) {
							ErrorTFTP p = (ErrorTFTP) paqueteEntrada;
							System.err.println("ERROR");
							System.exit(1);
						} else if (paqueteEntrada instanceof DatosTFTP) {
							DatosTFTP p = (DatosTFTP) paqueteEntrada;


							if (numBloque % 500 == 0) {
								System.out.print("\b.>");
							}
							if (numBloque % 15000 == 0) {
								System.out.println("\b.");
							}

							nuevaIP = p.obtenerDireccion();

							if (nuevoPuerto != 0 && nuevoPuerto != p.obtenerPuerto()) {
								continue;
							}
							nuevoPuerto = p.obtenerPuerto();


							if (numBloque != p.numeroBloque()) {
								throw new SocketTimeoutException();
							}

							bytesSalida = p.escribir(archivoSalida);

							ack = new ACKTFTP(numBloque);
							ack.enviar(nuevaIP, nuevoPuerto, socket);

							break;
						} else
							System.err.println("ERROR de servidor");
						System.exit(1);
					}

					catch (SocketTimeoutException t) {

						if (numBloque == 1) {
							System.out.println("No se pudo llegar al servidor");
							paqueteSolicitud.enviar(servidor, 6973, socket);
							limiteTiempo--;
						}

						else {
							System.out.println("Tiempo de conexión agotado, reenviar último ack. límite de tiempo restante=" + limiteTiempo);
							ack = new ACKTFTP(numBloque - 1);
							ack.enviar(nuevaIP, nuevoPuerto, socket);
							limiteTiempo--;
						}
					}
				}
				if (limiteTiempo == 0) {
					System.err.println("ERROR de conexion");
					System.exit(1);
				}
			}
			System.out.println("\nDescarga finalizada.\nNombre de archivo: " + nombreArchivo);

			archivoSalida.close();
			socket.close();
		} catch (IOException e) {
			System.out.println("Error de E/S, transferencia abortada");
			File archivoIncorrecto = new File(nombreArchivo);
			archivoIncorrecto.delete();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			File archivoIncorrecto = new File(nombreArchivo);
			archivoIncorrecto.delete();
		}
	}
}
