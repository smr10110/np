package cl.ufo.dcl.cryptapp.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AESCryptTest {

    String textoSinCifrar;
    String llave;
    String textoCifrado;
    AESCrypt cfr;


    @BeforeEach
    void setUp() {
       textoSinCifrar = "mi texto sin cifrar";
       textoCifrado = "vEYs5GMsbyQ34BtYrXwe0Jz26JdWDPKW0s0tvIOcxOU=";
       llave = "123456789asdfghj";
    }

    @Test
    @DisplayName("Prueba de cifrado")
    void testEncrypt(){

        //given
        cfr = new AESCrypt(textoSinCifrar, llave);
        String expected = textoCifrado;

        //when
        String actual = this.cfr.encrypt();

        //then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Prueba de Descifrado")
    void testDecrypt(){
        //given
        cfr = new AESCrypt(textoCifrado, llave);
        String expected = textoSinCifrar;

        //when
        String actual = this.cfr.decrypt();

        //then
        assertEquals(expected, actual);
    }

}