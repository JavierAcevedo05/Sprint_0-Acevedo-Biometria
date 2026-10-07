# PROMPT 5 · GENERAR LA LÓGICA FAKE EN EL NAVEGADOR

## Rol y objetivo
Eres un programador senior de la asignatura. A partir de esta especificación
**inmutable** tienes que generar la **lógica fake del navegador** del sistema
"beacon -> móvil -> servidor REST -> base de datos -> web" y sus tests
automáticos.

Igual que en el móvil, es un **proxy** de la lógica del negocio: método con
EXACTAMENTE el mismo nombre y los mismos datos de entrada/salida que la lógica
real del backend, que en lugar de tocar la base de datos envía una petición
REST al servidor. No añadas funciones ni ficheros más allá de lo especificado.

Copia exactamente el estilo del fichero `ux/logicaFake/diHola.js` del esqueleto
`EsqueletoWebAppEnPHPConSesion` (nota abajo).

---

## 1. DISEÑO GRÁFICO

```
   ux/Aplicacion.html  (la web, otro prompt)
        |
        |  obtenerUltimoCodigo( function( err, res ) { ... } )
        v
   ux/logicaFake/obtenerUltimoCodigo.js     <- "versión fake"
        |
        |  new XMLHttpRequest()
        |  open( "GET", "../rest/obtenerUltimoCodigo.php", true )
        |  send()
        v
   servidor REST (rest/obtenerUltimoCodigo.php)
        |
        v
   logica/obtenerUltimoCodigo.php  (lee la tabla CODIGO)
```

Este prompt genera SOLO el fichero `.js` (el proxy) y sus tests. La página
HTML que lo usa es el prompt de "UX en el navegador".

## 2. ACLARACIONES EN TEXTO

- La función se llama **`obtenerUltimoCodigo`** (idéntica a la lógica real,
  regla de paridad) y recibe un único parámetro `cb`: un callback al estilo
  Node con convención error-first.
- **Convención del callback** (igual que `diHola.js` del esqueleto):
  - éxito → `cb( null, res )` donde `res` es el objeto JSON parseado
    (`{id, valor, fecha}`);
  - fallo → `cb( error, null )` donde `error` es un `Texto` con el motivo.
- El servidor responde siempre HTTP 200 con un único JSON y nunca usa códigos
  de error; el campo `error` del JSON vale `0` si todo fue bien y un `Texto`
  si no. Por eso el proxy solo sabe atender `status == 200` y decide por el
  campo `error` del JSON.
- La URL es **relativa** (`../rest/obtenerUltimoCodigo.php`): la web se sirve
  desde el mismo servidor que el REST.
- El mismo fichero debe funcionar:
  1. en el navegador, cargado con `<script src="logicaFake/obtenerUltimoCodigo.js">`
     (define la función en el ámbito global),
  2. en Node para los tests (`require`). Usa la guarda:
     `if ( typeof module !== "undefined" ) { module.exports = obtenerUltimoCodigo }`.

## 3. DIRECTRICES (qué y cómo, exactamente)

### Fichero a generar: `ux/logicaFake/obtenerUltimoCodigo.js`

Cabecera de contrato (cópiala tal cual, con muro de 51 guiones como el
esqueleto):

```javascript
// ---------------------------------------------------
//
// versión fake de una función de la lógica
//
// -->
// obtenerUltimoCodigo() -->
// <--
// <--
// {id:Entero, valor:Entero, fecha:Texto} | error:Texto
//
// error = 0 si la consulta se ha atendido correctamente
// error = Texto con el motivo por el que no ha ido bien
//
// (objeto) | error:Texto : devuelto via callback( err, res )
// err = null si la consulta ha ido bien
// err = Texto si la consulta ha fallado
// res = null si la consulta ha fallado
//
// ---------------------------------------------------
function obtenerUltimoCodigo( cb ) {
```

Implementación exacta (el cuerpo de la función):

1. `var xmlhttp = new XMLHttpRequest()`
2. `xmlhttp.onreadystatechange = function() {`
   - saltar si `this.readyState != 4`;
   - si `this.status != 200` → `cb( "el servidor no ha respondido con exito", null ); return;`
   - intentar `var resultado = JSON.parse( this.responseText )` dentro de
     `try/catch`; si lanza excepción → `cb( "el servidor ha devuelto una respuesta no valida", null ); return;`
   - si `resultado.error != 0` → `cb( resultado.error, null ); return;`
   - si no → `cb( null, resultado )`.
3. `xmlhttp.onerror = function() { cb( "no se ha podido conectar con el servidor", null ); }`
4. `xmlhttp.open( "GET", "../rest/obtenerUltimoCodigo.php", true )`
5. `xmlhttp.send()`
6. Al final, fuera de la función, la guarda de exportación para Node.
7. Cierre de la función con `} // ()` como el esqueleto.

Reglas de estilo (JS de la asignatura):
- Indentación con **un tabulador por nivel** (como `diHola.js`).
- En este fichero TODAS las sentencias terminan en `;` (sé consistente dentro
  del fichero, aunque `diHola.js` del esqueleto no lo sea).
- Variables en minúscula sin separadores (`xmlhttp`, `resultado`, `res`,
  `err`, `error`).
- PROHIBIDO en este fichero: tocar el DOM (`document`, `alert`,
  `console.log` de resultados), `fetch`, `axios`, promesas, `async/await`.
  Solo `XMLHttpRequest` y el callback.

### Tests: `test/mainTest3-web.js` (mocha + assert, como la Práctica 6)

- Cabecera del fichero con muros estilo Práctica 6 (69 guiones):

```javascript
// .....................................................................
// mainTest3-web.js
// .....................................................................
const obtenerUltimoCodigo = require( "../ux/logicaFake/obtenerUltimoCodigo.js" )
var assert = require( "assert" )
```

- Antes de cada `describe`, se instala un `XMLHttpRequest` simulado en el
  ámbito global de Node:
  `global.XMLHttpRequest = FakeXMLHttpRequest` (una clase definida en el propio
  test). El fake guarda `metodo`, `url`, `cuerpo` y permite al testores
  "responder" simulando `readyState = 4`, `status`, `responseText` y llamar a
  `onerror` o al `onreadystatechange`.
- Estructura en `describe`/`it`, terminando con `// } ()` (estilo asignatura).

## 4. COMENTARIOS

- Muros por pares (51 guiones) que encuadran la cabecera de contrato.
- Comentarios breves de finalidad dentro del cuerpo, como el esqueleto:

```javascript
	// preparar la llamada remota
	var xmlhttp = new XMLHttpRequest()
```

- En los `it()` de los tests, un comentario breve que diga el caso que
  verifica (ej.: `// (2) ausencia de codigo`), y los muros de 52 guiones que
  usa la Práctica 6.
- Prohibido comentar línea por línea.

## 5. TESTS AUTOMÁTICOS (lista literal de casos)

1. **Obtención de código**: el fake responde
   `{"id":7,"valor":1234,"fecha":"2026-09-28 20:45:00","error":0}` →
   `err === null` y `res.valor === 1234`.
2. **Ausencia de código**: el fake responde
   `{"error":"no hay ningun codigo almacenado"}` → `err` es exactamente
   `"no hay ningun codigo almacenado"` y `res === null`.
3. **Simulación de error del servidor**: `status = 500` → `err` es un Texto y
   `res === null`.
4. **Simulación de respuesta no válida**: el `responseText` no es JSON
   (`"hola"`) → `err` es un Texto y `res === null`.
5. **Simulación de error de red**: se dispara `onerror` → `err` es un Texto y
   `res === null`.
6. **La petición es exacta**: se comprueba que se llamó a `open` con
   `"GET"`, `"../rest/obtenerUltimoCodigo.php"` y `true`, y a `send()` con
   `null` (sin cuerpo).
7. **Un único intento**: `open`/`send` se llaman exactamente una vez.
8. **La llamada respeta la convención**: en todos los casos de éxito `err`
   vale `null`; en todos los de fallo `res` vale `null`.

Comando de ejecución (documentado en el propio test):
`npx mocha test/mainTest3-web.js` (si no hay `package.json`, añade `npm init -y`
y `npm install --save-dev mocha`).

## 6. ENTREGABLES

- `ux/logicaFake/obtenerUltimoCodigo.js` y `test/mainTest3-web.js`.
- No generes la página HTML, el servidor REST ni el móvil.
- Al terminar muestra la lista de ficheros y el resultado de
  `npx mocha test/mainTest3-web.js`.