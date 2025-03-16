import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class AdministradorTraficoSSL {

    static String host_remoto_1;
    static int puerto_remoto_1;
    static String host_remoto_2;
    static int puerto_remoto_2;
    static int puerto_local;

    public static void main(String[] args) throws Exception {

        if (args.length != 5) {
            System.err.println("Uso:\njava AdministradorTraficoSSL <puerto-local> <host-remoto-1> <puerto-remoto-1> <host-remoto-2> <puerto-remoto-2>");
            System.exit(1);
        }

        // Asignación de parámetros de entrada
        puerto_local = Integer.parseInt(args[0]);
        host_remoto_1 = args[1];
        puerto_remoto_1 = Integer.parseInt(args[2]);
        host_remoto_2 = args[3];
        puerto_remoto_2 = Integer.parseInt(args[4]);

        // Configurar keystore del servidor SSL
        System.setProperty("javax.net.ssl.keyStore", "keystore_servidor.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "1234567");

        // Crear un servidor SSL que escuche en el puerto especificado
        SSLServerSocketFactory socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        ServerSocket ss = socket_factory.createServerSocket(puerto_local);

        System.out.println("Proxy SSL ejecutándose en el puerto: " + puerto_local);

        while (true) {
            // Espera conexiones del navegador (cliente HTTPS)
            Socket cliente = ss.accept();

            // Crea un hilo para manejar la conexión
            new Worker_1(cliente).start();
        }
    }

    // Hilo que atiende a un cliente HTTPS y reenvía a Servidor-1 y Servidor-2
    static class Worker_1 extends Thread {
        Socket cliente_1, cliente_2, cliente_3;

        Worker_1(Socket cliente_1) {
            this.cliente_1 = cliente_1;
        }

        public void run() {
            try {
                // Conexión a los servidores backend (HTTP sin SSL)
                cliente_2 = new Socket(host_remoto_1, puerto_remoto_1);
                cliente_3 = new Socket(host_remoto_2, puerto_remoto_2);

                // Hilo para recibir del servidor-1 y enviar al navegador (cliente HTTPS)
                new Worker_2(cliente_1, cliente_2).start();

                // Hilo para recibir del servidor-2 pero NO reenviarlo al navegador
                new Worker_3(cliente_3).start();

                // Leer la petición del navegador (cliente_1) y reenviarla a ambos servidores backend
                InputStream entrada_cliente = cliente_1.getInputStream();
                OutputStream salida_servidor_1 = cliente_2.getOutputStream();
                OutputStream salida_servidor_2 = cliente_3.getOutputStream();

                byte[] buffer = new byte[4096];
                int n;

                while ((n = entrada_cliente.read(buffer)) != -1) {
                    salida_servidor_1.write(buffer, 0, n);
                    salida_servidor_1.flush();

                    salida_servidor_2.write(buffer, 0, n);
                    salida_servidor_2.flush();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (cliente_1 != null) cliente_1.close();
                    if (cliente_2 != null) cliente_2.close();
                    if (cliente_3 != null) cliente_3.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    // Hilo que recibe respuesta del servidor-1 y la reenvía al navegador
    static class Worker_2 extends Thread {
        Socket cliente_1, cliente_2;

        Worker_2(Socket cliente_1, Socket cliente_2) {
            this.cliente_1 = cliente_1;
            this.cliente_2 = cliente_2;
        }

        public void run() {
            try {
                InputStream entrada_servidor = cliente_2.getInputStream();
                OutputStream salida_cliente = cliente_1.getOutputStream();

                byte[] buffer = new byte[4096];
                int n;

                while ((n = entrada_servidor.read(buffer)) != -1) {
                    salida_cliente.write(buffer, 0, n);
                    salida_cliente.flush();
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

    // Hilo que recibe respuesta del servidor-2, pero NO hace nada con ella
    static class Worker_3 extends Thread {
        Socket cliente_2;

        Worker_3(Socket cliente_2) {
            this.cliente_2 = cliente_2;
        }

        public void run() {
            try {
                InputStream entrada_servidor = cliente_2.getInputStream();

                byte[] buffer = new byte[4096];
                int n;

                // Solo lee, no envía a ningún lado
                while ((n = entrada_servidor.read(buffer)) != -1) {
                    // Simulación de recepción, puedes imprimir si deseas ver los datos
                    // System.out.write(buffer, 0, n);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (cliente_2 != null) cliente_2.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }
}
