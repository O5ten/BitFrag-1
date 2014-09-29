package net.comploud.code.bitfrag;

import java.io.*;
import java.util.UUID;

/**
 * Encoded data fragment.
 * Created by tek-ti on 2014-09-08.
 */
public class Fragment /*implements Serializable*/ {
    /**
     * Fragment format version.
     */
    protected short version;

    /**
     * UUID/Hash of the fully assembled data (aka the cluster UUID).
     * This hash can be used to verify the complete cluster data upon an attempt to defragment the cluster.
     * This may also be a potential security issue as the hash may help in a brute force scenario.
     */
    protected UUID clusterUuid;

    /**
     * Universal Unique IDentifier (type 3) for this particular fragment.
     * This is a type 3 (or version 3) UUID meaning it contains an MD5 hash apart from UUID version tag.
     * The MD5 hash (or "name") in the UUID is a hash of the fragment payload data.
     */
    protected UUID fragUuid;

    /**
     * Cluster size.
     * This is how many fragments the specified cluster contains.
     */
    protected int clusterSize;

    /**
     * Cluster overhead.
     * This is how many fragments that are redundant to the original data.
     * This is the amount of fragments that can be lost/tampered without compromizing the cluster.
     */
    protected int clusterOverhead;

    /**
     * The actual fragment data.
     */
    protected byte[] payload;


    /**
     * Constructor to be used internally when importing/parsing a fragment.
     */
    private Fragment(short version, UUID frag, UUID cluster, int clusterSize, int clusterOverhead, byte[] payload) {
        this.version = version;
        this.fragUuid = frag;
        this.clusterUuid = cluster;
        this.clusterSize = clusterSize;
        this.clusterOverhead = clusterOverhead;
        this.payload = payload;
    }

    /**
     * Constructor to be used when creating a fragment for the first time (during the fragmentation process).
     * @param version Fragment format version
     * @param cluster Cluster UUID (type 3) containing a hash of the complete cluster data
     * @param payload Payload data for this particular fragment
     */
    Fragment(short version, UUID cluster, byte[] payload) {
        this.version = version;
        this.clusterUuid = cluster;
        this.payload = payload;
        recalculateFragUUID();
    }

    /**
     * Parse a possible fragment from an open input stream.
     * The input stream have to initialized and readable.
     * This method will read an arbitrary amount of data from the stream (hence advancing its position).
     * @param src Open and readable input stream to read fragment from
     * @return A verified fragment
     * @throws IOException Upon a failed stream operation
     * @throws FragmentFormatException If fragment UUID (hash sum) mismatches with the payload data. Note that this is a subclass to IOException and should hence be caught before IOException!
     */
    static Fragment parseFragment(InputStream src) throws IOException, FragmentFormatException {  // TODO Use Serialization API instead?
        DataInputStream din = new DataInputStream(src);

        // Read version header
        short hVersion = din.readShort();
        if(hVersion != 1) {     // The only supported/expected version as of now
            throw new FragmentFormatException("Unsupported version header: " + hVersion);
        }

        // Read cluster UUID
        long hCUuidMBits = din.readLong();
        long hCUuidLBits = din.readLong();
        UUID hCUuid = new UUID(hCUuidMBits, hCUuidLBits);
        // Keep this for later

        // Read fragment UUID
        long hFUuidMBits = din.readLong();
        long hFUuidLBits = din.readLong();
        UUID hFUuid = new UUID(hFUuidMBits, hFUuidLBits);
        // Keep this for later

        // Read cluster parameters
        int hClusterSize = din.readInt();
        int hClusterOverhead = din.readInt();

        // Read the payload data size
        int hPayloadSize = din.readInt();

        // Finally, read the payload data
        byte[] hPayload = new byte[hPayloadSize];   // TODO Consider fancy Java API? (ByteBuffer / BufferedReader)
                                                    // TODO Protect against aggressive mallocs?

        // But first, let's do a consistency check!
        UUID checkUuid = UUID.nameUUIDFromBytes(hPayload);
        if(!checkUuid.equals(hFUuid)) {
            // Consistency check failed!
            throw new FragmentFormatException("Fragment possibly currupted (hash check failed)");
        }

        return new Fragment(hVersion, hFUuid, hCUuid, hClusterSize, hClusterOverhead, hPayload);
    }

    /**
     * Write this fragment to a stream.
     * The stream must be initialized and open.
     * @param dest Destination stream
     * @throws IOException Upon a failed stream operation
     */
    public void export(OutputStream dest) throws IOException {  // TODO Use Serialization API instead?
        DataOutputStream dout = new DataOutputStream(dest);

        // Write version and algorithm header
        dout.writeShort(getVersion());

        // Write cluster UUID
        dout.writeLong(getClusterUuid().getMostSignificantBits());
        dout.writeLong(getClusterUuid().getLeastSignificantBits());

        // Write frag UUID
        dout.writeLong(getFragUuid().getMostSignificantBits());
        dout.writeLong(getFragUuid().getLeastSignificantBits());

        // Write parameters
        dout.writeInt(getClusterSize());
        dout.writeInt(getClusterOverhead());

        // Write payload size
        dout.writeInt(getPayload().length);

        // Write payload data
        dout.write(getPayload());
    }


    /**
     * Recalculates the fragment UUID.
     * This method is used internally to update the fragment UUID hash based on the (new) payload data.
     * Note that the payload data must be updated _before_ invoking this method.
     */
    protected void recalculateFragUUID() {
        fragUuid = UUID.nameUUIDFromBytes(getPayload());
        //System.gc();
    }

    /**
     * Get the fragment format version.
     * @return Fragment format version
     */
    public int getVersion() { return version; }

    /**
     * Get the type 3 UUID for this particular fragment.
     * @return Fragment UUID
     */
    public UUID getFragUuid() { return fragUuid; }

    /**
     * Get the type 3 UUID for the complete cluster data.
     * @return Cluster UUID
     */
    public UUID getClusterUuid() { return clusterUuid; }

    /**
     * Get the specified cluster size.
     * @return Cluster size
     */
    public int getClusterSize() { return clusterSize; }

    /**
     * Get the specified cluster overhead.
     * @return Cluster overhead
     */
    public int getClusterOverhead() { return clusterOverhead; }

    /**
     * Get the payload data of this fragment.
     * @return Payload data
     */
    public byte[] getPayload() { return payload; }
}
