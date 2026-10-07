# PROMPT 1 - GENERAR LA BASE DE DATOS

objetivo
A partir de esta especificación tienes que generar la base de datos MySQL/MariaDB
 del sistema "beacon -> móvil -> servidor REST -> base de datos -> web" y sus tests
automáticos. No añadas ninguna tabla, columna, restricción o fichero que no
esté especificado aquí.

---

## 1. DISEÑO GRÁFICO

La base de datos consta de UNA sola tabla. Este es su diagrama (es el modelo
exacto que debes implementar):

```
CODIGO
-------------
id     (int, clave primaria, auto_increment)
valor  (int, obligatorio)
fecha  (DateTime, obligatorio)
```

Diagrama de contexto:

```
                      +-------------+
  móvil (POST) -----> |  SERVICIO   |
                      |  DE  DATOS  |
  web (GET) --------> |             |
                      +-------------+
                      /             \
                     v               v
              +-------------+  (solo lectura por la web)
              |   CODIGO    |
              |  id valor   |
              |  fecha      |
              +-------------+
```

## 2. ACLARACIONES EN TEXTO

- La tabla se llama **CODIGO**
- 'id' es la clave primaria y la rellena sola el motor (`auto_increment`).
  El móvil y la web NUNCA envían `id`.
- 'valor' es el código que trae el beacon. Es un entero en el rango
  [0, 65535] (es el campo minor del iBeacon, 2 bytes sin signo). La base
  de datos debe impedir cualquier valor fuera de ese rango.
- 'fecha' es el momento en que se recibe el código. La rellena sola la
  base de datos con DEFAULT CURRENT_TIMESTAMP en el mismo INSERT. El móvil
  NUNCA la envía.
- Motor InnoDB y juego de caracteres utf8mb4 (para evitar problemas con
  acentos).
- Hay una base de datos de producción (codex) y los tests crean y destruyen
  una base de datos de pruebas (codex_test). El script de SQL crea la de
  producción; los tests crean la suya copiando el mismo esquema.
- Las tablas se crean una sola vez a mano/script, como manda la asignatura:
  la aplicación solo hace INSERT y SELECT.

## 3. DIRECTRICES (qué y cómo, exactamente)

### Ficheros que debes generar

1. `bd/crearCodigo.sql` — crea la base de datos de producción y la tabla.
2. `bd/borrarCodigo.sql` — `DROP TABLE IF EXISTS CODIGO;` (lo usan los tests
   para poder empezar de cero).
3. `test/mainTest1-bd.php` — los tests automáticos .

### Contenido exacto del esquema

El fichero `bd/crearCodigo.sql` debe ejecutar, en este orden:

```sql
-- base de datos de producción
create database if not exists codex
  default character set utf8mb4;

use codex;

create table CODIGO (
  id     int       not null auto_increment,
  valor  int       not null,
  fecha  datetime  not null default current_timestamp,
  constraint CODIGO_PK primary key ( id ),
  constraint CODIGO_VALOR_RANGO
    check ( valor >= 0 and valor <= 65535 )
) 

Aclaraciones obligatorias:
- La sentencia de creación termina con `;`. Todas las sentencias SQL llevan
  `;` al final.
- Los identificadores SQL van en MAYÚSCULAS (`CODIGO`, `id`, `valor`,
  `fecha` en minúsculas, como en el diagrama).
- Nota en un comentario del SQL: el `CHECK` necesita MySQL 8.0.16

### Estructura de los tests

`test/mainTest1-bd.php` es un script PHP que se ejecuta con
`php test/mainTest1-bd.php`, no usa ninguna librería externa (solo PDO, que
viene con PHP), y:
- crea la base `codex_test` y la tabla `CODIGO` en ella (usando el mismo
  esquema),
- ejecuta cada prueba,
- imprime `OK` o `FALLO` con el nombre de cada prueba y, al final,
  `resultado final: N de N OK`;
- termina con `exit(0)` si todas pasan y `exit(1)` si alguna falla
  (argumento de `exit`).
- las pruebas se ejecutan al final sobre una base **destruida y recreada de
  nuevo**, de forma que el test es reproducible tantas veces como se lance.

Configuración de conexión (hardcodeada en el TEST, no en el SQL):
usuario `root`, password vacío, servidor `localhost`. Irá leyendo las
variables de entorno `CODEX_USUARIO_BD`, `CODEX_PASSWORD_BD`,
`CODEX_SERVIDOR_BD` y `CODEX_NOMBRE_BD_TEST` con esos valores por defecto.

## 4. COMENTARIOS

- Separa las distintas partes del código con DOS líneas de muro:

```
// ---------------------------------------------------------
// ---------------------------------------------------------
```

- Cada parte lleva una **breve explicación de su finalidad** en un comentario
  de una o dos líneas justo debajo del muro. Ejemplo:

```sql
// ---------------------------------------------------------
// creacion de la base de datos de produccion
// ---------------------------------------------------------
create database if not exists codex ...
```



## 5. TESTS AUTOMÁTICOS (lista literal de casos)

1. **Creación de la tabla**: existe la tabla `CODIGO` en `codex_test`; tiene
   exactamente las columnas `id`, `valor`, `fecha`; `id` es clave primaria;
   `fecha` es `datetime`.
2. **Inserción de un código**: `insert into CODIGO (valor) values (1234)`
   devuelve una fila con `id` > 0 y `fecha` no nula.
3. **Consulta de un código**: tras insertar el 1234, un
   `select valor from CODIGO where id = <el id>` devuelve exactamente 1234.
4. **Restricción valor obligatorio**: insertar sin `valor` da error
   (integridad). El fallo debe capturarse con try/catch sobre PDO; la prueba
   pasa si PDO lanza excepción.
5. **Restricción fecha obligatoria**: el `DEFAULT CURRENT_TIMESTAMP` hace que
   la `fecha` se rellene sola: insertar solo `valor` y comprobar que la
   `fecha` devuelta NO es nula (no hace falta insertar fecha).
6. **Restricción clave primaria**: insertar dos filas con el mismo `id`
   explícito da error (excepción).
7. **Restricción del rango de `valor`**: `valor = -1` da error y
   `valor = 65536` da error; `valor = 0` y `valor = 65535` se insertan bien.

## 6. ENTREGABLES

- `bd/crearCodigo.sql`, `bd/borrarCodigo.sql` y `test/mainTest1-bd.php`.
- No generes ningún fichero más (sobre todo, NO generes código PHP de
  aplicación ni HTML).
- Al terminar muestra la lista de ficheros creados y el resultado de
  ejecutar `php test/mainTest1-bd.php`.