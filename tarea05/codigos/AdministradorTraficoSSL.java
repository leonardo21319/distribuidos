import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

public class AdministradorTraficoSSL {
    static String host_remoto_1;
    static int puerto_remoto_1;
    static String host_remoto_2;
    static int puerto_remoto_2;
    static int puerto_local;
    
    // Ruta del keystore y contraseña
    static String keystorePath = "server.keystore"; // Ruta del keystore
    static String keystorePassword = "AsukaLeva02."; // Contraseña del keystore

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.err.println("Uso: java AdministradorTraficoSSL <host-remoto-1> <puerto-remoto-1> <host-remoto-2> <puerto-remoto-2> <puerto-local>");
            System.exit(1);
        }

        host_remoto_1 = args[0];
        puerto_remoto_1 = Integer.parseInt(args[1]);
        host_remoto_2 = args[2];
        puerto_remoto_2 = Integer.parseInt(args[3]);
        puerto_local = Integer.parseInt(args[4]);

        // Configuración SSL para el keystore
        System.setProperty("javax.net.ssl.keyStore", keystorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.trustStore", keystorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", keystorePassword);

        // Inicializar el contexto SSL
        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

        // Cargar el keystore
        FileInputStream keyStoreStream = new FileInputStream(keystorePath);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(keyStoreStream, keystorePassword.toCharArray());
        kmf.init(keyStore, keystorePassword.toCharArray());
        tmf.init(keyStore);

        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        // Iniciar la conexión SSL utilizando el contexto configurado
        ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket(puerto_local);
        System.out.println("Escuchando en el puerto local " + puerto_local);
        
        // Esperar las conexiones
        for(;;) {
            Socket cliente_1 = serverSocket.accept();
            // Hilo que dirige el tráfico del cliente al servidor remoto
            new Worker_1(cliente_1).start();
        }
    }

    static class Worker_1 extends Thread {
        Socket cliente_1, servidor_1, servidor_2;

        Worker_1(Socket cliente_1) {
            this.cliente_1 = cliente_1;
        }

        public void run() {
            try {
                // Se conecta al primer host remoto
                servidor_1 = new Socket(host_remoto_1, puerto_remoto_1);

                // Se conecta al segundo host remoto
                servidor_2 = new Socket(host_remoto_2, puerto_remoto_2);

                // Hilo que dirige el tráfico del host remoto 1 al cliente
                new Worker_2(cliente_1, servidor_1).start();

                InputStream entrada_1 = cliente_1.getInputStream();
                OutputStream salida_1 = servidor_1.getOutputStream();
                OutputStream salida_2 = servidor_2.getOutputStream();
                byte[] buffer = new byte[1024];
                int n;

                while((n = entrada_1.read(buffer)) != -1) {
                    // Reenvía los datos del cliente al primer servidor remoto
                    salida_1.write(buffer, 0, n);
                    salida_1.flush();

                    // Reenvía los datos del cliente al segundo servidor remoto (sin esperar la respuesta)
                    salida_2.write(buffer, 0, n);
                    salida_2.flush();
                }
            } catch (IOException e) {
            } finally {
                try {
                    if (cliente_1 != null) cliente_1.close();
                    if (servidor_1 != null) servidor_1.close();
                    if (servidor_2 != null) servidor_2.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    static class Worker_2 extends Thread {
        Socket cliente_1, servidor_1;

        Worker_2(Socket cliente_1, Socket servidor_1) {
            this.cliente_1 = cliente_1;
            this.servidor_1 = servidor_1;
        }

        public void run() {
            try {
                InputStream entrada_1 = servidor_1.getInputStream();
                OutputStream salida_1 = cliente_1.getOutputStream();
                byte[] buffer = new byte[4096];
                int n;

                while((n = entrada_1.read(buffer)) != -1) {
                    // Solo reenvía la respuesta del servidor 1 al cliente
                    salida_1.write(buffer, 0, n);
                    salida_1.flush();
                }
            } catch (IOException e) {
            } finally {
                try {
                    if (cliente_1 != null) cliente_1.close();
                    if (servidor_1 != null) servidor_1.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }
}
