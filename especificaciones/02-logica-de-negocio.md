# PROMPT 2 · GENERAR LA LÓGICA DE NEGOCIO DEL BACKEND (LA VERDADERA)

## Rol y objetivo
Eres un programador senior de la asignatura. A partir de esta especificación
**inmutable** tienes que generar la lógica de negocio del backend del sistema
"beacon -> móvil -> servidor REST -> base de datos -> web" y sus tests
automáticos. Esta es la lógica REAL: la que toca la base de datos. No añadas
funciones, parámetros o ficheros que no estén especificados aquí.

Copia exactamente el estilo del esqueleto `EsqueletoWebAppEnPHPConSesion`
(nota abajo).

---

## 1. DISEÑO GRÁFICO

La lógica es una capa de funciones PHP **puras**: reciben los datos, hacen su
trabajo contra MySQL y devuelven un objeto. No saben nada de HTTP ni de la
interfaz.

```
              +------------------------------------------+
   móvil /    |   logica/                                 |
   web (vía   |   configuracion.php   (datos de conexión) |
   rest) ---> |   conexionBd.php      (abre la conexión)  |
              |   insertarCodigo.php  (inserta un código) |
              |   obtenerUltimoCodigo.php (último código) |
              +----------------------+-------------------+
                                     |
                                     v
                               base de datos
                               (tabla CODIGO)
```

## 2. ACLARACIONES EN TEXTO

- El sistema guarda códigos enteros en el rango `[0, 65535]` (el campo
  *minor* del iBeacon). **Un código válido es un entero en ese rango.**
  Cualquier otra cosa (texto, decimal, negativo, mayor que 65535, `null`) es
  un **código inválido** y la lógica debe rechazarlo.
- La `fecha` **no la envía nadie**: la rellena la propia base de datos con
  `DEFAULT CURRENT_TIMESTAMP`. La lógica, al insertar, SOLO manda `valor`, y
  después lee de vuelta la fila para devolver `id`, `valor`, `fecha`.
- La consulta para la web devuelve **el último código almacenado**
  (`order by id desc limit 1`). Si la tabla está vacía, la lógica lo señala
  con el error `"no hay ningun codigo almacenado"`.
- En un diseño lógico NUNCA aparecen ni callbacks ni promesas: se devuelve un
  objeto creado con `new stdClass`.
- No hay usuarios, no hay sesión, no hay autenticación.
- Los tests usan una base de datos de pruebas `codex_test` (la configuración
  viene de variables de entorno, ver sección 3).

## 3. DIRECTRICES (qué y cómo, exactamente)

### Ficheros a generar (en la carpeta `logica/`)

1. `logica/configuracion.php`
2. `logica/conexionBd.php`
3. `logica/insertarCodigo.php`
4. `logica/obtenerUltimoCodigo.php`
5. `test/mainTest2-logica.php` (Tests, ver sección 5)

### Contrato exacto de cada función (cópialo en su cabecera)

**`logica/configuracion.php`** — solo define constantes leídas de variables
de entorno con valor por defecto. Cabecera:

```
// ---------------------------------------------------------
//
// SERVIDOR_BD:Texto   (entorno CODEX_SERVIDOR_BD,  "localhost")
// USUARIO_BD:Texto    (entorno CODEX_USUARIO_BD,   "root")
// PASSWORD_BD:Texto   (entorno CODEX_PASSWORD_BD,  "")
// NOMBRE_BD:Texto     (entorno CODEX_NOMBRE_BD,    "codex")
//
// la configuracion se lee de variables de entorno para que
// los tests puedan apuntar a la base codex_test sin tocar
// este codigo
//
// ---------------------------------------------------------
```

Define las constantes solo si no están definidas (con `defined()`), para que
los tests puedan fijarlas antes de `require`.

**`logica/conexionBd.php`** — abre una conexión PDO nueva en cada llamada.

```
// ---------------------------------------------------------
//
// -->
// conectarBd() -->
// <--
// <--
// PDO | error:Texto
//
// error:Texto : si no se puede conectar, la conexion falla
//              (PDOException) y el proceso termina con un
//              objeto de error
//
// ---------------------------------------------------------
```

- Usa `new PDO( $dsn, ... )` sobre la constante `NOMBRE_BD`,
  con `ERRMODE_EXCEPTION`. DSN =
  `mysql:host=SERVIDOR_BD;dbname=NOMBRE_BD;charset=utf8mb4`.
- Si no se puede conectar, devuelve `new stdClass` con
  `$objetoResultado->error = "no se puede conectar con la base de datos";`
  y lo devuelve (sin lanzar la excepción hacia arriba: la captura con
  try/catch).

**`logica/insertarCodigo.php`** — función `insertarCodigo( $valor )`.

```
// ---------------------------------------------------------
//
// valor:Entero -->
// insertarCodigo() -->
// <--
// <--
// {id:Entero, valor:Entero, fecha:Texto} | error:Texto
//
// valor: código recibido. Debe ser entero en [0, 65535]
// {id, valor, fecha}: fila insertada, leída de la base de datos
// error:Texto : si valor no es valido o si no queda almacenado
//
// ---------------------------------------------------------
```

Implementación exacta:
1. Validar `valor`:
   - que sea un entero (acepta `1234` y también la cadena `"1234"`, y
     devuelve `"el codigo no es valido"` si es cualquier otra cosa),
   - `0 <= valor <= 65535`. Si falla: `$objetoResultado->error = "el codigo no es
     valido";` y `return $objetoResultado;`.
2. Abrir conexión con `conectarBd()`. Si devuelve `error`, propagarlo y
   `return`.
3. `insert into CODIGO ( valor ) values ( :valor )` con **sentencia
   preparada** (PDO) — nunca concatenar el valor en la SQL. Parámetro con
   `bindValue` y `PARAM_INT`.
4. Recuperar la fila insertada:
   `select id, valor, fecha from CODIGO where id = :id` con
   `lastInsertId()`.
5. Devolver `new stdClass` con `id` (convertido a int), `valor` (int) y
   `fecha` (texto del formato `d-m-Y H:i:s` devuelto por MySQL). En el éxito
   **no** hay campo `error` en el objeto.
6. Si el `select` no devuelve fila (else imposible), devolver
   `error = "no se ha podido almacenar el codigo"`.

**`logica/obtenerUltimoCodigo.php`** — función `obtenerUltimoCodigo()`.

```
// ---------------------------------------------------------
//
// -->
// obtenerUltimoCodigo() -->
// <--
// <--
// {id:Entero, valor:Entero, fecha:Texto} | error:Texto
//
// devuelve el ultimo codigo almacenado (order by id desc limit 1)
// error:Texto : "no hay ningun codigo almacenado" si la tabla
//              no tiene filas
//
// ---------------------------------------------------------
```

Implementación exacta:
1. Abrir conexión con `conectarBd()`; propagar el `error` si lo hay.
2. `select id, valor, fecha from CODIGO order by id desc limit 1`.
3. Si no hay filas: `$objetoResultado->error = "no hay ningun codigo almacenado";`
   y `return`.
4. Si hay fila: devolver `{id, valor, fecha}` exactamente como en
   `insertarCodigo` (sin campo `error`).

### Reglas de aislamiento (prohibiciones) para TODOS los ficheros de `logica/`

- PROHIBIDO: `$_GET`, `$_POST`, `$_REQUEST`, `$_SESSION`, `$_SERVER`,
  `file_get_contents( "php://input" )`.
- PROHIBIDO: `echo`, `print`, `header(...)`, `json_encode`, `die`, `exit`,
  HTML, marcado, o cualquier cosa que escriba en la salida. Solo `return`.
- PROHIBIDO: modificar la base de datos de producción en los tests
  (los tests usan `codex_test`).
- Indentación de **2 espacios**. Variables con minúscula sin separadores
  (`$objetoResultado`, `$laConexion`, `$laSentencia`, `$laFila`). Funciones en
  minúscula sin separadores.
- La salida SIEMPRE es un `stdClass` (`$objetoResultado`), nunca un array.
- Los ficheros acaban con `?>` tal como el esqueleto.

### Estructura de los tests

`test/mainTest2-logica.php` es un script PHP autónomo (sin framework, solo
PDO) que:
- fija las `CODEX_*` para apuntar a `codex_test` (o las lee del entorno),
- crea la tabla `CODIGO` en `codex_test` (mismo esquema del prompt de BD),
- ejecuta cada prueba de la sección 5,
- imprime `OK`/`FALLO` por prueba y `resultado final: N de N OK`,
- termina con `exit(0)` / `exit(1)`,
- al final deja la base `codex_test` vacía (borra la tabla) para que sea
  reproducible.

## 4. COMENTARIOS

- Separa las distintas partes del código con DOS muros:

```
// ---------------------------------------------------------
// ---------------------------------------------------------
```

- La cabecera de cada función es el contrato de la sección 3, copiado tal
  cual, encuadrado entre dos muros.
- Añade comentarios **breves** que aclaren la función de cada parte, como el
  esqueleto:

```php
  // comprobación "rigurosa" del valor
  if ( $valor < 0 || $valor > 65535 ) { ... }
```

- Prohibido: comentarios línea por línea, redundantes o que digan lo que ya
  dice el código.

## 5. TESTS AUTOMÁTICOS (lista literal de casos)

1. **Recepción de un código válido**: `insertarCodigo( 1234 )` devuelve
   objeto sin `error`, con `valor = 1234` y `id > 0`.
2. **Recepción en los límites**: `insertarCodigo( 0 )` y
   `insertarCodigo( 65535 )` devuelven objeto sin `error`.
3. **Rechazo de un código inválido**: cada uno de `-1`, `65536`, `"abc"`,
   `12.5` y `null` devuelve `error = "el codigo no es valido"` y NO inserta
   nada (comprobar que la tabla no ha crecido).
4. **Almacenamiento correcto**: tras `insertarCodigo( 777 )`, un
   `select valor from CODIGO where id = <el id>` devuelve 777.
5. **Consulta correcta**: insertar 111, 222 y 333 en ese orden;
   `obtenerUltimoCodigo()` devuelve `valor = 333`.
6. **Consulta vacía**: con la tabla vacía, `obtenerUltimoCodigo()` devuelve
   `error = "no hay ningun codigo almacenado"`.
7. **Registro de la fecha de recepción**: `insertarCodigo( 1234 )` devuelve
   una `fecha` no nula, con formato de fecha/hora, y que está dentro de una
   ventana de ±10 segundos respecto a `date( "Y-m-d H:i:s" )` en el momento
   de la prueba. Además, insertar dos códigos seguidos y comprobar que la
   `fecha` del segundo no es anterior a la del primero.
8. **Códigos consecutivos**: dos `insertarCodigo` seguidos devuelven `id`
   distintos y crecientes.

## 6. ENTREGABLES

- `logica/configuracion.php`, `logica/conexionBd.php`,
  `logica/insertarCodigo.php`, `logica/obtenerUltimoCodigo.php`,
  `test/mainTest2-logica.php`.
- No generes HTML, ni el servidor REST, ni la web, ni el móvil.
- Al terminar muestra la lista de ficheros y el resultado de
  `php test/mainTest2-logica.php`.