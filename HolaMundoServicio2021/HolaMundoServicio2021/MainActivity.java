package org.jordi.holamundoservicio;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

// -------------------------------------------------------------------------------------------------
// Activity que controla el servicio ServicioEscuharBeacons.
//
// Dos botones:
//   - "Arrancar": inicia el servicio en segundo plano
//   - "Detener": para el servicio
// -------------------------------------------------------------------------------------------------
public class MainActivity extends AppCompatActivity {

    private static final String ETIQUETA_LOG = ">>>>";

    // Intent del servicio (null = servicio parado, != null = servicio activo)
    private Intent elIntentDelServicio = null;

    // ---------------------------------------------------------------------------------------------
    // botonArrancarServicioPulsado(): arranca el servicio en segundo plano
    // Se llama desde el XML (android:onClick="botonArrancarServicioPulsado")
    // ---------------------------------------------------------------------------------------------
    public void botonArrancarServicioPulsado(View v) {
        Log.d(ETIQUETA_LOG, "boton arrancar servicio pulsado");

        if (this.elIntentDelServicio != null) {
            // Ya estaba arrancado, no hacer nada
            return;
        }

        Log.d(ETIQUETA_LOG, "voy a arrancar el servicio");

        // Crear Intent para iniciar el servicio
        this.elIntentDelServicio = new Intent(this, ServicioEscuharBeacons.class);

        // Pasar el tiempo de espera como extra (en milisegundos)
        this.elIntentDelServicio.putExtra("tiempoDeEspera", (long) 5000);

        // Iniciar el servicio
        startService(this.elIntentDelServicio);
    }

    // ---------------------------------------------------------------------------------------------
    // botonDetenerServicioPulsado(): detiene el servicio en segundo plano
    // Se llama desde el XML (android:onClick="botonDetenerServicioPulsado")
    // ---------------------------------------------------------------------------------------------
    public void botonDetenerServicioPulsado(View v) {
        Log.d(ETIQUETA_LOG, "boton detener servicio pulsado");

        if (this.elIntentDelServicio == null) {
            // No estaba arrancado, no hacer nada
            return;
        }

        // Detener el servicio
        stopService(this.elIntentDelServicio);

        // Marcar como parado
        this.elIntentDelServicio = null;
    }

    // ---------------------------------------------------------------------------------------------
    // onCreate(): inicializa la Activity
    // ---------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(ETIQUETA_LOG, "MainActivity.onCreate(): empieza");
        Log.d(ETIQUETA_LOG, "MainActivity.onCreate(): termina");
    }
}
