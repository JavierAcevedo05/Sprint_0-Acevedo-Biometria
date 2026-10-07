# PROMPT 6 · GENERAR LA UX EN EL NAVEGADOR

## Rol y objetivo
Eres un programador senior de la asignatura. A partir de esta especificación
**inmutable** tienes que generar la **interfaz gráfica de la web** del sistema
"beacon -> móvil -> servidor REST -> base de datos -> web" y sus tests
automáticos.

La web consta **simplemente de un texto en mitad de la pantalla que muestra el
código consultado al servidor**. No añadas botones, formularios, menús ni
funcionalidades que el enunciado no pide. La página NUNCA habla con la BD:
toda consulta pasa por la lógica fake (`ux/logicaFake/obtenerUltimoCodigo.js`).

Copia el estilo del `ux/Aplicacion.html` del esqueleto
`EsqueletoWebAppEnPHPConSesion` (muros HTML, `<script src=...>` + bloque
`<script>` propio).

---

## 1. DISEÑO GRÁFICO (mockup)

La página es un lienzo blanco con UN ÚNICO texto centrado (hueco y alto de
toda la ventana):

```
┌──────────────────────────────────────────────┐
│                                              │
│                                              │
│                                              │
│               Código: 1234                   │
│                                              │
│                                              │
│                                              │
└──────────────────────────────────────────────┘
```

Qué se muestra según el estado de la única consulta (hecha al cargar la
página):

| Estado | Texto mostrado |
|---|---|
| todavía no hay respuesta (carga) | nada (pantalla en blanco) |
| el servidor devuelve un código | `Código: <valor>` |
| el servidor dice que no hay códigos | `No hay ningún código` |
| el servidor no responde / error | `No se ha podido consultar el código` |

## 2. ACLARACIONES EN TEXTO

- La consulta se hace **UNA sola vez**, al cargar la página
  (`DOMContentLoaded`). Nada de refrescos periódicos ni botones: el flujo del
  enunciado es "web se conecta servidor -> web consulta BD -> web muestra
  código".
- La página se sirve desde el **mismo servidor** que el REST (se arranca con
  `php -S localhost:8080 -t .` en la raíz del proyecto) y se abre en
  `http://localhost:8080/ux/Aplicacion.html`.
- La página **no conoce** las rutas REST: llama a
  `obtenerUltimoCodigo( function( err, res ) { ... } )` de
  `logicaFake/obtenerUltimoCodigo.js` (lógica fake) y solo pinta lo que le
  devuelve el callback.
- Convención del callback de la lógica fake:
  - `err === null` → hay resultado; `res.valor` es el código a pintar.
  - `err !== null` → ha fallado; si `err === "no hay ningun codigo almacenado"`
    se muestra `No hay ningún código`; cualquier otro error se muestra como
    `No se ha podido consultar el código`.
  - La lógica fake nunca pinta nada: la página es la que decide los textos.

## 3. DIRECTRICES (qué y cómo, exactamente)

### Fichero a generar: `ux/Aplicacion.html`

Estructura obligatoria (identada a 2 espacios, muros HTML `<!-- ... -->` de
52 guiones como el esqueleto):

1. Al principio del fichero, DOS muros en pareja:

```html
<!-- ---------------------------------------------------- -->
<!-- ---------------------------------------------------- -->
```

2. `<!DOCTYPE html>` y `<html>`.
3. `<head>` precedido de OTRA pareja de muros:
   - `<meta charset="utf-8"/>`.
   - `<title>Codex</title>`.
   - un `<style>` breve que centre el texto:

```html
<style>
html, body { height: 100%; margin: 0; }
#salida {
  display: flex;
  height: 100vh;
  align-items: center;
  justify-content: center;
  font-size: 2em;
  text-align: center;
}
</style>
```

   - un muro simple y debajo:
     `<script src="logicaFake/obtenerUltimoCodigo.js"></script>`
   - un muro simple y debajo el `<script>` propio con la función
     `alCargarLaPagina()` (ver abajo).
4. `<body>` precedido de una pareja de muros. Contenido del `body`:
   `<p id="salida"></p>` (el texto de estado).
5. Tras `</html>`, CINCO muros en parejas de cierre (pie del fichero de la
   misma forma que el esqueleto).

### Función de arranque (dentro del `<script>` propio)

```html
<script>
function alCargarLaPagina() {

  // llamo a la función de la lógica (versión fake)
  obtenerUltimoCodigo( function( err, res ) {
    if ( err ) {
      // no hay código o ha fallado la consulta
      if ( err == "no hay ningun codigo almacenado" ) {
        document.getElementById("salida").innerHTML = "No hay ningún código";
      } else {
        document.getElementById("salida").innerHTML = "No se ha podido consultar el código";
      }
      return;
    }

    // hay código: lo muestro
    document.getElementById("salida").innerHTML = "Código: " + res.valor;
  })

}
document.addEventListener( "DOMContentLoaded", alCargarLaPagina );
</script>
```

Reglas obligatorias:
- La única función del párrafo se llama `alCargarLaPagina` y se registra con
  `DOMContentLoaded` (NUNCA con `window.onload`).
- Solo hay UN elemento con `id="salida"`. No hay `<button>`, `<form>`,
  `<input>`, `<a>`, `<img>`, menús ni nada más en el `body`.
- Los cuatro textos son exactamente:
  - `Código: ` (concatenado con el valor),
  - `No hay ningún código`,
  - `No se ha podido consultar el código`,
  - y el estado de "carga" es la página en blanco (`<p>` vacío).
- El texto "Código: " lleva el espacio tras los dos puntos tal cual.
- Sin librerías externas, sin framework, sin CSS externo.

### Tests automáticos

Fichero `test/mainTest5-ux-web.js` (mocha + assert):

A) **Test de contrato sobre el HTML** (sin dependencias, lee el fichero como
   texto):
1. Existe el fichero `ux/Aplicacion.html`.
2. Contiene `<p id="salida"></p>` exactamente una vez.
3. Contiene `<script src="logicaFake/obtenerUltimoCodigo.js">`.
4. Contiene `DOMContentLoaded`.
5. NO contiene `<button`, `<form`, `<input`, `<a ` (la página es solo texto).
6. Contiene los textos `No hay ningún código` y
   `No se ha podido consultar el código`.

B) **Test de comportamiento con el DOM** (solo si está disponible `jsdom`
   como devDependency; si no, se omite y A) es suficiente):
7. Con el HTML cargado en un DOM jsdom y un `XMLHttpRequest` simulado global
   que responde éxito (`valor = 4321`), al disparar `DOMContentLoaded` el
   `#salida` acaba conteniendo `Código: 4321`.
8. Con el servidor simulando `{"error":"no hay ningun codigo almacenado"}`,
   `#salida` acaba contiendo `No hay ningún código`.

Comando: `npx mocha test/mainTest5-ux-web.js`.

## 4. COMENTARIOS

- Muros HTML `<!-- ... -->` de 52 guiones por parejas para las secciones del
  documento y en simple para destacar cada `<script>` (como el esqueleto).
- Comentarios breves de finalidad (ej.: `// muestro el código recibido`).
- Prohibido comentar línea por línea o comentarios redundantes.

## 5. TESTS AUTOMÁTICOS (lista literal de casos)

1. Existe `ux/Aplicacion.html` y es cargable como documento.
2. La página tiene UN solo texto: `id="salida"` (el único elemento con id).
3. La página NO tiene botones, formularios ni enlaces (solo el texto).
4. La lógica fake se carga: la etiqueta `<script src="logicaFake/obtenerUltimoCodigo.js">`.
5. La consulta se lanza una vez al cargar (registro con `DOMContentLoaded`).
6. Con respuesta correcta se muestra `Código: <valor>`.
7. Con ausencia de código se muestra `No hay ningún código`.
8. Con error de servidor se muestra `No se ha podido consultar el código`.
9. Los cuatro textos del sistema son exactamente los de la tabla de la sección 1.

## 6. ENTREGABLES

- `ux/Aplicacion.html` y `test/mainTest5-ux-web.js`.
- No generes la lógica fake, el servidor REST, el móvil ni la base de datos.
- Al terminar muestra la lista de ficheros y el resultado de
  `npx mocha test/mainTest5-ux-web.js`.