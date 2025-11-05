/**
 * Se debe considerar que la llave pública debe ser generada de manera aleatoria
 */
package cl.ufo.dcl.cryptapp.utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author dci
 */
public class AESCrypt {

    private final SecretKey secretKey;
    private Cipher cipher = null;
    private final String plainText;

    /**
     * COnstructor para acceder a objeto de cifrado
     * @param texto
     * @param llave
     */
    public AESCrypt(String texto, String llave) {

        Logger.getLogger(AESCrypt.class.getName()).log(Level.INFO, "Texto plano: " + texto);
        Logger.getLogger(AESCrypt.class.getName()).log(Level.INFO, "Llave: " + llave);

        this.secretKey = new SecretKeySpec(llave.getBytes(), 0, 16, "AES");
        this.plainText = texto;
        try {
            this.cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Permite generar una llave cifrada a partir de la clave privada del usuario y la llave publica
     * @return
     */
    public String encrypt() {
        byte[] encryptedByte = null;
        byte[] plainTextByte = this.plainText.getBytes();
        try {
            this.cipher.init(Cipher.ENCRYPT_MODE, this.secretKey);
            try {
                encryptedByte = this.cipher.doFinal(plainTextByte);
            } catch (IllegalBlockSizeException | BadPaddingException ex) {
                Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (InvalidKeyException ex) {
            Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, ex);
        }

        Base64.Encoder encoder = Base64.getEncoder();
        String encryptedText = encoder.encodeToString(encryptedByte);
        return encryptedText;
    }

    /**
     * Mediante las llaves publica y llave generada se descifra y genera llave privada
     * @return
     */
    public String decrypt() {
        byte[] decryptedByte = null;
        try {

            Base64.Decoder decoder = Base64.getDecoder();
            byte[] encryptedTextByte = decoder.decode(this.plainText);
            try {
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            } catch (InvalidKeyException ex) {
                Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, ex);
            }
            decryptedByte = cipher.doFinal(encryptedTextByte);

        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(AESCrypt.class.getName()).log(Level.SEVERE, null, ex);
        }
        String decryptedText = new String(decryptedByte);
        return decryptedText;
    }
}
