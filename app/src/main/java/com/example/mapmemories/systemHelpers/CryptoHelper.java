package com.example.mapmemories.systemHelpers;

import android.util.Base64;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoHelper {

    // Тот самый ключ (32 байта). В будущем можешь убрать его в C++ библиотеку (NDK) для защиты от реверса.
    private static final byte[] KEY = "SuperSecretKey32BytesLongMapMem!".getBytes();

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final String PREFIX = "ENC:"; // Метка шифрованного сообщения

    // ШИФРУЕМ
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec secretKeySpec = new SecretKeySpec(KEY, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

            byte[] encryptedData = new byte[IV_LENGTH + cipherText.length];
            System.arraycopy(iv, 0, encryptedData, 0, IV_LENGTH);
            System.arraycopy(cipherText, 0, encryptedData, IV_LENGTH, cipherText.length);

            // Добавляем префикс, чтобы потом узнать наше шифрование
            return PREFIX + Base64.encodeToString(encryptedData, Base64.NO_WRAP);

        } catch (Exception e) {
            e.printStackTrace();
            return plainText;
        }
    }

    // РАСШИФРОВЫВАЕМ
    public static String decrypt(String text) {
        // Если текста нет, или он старый (не зашифрованный), или системный (📷 Фото) - отдаем как есть
        if (text == null || !text.startsWith(PREFIX)) return text;

        try {
            // Отрезаем префикс "ENC:"
            String base64Data = text.substring(PREFIX.length());
            byte[] encryptedData = Base64.decode(base64Data, Base64.NO_WRAP);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, IV_LENGTH);

            int cipherTextLength = encryptedData.length - IV_LENGTH;
            byte[] cipherText = new byte[cipherTextLength];
            System.arraycopy(encryptedData, IV_LENGTH, cipherText, 0, cipherTextLength);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec secretKeySpec = new SecretKeySpec(KEY, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
            return "🔒 Ошибка дешифровки";
        }
    }
}