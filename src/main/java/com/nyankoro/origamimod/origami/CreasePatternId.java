package com.nyankoro.origamimod.origami;

import java.nio.charset.StandardCharsets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HexFormat;


/*
 * .cp展開図そのものを識別するID。
 *
 * 同じ内容のCPなら、
 * ファイル名が違っていても同じIDになる。
 */
public final class CreasePatternId {

    private static final String VERSION =
            "origamimod-crease-pattern-v1";


    private CreasePatternId() {
    }


    public static String calculate(
            byte[] cpData
    ) {

        if (cpData == null
                || cpData.length == 0) {

            throw new IllegalArgumentException(
                    "CP data must not be empty"
            );
        }


        final MessageDigest digest;


        try {

            digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 is not available",
                    e
            );
        }


        digest.update(
                VERSION.getBytes(
                        StandardCharsets.UTF_8
                )
        );


        updateInt(
                digest,
                cpData.length
        );


        digest.update(
                cpData
        );


        return HexFormat.of()
                .formatHex(
                        digest.digest()
                );
    }


    public static boolean isValidFormat(
            String creasePatternId
    ) {

        if (creasePatternId == null
                || creasePatternId.length() != 64) {

            return false;
        }


        for (int i = 0;
             i < creasePatternId.length();
             i++) {

            char c =
                    creasePatternId.charAt(
                            i
                    );


            boolean digit =
                    c >= '0'
                            && c <= '9';

            boolean lowerHex =
                    c >= 'a'
                            && c <= 'f';


            if (!digit
                    && !lowerHex) {

                return false;
            }
        }


        return true;
    }


    private static void updateInt(
            MessageDigest digest,
            int value
    ) {

        digest.update(
                (byte) (
                        value >>> 24
                )
        );

        digest.update(
                (byte) (
                        value >>> 16
                )
        );

        digest.update(
                (byte) (
                        value >>> 8
                )
        );

        digest.update(
                (byte) value
        );
    }
}