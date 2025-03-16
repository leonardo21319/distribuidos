/*
  ServidorHTTP.java
*/

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

class ServidorHTTP {
  
  // Simulamos la fecha de última modificación de un recurso.
  static Date lastModified = new Date(); // Esta será la fecha de la última modificación del recurso.

  static class Worker extends Thread {
    Socket conexion;

    Worker(Socket conexion) {
      this.conexion = conexion;
    }

    // Esta función devuelve el valor de los parámetros "a", "b", y "c" enviados en la URL.
    int valor(String parametros, String variable) throws Exception {
      String[] p = parametros.split("&");
      for (int i = 0; i < p.length; i++) {
        String[] s = p[i].split("=");
        if (s[0].equals(variable))
          return Integer.parseInt(s[1]);
      }
      throw new Exception("Se espera la variable: " + variable);
    }

    public void run() {
      try {
        BufferedReader entrada = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
        PrintWriter salida = new PrintWriter(conexion.getOutputStream());

        String req = entrada.readLine();
        System.out.println("Petición: " + req);

        // Leer los encabezados de la solicitud
        String ifModifiedSince = null;
        for (;;) {
          String encabezado = entrada.readLine();
          System.out.println("Encabezado: " + encabezado);
          if (encabezado.equals("")) break;

          if (encabezado.startsWith("If-Modified-Since: ")) {
            ifModifiedSince = encabezado.split(": ")[1];
          }
        }

        // Comprobar si el recurso ha sido modificado
        boolean isModified = true;
        if (ifModifiedSince != null) {
          // Comparamos la fecha recibida en "If-Modified-Since" con la última modificación
          Date lastModifiedDate = new Date(lastModified.getTime());
          if (ifModifiedSince.equals(lastModifiedDate.toString())) {
            isModified = false; // Si no ha cambiado, respondemos con 304
          }
        }

        if (req.startsWith("GET / ")) {
          // Si el recurso no ha cambiado, devolver un 304 Not Modified
          if (!isModified) {
            salida.println("HTTP/1.1 304 Not Modified");
            salida.println("Connection: close");
            salida.println();
            salida.flush();
            return;
          }

          // Responder con el contenido solicitado
          String respuesta =
            "<html>" +
              "<script>" +
                "function get(req,callback){" +
                  "const xhr = new XMLHttpRequest();" +
                  "xhr.open('GET', req, true);" +
                  "xhr.onload=function(){" +
                    "if (callback != null) callback(xhr.status,xhr.response);" +
                  "};" +
                  "xhr.send();" +
                "}" +
              "</script>" +
              "<body>" +
                "<button onclick=\"get('/suma?a=1&b=2&c=3',function(status,response){alert(status + ' ' + response);})\">Aceptar</button>" +
              "</body>" +
            "</html>";

          // Incluir el encabezado LAST-MODIFIED
          salida.println("HTTP/1.1 200 OK");
          salida.println("Content-type: text/html; charset=utf-8");
          salida.println("Content-length: " + respuesta.length());
          salida.println("Connection: close");
          salida.println("Last-Modified: " + lastModified); // Fecha de última modificación
          salida.println();
          salida.println(respuesta);
          salida.flush();

        } else if (req.startsWith("GET /suma?")) {
          String parametros = req.split(" ")[1].split("\\?")[1];
          String respuesta = String.valueOf(valor(parametros, "a") + valor(parametros, "b") + valor(parametros, "c"));

          // Incluir el encabezado LAST-MODIFIED
          salida.println("HTTP/1.1 200 OK");
          salida.println("Content-type: text/plain; charset=utf-8");
          salida.println("Content-length: " + respuesta.length());
          salida.println("Connection: close");
          salida.println("Last-Modified: " + lastModified); // Fecha de última modificación
          salida.println();
          salida.println(respuesta);
          salida.flush();
        } else {
          salida.println("HTTP/1.1 404 File Not Found");
          salida.flush();
        }

      } catch (Exception e) {
        System.err.println("Error en la conexión: " + e.getMessage());
      } finally {
        try {
          conexion.close();
        } catch (Exception e) {
          System.err.println("Error en close: " + e.getMessage());
        }
      }
    }
  }

  public static void main(String[] args) throws Exception {
    ServerSocket servidor = new ServerSocket(80);

    for (;;) {
      Socket conexion = servidor.accept();
      new Worker(conexion).start();
    }
  }
}
