package org.jordi.prueba2025;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Clase de utilidades para conversiones de tipos usadas en el parseado BLE/iBeacon.
 * Todos los metodos son estaticos (no requieren instancia).
 *
 * Conversiones soportadas:
 *   - String <-> byte[]
 *   - UUID <-> String (texto y hexadecimal)
 *   - byte[] <-> int / long
 *   - byte[] -> String hexadecimal
 *
 * @author Jordi Bataller i Mascarell
 */
public class Utilidades {

    // ======================== STRING <-> BYTES ========================

    /**
     * Convierte un string a array de bytes usando codificacion UTF-8.
     *
     * @param texto  Cadena de entrada
     * @return       Array de bytes en UTF-8, o null si texto es null
     *
     * Ejemplo: "Hola" -> [72, 111, 108, 97]
     */
    public static byte[] stringToBytes(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Convierte un array de bytes a string interpretando cada byte como caracter ASCII.
     *
     * @param bytes  Array de entrada
     * @return       String resultante, o cadena vacia si bytes es null
     *
     * Ejemplo: [72, 111, 108, 97] -> "Hola"
     */
    public static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    // ======================== UUID <-> STRING ========================

    /**
     * Convierte un string de 16 caracteres a un objeto UUID.
     * Divide el string en dos mitades de 8 caracteres y las convierte a los dos long
     * que componen un UUID (most significant bits + least significant bits).
     *
     * @param uuid  String de exactamente 16 caracteres (no es un UUID estandar con guiones)
     * @return      Objeto UUID
     * @throws IllegalArgumentException si el string no tiene 16 caracteres
     *
     * Ejemplo: "ABCDEFGHIJKLMNOP" -> UUID(0x4142434445464748, 0x494A4B4C4D4E4F50)
     */
    public static UUID stringToUUID(String uuid) {
        if (uuid == null || uuid.length() != 16) {
            throw new IllegalArgumentException(
                "stringToUUID: el string debe tener exactamente 16 caracteres (recibido: "
                + (uuid == null ? "null" : uuid.length()) + ")"
            );
        }

        // Dividimos en dos mitades de 8 caracteres cada una
        String parteAlta = uuid.substring(0, 8);   // Bytes [0-7]  -> most significant bits
        String parteBaja = uuid.substring(8, 16);   // Bytes [8-15] -> least significant bits

        // Convertimos cada mitade a long y construimos el UUID
        long msb = bytesToLong(parteAlta.getBytes(StandardCharsets.UTF_8));
        long lsb = bytesToLong(parteBaja.getBytes(StandardCharsets.UTF_8));

        return new UUID(msb, lsb);
    }

    /**
     * Convierte un UUID a string de 16 caracteres (representacion textual de los bytes).
     *
     * @param uuid  Objeto UUID
     * @return      String de 16 caracteres
     *
     * Ejemplo: UUID(0x4142434445464748, 0x494A4B4C4D4E4F50) -> "ABCDEFGHIJKLMNOP"
     */
    public static String uuidToString(UUID uuid) {
        if (uuid == null) {
            return "";
        }
        byte[] bytes = dosLongToBytes(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        return bytesToString(bytes);
    }

    /**
     * Convierte un UUID a su representacion hexadecimal de 32 caracteres.
     *
     * @param uuid  Objeto UUID
     * @return      String hexadecimal (32 caracteres, sin guiones)
     *
     * Ejemplo: UUID(0x0A1B2C3D..., ...) -> "0a1b2c3d..."
     */
    public static String uuidToHexString(UUID uuid) {
        if (uuid == null) {
            return "";
        }
        byte[] bytes = dosLongToBytes(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
        return bytesToHexString(bytes);
    }

    // ======================== CONVERSIONES NUMERICAS ========================

    /**
     * Combina dos long en un solo array de 16 bytes.
     * Primer long = bytes [0-7] (most significant), segundo long = bytes [8-15] (least significant).
     *
     * @param masSignificativos    Primeros 8 bytes
     * @param menosSignificativos  Ultimos 8 bytes
     * @return                     Array de 16 bytes
     */
    public static byte[] dosLongToBytes(long masSignificativos, long menosSignificativos) {
        ByteBuffer buffer = ByteBuffer.allocate(2 * Long.BYTES); // 16 bytes
        buffer.putLong(masSignificativos);   // Bytes [0-7]
        buffer.putLong(menosSignificativos); // Bytes [8-15]
        return buffer.array();
    }

    /**
     * Convierte un array de bytes a int usando BigInteger.
     * Soporta cualquier longitud de bytes.
     *
     * @param bytes  Array de entrada
     * @return       Valor int resultante
     * @throws IllegalArgumentException si bytes es null o vacio
     */
    public static int bytesToInt(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytesToInt: el array no puede ser null o vacio");
        }
        return new BigInteger(bytes).intValue();
    }

    /**
     * Convierte un array de bytes a long usando BigInteger.
     * Soporta cualquier longitud de bytes.
     *
     * @param bytes  Array de entrada
     * @return       Valor long resultante
     * @throws IllegalArgumentException si bytes es null o vacio
     */
    public static long bytesToLong(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytesToLong: el array no puede ser null o vacio");
        }
        return new BigInteger(bytes).longValue();
    }

    /**
     * Convierte un array de bytes a int usando operaciones bit a bit (sin BigInteger).
     * Maximo 4 bytes. Maneja signo con complemento a 2.
     *
     * @param bytes  Array de entrada (maximo 4 bytes)
     * @return       Valor int resultante
     * @throws IllegalArgumentException si bytes es null o tiene mas de 4 bytes
     *
     * Algoritmo:
     *   1. Para cada byte, desplazar el resultado 8 bits a la izquierda (* 256)
     *   2. Sumar el byte actual usando (b & 0xFF) para tratarlo como unsigned
     *   3. Si el bit de signo (bit 7 del primer byte) esta activo, aplicar complemento a 2
     */
    public static int bytesToIntOK(byte[] bytes) {
        if (bytes == null) {
            return 0;
        }
        if (bytes.length > 4) {
            throw new IllegalArgumentException(
                "bytesToIntOK: maximo 4 bytes para convertir a int (recibidos: " + bytes.length + ")"
            );
        }

        int res = 0;

        // Construir el int byte a byte, de izquierda a derecha (big-endian)
        for (byte b : bytes) {
            res = (res << 8)          // Desplazar 8 bits a la izquierda (* 256)
                + (b & 0xFF);         // Mask FF: tratar byte como unsigned (0-255)
        }

        // Complemento a 2: si el bit de signo (bit 7 del primer byte) esta activo
        // NOTA: se usa 0x80 (bit 7) para detectar negativo, NO 0x8 (bit 3)
        if ((bytes[0] & 0x80) != 0) {
            res = -(~res + 1);        // Invertir bits y sumar 1 (negacion en complemento a 2)
        }

        return res;
    }

    // ======================== HEXADECIMAL ========================

    /**
     * Convierte un array de bytes a su representacion hexadecimal con separador ':'.
     *
     * @param bytes  Array de entrada
     * @return       String hexadecimal (ej: "0A:FF:01"), o cadena vacia si bytes es null
     */
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));  // 2 digitos hex en minusculas
            if (i < bytes.length - 1) {                  // Separador ':' entre bytes, pero NO al final
                sb.append(':');
            }
        }
        return sb.toString();
    }
}
