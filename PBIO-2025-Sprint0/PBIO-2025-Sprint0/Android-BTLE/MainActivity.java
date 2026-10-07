package org.jordi.prueba2025;

import android.bluetooth.le.ScanSettings;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;

import android.os.Bundle;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Activity principal que escanea dispositivos Bluetooth Low Energy (BLE).
 *
 * Funcionalidades:
 *   1. Escanear TODOS los dispositivos BLE cercanos
 *   2. Escanear un dispositivo especifico filtrado por nombre
 *   3. Mostrar informacion del dispositivo encontrado (parseado como iBeacon)
 *
 * Flujo tipico:
 *   onCreate -> inicializarBlueTooth (permisos + scanner)
 *   Boton "Buscar todos" -> buscarTodosLosDispositivosBTLE()
 *   Boton "Buscar nuestro" -> buscarEsteDispositivoBTLE("GTI3A-2025")
 *   Callback onScanResult -> mostrarInformacionDispositivoBTLE()
 *   Boton "Detener" -> detenerBusquedaDispositivosBTLE()
 *
 * @author Jordi Bataller i Mascarell
 */
public class MainActivity extends AppCompatActivity {

    private static final String ETIQUETA_LOG = ">>>>";

    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    // Scanner BLE y callback activo
    private BluetoothLeScanner elEscanner;
    private ScanCallback callbackDelEscaneo = null;

    // ======================== ESCANEO ========================

    /**
     * Escanea TODOS los dispositivos BLE cercanos (sin filtros).
     * Por cada dispositivo encontrado, llama a mostrarInformacionDispositivoBTLE().
     */
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): empieza");

        // Crear el callback que se ejecutara por cada dispositivo encontrado
        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): onScanResult()");
                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): onBatchScanResults()");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): onScanFailed() errorCode=" + errorCode);
            }
        };

        // Comprobar permiso BLUETOOTH_SCAN antes de escanear
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): sin permiso BLUETOOTH_SCAN");
            return;
        }

        // Iniciar escaneo SIN filtros (todos los dispositivos)
        this.elEscanner.startScan(this.callbackDelEscaneo);
        Log.d(ETIQUETA_LOG, "buscarTodosLosDispositivosBTLE(): startScan sin filtros");
    }

    /**
     * Muestra toda la informacion de un dispositivo BLE encontrado.
     * Si el dispositivo es un iBeacon, parsea la trama y muestra todos sus campos.
     *
     * @param resultado  Resultado del escaneo BLE con los datos del dispositivo
     */
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {
        Log.d(ETIQUETA_LOG, "mostrarInformacionDispositivoBTLE(): empieza");

        BluetoothDevice bluetoothDevice = resultado.getDevice();
        byte[] bytes = resultado.getScanRecord().getBytes();
        int rssi = resultado.getRssi();

        Log.d(ETIQUETA_LOG, "****************************************************");
        Log.d(ETIQUETA_LOG, "DISPOSITIVO DETECTADO BTLE");
        Log.d(ETIQUETA_LOG, "****************************************************");

        // Comprobar permiso BLUETOOTH_CONNECT para acceder a nombre/direccion
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, "sin permiso BLUETOOTH_CONNECT, no puedo mostrar nombre");
            return;
        }

        // Informacion basica del dispositivo
        Log.d(ETIQUETA_LOG, "nombre    = " + bluetoothDevice.getName());
        Log.d(ETIQUETA_LOG, "direccion = " + bluetoothDevice.getAddress());
        Log.d(ETIQUETA_LOG, "rssi      = " + rssi + " dBm");
        Log.d(ETIQUETA_LOG, "bytes (" + bytes.length + ") = " + Utilidades.bytesToHexString(bytes));

        // Parsear como iBeacon y mostrar campos detallados
        try {
            TramaIBeacon tib = new TramaIBeacon(bytes);

            Log.d(ETIQUETA_LOG, "---------- iBeacon parseado ----------");
            Log.d(ETIQUETA_LOG, "prefijo       = " + Utilidades.bytesToHexString(tib.getPrefijo()));
            Log.d(ETIQUETA_LOG, "  advFlags    = " + Utilidades.bytesToHexString(tib.getAdvFlags()));
            Log.d(ETIQUETA_LOG, "  advHeader   = " + Utilidades.bytesToHexString(tib.getAdvHeader()));
            Log.d(ETIQUETA_LOG, "  companyID   = " + Utilidades.bytesToHexString(tib.getCompanyID()));
            Log.d(ETIQUETA_LOG, "  type        = 0x" + Integer.toHexString(tib.getiBeaconType()));
            Log.d(ETIQUETA_LOG, "  length      = 0x" + Integer.toHexString(tib.getiBeaconLength())
                    + " (" + tib.getiBeaconLength() + ")");
            Log.d(ETIQUETA_LOG, "uuid          = " + Utilidades.bytesToHexString(tib.getUUID()));
            Log.d(ETIQUETA_LOG, "uuid (texto)  = " + Utilidades.bytesToString(tib.getUUID()));
            Log.d(ETIQUETA_LOG, "major         = " + Utilidades.bytesToHexString(tib.getMajor())
                    + " (" + Utilidades.bytesToInt(tib.getMajor()) + ")");
            Log.d(ETIQUETA_LOG, "minor         = " + Utilidades.bytesToHexString(tib.getMinor())
                    + " (" + Utilidades.bytesToInt(tib.getMinor()) + ")");
            Log.d(ETIQUETA_LOG, "txPower       = 0x" + Integer.toHexString(tib.getiBeaconLength())
                    + " (" + tib.getTxPower() + ")");
            Log.d(ETIQUETA_LOG, "apple         = " + tib.isAppleBeacon());
            Log.d(ETIQUETA_LOG, "valido        = " + tib.isValidBeacon());
            Log.d(ETIQUETA_LOG, "--------------------------------------");
        } catch (IllegalArgumentException e) {
            Log.d(ETIQUETA_LOG, "No es un iBeacon valido: " + e.getMessage());
        }
    }

    /**
     * Escanea un dispositivo BLE especifico filtrado por nombre.
     *
     * @param dispositivoBuscado  Nombre del dispositivo a buscar (ej: "GTI3A-2025")
     */
    private void buscarEsteDispositivoBTLE(final String dispositivoBuscado) {
        Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): buscando '" + dispositivoBuscado + "'");

        // Crear filtro por nombre de dispositivo
        ScanFilter sf = new ScanFilter.Builder()
                .setDeviceName(dispositivoBuscado)
                .build();

        // Configurar modo de escaneo (LOW_LATENCY = rapido pero mas bateria)
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        ArrayList<ScanFilter> filtros = new ArrayList<>();
        filtros.add(sf);

        // Crear el callback
        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): onScanResult()");
                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): onBatchScanResults()");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): onScanFailed() errorCode=" + errorCode);
            }
        };

        // Comprobar permiso BLUETOOTH_SCAN
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): sin permiso BLUETOOTH_SCAN");
            return;
        }

        // Iniciar escaneo CON filtro y settings (UNA sola vez)
        this.elEscanner.startScan(filtros, settings, this.callbackDelEscaneo);
        Log.d(ETIQUETA_LOG, "buscarEsteDispositivoBTLE(): startScan con filtro '" + dispositivoBuscado + "'");
    }

    /**
     * Detiene el escaneo BLE activo y libera el callback.
     */
    private void detenerBusquedaDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, "detenerBusquedaDispositivosBTLE(): empieza");

        if (this.callbackDelEscaneo == null) {
            Log.d(ETIQUETA_LOG, "detenerBusquedaDispositivosBTLE(): no hay escaneo activo");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, "detenerBusquedaDispositivosBTLE(): sin permiso BLUETOOTH_SCAN");
            return;
        }

        this.elEscanner.stopScan(this.callbackDelEscaneo);
        this.callbackDelEscaneo = null; // Limpiar referencia
        Log.d(ETIQUETA_LOG, "detenerBusquedaDispositivosBTLE(): scan detenido");
    }

    // ======================== HANDLERS DE BOTONES ========================

    /** Handler del boton "Buscar todos los dispositivos BTLE" */
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton buscar dispositivos BTLE pulsado");
        this.buscarTodosLosDispositivosBTLE();
    }

    /** Handler del boton "Buscar nuestro dispositivo BTLE" */
    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton nuestro dispositivo BTLE pulsado");
        this.buscarEsteDispositivoBTLE("GTI3A-2025");
    }

    /** Handler del boton "Detener busqueda" */
    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton detener busqueda dispositivos BTLE pulsado");
        this.detenerBusquedaDispositivosBTLE();
    }

    // ======================== INICIALIZACION BLUETOOTH ========================

    /**
     * Inicializa el Bluetooth: obtiene adaptador, scanner y pide permisos necesarios.
     * En Android 12+ se necesitan permisos BLUETOOTH_SCAN y BLUETOOTH_CONNECT.
     */
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): empieza");

        // Obtener adaptador Bluetooth
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): SOCORRO - No hay adaptador Bluetooth");
            return;
        }

        // Habilitar Bluetooth si no esta activo
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): sin permiso BLUETOOTH_CONNECT");
        } else if (!bta.isEnabled()) {
            // Nota: en Android 12+ esto puede no funcionar. Usar ACTION_REQUEST_ENABLE.
            bta.enable();
        }

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): habilitado=" + bta.isEnabled()
                + " estado=" + bta.getState());

        // Obtener scanner BLE
        this.elEscanner = bta.getBluetoothLeScanner();
        if (this.elEscanner == null) {
            Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): SOCORRO - No se obtuvo el scanner BLE");
            return;
        }

        // Comprobar estado de permisos (solo para log, no bloquea)
        logPermisos();

        // Pedir permisos necesarios para Android 12+
        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): pidiendo permisos");
        requestPermissions(
                new String[]{
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                CODIGO_PETICION_PERMISOS
        );

        Log.d(ETIQUETA_LOG, "inicializarBlueTooth(): termina");
    }

    /**
     * Metodo auxiliar que registra en el log el estado de cada permiso.
     * Util para depurar problemas de permisos.
     */
    private void logPermisos() {
        String[] permisos = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
        for (String permiso : permisos) {
            boolean concedido = ContextCompat.checkSelfPermission(this, permiso)
                    == PackageManager.PERMISSION_GRANTED;
            Log.d(ETIQUETA_LOG, "  permiso " + permiso + " = " + (concedido ? "SI" : "NO"));
        }
    }

    // ======================== CICLO DE VIDA ========================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(ETIQUETA_LOG, "onCreate(): empieza");
        inicializarBlueTooth();
        Log.d(ETIQUETA_LOG, "onCreate(): termina");
    }

    /**
     * Cuando la Activity pierde el focu (ej: usuario cambia de app),
     * paramos el escaneo para ahorrar bateria.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(ETIQUETA_LOG, "onPause(): paramos escaneo si esta activo");
        detenerBusquedaDispositivosBTLE();
    }

    /**
     * Callback de resultado de peticion de permisos.
     * Comprueba SI TODOS los permisos fueron concedidos (no solo el primero).
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(ETIQUETA_LOG, "onRequestPermissionsResult(): empieza");

        if (requestCode != CODIGO_PETICION_PERMISOS) {
            return;
        }

        // Comprobar que TODOS los permisos fueron concedidos
        boolean todosConcedidos = true;
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Log.d(ETIQUETA_LOG, "onRequestPermissionsResult(): permiso DENEGADO = " + permissions[i]);
                todosConcedidos = false;
            }
        }

        if (todosConcedidos) {
            Log.d(ETIQUETA_LOG, "onRequestPermissionsResult(): todos los permisos concedidos");
        } else {
            Log.d(ETIQUETA_LOG, "onRequestPermissionsResult(): SOCORRO - faltan permisos");
        }
    }
}
