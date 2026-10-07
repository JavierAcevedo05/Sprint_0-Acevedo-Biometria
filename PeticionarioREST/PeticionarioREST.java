package org.jordi.clienterestandroid;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.os.AsyncTask;
import android.util.Log;

// ------------------------------------------------------------------------
// Clase que realiza peticiones HTTP REST en segundo plano usando AsyncTask.
//
// Patron de uso:
//   1. Crear instancia: PeticionarioREST p = new PeticionarioREST();
//   2. Lanzar peticion: p.hacerPeticionREST("GET", url, null, callback);
//   3. Recibir resultado en el callback: callback(codigoHTTP, cuerpoRespuesta)
// ------------------------------------------------------------------------
public class PeticionarioREST extends AsyncTask<Void, Void, Boolean> {

    // --------------------------------------------------------------------
    // Interfaz callback para recibir la respuesta REST
    // --------------------------------------------------------------------
    public interface RespuestaREST {
        void callback(int codigo, String cuerpo);
    }

    // Datos de la peticion (se rellenan antes de lanzar el hilo)
    private String elMetodo;
    private String urlDestino;
    private String elCuerpo = null;
    private RespuestaREST laRespuesta;

    // Resultado de la peticion
    private int codigoRespuesta;
    private String cuerpoRespuesta = "";

    // --------------------------------------------------------------------
    // hacerPeticionREST(): metodo publico para lanzar una peticion HTTP
    //
    // Parametros:
    //   metodo:     "GET", "POST", "PUT", "DELETE", etc.
    //   urlDestino: URL completa del servidor
    //   cuerpo:     Body de la peticion (JSON, XML, etc.) o null para GET
    //   laRespuesta: callback que recibira el codigo HTTP y el cuerpo
    // --------------------------------------------------------------------
    public void hacerPeticionREST(String metodo, String urlDestino, String cuerpo, RespuestaREST laRespuesta) {
        this.elMetodo = metodo;
        this.urlDestino = urlDestino;
        this.elCuerpo = cuerpo;
        this.laRespuesta = laRespuesta;

        this.execute(); // otro thread ejecutara doInBackground()
    }

    // --------------------------------------------------------------------
    // constructor
    // --------------------------------------------------------------------
    public PeticionarioREST() {
        Log.d("clienterestandroid", "constructor()");
    }

    // --------------------------------------------------------------------
    // doInBackground(): ejecuta la peticion HTTP en un hilo de fondo
    //
    // Flujo:
    //   1. Abrir conexion HTTP
    //   2. Configurar metodo, headers y body
    //   3. Enviar peticion
    //   4. Leer respuesta
    //   5. Devolver true (exito) o false (error)
    // --------------------------------------------------------------------
    @Override
    protected Boolean doInBackground(Void... params) {
        Log.d("clienterestandroid", "doInBackground()");

        HttpURLConnection connection = null;

        try {
            Log.d("clienterestandroid", "doInBackground() me conecto a >" + urlDestino + "<");

            // Crear conexion HTTP
            URL url = new URL(urlDestino);
            connection = (HttpURLConnection) url.openConnection();

            // Configurar peticion
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestMethod(this.elMetodo);
            connection.setConnectTimeout(5000);  // 5 segundos timeout conexion
            connection.setReadTimeout(5000);     // 5 segundos timeout lectura
            connection.setDoInput(true);

            // Enviar body si no es GET y hay cuerpo
            if (!this.elMetodo.equals("GET") && this.elCuerpo != null) {
                Log.d("clienterestandroid", "doInBackground(): no es GET, pongo cuerpo");
                connection.setDoOutput(true);
                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                dos.writeBytes(this.elCuerpo);
                dos.flush();
                dos.close();
            }

            // Peticion enviada, ahora obtener respuesta
            Log.d("clienterestandroid", "doInBackground(): peticion enviada");

            int rc = connection.getResponseCode();
            String rm = connection.getResponseMessage();
            Log.d("clienterestandroid", "doInBackground() recibo respuesta = " + rc + " : " + rm);
            this.codigoRespuesta = rc;

            // Leer body: getInputStream() para exito (2xx/3xx), getErrorStream() para errores (4xx/5xx)
            InputStream is;
            if (rc >= 400) {
                is = connection.getErrorStream();
            } else {
                is = connection.getInputStream();
            }

            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                Log.d("clienterestandroid", "leyendo cuerpo");
                StringBuilder acumulador = new StringBuilder();
                String linea;
                while ((linea = br.readLine()) != null) {
                    Log.d("clienterestandroid", linea);
                    acumulador.append(linea);
                }
                Log.d("clienterestandroid", "FIN leyendo cuerpo");

                this.cuerpoRespuesta = acumulador.toString();
                Log.d("clienterestandroid", "cuerpo recibido=" + this.cuerpoRespuesta);
            }

            // Cerrar conexion siempre (exito o error)
            if (connection != null) {
                connection.disconnect();
            }

            return true;

        } catch (Exception ex) {
            Log.d("clienterestandroid", "doInBackground(): excepcion: " + ex.getMessage());
            // Cerrar conexion en caso de error tambien
            if (connection != null) {
                connection.disconnect();
            }
        }

        return false;
    } // ()

    // --------------------------------------------------------------------
    // onPostExecute(): se ejecuta en el hilo UI despues de doInBackground()
    // Llama al callback con el codigo HTTP y el cuerpo de la respuesta
    // --------------------------------------------------------------------
    protected void onPostExecute(Boolean comoFue) {
        Log.d("clienterestandroid", "onPostExecute() comoFue = " + comoFue);

        if (this.laRespuesta != null) {
            this.laRespuesta.callback(this.codigoRespuesta, this.cuerpoRespuesta);
        }
    }

} // class


