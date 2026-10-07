package org.jordi.holamundoservicio;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

// -------------------------------------------------------------------------------------------------
// Servicio en segundo plano que ejecuta un bucle de espera/logging.
//
// NOTA: IntentService esta deprecated desde API 30. Para proyectos nuevos usar
//       JobIntentService o WorkManager. Este codigo es para fines educativos.
//
// Flujo:
//   1. Se inicia con startService() pasando "tiempoDeEspera" en el Intent
//   2. onHandleIntent() ejecuta un bucle en un worker thread
//   3. El bucle duerme, muestra un log y repite
//   4. Se detiene con parar() o stopService()
// -------------------------------------------------------------------------------------------------
public class ServicioEscuharBeacons extends IntentService {

    private static final String ETIQUETA_LOG = ">>>>";

    // Tiempo de pausa entre iteraciones del bucle (en milisegundos)
    private long tiempoDeEspera = 10000;

    // Flag para controlar el bucle. volatile: garantiza visibilidad entre threads
    private volatile boolean seguir = true;

    // ---------------------------------------------------------------------------------------------
    // Constructor: nombre del worker thread ("HelloIntentService" es el ejemplo original)
    // ---------------------------------------------------------------------------------------------
    public ServicioEscuharBeacons() {
        super("HelloIntentService");
        Log.d(ETIQUETA_LOG, "ServicioEscucharBeacons.constructor: termina");
    }

    // ---------------------------------------------------------------------------------------------
    // parar(): detiene el servicio y el bucle
    // Se puede llamar desde el main thread para parar el servicio
    // ---------------------------------------------------------------------------------------------
    public void parar() {
        Log.d(ETIQUETA_LOG, "ServicioEscucharBeacons.parar()");

        if (this.seguir == false) {
            return; // ya esta parado
        }

        this.seguir = false;  // El bucle en onHandleIntent() se detendra
        this.stopSelf();      // Detiene el servicio

        Log.d(ETIQUETA_LOG, "ServicioEscucharBeacons.parar(): acaba");
    }

    // ---------------------------------------------------------------------------------------------
    // onDestroy(): se ejecuta cuando el sistema destruye el servicio
    // ---------------------------------------------------------------------------------------------
    public void onDestroy() {
        Log.d(ETIQUETA_LOG, "ServicioEscucharBeacons.onDestroy()");
        this.parar();
    }

    // ---------------------------------------------------------------------------------------------
    // onHandleIntent(): METODO PRINCIPAL - se ejecuta en un WORKER THREAD
    //
    // Recibe el Intent con el parametro "tiempoDeEspera" y ejecuta un bucle
    // que duerme, muestra un log y repite hasta que seguir sea false.
    //
    // Cuando este metodo termina, IntentService para el servicio automaticamente.
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onHandleIntent(Intent intent) {
        // Leer tiempo de espera del Intent (default: 50000ms = 50s)
        this.tiempoDeEspera = intent.getLongExtra("tiempoDeEspera", 50000);
        this.seguir = true;

        long contador = 1;

        Log.d(ETIQUETA_LOG, "ServicioEscucharBeacons.onHandleIntent: empieza thread="
                + Thread.currentThread().getId());

        try {
            while (this.seguir) {
                // Dormir el tiempo especificado
                Thread.sleep(tiempoDeEspera);

                Log.d(ETIQUETA_LOG, "ServicioEscucharBeacons.onHandleIntent: tras la espera: " + contador);
                contador++;
            }

            Log.d(ETIQUETA_LOG, "ServicioEscucharBeacons.onHandleIntent: tarea terminada");

        } catch (InterruptedException e) {
            // Restaurar el estado de interrupcion del thread
            Log.d(ETIQUETA_LOG, "ServicioEscucharBeacons.onHandleIntent: problema con el thread");
            Thread.currentThread().interrupt();
        }

        Log.d(ETIQUETA_LOG, "ServicioEscucharBeacons.onHandleIntent: termina");
    }
}
