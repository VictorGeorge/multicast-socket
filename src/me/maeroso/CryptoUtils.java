package me.maeroso;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

class CryptoUtils {
    void generateAES() throws NoSuchAlgorithmException {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128);
        SecretKey skey = kgen.generateKey();

        byte[] iv = new byte[128/8];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);
    }

    void generateRSA() throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        try (FileOutputStream out = new FileOutputStream(PeerManager.getInstance().getOurPeer().getPort() + ".key")) {
            out.write(kp.getPrivate().getEncoded());
        }

        try (FileOutputStream out = new FileOutputStream(PeerManager.getInstance().getOurPeer().getPort() + ".pub")) {
            out.write(kp.getPublic().getEncoded());
        }
    }

    void recoverPublic() {
//        FIXME
        /*byte[] bytes = Files.readAllBytes(Paths.get(pubKeyFile));
        X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pub = kf.generatePublic(ks);*/
    }

    void recoverPrivate() {
//        FIXME
        /*byte[] bytes = Files.readAllBytes(Paths.get(pvtKeyFile));
        PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey pvt = kf.generatePrivate(ks);*/
    }


    static byte[] encrypt(PublicKey publicKey, byte[] toEncrypt) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        return cipher.doFinal(toEncrypt);
    }

    static byte[] decrypt(PrivateKey privateKey, byte[] toDecrypt) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        return cipher.doFinal(toDecrypt);
    }
}
