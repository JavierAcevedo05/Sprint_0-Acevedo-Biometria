package org.jordi.prueba2025;

import android.bluetooth.le.ScanSettings;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;

import android.os.Build;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {


    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private static final String ETIQUETA_LOG = ">>>>";

    private static final int CODIGO_PETICION_PERMISOS = 11223344;

    private static final int FABRICANTE_APPLE = 0x004C;

    private static final String UUID_NUESTRO_BEACON = "EPSG-GTI-PROY-3A";

    private static final byte[] PREFIJO_I_BEACON = {
            0x02, 0x15, 0x45, 0x50, 0x53, 0x47, 0x2D, 0x47, 0x54, 0x49
    };

    private static final byte[] MASCARA_I_BEACON = {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    };

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private BluetoothLeScanner elEscanner;

    private ScanCallback callbackDelEscaneo = null;

    private TextView textoDatoBeacon;

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void buscarTodosLosDispositivosBTLE() {
        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empieza ");

        this.textoDatoBeacon.setText("Buscando el beacon 1234...");

        if (this.elEscanner == null) {
            this.textoDatoBeacon.setText("Bluetooth no está listo");
            return;
        }

        if (!puedeEscanear()) {
            this.textoDatoBeacon.setText("Faltan permisos de Bluetooth");
            return;
        }

        if (this.callbackDelEscaneo != null) {
            this.elEscanner.stopScan(this.callbackDelEscaneo);
        }

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): instalamos scan callback ");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onScanResult() ");

                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (ScanResult resultado : results) {
                    mostrarInformacionDispositivoBTLE(resultado);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): onScanFailed() ");

            }
        };

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): empezamos a escanear ");

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): startScan ");
        this.elEscanner.startScan(this.callbackDelEscaneo);

        Log.d(ETIQUETA_LOG, " buscarTodosLosDispositivosBTL(): termina ");
    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private void mostrarInformacionDispositivoBTLE(ScanResult resultado) {
        ScanRecord scanRecord = resultado.getScanRecord();
        if (scanRecord == null) {
            return;
        }

        byte[] bytes = obtenerDatosAnuncio(scanRecord);
        if (bytes == null || bytes.length < 30) {
            return;
        }

        if ((bytes[5] & 0xFF) != 0x4C || bytes[6] != 0x00 ||
                bytes[7] != 0x02 || bytes[8] != 0x15) {
            return;
        }

        TramaIBeacon tib = new TramaIBeacon(bytes);
        if (!UUID_NUESTRO_BEACON.equals(Utilidades.bytesToString(tib.getUUID()))) {
            return;
        }

        BluetoothDevice bluetoothDevice = resultado.getDevice();
        int rssi = resultado.getRssi();

        Log.d(ETIQUETA_LOG, " ****************************************************");
        Log.d(ETIQUETA_LOG, " ****** BEACON ENCONTRADO **************************** ");
        Log.d(ETIQUETA_LOG, " ****************************************************");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            Log.d(ETIQUETA_LOG, " nombre = " + bluetoothDevice.getName());
            Log.d(ETIQUETA_LOG, " direccion = " + bluetoothDevice.getAddress());
        }

        Log.d(ETIQUETA_LOG, " rssi = " + rssi);
        Log.d(ETIQUETA_LOG, " bytes (" + bytes.length + ") = " + Utilidades.bytesToHexString(bytes));
        Log.d(ETIQUETA_LOG, " uuid = " + Utilidades.bytesToString(tib.getUUID()));

        byte[] minor = tib.getMinor();
        int valorMinor = ((minor[0] & 0xFF) << 8) | (minor[1] & 0xFF);

        Log.d(ETIQUETA_LOG, " minor = " + Utilidades.bytesToHexString(minor) + " (" + valorMinor + ")");

        this.runOnUiThread(() -> this.textoDatoBeacon.setText("Dato recibido: " + valorMinor));
    } // ()

    private byte[] obtenerDatosAnuncio(ScanRecord scanRecord) {
        byte[] registro = scanRecord.getBytes();
        int desplazamiento = 0;

        while (desplazamiento < registro.length) {
            int longitud = registro[desplazamiento] & 0xFF;
            if (longitud == 0 || desplazamiento + longitud >= registro.length) {
                return null;
            }

            int tipo = registro[desplazamiento + 1] & 0xFF;
            int datosManufacturer = longitud - 1;
            if (tipo == 0xFF) {
                byte[] carga = new byte[datosManufacturer];
                System.arraycopy(registro, desplazamiento + 2, carga, 0, datosManufacturer);
                return Utilidades.crearPaqueteIBeacon(carga);
            }

            desplazamiento += longitud + 1;
        }

        return null;
    }

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void buscarNuestroDispositivoBTLE() {
        Log.d(ETIQUETA_LOG, " buscarNuestroDispositivoBTLE(): empieza");

        if (this.elEscanner == null) {
            this.textoDatoBeacon.setText("Bluetooth no está listo");
            return;
        }

        if (!puedeEscanear()) {
            this.textoDatoBeacon.setText("Faltan permisos de Bluetooth");
            return;
        }

        if (this.callbackDelEscaneo != null) {
            this.elEscanner.stopScan(this.callbackDelEscaneo);
        }

        this.textoDatoBeacon.setText("Buscando el beacon 1234...");

        this.callbackDelEscaneo = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult resultado) {
                super.onScanResult(callbackType, resultado);
                mostrarInformacionDispositivoBTLE(resultado);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (ScanResult resultado : results) {
                    mostrarInformacionDispositivoBTLE(resultado);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(ETIQUETA_LOG, " errorCode = " + errorCode);
                MainActivity.this.runOnUiThread(() ->
                        MainActivity.this.textoDatoBeacon.setText("No se pudo iniciar el escaneo")
                );
            }
        };

        ScanFilter filtro = new ScanFilter.Builder()
                .setManufacturerData(FABRICANTE_APPLE, PREFIJO_I_BEACON, MASCARA_I_BEACON)
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        ArrayList<ScanFilter> filtros = new ArrayList<>();
        filtros.add(filtro);

        this.elEscanner.startScan(filtros, settings, this.callbackDelEscaneo);
    } // ()

    // --------------------------------------------------------------
    // ----------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private void detenerBusquedaDispositivosBTLE() {

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): empieza");
        if (this.callbackDelEscaneo == null) {
            Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): termina");
            return;
        }

        if (this.elEscanner == null || !puedeEscanear()) {
            return;
        }

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): parando scan");
        this.elEscanner.stopScan(this.callbackDelEscaneo);
        this.callbackDelEscaneo = null;

        Log.d(ETIQUETA_LOG, " detenerBusquedaDispositivosBTLE(): termina");
    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    public void botonBuscarDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton buscar dispositivos BTLE Pulsado");
        this.buscarTodosLosDispositivosBTLE();
    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    public void botonBuscarNuestroDispositivoBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton nuestro dispositivo BTLE Pulsado");
        this.buscarNuestroDispositivoBTLE();
    } // ()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    public void botonDetenerBusquedaDispositivosBTLEPulsado(View v) {
        Log.d(ETIQUETA_LOG, " boton detener busqueda dispositivos BTLE Pulsado");
        this.detenerBusquedaDispositivosBTLE();
    } // ()

    private boolean faltanPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED);
    }

    private String[] permisosNecesarios() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        }

        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    private boolean puedeEscanear() {
        return !faltanPermisos();
    }

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    private void inicializarBlueTooth() {
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): empieza ");
        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos adaptador BT ");

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            this.textoDatoBeacon.setText("Este dispositivo no tiene Bluetooth");
            return;
        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): comprobamos adaptador BT ");

        if (faltanPermisos()) {
            requestPermissions(permisosNecesarios(), CODIGO_PETICION_PERMISOS);
            return;
        }

        if (!bta.isEnabled()) {
            this.textoDatoBeacon.setText("Activa Bluetooth para continuar");
            return;
        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): habilitado =  " + bta.isEnabled() );

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): estado =  " + bta.getState() );

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): obtenemos escaner btle ");

        this.elEscanner = bta.getBluetoothLeScanner();

        if ( this.elEscanner == null ) {
            this.textoDatoBeacon.setText("No se pudo iniciar Bluetooth LE");
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): no hemos obtenido escaner btle");
        } else {
            this.textoDatoBeacon.setText("Listo para buscar el beacon 1234");
        }

        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): voy a perdir permisos (si no los tuviera) !!!!");

        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): NO tengo permiso de bluetooth " );
        } else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): SI tengo permiso de bluetooth ");
        }

        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): NO tengo permiso de bluetooth admin" );
        } else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): SI tengo permiso de bluetooth admin");
        }

        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): NO tengo permiso de coarse location" );
        } else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): SI tengo permiso de coarse location");
        }

        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): NO tengo permiso de bluetooth scan" );
        } else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): SI tengo permiso de bluetooth scan ");
        }

        if ( ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ) {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): NO tengo permiso de bluetooth connect" );
        } else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): SI tengo permiso de bluetooth connect ");
        }




        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): pidiendo permisos " );

            /*
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                        //|| ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        )
        {


            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.BLUETOOTH_SCAN,
                            //Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    CODIGO_PETICION_PERMISOS);

             */


            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): permisos comprobados" );
            /*
        }
        else {
            Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): parece que YA tengo los permisos necesarios !!!!");

        }
             */








        Log.d(ETIQUETA_LOG, " inicializarBlueTooth(): TERMINA " );
    } // ()


    // --------------------------------------------------------------
    // --------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.textoDatoBeacon = findViewById(R.id.textoDatoBeacon);

        Log.d(ETIQUETA_LOG, " onCreate(): empieza ");

        inicializarBlueTooth();

        Log.d(ETIQUETA_LOG, " onCreate(): termina ");

    } // onCreate()

    // --------------------------------------------------------------
    // --------------------------------------------------------------
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != CODIGO_PETICION_PERMISOS) {
            return;
        }

        if (puedeEscanear()) {
            inicializarBlueTooth();
        } else {
            this.textoDatoBeacon.setText("Concede los permisos para buscar beacons");
        }
    } // ()



} // class