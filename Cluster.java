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
     * UUID (type 3) including a hash of the fully assembled data.
     * This hash can be used to verify the complete cluster data upon an attempt to reconstruct the clusters' data.
     */
    private final UUID uuid;    // This may also be a potential security issue as the any verification aid may help an exhaustive search.

    /**
     * Size of the data contained within this cluster (in bytes).
     */
    private final int dataSize;

    /**
     * Amount of fragments associated with this cluster.
     */
    private final int clusterSize;

    /**
     * The amount of fragments that are redundant to the original data.
     * This is the amount of fragments that can be lost/tampered without compromizing the cluster.
     */
    private final int clusterOverhead;

    /**
     * Fragment format version of this cluster.
     */
    private final short formatVersion;

    /**
     * Supported fragment format as of this version.
     * This works like a magic cookie to identify the fragment format. When the fragment format is changed, this number
     * will be re-generated during development.
     * So, just a little heads up for those of you who wishes to actually use BitFrag in its early versions:
     * This may change in future versions with no backwards compatibility. However, when BitFrag hits its first stable
     * release, there will be backwards compatibility in case of fragment format is changed.
     */
    public static final short SUPPORTED_FRAGMENT_VERSION = 0x6C96;    // Calculated by fair die roll


    /**
     * Create a new cluster from a set of original data.
     * This is the constructor to invoke when creating a cluster for the first time.
     * The fragmentation process will occur in this constructor.
     * This is when the data is split into fragments (including the parity fragment).
     * An cluster created by this constructor is ready to be used for distributing its fragments.
     */
    public Cluster(ByteBuffer data, int clusterSize, int clusterOverhead) {
        this.dataSize = data.array().length;
        this.clusterSize = clusterSize;
        this.clusterOverhead = clusterOverhead;
        this.formatVersion = SUPPORTED_FRAGMENT_VERSION;

        // Calculate the cluster UUID (by hashing the original data) and create it
        this.uuid = UUID.nameUUIDFromBytes(data.array());

        // Perform fragmentation
        // TODO Implement the Reed-Solomon algorithm. For now, just do the simple XOR algorithm here.
        byte[] dataRaw = data.array();                      int dri = 0;
        int fragSize = dataRaw.length / 2;
        int fragSizeSpare = dataRaw.length % 2;             // Is, clearly, either 0 or 1
        byte[] x1 = new byte[fragSize + fragSizeSpare];     int x1i = 0;
        byte[] x2 = new byte[fragSize];                     int x2i = 0;
        byte[] p = new byte[fragSize + fragSizeSpare];      int pi = 0;
        while(dri < dataRaw.length) {
            // This is every even byte
            x1[x1i++] = dataRaw[dri++];
            if(dri < dataRaw.length) {  // Yes, same check, I know...
                // This is every odd byte
                x2[x2i++] = dataRaw[dri++];
                p[pi++] = (byte)(x1[x1i-1] ^ x2[x2i-1]);  // XOR operation to calculate parity
            } else {
                // Special case: There is no x2 for this x1 byte. Pad it!
                p[pi++] = x1[x1i-1];    // Actually, XOR operation with 0x00 pad is not necessary as it evaluates to x1
                // The payload size parameter will make sure that the x2 pad is not accidentally "included" during reconstruction
            }
        }

        add(new Fragment(this, (byte)1, x1));   // TODO Use enum or something neat to represent the piece parameter?
        add(new Fragment(this, (byte)2, x2));
        add(new Fragment(this, (byte)3, p));
    }

    /**
     * Create a new empty cluster.
     * This constructor can be used to collect fragments to eventually reconstruct the original data.
     */
    public Cluster(UUID uuid, int dataSize, int clusterSize, int clusterOverhead, short formatVersion) {
        this.uuid = uuid;
        this.dataSize = dataSize;
        this.clusterSize = clusterSize;
        this.clusterOverhead = clusterOverhead;
        this.formatVersion = formatVersion;
    }


    /**
     * Use the current set of fragments in an attempt to reconstruct the original data.
     * @return Original data, or null if insufficient fragments are set
     */
    public byte[] reconstructData() {
        // TODO Implement the Reed-Solomon algorithm. For now, just do the simple XOR algorithm here.
        Iterator<Fragment> iter = iterator();
        byte[] dataRaw = new byte[dataSize];    int dri = 0;    // TODO Use Java NIO?
        byte[] x1 = null;                       int x1i = 0;
        byte[] x2 = null;                       int x2i = 0;
        byte[] p = null;                        int pi = 0;

        // Find/sort the different pieces (fragments)
        while(iter.hasNext()) {
            Fragment f = iter.next();
            switch(f.getPiece()) {      // Heh, it's not everyday you get a reason to use switch-case... Is this uncool?
                case 1:
                    x1 = f.getPayload();
                    break;
                case 2:
                    x2 = f.getPayload();
                    break;
                case 3:
                    p = f.getPayload();
                    break;
                default:
                    // This wrong (at the moment of XOR test implementation)
                    break;
            }
        }

        // Now, let's see what we have
        // TODO Add the case where all fragments are known. In this case, parity should be used as a verification to detect/correct malicious fragment modifications
        if(x1 != null && x2 != null) {
            // This is the easiest case: Just byte-by-byte concatenate x1 and x2
            while(dri < dataRaw.length) {
                if(dri % 2 == 0) {
                    // This is every even byte
                    if(x1i < x1.length) {
                        dataRaw[dri++] = x1[x1i++];
                    } else {
                        // This is an erroneous state (x1 is less than required)!
                        throw new Error("Erroneous state during reconstruction!");
                    }
                } else {
                    // This is every odd byte
                    if(x2i < x2.length) {
                        dataRaw[dri++] = x2[x2i++];
                    } else {
                        // There's an x1 but no x2. The previous byte was the last one. We're done here.
                        // Actually, this is an erroneous state (x2 is smaller than required)
                        throw new Error("Erroneous state during reconstruction!");
                    }
                }
            }
        } else if(x1 != null && p != null) {
            // In this case, we've got x1 and the parity
            while(dri < dataRaw.length) {
                if(x1i < x1.length) {
                    if(dri % 2 == 0) {
                        // Even: Time for a x1 byte (this is trivial)
                        dataRaw[dri++] = x1[x1i];   // Note that x1i is NOT incremented! The next byte (parity) will need this intact (see the while statement)
                    } else {
                        // Odd: Time for an x2 byte. This need to be calculated from parity.
                        if(pi < p.length) {
                            dataRaw[dri++] = (byte)(x1[x1i++] ^ p[pi++]);   // XOR operation to calculate x2 from parity
                        } else {
                            // This is an erroneous state (p is smaller than necessary)!
                            throw new Error("Erroneous state during reconstruction!");
                            // Special case: There's an x1 but no p. This is the last byte.
                            //dataRaw[dri++] = x1[x1i++] /* ^ 0x00*/;         // XOR with padding not necessary
                        }
                    }
                } else {
                    // This is an erroneous condition (x1 is smaller than necessary)!
                    throw new Error("Erroneous state during reconstruction!");
                }
            }
        } else if(x2 != null && p != null) {
            // This is _almost_ the same case as above, but with x2 instead if x1 (and a different special case)
            while(dri < dataRaw.length) {
                if(x2i < x2.length) {
                    if(dri % 2 == 0) {
                        // Even: Time for an x1 byte. This needs to be calculated from parity.
                        if(pi < p.length) {
                            dataRaw[dri++] = (byte)(x2[x2i] ^ p[pi++]); // XOR operation to calculate x1 from parity
                                                                        // Note that x2i is NOT incremented!
                        } else {
                            // This is an erroneous condition (p is smaller than necessary)!
                            throw new Error("Erroneous state during reconstruction!");
                            // Special case: There's an x2 but no p left. Use padding.
                            //dataRaw[dri++] = x2[x2i++]/* ^ 0x00*/;  // XOR with padding not necessary
                        }
                    } else {
                        // Odd: This is an x2 byte
                        dataRaw[dri++] = x2[x2i++];
                    }
                } else {
                    // Special case: There is an x1 byte (missing) and no x2 byte. Use padding with the next (last) parity byte.
                    if(pi < p.length) {
                        dataRaw[dri++] = p[pi++]/* ^ 0x00*/;    // XOR with padding not necessary
                    } else {
                        // This is an erroneous state (p is smaller than necessary)!
                        throw new Error("Erroneous state during reconstruciton!");
                    }
                }
            }
        } else {
            // There are insufficient fragments to reconstruct the data!
            return null;        // TODO Throw exception?
        }

        // Calculate (and verify) the supplied hash with the defragged data
        UUID verification = UUID.nameUUIDFromBytes(dataRaw);
        if(!verification.equals(getUuid())) {
            // Verification failed
            return null;    // TODO Throw exception!
        } else {
            // All cases have been covered. Success!
            return dataRaw;
        }
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
            return getClusterSize() == otherCluster.getClusterSize() &&           // Is this big chunk of lines good-looking code?
                    getOverhead() == otherCluster.getOverhead() &&
                    getFormatVersion() == otherCluster.getFormatVersion() &&
                    getUuid().equals(otherCluster.getUuid());
        }   // Wow, this method looks like hell, haha!
    }

    @Override
    public int hashCode() {
        // Quite sloppy way really. Certainly not uniform in practice but should work for now.
        // TODO Do a proper hash (may boost performance in large data sets)
        return (clusterSize != 0 ? clusterSize : 0) *
                (clusterOverhead != 0 ? clusterOverhead : 0) *
                (formatVersion != 0 ? formatVersion : 0) *
                (int)getUuid().getMostSignificantBits() *
                (int)getUuid().getLeastSignificantBits();
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
     * Get the size of the data contained within this cluster (in bytes).
     * @return Data size
     */
    public int getDataSize() { return dataSize; }

    /**
     * Get the specified cluster size.
     * @return Cluster size
     */
    public int getClusterSize() { return clusterSize; }

    /**
     * Get the specified cluster overhead.
     * @return Cluster overhead
     */
    public int getOverhead() { return clusterOverhead; }

}
