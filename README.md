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

- Descargar(Leer) archivo: **“ClienteTFTP [host] L [nombreArchivo]”**
- Subir(Escribir) archivo: **“ClienteTFTP [host] E [nombreArchivo]”**
- **Explicacion:**
Al descargar un archivo el Cliente puede leer un archivo que esta en el src del servidor.
Y al subir un archivo el Cliente escribe un fichero en el src del servidor y este lo guarda ahí.