

## Pablo Andérica Torrado y Alejandro Julian Matías Gutierrez.

# Servidor y Cliente TFTP en Java

Este proyecto implementa un servidor y cliente TFTP (Trivial File Transfer Protocol) en Java. TFTP es un protocolo simple de transferencia de archivos utilizado para la transferencia de archivos a través de una red.

## Características

- **Servidor TFTP:** Permite la transferencia de archivos desde el servidor al cliente.
- **Cliente TFTP:** Solicita y recibe archivos del servidor.

## Requisitos
- Java SDK 17 o superior

## Operaciones TFTP Principales:

-   **RRQ (Read Request):** El cliente solicita leer un archivo al servidor.
-   **WRQ (Write Request):** El cliente solicita escribir un archivo al servidor.
-   **DATA:** Transferencia de datos desde el servidor al cliente.
-   **ACK (Acknowledgment):** Confirmación del cliente al servidor de la recepción de bloques de datos.
-   **ERROR:** Informa al cliente sobre errores durante la transferencia.

## Uso del Programa

- javac *.java
- **Lanzar Servidor**: java TFTPServer
- **Descargar**(Leer) archivo: **“java ClienteTFTP [host] L [nombreArchivo]”**
- **Subir**(Escribir) archivo: **“java ClienteTFTP [host] E [nombreArchivo]”**
- **Explicacion:**    
  Al descargar un archivo el Cliente puede leer un archivo que esta en el src del servidor.    
  Y al subir un archivo el Cliente escribe un fichero en el src del servidor y este lo guarda ahí.
- **Ejemplo:**    
  java TFTPServer    
  java ClienteTFTP localhost L server2.txt

## Explicación Código

### Cliente:

- **ClienteTFTP:**
  Este código Java implementa un cliente para el Protocolo TFTP. El programa recibe argumentos en la línea de comandos para especificar el host, la operación (lectura o escritura) y el nombre del archivo. Dependiendo del tipo, crea instancias de las clases `TFTPClienteRRQ` o `TFTPClienteWRQ`, que parecen manejar las operaciones de solicitud de lectura y escritura.
- **TFTPClienteRRQ**
  Esta clase implementa la lógica de descarga de archivos mediante la operación de solicitud de lectura (RRQ). La descarga se realiza en bloques de datos (512 bytes), y se incorpora control de errores y reintentos para garantizar la integridad de la transferencia.
- **TFTPClienteWRQ**
  Esta clase representa un cliente diseñado para enviar archivos al servidor. Utiliza el protocolo TFTP para gestionar la transferencia de datos y controlar errores durante el proceso de carga.
- **PaqueteTFTP**
##### Clase `DatosTFTP`:

-   Representa un paquete para la transferencia de datos.
-   Métodos:
  -   `numeroBloque()`: Devuelve el número de bloque.
  -   `escribir(FileOutputStream out)`: Escribe los datos del paquete en un flujo de salida (`FileOutputStream`).

##### Clase `ErrorTFTP`:

-   Representa un paquete TFTP para controlar errores.
-   Constructores:
  -   Crea un paquete de error con un número y mensaje específicos.
-   Métodos:
  -   `numero()`: Devuelve el número de error.
  -   `mensaje()`: Devuelve el mensaje de error.

##### Clase `LecturaTFTP`:

-   Representa un paquete TFTP para solicitudes de lectura.
-   Constructores:
  -   Crea un paquete de solicitud de lectura con el nombre del archivo y el modo de datos especificados.
-   Métodos:
  -   `nombreArchivo()`: Devuelve el nombre del archivo.
  -   `tipoSolicitud()`: Devuelve el tipo de solicitud.

##### Clase `EscrituraTFTP`:

-   Representa un paquete TFTP para solicitudes de escritura.
-   Constructores:
  -   Crea un paquete de solicitud de escritura con el nombre del archivo y el modo de datos especificados.
-   Métodos:
  -   `nombreArchivo()`: Devuelve el nombre del archivo.
  -   `tipoSolicitud()`: Devuelve el tipo de solicitud.

### Servidor:

-**TFTPServer:**
Este código establece un servidor TFTP que puede controlar solicitudes de lectura y escritura de clientes. La lógica específica de cómo se manejan estas solicitudes se implementa en las clases `TFTPserverRRQ` y `TFTPserverWRQ`
-**TFTPserverRRQ:**
Este código controla una solicitud de lectura, enviando bloques de datos al cliente. El proceso continúa hasta que se han transferido todos los bloques o se produce un error.
-**TFTPserverWRQ:**
Este código controla una solicitud de escritura, recibiendo bloques de datos del cliente y escribiéndolos en un archivo en el servidor. El proceso continúa hasta que recibe un bloque de datos con menos de 512 bytes.
-**TFTPpacket**
##### 1.  **`TFTPdata`:**

-  Extiende `TFTPpacket` y se utiliza para representar paquetes de datos en TFTP.
   -   Contiene el método `write` para escribir los datos en un `FileOutputStream`.
##### 2.  **`TFTPerror`:**

-   Extiende `TFTPpacket` y se utiliza para representar paquetes de error en TFTP.
    -   Tiene constructores para crear un paquete de error a partir de un número de error y un mensaje.
##### 3.  **`TFTPread`:**

-   Extiende `TFTPpacket` y se utiliza para representar paquetes de solicitud de lectura en TFTP.
    -   Tiene un constructor para crear un paquete de solicitud de lectura a partir de un nombre de archivo y un modo de datos.
##### 4.  **`TFTPwrite`:**

-   Extiende `TFTPpacket` y se utiliza para representar paquetes de solicitud de escritura.
    -   Tiene un constructor para crear un paquete de solicitud de escritura a partir de un nombre de archivo y un modo de datos.

 ## Documentación
 https://github.com/collinprice/TFTP/blob/master/src/server/TFTPServer.java
 RFC1350.txt
 https://github.com/JamieMZhang/TFTP/blob/master/TFTPServer/src/TFTPServer.java
 http://www.java2s.com/Code/Java/Network-Protocol/AsimpleJavatftpclient.htm
