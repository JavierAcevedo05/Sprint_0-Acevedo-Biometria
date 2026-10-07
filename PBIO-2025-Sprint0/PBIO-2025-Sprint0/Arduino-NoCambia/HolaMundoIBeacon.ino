// -*-c++-*-

// --------------------------------------------------------------
//
// Jordi Bataller i Mascarell
// 2019-07-07
//
// --------------------------------------------------------------

// https://learn.sparkfun.com/tutorials/nrf52840-development-with-arduino-and-circuitpython

// https://stackoverflow.com/questions/29246805/can-an-ibeacon-have-a-data-payload

// --------------------------------------------------------------
// --------------------------------------------------------------
// Libreria BLE para placas Nordic nRF52840 (Adafruit Bluefruit)
#include <bluefruit.h>

#undef min // vaya tela, están definidos en bluefruit.h y  !
#undef max // colisionan con los de la biblioteca estándar

// --------------------------------------------------------------
// --------------------------------------------------------------
// Clases auxiliares: LED y PuertoSerie (definidas en archivos separados)
#include "LED.h"
#include "PuertoSerie.h"

// --------------------------------------------------------------
// Namespace global: objetos compartidos por todo el sketch
// --------------------------------------------------------------
namespace Globales {

  // LED conectado al pin 7 de la placa
  LED elLED ( /* NUMERO DEL PIN LED = */ 7 );

  // Puerto serie a 115200 baudios para comunicacion con el PC
  PuertoSerie elPuerto ( /* velocidad = */ 115200 ); // 115200 o 9600 o ...

  // Serial1 en el ejemplo de Curro creo que es la conexión placa-sensor
};

// --------------------------------------------------------------
// --------------------------------------------------------------
// Clases BLE: emisora, publicador y medidor de CO2
#include "EmisoraBLE.h"
#include "Publicador.h"
#include "Medidor.h"


// --------------------------------------------------------------
// --------------------------------------------------------------
namespace Globales {

  // Publicador: envia datos por BLE (como iBeacon)
  Publicador elPublicador;

  // Medidor: lee el sensor de CO2
  Medidor elMedidor;

}; // namespace

// --------------------------------------------------------------
// Inicializacion adicional de la placa (pendiente de implementar)
// --------------------------------------------------------------
void inicializarPlaquita () {

  // de momento nada

} // ()

// --------------------------------------------------------------
// setup(): se ejecuta UNA sola vez al encender la placa
// --------------------------------------------------------------
void setup() {

  // Esperar a que el puerto serie este listo para comunicar
  Globales::elPuerto.esperarDisponible();

  // 
  // 
  // 
  inicializarPlaquita();

  // Suspend Loop() to save power
  // suspendLoop();

  // 
  // 
  // 
  // Encender la emisora BLE para empezar a transmitir anuncios
  Globales::elPublicador.encenderEmisora();

  // Globales::elPublicador.laEmisora.pruebaEmision();
  
  // 
  // 
  // 
  // Iniciar el medidor de CO2
  Globales::elMedidor.iniciarMedidor();

  // 
  // 
  // 
  // Pequena pausa para estabilizar
  esperar( 1000 );

  Globales::elPuerto.escribir( "---- setup(): fin ---- \n " );

} // setup ()

// --------------------------------------------------------------
// lucecitas(): patron visual de parpadeo LED para indicar actividad
// --------------------------------------------------------------
inline void lucecitas() {
  using namespace Globales;

  elLED.brillar( 100 ); // 100 encendido
  esperar ( 400 ); //  100 apagado
  elLED.brillar( 100 ); // 100 encendido
  esperar ( 400 ); //  100 apagado
  Globales::elLED.brillar( 100 ); // 100 encendido
  esperar ( 400 ); //  100 apagado
  Globales::elLED.brillar( 1000 ); // 1000 encendido
  esperar ( 1000 ); //  100 apagado
} // ()

// --------------------------------------------------------------
// Variables del loop
// --------------------------------------------------------------
namespace Loop {
  int cont = 0; // FIX: era uint8_t (se desbordaba a 255). Ahora es int
};

// ..............................................................
// loop(): se ejecuta REPETIDAMENTE despues de setup()
// ..............................................................
void loop () {

  using namespace Loop;
  using namespace Globales;

  cont++;

  elPuerto.escribir( "\n---- loop(): empieza " );
  elPuerto.escribir( cont );
  elPuerto.escribir( "\n" );


  lucecitas();

  // 
  // mido y publico
  // 
  // Leer valor de CO2 del sensor
  int valorCO2 = elMedidor.medirCO2();
  
  // Publicar el valor de CO2 por BLE como iBeacon
  // Parametros: valor CO2, contador de iteracion, intervalo de emision (ms)
  elPublicador.publicarCO2( valorCO2,
							cont,
							1000 // intervalo de emisión
							);
  
  /*
  // 
  // mido y publico
  // 
  int valorTemperatura = elMedidor.medirTemperatura();
  
  elPublicador.publicarTemperatura( valorTemperatura, 
									cont,
									1000 // intervalo de emisión
									);

  // 
  // prueba para emitir un iBeacon y poner
  // en la carga (21 bytes = uuid 16 major 2 minor 2 txPower 1 )
  // lo que queramos (sin seguir dicho formato)
  // 
  // Al terminar la prueba hay que hacer Publicador::laEmisora privado
  // 
  char datos[21] = {
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H', 'o', 'l', 'a',
	'H'
  };

  // elPublicador.laEmisora.emitirAnuncioIBeaconLibre ( &datos[0], 21 );
  elPublicador.laEmisora.emitirAnuncioIBeaconLibre ( "MolaMolaMolaMolaMolaM", 21 );

  esperar( 2000 );

  elPublicador.laEmisora.detenerAnuncio();

  */
  
  // 
  // 
  // 
  elPuerto.escribir( "---- loop(): acaba **** " );
  elPuerto.escribir( cont );
  elPuerto.escribir( "\n" );
  
} // loop ()
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
// --------------------------------------------------------------
