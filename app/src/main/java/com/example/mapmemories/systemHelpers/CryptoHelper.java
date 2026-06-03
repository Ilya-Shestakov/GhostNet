package com.example.mapmemories.systemHelpers;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.google.firebase.auth.FirebaseAuth;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoHelper {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";
    private static final String AES_MODE = "AES/GCM/NoPadding";

    private static String getAlias(String uid) {
        if (uid == null || uid.isEmpty()) {
            uid = FirebaseAuth.getInstance().getUid();
        }
        return "GhostNet_Key_" + (uid != null ? uid : "default");
    }

    public static String generateKeyPair(String uid) throws Exception {
        String alias = getAlias(uid);
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE);
        kpg.initialize(new KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .build());
        kpg.generateKeyPair();

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);
        PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
        return Base64.encodeToString(publicKey.getEncoded(), Base64.NO_WRAP);
    }

    public static String getLocalPublicKey(String uid) {
        try {
            String alias = getAlias(uid);
            KeyStore ks = KeyStore.getInstance(KEYSTORE);
            ks.load(null);
            java.security.cert.Certificate cert = ks.getCertificate(alias);
            if (cert == null) return null;
            return Base64.encodeToString(cert.getPublicKey().getEncoded(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    public static String encryptForRecipient(String message, String recipientPublicKeyBase64) {
        try {
            if (message == null || recipientPublicKeyBase64 == null || recipientPublicKeyBase64.isEmpty()) return message;

            byte[] keyBytes = Base64.decode(recipientPublicKeyBase64, Base64.NO_WRAP);
            PublicKey recipientKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes));

            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey aesKey = keyGen.generateKey();

            Cipher aesCipher = Cipher.getInstance(AES_MODE);
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            byte[] encryptedMessage = aesCipher.doFinal(message.getBytes("UTF-8"));

            Cipher rsaCipher = Cipher.getInstance(RSA_MODE);
            rsaCipher.init(Cipher.ENCRYPT_MODE, recipientKey);
            byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

            return "ENC_V3:" +
                    Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(encryptedMessage, Base64.NO_WRAP);
        } catch (Exception e) {
            return message;
        }
    }

    public static String decrypt(String encryptedPackage) {
        try {
            if (encryptedPackage == null || !encryptedPackage.startsWith("ENC_V3:")) return encryptedPackage;

            String[] parts = encryptedPackage.substring(7).split(":");
            byte[] encryptedAesKey = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] iv = Base64.decode(parts[1], Base64.NO_WRAP);
            byte[] encryptedMessage = Base64.decode(parts[2], Base64.NO_WRAP);

            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
            keyStore.load(null);

            String alias = getAlias(null);

            if (!keyStore.containsAlias(alias)) {
                return "[Ошибка: Ключ не найден для этого аккаунта]";
            }

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, null);

            Cipher rsaCipher = Cipher.getInstance(RSA_MODE);
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");

            Cipher aesCipher = Cipher.getInstance(AES_MODE);
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            return new String(aesCipher.doFinal(encryptedMessage), "UTF-8");
        } catch (Exception e) {
            return "[Ошибка дешифровки]";
        }
    }
}