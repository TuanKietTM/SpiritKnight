package com.soulknight.database;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {
//    khong luu truc tiep mat khau
//    su dung hash de luu mat khau

    private static final String ALGORITHM =
            "PBKDF2WithHmacSHA256";

    private static final int ITERATIONS = 210_000;
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Mat khau khong duoc de trong."
            );
        }

        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        byte[] hash = generateHash(
                password.toCharArray(),
                salt,
                ITERATIONS,
                HASH_LENGTH
        );

        return ITERATIONS
                + ":"
                + Base64.getEncoder().encodeToString(salt)
                + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(
            String password,
            String storedPassword
    ) {
        if (password == null || storedPassword == null) {
            return false;
        }

        try {
            String[] parts = storedPassword.split(":");

            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt =
                    Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash =
                    Base64.getDecoder().decode(parts[2]);

            byte[] actualHash = generateHash(
                    password.toCharArray(),
                    salt,
                    iterations,
                    expectedHash.length
            );

            return MessageDigest.isEqual(
                    expectedHash,
                    actualHash
            );

        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static byte[] generateHash(
            char[] password,
            byte[] salt,
            int iterations,
            int hashLength
    ) {
        PBEKeySpec specification = new PBEKeySpec(
                password,
                salt,
                iterations,
                hashLength * 8
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance(ALGORITHM);

            return factory
                    .generateSecret(specification)
                    .getEncoded();

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Khong the hash mat khau.",
                    exception
            );
        } finally {
            specification.clearPassword();
        }
    }
}