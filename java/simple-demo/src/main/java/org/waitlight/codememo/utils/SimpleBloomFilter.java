package org.waitlight.codememo.utils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

public class SimpleBloomFilter {

    private final int bitSetSize;
    private BitSet bitset;
    private final int hashFunctionCount;
    private final MessageDigest[] digestFunctions;

    public SimpleBloomFilter(int bitSetSize, int hashFunctionCount) throws NoSuchAlgorithmException {
        this.bitSetSize = bitSetSize;
        this.bitset = new BitSet(bitSetSize);
        this.hashFunctionCount = hashFunctionCount;
        this.digestFunctions = new MessageDigest[hashFunctionCount];
        for (int i = 0; i < hashFunctionCount; i++) {
            digestFunctions[i] = MessageDigest.getInstance("MD5");
        }
    }

    public void add(String value) {
        for (int i = 0; i < hashFunctionCount; i++) {
            int hashValue = getHash(value, i);
            bitset.set(Math.abs(hashValue) % bitSetSize, true);
        }
    }

    public boolean contains(String value) {
        for (int i = 0; i < hashFunctionCount; i++) {
            int hashValue = getHash(value, i);
            if (!bitset.get(Math.abs(hashValue) % bitSetSize)) {
                return false;
            }
        }
        return true;
    }

    private int getHash(String value, int index) {
        MessageDigest md = digestFunctions[index];
        md.update(value.getBytes());
        byte[] hashBytes = md.digest();
        ByteBuffer byteBuffer = ByteBuffer.wrap(hashBytes);
        return byteBuffer.getInt();
    }
}