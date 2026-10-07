package org.jordi.clienterestandroid;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

// ------------------------------------------------------------------------
// Activity principal del cliente REST Android.
//
// Flujo:
//   1. Usuario pulsa el boton "Enviar"
//   2. Se lanza una peticion GET a un servidor en segundo plano
//   3. La respuesta se muestra en el TextView
// ------------------------------------------------------------------------
public class MainActivity extends AppCompatActivity {

    private TextView elTexto;
    private Button elBotonEnviar;

    // --------------------------------------------------------------------
    // onCreate(): inicializa la interfaz y vincula los elementos del layout
    // --------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Vincular elementos del layout con las variables del codigo
        this.elTexto = findViewById(R.id.elTexto);
        this.elBotonEnviar = findViewById(R.id.botonEnviar);

        Log.d("clienterestandroid", "fin onCreate()");
    }

    // --------------------------------------------------------------------
    // boton_enviar_pulsado(): handler del boton "Enviar"
    // Lanza una peticion GET al servidor y muestra la respuesta en el TextView
    //
    // Nota: este metodo se llama desde el XML (android:onClick="boton_enviar_pulsado")
    // --------------------------------------------------------------------
    public void boton_enviar_pulsado(View quien) {
        Log.d("clienterestandroid", "boton_enviar_pulsado");
        this.elTexto.setText("pulsado");

        // Crear una nueva instancia de PeticionarioREST
        // IMPORTANTE: crear uno nuevo cada vez para evitar problemas de estado
        PeticionarioREST elPeticionario = new PeticionarioREST();

        // Ejemplo 1: peticion GET a un servidor local
        elPeticionario.hacerPeticionREST("GET", "http://158.42.144.126:8080/prueba", null,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        // Actualizar la interfaz con el resultado
                        elTexto.setText("codigo respuesta= " + codigo + " <-> \n" + cuerpo);
                    }
                }
        );

        // Ejemplo 2: peticion POST con JSON (comentado)
        /*
        elPeticionario.hacerPeticionREST("POST", "http://192.168.1.113:8080/mensaje",
                "{\"dni\": \"A9182342W\", \"nombre\": \"Android\", \"apellidos\": \"De Los Palotes\"}",
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        elTexto.setText("codigo respuesta: " + codigo + " <-> \n" + cuerpo);
                    }
                });
        */

        // Ejemplo 3: peticion GET a JSONPlaceholder (comentado)
        /*
        elPeticionario.hacerPeticionREST("GET", "https://jsonplaceholder.typicode.com/posts/2", null,
                new PeticionarioREST.RespuestaREST() {
                    @Override
                    public void callback(int codigo, String cuerpo) {
                        elTexto.setText("codigo respuesta: " + codigo + " <-> \n" + cuerpo);
                    }
                });
        */
    } // boton_enviar_pulsado()

    // --------------------------------------------------------------------
    // onCreateOptionsMenu(): infla el menu de la action bar
    // --------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

} // class
