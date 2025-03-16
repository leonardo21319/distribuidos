/*Proxy Inverso
 * author Leonardo Dominguez
*/

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

class AdministradorTrafico {

  static String host_remoto_1;
  static int puerto_remoto_1;
  static String host_remoto_2;
  static int puerto_remoto_2;
  static int puerto_local;

  static class Worker_1 extends Thread {
    Socket cliente_1, cliente_2;

    Worker_1(Socket cliente_1) {
      this.cliente_1 = cliente_1;
    }

    public void run() {
      try {
        // Conexión al primer servidor (Servidor-1)
        cliente_2 = new Socket(host_remoto_1, puerto_remoto_1);

        // Thread que dirige el tráfico del servidor (Servidor-1) al cliente
        new Worker_2(cliente_1, cliente_2).start();

        InputStream entrada_1 = cliente_1.getInputStream();
        OutputStream salida_2 = cliente_2.getOutputStream();
        byte[] buffer = new byte[1024];
        int n;

        // Leer datos del cliente y enviarlos al servidor remoto (Servidor-1)
        while ((n = entrada_1.read(buffer)) != -1) {
          salida_2.write(buffer, 0, n);
          salida_2.flush();
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if (cliente_1 != null) cliente_1.close();
          if (cliente_2 != null) cliente_2.close();
        } catch (IOException e2) {
          e2.printStackTrace();
        }
      }
    }
  }

  static class Worker_2 extends Thread {
    Socket cliente_1, cliente_2;

    Worker_2(Socket cliente_1, Socket cliente_2) {
      this.cliente_1 = cliente_1;
      this.cliente_2 = cliente_2;
    }

    public void run() {
      try {
        InputStream entrada_2 = cliente_2.getInputStream();
        OutputStream salida_1 = cliente_1.getOutputStream();
        byte[] buffer = new byte[4096];
        int n;

        // Leer datos del servidor remoto (Servidor-1) y enviarlos al cliente
        while ((n = entrada_2.read(buffer)) != -1) {
          salida_1.write(buffer, 0, n);
          salida_1.flush();
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if (cliente_1 != null) cliente_1.close();
          if (cliente_2 != null) cliente_2.close();
        } catch (IOException e2) {
          e2.printStackTrace();
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    // Verificar que se pasen 5 parámetros
    if (args.length != 5) {
      System.err.println("Uso:\njava AdministradorTrafico <puerto-local> <host-remoto-1> <puerto-remoto-1> <host-remoto-2> <puerto-remoto-2>");
      System.exit(1);
    }

    // Asignación de parámetros de entrada
    puerto_local = Integer.parseInt(args[0]);  // Puerto donde escucha el proxy
    host_remoto_1 = args[1];  // IP del Servidor-1
    puerto_remoto_1 = Integer.parseInt(args[2]);  // Puerto del Servidor-1
    host_remoto_2 = args[3];  // IP del Servidor-2
    puerto_remoto_2 = Integer.parseInt(args[4]);  // Puerto del Servidor-2

    // Imprimir los parámetros recibidos
    System.out.println("Puerto Local: " + puerto_local);
    System.out.println("Host Servidor-1: " + host_remoto_1 + ", Puerto: " + puerto_remoto_1);
    System.out.println("Host Servidor-2: " + host_remoto_2 + ", Puerto: " + puerto_remoto_2);

    // Crear el servidor en el puerto indicado
    ServerSocket ss = new ServerSocket(puerto_local);

    for (;;) {
      // Espera una conexión del cliente
      Socket cliente_1 = ss.accept();

      // Thread que dirige el tráfico del cliente a los servidores (Servidor-1 y Servidor-2)
      new Worker_1(cliente_1).start();
    }
  }
}
