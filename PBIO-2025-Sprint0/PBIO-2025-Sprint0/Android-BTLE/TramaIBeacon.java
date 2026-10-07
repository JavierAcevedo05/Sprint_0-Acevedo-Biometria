package org.jordi.prueba2025;

import java.util.Arrays;

/**
 * Clase que representa y parsea un paquete de anuncio iBeacon (Bluetooth Low Energy).
 *
 * Estructura de la trama iBeacon (30 bytes en total):
 *
 *  Bytes   Campo             Descripcion
 *  ------  ----------------  --------------------------------------------------
 *  [0-2]   Adv Flags         Banderas de anuncio BLE (siempre 3 bytes)
 *  [3-4]   Adv Header        Cabecera del anuncio (longitud + tipo)
 *  [5-6]   Company ID        ID del fabricante (Apple = 0x004C)
 *  [7]     iBeacon Type      Tipo de beacon (siempre 0x02)
 *  [8]     iBeacon Length    Longitud del payload iBeacon (siempre 0x15 = 21)
 *  [9-24]  UUID              Identificador unico del beacon (16 bytes)
 *  [25-26] Major             Valor mayor - agrupa beacons (2 bytes)
 *  [27-28] Minor             Valor menor - identifica beacon individual (2 bytes)
 *  [29]    TX Power          Potencia calibrada a 1 metro (1 byte, signed)
 *
 * @author Jordi Bataller i Mascarell
 */
public class TramaIBeacon {

    // Longitud minima que debe tener un paquete iBeacon valido
    private static final int IBEACON_MIN_LENGTH = 30;

    // Valores magicos que identifican un iBeacon de Apple
    private static final byte IBEACON_TYPE = 0x02;
    private static final byte IBEACON_LENGTH = 0x15; // 21 en decimal
    private static final byte APPLE_COMPANY_ID_LOW = 0x4C;
    private static final byte APPLE_COMPANY_ID_HIGH = 0x00;

    // --- Campos del paquete iBeacon (final = inmutables tras constructor) ---

    private final byte[] prefijo;       // 9 bytes: cabecera completa (Flags + Header + CompanyID + Type + Length)
    private final byte[] uuid;          // 16 bytes: identificador unico del beacon
    private final byte[] major;         // 2 bytes: valor mayor (agrupa beacons)
    private final byte[] minor;         // 2 bytes: valor menor (identifica beacon individual)
    private final byte txPower;         // 1 byte: potencia calibrada a 1 metro (signed)

    // --- Sub-campos extraidos del prefijo (bytes 0-8) ---

    private final byte[] advFlags;      // 3 bytes: banderas de anuncio BLE
    private final byte[] advHeader;     // 2 bytes: cabecera del anuncio
    private final byte[] companyID;     // 2 bytes: ID del fabricante
    private final byte iBeaconType;     // 1 byte: tipo de beacon
    private final byte iBeaconLength;   // 1 byte: longitud del payload

    // Copia completa de la trama original
    private final byte[] losBytes;

    // ======================== GETTERS ========================
    // clone() devuelve copia defensiva: el estado interno no se puede modificar desde fuera

    public byte[] getPrefijo()    { return prefijo    != null ? prefijo.clone()    : null; }
    public byte[] getUUID()       { return uuid       != null ? uuid.clone()       : null; }
    public byte[] getMajor()      { return major      != null ? major.clone()      : null; }
    public byte[] getMinor()      { return minor      != null ? minor.clone()      : null; }
    public byte   getTxPower()    { return txPower; }
    public byte[] getLosBytes()   { return losBytes   != null ? losBytes.clone()   : null; }
    public byte[] getAdvFlags()   { return advFlags   != null ? advFlags.clone()   : null; }
    public byte[] getAdvHeader()  { return advHeader  != null ? advHeader.clone()  : null; }
    public byte[] getCompanyID()  { return companyID  != null ? companyID.clone()  : null; }
    public byte   getiBeaconType()   { return iBeaconType; }
    public byte   getiBeaconLength() { return iBeaconLength; }

    // ======================== CONSTRUCTOR ========================

    /**
     * Constructor que parsea una trama iBeacon desde un array de bytes.
     *
     * @param bytes Array de bytes con la trama completa (minimo 30 bytes)
     * @throws IllegalArgumentException si bytes es null o tiene menos de 30 bytes
     */
    public TramaIBeacon(byte[] bytes) {

        // Validacion de entrada: evita NullPointerException / ArrayIndexOutOfBounds
        if (bytes == null || bytes.length < IBEACON_MIN_LENGTH) {
            throw new IllegalArgumentException(
                "Array nulo o con menos de 30 bytes (" + (bytes == null ? 0 : bytes.length) + ")"
            );
        }

        // Copia defensiva: si el llamador modifica el array original, este objeto no se afecta
        this.losBytes = Arrays.copyOf(bytes, bytes.length);

        // --- Extraccion de los campos principales del iBeacon ---
        // copyOfRange(inicio, fin) extrae desde 'inicio' hasta 'fin-1' (fin es exclusivo)

        prefijo  = Arrays.copyOfRange(losBytes, 0, 9);    // Bytes [0-8]: cabecera completa
        uuid     = Arrays.copyOfRange(losBytes, 9, 25);   // Bytes [9-24]: UUID (16 bytes)
        major    = Arrays.copyOfRange(losBytes, 25, 27);   // Bytes [25-26]: Major (2 bytes)
        minor    = Arrays.copyOfRange(losBytes, 27, 29);   // Bytes [27-28]: Minor (2 bytes)
        txPower  = losBytes[29];                            // Byte [29]: TX Power (1 byte)

        // --- Descomposicion del prefijo en sus sub-campos ---

        advFlags      = Arrays.copyOfRange(prefijo, 0, 3); // Bytes [0-2]: Flags BLE
        advHeader     = Arrays.copyOfRange(prefijo, 3, 5); // Bytes [3-4]: Cabecera BLE
        companyID     = Arrays.copyOfRange(prefijo, 5, 7); // Bytes [5-6]: Company ID
        iBeaconType   = prefijo[7];                         // Byte [7]: Tipo iBeacon
        iBeaconLength = prefijo[8];                         // Byte [8]: Longitud iBeacon
    }

    // ======================== UTILIDADES ========================

    /** Comprueba si el Company ID corresponde a Apple (0x004C). */
    public boolean isAppleBeacon() {
        return companyID[0] == APPLE_COMPANY_ID_LOW &&
               companyID[1] == APPLE_COMPANY_ID_HIGH;
    }

    /** Comprueba si los campos magicos del iBeacon son correctos (type=0x02, length=0x15). */
    public boolean isValidBeacon() {
        return iBeaconType == IBEACON_TYPE && iBeaconLength == IBEACON_LENGTH;
    }

    /** Representacion en texto con todos los campos parseados (util para depuracion). */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== iBeacon ===\n");
        sb.append("UUID:      ").append(bytesToHex(uuid)).append("\n");
        sb.append("Major:     ").append(bytesToHex(major)).append("\n");
        sb.append("Minor:     ").append(bytesToHex(minor)).append("\n");
        sb.append("TX Power:  ").append(txPower).append(" dBm\n");
        sb.append("CompanyID: ").append(bytesToHex(companyID));
        sb.append(isAppleBeacon() ? " (Apple)" : " (desconocido)").append("\n");
        sb.append("Type:      0x").append(String.format("%02X", iBeaconType));
        sb.append(isValidBeacon() ? " (valido)" : " (INVALIDO)").append("\n");
        sb.append("Length:    0x").append(String.format("%02X", iBeaconLength)).append("\n");
        sb.append("=============");
        return sb.toString();
    }

    /** Convierte un array de bytes a su representacion hexadecimal. Ejemplo: "0A:FF:01" */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i]));
            if (i < bytes.length - 1) sb.append(":");
        }
        return sb.toString();
    }
}
