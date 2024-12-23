package com.sorted.rest.services.payment.utils;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class HashUtils {

    public String getSha512encryptedString(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        String hashtext = no.toString(16);
        while (hashtext.length() < 128) hashtext = "0" + hashtext;
        return hashtext;
    }
}
