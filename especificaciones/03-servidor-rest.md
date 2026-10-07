# PROMPT 3 · GENERAR EL SERVIDOR REST DEL BACKEND

## Rol y objetivo
Eres un programador senior de la asignatura. A partir de esta especificación
**inmutable** tienes que generar el servidor REST del sistema
"beacon -> móvil -> servidor REST -> base de datos -> web" y sus tests
automáticos. El servidor REST es la única puerta hacia la lógica de negocio:
recibe una petición HTTP, llama a la función de la lógica y devuelve su
resultado en un JSON. No añadas rutas, parámetros o ficheros que no estén
especificados aquí.

Copia exactamente el estilo de los `rest/*.php` del esqueleto
`EsqueletoWebAppEnPHPConSesion` (nota abajo).

---

## 1. DISEÑO GRÁFICO

```
   móvil                    servidor REST                 lógica
   (POST)                   rest/insertarCodigo.php      logica/insertarCodigo.php
   --------->  JSON cuerpo ->  require_once -->  ------------------>  (inserta en BD)
              <----------------------- JSON resultado <------------------


   web                      servidor REST                   lógica
   (GET)                    rest/obtenerUltimoCodigo.php   logica/obtenerUltimoCodigo.php
   ------->  sin cuerpo --->  require_once -->  ------------------>  (lee de BD)
   <----------------------- JSON resultado <------------------
```

Arquitectura del fichero REST (mismo patrón en los dos):

```
<?php
require_once( '../logica/NOMBRE.php' );   // 1. cargar la lógica

// ---------- cabecera de contrato (ver sección 3) ----------

// 2. preparar la respuesta (new stdClass)
// 3. leer los parámetros de entrada
// 4. /llamada a la verdadera función./
// 5. si la función devuelve error -> devolverlo y cortar
// 6. si ha ido bien -> $objetoResultado->error = 0;
// 7. /echo == devolver/  echo json_encode( $objetoResultado );
```

## 2. ACLARACIONES EN TEXTO

- Solo existen DOS rutas:
  1. `POST rest/insertarCodigo.php` — el móvil envía el código.
  2. `GET rest/obtenerUltimoCodigo.php` — la web consulta el último código.
- El cuerpo de las peticiones y las respuestas van en JSON.
- El servidor rest responde SIEMPRE con HTTP 200 y un único JSON; el resultado
  o el motivo del error van **dentro** del JSON, en el campo `error`
  (convención del esqueleto: `error = 0` = todo bien; `error = Texto` = no se
  ha podido). Nunca se usan códigos HTTP de error (400, 500...).
- No hay sesión, no hay usuarios, no hay autenticación: se descarta
  `session_start()` y `$_SESSION` del esqueleto porque el enunciado no define
  usuarios.
- No hace falta CORS: la web se sirve desde el mismo servidor y mismo puerto
  (mismo origen).
- El servidor NO contiene lógica de negocio: valida únicamente el "formato"
  de la petición (que llegue un JSON, que exista el campo `valor`); la
  validación del "valor" (rango 0..65535) la hace la lógica y aquí solo se
  reenvía su `error`.
- Para arrancar el servidor se escribe el fichero `00-Leeme.txt` en la raíz
  del proyecto con el siguiente contenido:

```
1. Hay que arrancar un servidor http+php con este directorio como base.
Por ejemplo:

	php -S localhost:8080 -t .

2. Probar las rutas REST con el navegador o con curl:

	curl -X POST http://localhost:8080/rest/insertarCodigo.php -H "Content-Type: application/json" -d '{"valor": 1234}'
	curl http://localhost:8080/rest/obtenerUltimoCodigo.php
```

## 3. DIRECTRICES (qué y cómo, exactamente)

### `rest/insertarCodigo.php`

Cabecera de contrato (cópiala tal cual):

```
// -----------------------------------------------------------------------
//
// POST ../rest/insertarCodigo.php
//
// cuerpo: {valor:Entero}
// -->
// insertarCodigo() -->
// <--
// <--
// {id:Entero, valor:Entero, fecha:Texto} | error:Texto
//
// (id, valor, fecha, error) : devueltos en un mismo JSON
// error = 0    si la peticion se ha atendido correctamente
// error = Texto con el motivo por el que no se ha atendido
//
// -----------------------------------------------------------------------
```

Implementación exacta, en orden:
1. `require_once( '../logica/insertarCodigo.php' );` (sin espacios dentro de
   los paréntesis, como el esqueleto).
2. `header( 'Content-Type: application/json; charset=utf-8' );`
3. `$objetoResultado = new stdClass;`
4. Leer el cuerpo: `$elCuerpo = json_decode( file_get_contents( 'php://input' ), true );`.
   - Si `$elCuerpo` es `null` o `false`: `$objetoResultado->error = "el cuerpo no
     es un JSON valido";` `echo json_encode( $objetoResultado );` `return;`
5. Si no existe `$elCuerpo["valor"]`: `$objetoResultado->error = "falta el campo valor";`
   `echo json_encode( $objetoResultado );` `return;`
6. `$valor = $elCuerpo["valor"];`
7. Marcador de llamada a la lógica (3 líneas):

```php
//
// llamada a la verdadera función.
//
```

8. `$objetoResultado = insertarCodigo( $valor );`
9. Si `$objetoResultado->error` está definido (la lógica devolvió un error):
   `echo json_encode( $objetoResultado );` `return;`
10. `$objetoResultado->error = 0;`
11. `// echo == devolver` y `echo json_encode( $objetoResultado );`

### `rest/obtenerUltimoCodigo.php`

Cabecera de contrato (cópiala tal cual):

```
// -----------------------------------------------------------------------
//
// GET ../rest/obtenerUltimoCodigo.php
//
// -->
// obtenerUltimoCodigo() -->
// <--
// <--
// {id:Entero, valor:Entero, fecha:Texto} | error:Texto
//
// (id, valor, fecha, error) : devueltos en un mismo JSON
// error = 0    si la peticion se ha atendido correctamente
// error = Texto con el motivo por el que no se ha atendido
//
// -----------------------------------------------------------------------
```

Implementación exacta:
1. `require_once( '../logica/obtenerUltimoCodigo.php' );`
2. `header( 'Content-Type: application/json; charset=utf-8' );`
3. `$objetoResultado = new stdClass;`
4. Marcador `// llamada a la verdadera función.`
5. `$objetoResultado = obtenerUltimoCodigo();`
6. Si hay `error`: `echo json_encode( $objetoResultado );` `return;`
7. `$objetoResultado->error = 0;`
8. `// echo == devolver` y `echo json_encode( $objetoResultado );`

### Reglas de aislamiento (prohibiciones)

- PROHIBIDO: escribir lógica de negocio en `rest/` (solo `require_once` +
  llamada + JSON). La única validación permitida es la "de formato" descrita.
- PROHIBIDO: `session_start`, `$_SESSION`, login, redirecciones.
- PROHIBIDO: devolver HTML o texto plano.
- PROHIBIDO: salir con `exit` sin devolver antes el JSON del error.
- Indentación de 2 espacios; `$objetoResultado` siempre `stdClass`.

### Tests (fichero `test/mainTest4-rest.php`, script PHP autónomo)

- Sin framework, solo PHP: se conecta por HTTP al servidor en marcha
  (`http://localhost:8080`) en lugar de irse a la base de datos, y comprueba
  el JSON de respuesta con `json_decode`. Antes, limpia la tabla `CODIGO` de
  `codex_test` apuntando las variables de entorno a ella (la lógica la
  escribe). Invocación documentada en el propio test:
  `php -S localhost:8080 -t .` en un terminal y `php test/mainTest4-rest.php`
  en otro.
- Imprime `OK`/`FALLO` por prueba, `resultado final: N de N OK` y termina con
  `exit(0)`/`exit(1)`.

## 4. COMENTARIOS

- Muros por pares para separar partes; cabecera de contrato copiada tal cual.
- Marcadores literales dentro del cuerpo (ya dados arriba):
  `// llamada a la verdadera función.` y `// echo == devolver`.
- Comentarios breves de finalidad (ej.: `// obtengo valores de los parámetros`).
- Prohibido comentar línea por línea.

## 5. TESTS AUTOMÁTICOS (lista literal de casos)

Sobre `POST rest/insertarCodigo.php`:
1. **Inserción correcta**: con `{"valor": 1234}` devuelve JSON con
   `error == 0`, `valor == 1234`, `id > 0` y `fecha` no vacía.
2. **Cuerpo no JSON**: enviar `hola` como cuerpo devuelve
   `error == "el cuerpo no es un JSON valido"`.
3. **Falta el campo valor**: enviar `{"otro": 1}` devuelve
   `error == "falta el campo valor"`.
4. **Valor fuera de rango (la lógica lo rechaza)**: enviar `{"valor": 70000}`
   devuelve `error == "el codigo no es valido"`.
5. **Cabecera de respuesta**: la respuesta lleva
   `Content-Type: application/json; charset=utf-8`.

Sobre `GET rest/obtenerUltimoCodigo.php`:
6. **Hay código**: tras insertar 555, el GET devuelve `error == 0` y
   `valor == 555`.
7. **No hay código**: con la tabla vacía, el GET devuelve
   `error == "no hay ningun codigo almacenado"` (sin `valor`).
8. **Ninguna ruta devuelve HTML**: el cuerpo de ambas respuestas se parsea
   con `json_decode` sin `null`.

## 6. ENTREGABLES

- `rest/insertarCodigo.php`, `rest/obtenerUltimoCodigo.php`,
  `00-Leeme.txt` y `test/mainTest4-rest.php`.
- No generes la web, el móvil ni la interfaz.
- Al terminar muestra la lista de ficheros y el resultado de los tests (o,
  si no tienes servidor en marcha, deja documentado exactamente cómo se
  ejecutan).