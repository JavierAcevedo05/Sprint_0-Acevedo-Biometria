// -*- mode: c++ -*-

#ifndef MEDIDOR_H_INCLUIDO
#define MEDIDOR_H_INCLUIDO

// ------------------------------------------------------
// Clase Medidor: proporciona lecturas de CO2 y temperatura.
// Actualmente es un stub con valores ficticios para poder
// compilar y probar el flujo del programa sin sensor real.
// ------------------------------------------------------
class Medidor {

private:

public:

  // constructor
  Medidor() {
  } // ()

  // Inicializacion del sensor (vacio por ahora)
  void iniciarMedidor() {
  } // ()

  // Devuelve concentracion de CO2 en ppm (valores tipicos: 400-5000 ppm)
  // STUB: devuelve 123
  int medirCO2() {
    return 123;
  } // ()

  // Devuelve temperatura en grados Celsius
  // STUB: devuelve -12
  int medirTemperatura() {
    return -12;
  } // ()

}; // class

#endif
