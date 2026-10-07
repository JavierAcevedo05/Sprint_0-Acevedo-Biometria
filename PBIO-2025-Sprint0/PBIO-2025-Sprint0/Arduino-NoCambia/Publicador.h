// -*- mode: c++ -*-

// --------------------------------------------------------------
// Jordi Bataller i Mascarell
// --------------------------------------------------------------

#ifndef PUBLICADOR_H_INCLUIDO
#define PUBLICADOR_H_INCLUIDO

// --------------------------------------------------------------
// Clase Publicador: envia datos de sensores por BLE como iBeacon.
//
// Codificacion de datos en el anuncio iBeacon:
//   - major = (tipo_medicion << 8) + contador
//   - minor = valor_medicion
//
// Ejemplo: CO2=500 ppm, contador=3
//   major = (11 << 8) + 3 = 0x0B03  ->  tipo=CO2, contador=3
//   minor = 500                      ->  valor de CO2
// --------------------------------------------------------------
class Publicador {

private:

  // UUID del beacon (16 bytes en ASCII)
  // NOTA: este UUID no coincide con el nombre de la emisora "GTI3A-2025"
  uint8_t beaconUUID[16] = {
	'E', 'P', 'S', 'G', '-', 'G', 'T', 'I',
	'-', 'P', 'R', 'O', 'Y', '-', '3', 'A'
  };

public:

  // Emisora BLE: nombre visible, ID fabricante Apple (0x004C), potencia calibrada
  EmisoraBLE laEmisora {
	"GTI3A-2025", // nombre emisora (el que busca Android)
	0x004c,       // fabricanteID (Apple)
	4             // txPower
  };

  // RSSI de referencia a 1 metro (para estimar distancia)
  const int RSSI = -53;

  // Enumeracion de tipos de medicion
  // El valor se usa en los 8 bits altos del campo major
  enum MedicionesID {
	CO2 = 11,
	TEMPERATURA = 12,
	RUIDO = 13
  };

  // .....................................................
  // constructor: no inicializa la emisora (usar encenderEmisora en setup)
  // .....................................................
  Publicador() {
  } // ()

  // .....................................................
  // encenderEmisora(): activa la emisora BLE
  // .....................................................
  void encenderEmisora() {
	(*this).laEmisora.encenderEmisora();
  } // ()

  // .....................................................
  // publicarCO2(): emite un anuncio iBeacon con el valor de CO2
  //
  // Parametros:
  //   valorCO2:      concentracion de CO2 en ppm
  //   contador:      numero de iteracion (0-255)
  //   tiempoEspera:  tiempo en ms que dura el anuncio antes de pararlo
  //
  // Codificacion:
  //   major = (CO2=11 << 8) + contador
  //   minor = valorCO2
  // .....................................................
  void publicarCO2(int16_t valorCO2, uint8_t contador,
					long tiempoEspera) {

	// Combinar tipo de medicion (11) con contador en el campo major
	uint16_t major = ((uint16_t)MedicionesID::CO2 << 8) + contador;

	// Emitir anuncio iBeacon con UUID, major y minor
	(*this).laEmisora.emitirAnuncioIBeacon(
		(*this).beaconUUID,
		major,
		valorCO2,       // minor = valor de CO2
		(*this).RSSI
	);

	// Mantener el anuncio activo durante el tiempo indicado
	esperar(tiempoEspera);

	// Parar el anuncio
	(*this).laEmisora.detenerAnuncio();
  } // ()

  // .....................................................
  // publicarTemperatura(): emite un anuncio iBeacon con la temperatura
  //
  // Parametros:
  //   valorTemperatura: temperatura en grados Celsius
  //   contador:         numero de iteracion (0-255)
  //   tiempoEspera:     tiempo en ms que dura el anuncio
  //
  // Codificacion:
  //   major = (TEMPERATURA=12 << 8) + contador
  //   minor = valorTemperatura
  // .....................................................
  void publicarTemperatura(int16_t valorTemperatura,
							uint8_t contador, long tiempoEspera) {

	uint16_t major = ((uint16_t)MedicionesID::TEMPERATURA << 8) + contador;

	(*this).laEmisora.emitirAnuncioIBeacon(
		(*this).beaconUUID,
		major,
		valorTemperatura,  // minor = valor de temperatura
		(*this).RSSI
	);

	esperar(tiempoEspera);

	(*this).laEmisora.detenerAnuncio();
  } // ()

}; // class

#endif
