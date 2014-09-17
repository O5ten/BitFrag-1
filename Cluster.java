package net.comploud.code.bitfrag;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

/**
 * A cluster is a complete set of fragments for a block of "original" data.
 * This class is used to represent a set - but not necessarily the complete set - of fragments.
 * A set of fragments may or may not be complete, meaning it may or may not be sufficient to represent the original data.
 * Created by tek-ti on 2014-09-09.
 */
public class Cluster extends HashSet<Fragment> {
    /**
     * Cluster UUID (type 3).
     * This includes an MD5 hash of the original data.
     */
    private final UUID uuid;

    /**
     * Fragmentation algorithm in use.
     * Each fragment may have different algorithms specified. This is erroneous and thus this spec is always used.
     */
    private final AlgorithmSpec algorithm;


    /**
     * Create a complete set of fragments from a block of data.
     * @param data Data block to fragment
     */
    public Cluster(ByteBuffer data, AlgorithmSpec algorithm) {      // Throw UnsupportedOperationException and/or ReadOnlyBufferException? (from ByteBuffer)
        // Calculate the cluster UUID (by hashing the original data) and create it
        this.uuid = UUID.nameUUIDFromBytes(data.array());
        this.algorithm = algorithm;

        // This is where the magic happens...
        // TODO Insert OO-magic for invoking algorithm specific sub-classes etc
        // For now, just do the simple XOR algorithm here
        int fragSize = data.array().length / 2;
        byte[] x1 = new byte[fragSize];     int x1i = 0;
        byte[] x2 = new byte[fragSize];     int x2i = 0;
        byte[] p = new byte[fragSize];      int pi = 0;
        byte[] dataRaw = data.array();
        // Warning: This code is risky business! Is it safe? Is it correct on different systems?
        for(int i = 0; i < dataRaw.length; i++) {
            if(i % 2 == 0) {
                // This is even
                x1[x1i++] = dataRaw[i];
            } else {
                // This is odd
                x2[x2i++] = dataRaw[i];
                p[pi++] = (byte)(x1[x1i-1] ^ x2[x2i-1]);  // XOR operation to calculate parity
            }
        }

        add(new Fragment((short)1, AlgorithmSpec.XOR, uuid, x1));
        add(new Fragment((short)1, AlgorithmSpec.XOR, uuid, x2));
        add(new Fragment((short)1, AlgorithmSpec.XOR, uuid, p));
    }


    /**
     * Create a new empty cluster.
     * This constructor can be used to collect fragments to eventually reconstruct the original data.
     */
    public Cluster(UUID uuid, AlgorithmSpec algorithm) {
        this.uuid = uuid;
        this.algorithm = algorithm;
    }


    /**
     * Use the current set of fragments in an attempt to reconstruct the original data.
     * @return Original data, or null if insufficient fragments in set
     */
    public byte[] reconstructData() {
        // TODO Insert algorithm sub-class 00-magic here
        // For now, just do the simple XOR algorithm here
        // Warning: This code is risky business! Is it safe? Is it correct on different systems?
        /*Iterator<Fragment> iter = iterator();
        byte[] x1 = iter.next().getPayload();            int x1i = 0;
        byte[] x2 = iter.next().getPayload();            int x2i = 0;
        if(x1.length != x2.length) {
            return null;    // Failure. Throw Exception instead?
        } else {
            byte[] data = new byte[x1.length * 2];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) (x1[x1i] ^ x2[x2i]);  // XOR operation to calculate parity
            }

            return null;
        }*/
        // TODO DAMMIT! The parity fragment needs to be marked in the format in order to cover the algorithm...
    }
}
