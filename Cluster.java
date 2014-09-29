package net.comploud.code.bitfrag;

import java.nio.ByteBuffer;
import java.util.HashSet;
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
     * Cluster size.
     * This is how many fragments the specified cluster contains.
     */
    private final int clusterSize;

    /**
     * Cluster overhead.
     * This is how many fragments that are redundant to the original data.
     * This is the amount of fragments that can be lost/tampered without compromizing the cluster.
     */
    private final int clusterOverhead;

    /**
     * Fragment format version.
     */
    private final short formatVersion;


    /**
     * Create a new cluster from a set of original data.
     * This is the constructor to use when creating a cluster for the first time.
     */
    public Cluster(byte[] data, int clusterSize, int clusterOverhead, short formatVersion) {
        this.clusterSize = clusterSize;
        this.clusterOverhead = clusterOverhead;
        this.formatVersion = formatVersion;

        // TODO Split data into fragments
        // Don't forget to initialize everything (like UUID)
        this.uuid = null;   // TODO This is DEFINITELY NOT correct, just a stub!
    }

    /**
     * Create a new empty cluster.
     * This constructor can be used to collect fragments to eventually reconstruct the original data.
     */
    public Cluster(UUID uuid, int clusterSize, int clusterOverhead, short formatVersion) {
        this.uuid = uuid;
        this.clusterSize = clusterSize;
        this.clusterOverhead = clusterOverhead;
        this.formatVersion = formatVersion;
    }


    /**
     * Create a complete set of fragments from a block of data.
     * @param data Data block to fragment
     */
/*    public Cluster(ByteBuffer data) {      // Throw UnsupportedOperationException and/or ReadOnlyBufferException? (from ByteBuffer)
        // Calculate the cluster UUID (by hashing the original data) and create it
        this.uuid = UUID.nameUUIDFromBytes(data.array());

        // This is where the magic happens...
        // TODO Implement the Reed-Solomon algorithm
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
                x2[x2i++] = dataRaw[i];hFUuid
                p[pi++] = (byte)(x1[x1i-1] ^ x2[x2i-1]);  // XOR operation to calculate parity
            }
        }

        add(new Fragment((short)0, uuid, x1));
        add(new Fragment((short)0, uuid, x2));
        add(new Fragment((short)0, uuid, p));
    }*/

    /**
     * Use the current set of fragments in an attempt to reconstruct the original data.
     * @return Original data, or null if insufficient fragments in set
     */
    public byte[] reconstructData() {
        // TODO Implement the Reed-Solomon algorithm
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
        return null;    // Stub
    }

    /**
     * Matches this cluster to another.
     * For two clusters to be considered the same, all cluster parameters and the UUID must match.
     * @param other Another cluster instance to compare to
     * @return true if all essential properties of the instances match (and hence are the same cluster), false otherwise
     */
    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof Cluster)) {  // Yep, instanceof. What can you do about it!? :/
            return false;
        } else {
            Cluster otherCluster = (Cluster)other;
            return getSize() == otherCluster.getSize() &&           // Is this big chunk of lines good-looking code?
                    getOverhead() == otherCluster.getOverhead() &&
                    getFormatVersion() == otherCluster.getFormatVersion() &&
                    getUuid().equals(otherCluster.getUuid());      // TODO Verify that this resolves as intended
        }   // Wow, this method looks like hell, haha!
    }

    /**
     * Get the fragment format version.
     * @return Fragment format version
     */
    public int getFormatVersion() { return formatVersion; }

    /**
     * Get the type 3 UUID for the complete cluster data.
     * @return Cluster UUID
     */
    public UUID getUuid() { return uuid; }

    /**
     * Get the specified cluster size.
     * @return Cluster size
     */
    public int getSize() { return clusterSize; }

    /**
     * Get the specified cluster overhead.
     * @return Cluster overhead
     */
    public int getOverhead() { return clusterOverhead; }

}
