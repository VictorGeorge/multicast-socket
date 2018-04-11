package me.maeroso;

import javax.crypto.Cipher;
import java.security.PrivateKey;
import java.security.PublicKey;

public class RSASigning {
    public static byte[] sign(PrivateKey privateKey, byte[] message) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);

        return cipher.doFinal(message);
    }

    public static byte[] decrypt(PublicKey publicKey, byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);

        return cipher.doFinal(encrypted);
    }
}
