# PROMPT 4 · GENERAR LA LÓGICA FAKE EN EL TELÉFONO (ANDROID)

## Rol y objetivo
Eres un programador senior de la asignatura. A partir de esta especificación
**inmutable** tienes que generar, dentro de la aplicación Android del
sistema "beacon -> móvil -> servidor REST -> base de datos -> web", **la
lógica fake del teléfono** y sus tests automáticos.

La "lógica fake" es un **proxy** de la lógica del negocio: expone métodos con
EXACTAMENTE el mismo nombre y los mismos datos de entrada/salida que la lógica
real del backend (regla de la asignatura), pero en lugar de tocar la base de
datos envía peticiones REST al servidor. Además, el teléfono necesita un
"detector de beacon" que se pueda sustituir por una versión simulada para que
los tests no dependan de dispositivos físicos.

Para poder probar la lógica fake necesita una pantalla mínima (los 3 botones
del enunciado); esa pantalla también se genera aquí. No añadas funcionalidades
fuera de lo especificado.

---

## 1. DISEÑO GRÁFICO (mockups)

### Pantalla de la aplicación (UX mínima)

Estado inicial (por defecto la pantalla NO muestra texto):

```
┌───────────────────────────────────────┐
│                                       │
│                                       │
│        (zona del texto, vacía)        │
│                                       │
│            [ Escanear ]               │
│            [ Enviar ]   (oculto)      │
│                                       │
└───────────────────────────────────────┘
```

Los tres estados que va mostrando el texto en la zona central (un `TextView`
multilínea centrado):

| Situación | Texto visible |
|---|---|
| Pulsado [ Escanear ] y aún no hay beacon | `Escaneando...` |
| Beacon detectado | `Código: XXXX` (XXXX = el valor leído) |
| Pulsado [ Enviar ] y el servidor acepta | `¡Enviado con éxito!` |
| Pulsado [ Enviar ] y el servidor rechaza / no hay red | `Error al enviar` |

Comportamiento de los botones:
- `[ Escanear ]` empieza la detección. Al pulsarlo se oculta `Escanear` y
  aparece `[ Detener ]` en su lugar.
- `[ Detener ]` sustituye a `[ Escanear ]` mientras se está escaneando; al
  pulsarlo detiene la detección y vuelve a aparecer `[ Escanear ]`.
- `[ Enviar ]` está **oculto** (GONE) por defecto y únicamente aparece cuando
  se ha detectado un código correctamente. Al pulsarlo envía el código al
  servidor.

### Lógica fake (proxy REST): diagrama

```
   MainActivity
      |
      | insertarCodigo( 1234, cb )
      v
   logicaFake/LogicaFake.java        <- "versión fake" con el mismo
      |                                  nombre que la lógica real
      | usarTransporte.hacerPeticion("POST", url, cuerpoJSON, cb)
      v
   PeticionarioREST.java             <- transporte real (AsyncTask)
      | HttpURLConnection
      v
   servidor REST (rest/insertarCodigo.php)
```

## 2. ACLARACIONES EN TEXTO

- Paquete de la app: **`org.jordi.codex`**. Nombre de la app: **`Codex`**.
- El código que se envía es el **minor** del iBeacon (entero en
  `[0, 65535]`, 2 bytes sin signo). La detección se hace con el
  `BluetoothLeScanner` estándar de Android y las clases `TramaIBeacon` y
  `Utilidades` (se reutilizan tal cual del material de la asignatura, se
  copian a este proyecto sin cambiar su código).
- El móvil SOLO escribe (envía el código): NO implementa `obtenerUltimoCodigo`
  porque el flujo del enunciado no lo pide en el móvil.
- `LogicaFake` NUNCA toca la base de datos y NUNCA sabe nada de beacons: solo
  recibe un `int` y lo manda por REST. La detección del beacon es trabajo del
  detector (sección siguiente).
- La detección real y la simulación se separan con una interfaz común
  (`DetectorBeacons`): `MainActivity` usa la real, los tests usan la
  simulada (`DetectorBeaconsFake`). Así los tests NO dependen de dispositivos
  físicos.
- El nombre del método del proxy es **`insertarCodigo`** (idéntico al de la
  lógica del backend, por la regla de paridad de la asignatura), aunque por
  dentro haga un POST.
- La URL del servidor va en UNA constante de `LogicaFake`
  (`IP_PUERTO_SERVIDOR`), porque el teléfono no estará en `localhost`: el
  servidor PHP se arranca en el ordenador y el móvil se conecta por la IP de
  la red local.
- `PeticionarioREST` (del material) es el transporte real: `AsyncTask` +
  `HttpURLConnection` + callback `RespuestaREST { void callback(int codigo,
  String cuerpo); }`. Se copia tal cual. Para poder testearlo se extrae una
  interfaz `TransporteREST` que implementa.

## 3. DIRECTRICES (qué y cómo, exactamente)

### Ficheros del proyecto (a generar en la raíz del proyecto Android)

```
android/
|-- settings.gradle
|-- build.gradle
|-- gradle.properties
`-- app/
    |-- build.gradle
    `-- src/
        |-- main/
        |   |-- AndroidManifest.xml
        |   |-- java/org/jordi/codex/
        |   |   |-- MainActivity.java
        |   |   |-- logicaFake/LogicaFake.java
        |   |   |-- logicaFake/PeticionarioREST.java      (copia del material)
        |   |   |-- logicaFake/TransporteREST.java
        |   |   |-- beacon/DetectorBeacons.java
        |   |   |-- beacon/NotificacionBeacon.java
        |   |   |-- beacon/DetectorBeaconsReal.java
        |   |   |-- beacon/DetectorBeaconsFake.java
        |   |   |-- beacon/TramaIBeacon.java              (copia del material)
        |   |   `-- beacon/Utilidades.java                (copia del material)
        |   `-- res/layout/activity_main.xml
        `-- test/java/org/jordi/codex/
            |-- TramaIBeaconTest.java
            |-- UtilidadesTest.java
            |-- DetectorBeaconsFakeTest.java
            `-- LogicaFakeTest.java
```

### Gradle y manifest

- `minSdk 28`, `targetSdk 34`, Java 8/11, AndroidX (appcompat), dependencias:
  `junit:junit:4.13.2` para `testImplementation`. El Gradle raíz usa AGP
  8.5.2 / Gradle 8.7 (como el material del curso); si no, usa el más parecido
  disponible.
- Permisos en el manifest (envueltos en las dos comprobaciones de siempre):
  `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`,
  `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `INTERNET`.
  `uses-feature android.hardware.bluetooth_le required="true"`.
- En `MainActivity` pide los permisos de localización y bluetooth en tiempo de
  ejecución (código de petición propio); si no los concede, el TextView
  muestra `Faltan permisos de Bluetooth` y no se escanea.

### Cabeceras de contrato (en "nuestra notación", encuadradas con muros)

Cada método/clase lleva su cabecera. Modelos a copiar:

`TransporteREST.java`:
```java
// -------------------------------------------------------------
// -------------------------------------------------------------
//
// interfaz del transporte REST: aislar la peticion HTTP real de
// su uso en LogicaFake, para poder simularla en los tests
//
// metodo:Texto -> usarTransporte() --> <-- | error:Texto
//
// la respuesta viaja por el callback: ( codigoHTTP, cuerpo )
//
// -------------------------------------------------------------
public interface TransporteREST {
  void hacerPeticion( String metodo, String urlDestino, String cuerpo, PeticionarioREST.RespuestaREST laRespuesta );
}
```

`LogicaFake.java`:
```java
// -------------------------------------------------------------
// -------------------------------------------------------------
//
// version fake de una funcion de la logica
//
// valor:Entero -->
// insertarCodigo() -->
// <--
// <--
// {id:Entero, valor:Entero, fecha:Texto} | error:Texto
//
// valor: codigo leido del beacon (el minor del iBeacon)
// la respuesta viaja por el callback( codigoHTTP, cuerpo )
//
// -------------------------------------------------------------
public class LogicaFake {
```

### `LogicaFake` (proxy) — implementación exacta

- Constructor: `LogicaFake( TransporteREST elTransporte )`. Guarda la
  constante `private static final String IP_PUERTO_SERVIDOR = "http://192.168.1.100:8080";`
  (único sitio donde se cambia la IP).
- Método único:
  `public void insertarCodigo( int valor, PeticionarioREST.RespuestaREST cb )`
- Hace exactamente:
  1. Construye el cuerpo JSON a mano:
     `"{\"valor\": " + valor + "}"`.
  2. Llama: `elTransporte.hacerPeticion( "POST", IP_PUERTO_SERVIDOR + "/rest/insertarCodigo.php", cuerpo, cb )`.
  Nada más. Sin parsear, sin tocar la BD, sin validar.
- En Java, las variables y métodos con el estilo del material:
  `elTransporte`, `laRespuesta`, nombres en minúscula sin separadores.

### Detector de beacons

`DetectorBeacons.java` (interfaz):
```java
// -------------------------------------------------------------
// -------------------------------------------------------------
//
// escanear() --> <--          : empieza la deteccion de beacons
// detener()  --> <--          : la para
//
// el codigo detectado (o el error) se notifica por callback:
//   NotificacionBeacon.codigoDetectado( int elCodigo )
//   NotificacionBeacon.error( String elMensaje )
//
// -------------------------------------------------------------
public interface DetectorBeacons {
  void escanear( NotificacionBeacon elNotificador );
  void detener();
}
```

`NotificacionBeacon.java`:
```java
public interface NotificacionBeacon {
  void codigoDetectado( int elCodigo );
  void error( String elMensaje );
}
```

`DetectorBeaconsReal.java` — copia la estrategia del material
`PBIO-2025/Android-BTLE/MainActivity`:
- `BluetoothLeScanner.startScan` con `ScanFilter` del fabricante Apple
  `0x004C` + prefijo iBeacon + máscara, `ScanMode LOW_LATENCY`, y el
  `ScanCallback` con `mostrarInformacionDispositivoBTLE`.
- Al recibir el resultado: valida company ID `0x4C 0x00`, tipo `0x02`,
  longitud `0x15`, construye `TramaIBeacon`, comprueba el UUID del beacon
  contra la constante `UUID_NUESTRO_BEACON = "EPSG-GTI-PROY-3A"`
  (`Utilidades.bytesToString( tib.getUUID() )`).
- Si coincide: `int elCodigo = Utilidades.bytesToIntOK( tib.getMinor() );`
  y avisa en el hilo de la interfaz
  (`runOnUiThread`/handler) con `elNotificador.codigoDetectado( elCodigo )`.
- Si el escaneo no se puede iniciar: `elNotificador.error( "no se puede iniciar el escaneo" )`.
- `detener()`: `stopScan` con el mismo callback y pone el callback a `null`.

`DetectorBeaconsFake.java` — simulación SIN bluetooth ni red:
- Constructor con un código simulado y una bandera de fallo:
  `DetectorBeaconsFake( int elCodigoSimulado, boolean debeFallar )`.
- `escanear( elNotificador )`:
  - si `debeFallar` → `elNotificador.error( "simulacion de error en el beacon" )`;
  - si no → espera ~100 ms en un hilo y llama
    `elNotificador.codigoDetectado( elCodigoSimulado )`.
- `detener()`: marca una bandera `detenido = true`; el hilo de `escanear`
  NO notifica si ya se ha detenido.

### `MainActivity` (UX mínima de los 3 botones)

- `setContentView(R.layout.activity_main)`; `findViewById` de:
  `textoEstado`, `botonEscanear`, `botonDetener` y `botonEnviar`.
- Handlers declarados en el XML (`android:onClick`):
  - `botonEscanearPulsado( View )`: oculta `botonEscanear`, muestra
    `botonDetener`, pone `textoEstado.setText( "Escaneando..." )`, crea el
    `DetectorBeaconsReal` y `escanear( this )`. `MainActivity` implementa
    `NotificacionBeacon`:
    - `codigoDetectado( elCodigo )`: guarda `this.elCodigo = elCodigo`,
      `textoEstado.setText( "Código: " + elCodigo )`, muestra `botonEnviar`,
      oculta `botonDetener`, muestra de nuevo `botonEscanear`.
    - `error( mensaje )`: `textoEstado.setText( mensaje )`, restaura
      `botonEscanear`/oculta `botonDetener`.
  - `botonDetenerPulsado( View )`: `detener()` del detector activo, vuelve a
    mostrar `botonEscanear` y oculta `botonDetener`.
  - `botonEnviarPulsado( View )`: `new LogicaFake( new PeticionarioREST() ).insertarCodigo( this.elCodigo, callback )`; en el callback:
    - si `codigo == 200` y el `cuerpo` contiene `"error":0` →
      `textoEstado.setText( "¡Enviado con éxito!" )` y oculta `botonEnviar`;
    - si `codigo == 200` y el `cuerpo` contiene `"error"` con texto (p.ej.
      "el codigo no es valido") → `textoEstado.setText( "Error al enviar" )`;
    - si `codigo != 200` o excepción de red → `textoEstado.setText( "Error al enviar" )`.
    (El cuerpo es un String JSON sin parsear, como hace `MainActivity` del
    material.)

### `activity_main.xml` (layout)

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center">

    <TextView
        android:id="@+id/textoEstado"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="22sp"
        android:gravity="center"
        android:text="" />

    <Button
        android:id="@+id/botonEscanear"
        android:text="Escanear"
        android:onClick="botonEscanearPulsado"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />

    <Button
        android:id="@+id/botonDetener"
        android:text="Detener"
        android:onClick="botonDetenerPulsado"
        android:visibility="gone"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />

    <Button
        android:id="@+id/botonEnviar"
        android:text="Enviar"
        android:onClick="botonEnviarPulsado"
        android:visibility="gone"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content" />

</LinearLayout>
```

### Reglas de aislamiento (prohibiciones)

- `LogicaFake` PROHIBIDO: `BluetoothLeScanner`, `ScanCallback`,
  `TramaIBeacon`, `SQLite`, `SharedPreferences`, JSONObject (el cuerpo se
  construye con String). Solo `TransporteREST`.
- `MainActivity` PROHIBIDO: peticiones REST directas (usa `LogicaFake` y las
  clases del detector). El texto mostrado es SIEMPRE el de la tabla de la
  sección 1, sin invenciones.
- Los tests PROHIBIDO: tocar bluetooth, red o base de datos.

## 4. COMENTARIOS

- Separa las partes del código con DOS muros (52 guiones, como el material):

```
// -------------------------------------------------------------
// -------------------------------------------------------------
```

- Cabecera de contrato de cada clase/método (notación de la sección 3),
  encuadrada entre muros; cabeceras "en Javadoc" están permitidas para
  describir la finalidad, pero el contrato `entradas -> f() -> salidas`
  con muro es OBLIGATORIO.
- Comentarios breves de finalidad. Prohibido comentar línea por línea.

## 5. TESTS AUTOMÁTICOS (JUnit 4, sin dispositivo físico)

`TramaIBeaconTest.java`:
1. **Detección (parseo)**: una trama iBeacon de 30 bytes fabricada en el test
   (Apple `0x4C 0x00`, type `0x02`, length `0x15`, UUID
   `EPSG-GTI-PROY-3A`, minor con valor `1234`) se construye con
   `new TramaIBeacon( bytes )` y devuelve `uuid` correcto, `isAppleBeacon()`
   true y `isValidBeacon()` true.
2. **Obtención del código**: `Utilidades.bytesToIntOK( tib.getMinor() )`
   devuelve `1234`; con minor `0xFFFF` devuelve `65535`.
3. **Trama inválida**: `new TramaIBeacon( null )` y una trama de 20 bytes
   lanzan `IllegalArgumentException`.

`DetectorBeaconsFakeTest.java` (simulación):
4. **Detección simulada de beacon**: `escanear( notificador )` termina
   llamando `codigoDetectado( elCodigoSimulado )` (espera hasta 1 s con
   `CountDownLatch`).
5. **Detener**: si se llama `detener()` antes de que pase el retardo, el
   notificador NO recibe ninguna llamada.
6. **Simulación de errores**: con `debeFallar = true`, `escanear` llama a
   `error( ... )` y nunca a `codigoDetectado`.

`LogicaFakeTest.java` (envío simulado, con una implementación de
`TransporteREST` de prueba que captura la llamada y devuelve el JSON que se le
diga):
7. **Envío simulado correcto**: el transporte simulado responde
   `{"id":1,"valor":1234,"fecha":"...","error":0}` → el callback recibe
   `codigo == 200` y el cuerpo con `"error":0`.
8. **La petición es exacta**: se comprueba que el transporte recibió
   `"POST"`, la URL `IP_PUERTO_SERVIDOR + "/rest/insertarCodigo.php"` y el
   cuerpo `{"valor": 1234}`.
9. **Simulación de error del servidor**: el transporte responde
   `{"error":"el codigo no es valido"}` → el callback lo recibe sin lanzar
   excepción.
10. **Simulación de error de red**: el transporte devuelve
    `codigo == 0` y cuerpo `""` → el callback lo recibe con `codigo == 0`.

## 6. ENTREGABLES

- Todo el proyecto `android/` especificado (Gradle, manifest, fuentes y
  tests). Los ficheros copiados del material (`PeticionarioREST`,
  `TramaIBeacon`, `Utilidades`) se marcan con un comentario
  `// copiado del material de la asignatura, no modificar`.
- Comando de tests: `./gradlew test` en `android/` (JUnit 4 local,
  sin emulador).
- Al terminar muestra la lista de ficheros y el resultado de los tests.