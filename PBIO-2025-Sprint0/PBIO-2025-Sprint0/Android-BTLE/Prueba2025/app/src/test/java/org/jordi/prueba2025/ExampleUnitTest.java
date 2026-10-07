package org.jordi.prueba2025;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExampleUnitTest {
    @Test
    public void parsesMinor1234() {
        byte[] manufacturerData = {
                0x4C, 0x00, 0x02, 0x15,
                0x45, 0x50, 0x53, 0x47, 0x2D, 0x47, 0x54, 0x49,
                0x2D, 0x50, 0x52, 0x4F, 0x59, 0x2D, 0x33, 0x41,
                0x00, 0x00,
                0x04, (byte) 0xD2,
                (byte) 0xCB
        };

        byte[] advertisement = Utilidades.crearPaqueteIBeacon(manufacturerData);
        TramaIBeacon beacon = new TramaIBeacon(advertisement);

        assertEquals(30, advertisement.length);
        assertEquals("EPSG-GTI-PROY-3A", Utilidades.bytesToString(beacon.getUUID()));
        assertEquals(1234, Utilidades.bytesToInt(beacon.getMinor()));
    }
}