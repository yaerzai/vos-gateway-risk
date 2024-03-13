package com.ytl.vos.gateway.risk.utils;

import cn.hutool.crypto.symmetric.SymmetricAlgorithm;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * @author lingchuanyu
 * @date 2023/7/22-11:40
 */
public class EncryptionUtils {

    private static final String CHARSET_NAME = "utf-8";

    // 可配置的加密算法,当前为aes对称加密
    private static final String ALGORITHM = SymmetricAlgorithm.AES.getValue();

    public static String encrypt(String valueToEncrypt,String secretKey) throws Exception {
        Key key = generateKey(secretKey);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedByteValue = cipher.doFinal(valueToEncrypt.getBytes(CHARSET_NAME));
        return Base64.getEncoder().encodeToString(encryptedByteValue);
    }

    public static String decrypt(String encryptedValue,String secretKey) throws Exception {
        Key key = generateKey(secretKey);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedByteValue = Base64.getDecoder().decode(encryptedValue);
        byte[] decryptedValue = cipher.doFinal(decryptedByteValue);
        return new String(decryptedValue);
    }

    private static Key generateKey(String secretKey) throws Exception {
        byte[] keyValue = secretKey.getBytes(CHARSET_NAME);
        return new SecretKeySpec(keyValue, ALGORITHM);
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    // 使用私钥进行签名
    public static byte[] sign(PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        return signature.sign();
    }

    // 使用公钥进行验证
    public static boolean verify( byte[] signature, PublicKey publicKey) throws Exception {
        Signature verifySignature = Signature.getInstance("SHA256withRSA");
        verifySignature.initVerify(publicKey);
        return verifySignature.verify(signature);
    }
}
