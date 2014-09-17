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
     * Cluster algorithm used.
     * This determines quite a lot about the properties of the cluster.
     * However, the fragment format is unaffected by this.
     */
    protected AlgorithmSpec algorithm;

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
     * The actual fragment data.
     */
    protected byte[] payload;


    /**
     * Constructor to be used internally when importing/parsing a fragment.
     */
    private Fragment(short version, AlgorithmSpec algorithm, UUID frag, UUID cluster, byte[] payload) {
        this.version = version;
        this.algorithm = algorithm;
        this.fragUuid = frag;
        this.clusterUuid = cluster;
        this.payload = payload;
    }

    /**
     * Constructor to be used when creating a fragment for the first time (during the fragmentation process).
     * @param version Fragment format version
     * @param cluster Cluster UUID (type 3) containing a hash of the complete cluster data
     * @param payload Payload data for this particular fragment
     */
    Fragment(short version, AlgorithmSpec algorithm, UUID cluster, byte[] payload) {
        this.version = version;
        this.algorithm = algorithm;
        this.clusterUuid = cluster;
        this.payload = payload;
        recalculateFragUUID();
    }

    /**
     * Parse a possible fragment from an open input stream.
     * The input stream have to initialized and readable.
     * This method will read an arbitrary amount of data from the stream (hence advancing its position). If the
     * @param in Open and readable input stream to read fragment from
     * @return A verified fragment
     */
    static Fragment parseFragment(InputStream in) throws IOException {  // TODO Use Serialization API instead?
        DataInputStream din = new DataInputStream(in);

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

        // Read algorithm header
        AlgorithmSpec hAlgorithm = AlgorithmSpec.lookup(din.readShort());
        if(hAlgorithm != AlgorithmSpec.XOR) {     // The only supported/expected algorithm as of now
            throw new FragmentFormatException("Unsupported algorithm: " + hAlgorithm);
        } // Should be an else statement here for clean code but whatever...

        // Read fragment UUID
        long hFUuidMBits = din.readLong();
        long hFUuidLBits = din.readLong();
        UUID hFUuid = new UUID(hFUuidMBits, hFUuidLBits);
        // Keep this for later

        // Read algorithm specific flags
        // TODO How to protocol this? Just use a fixed all-around field or allow recursive container format style separating cluster and fragment fields by algorithm specific parameters etc..?

        // Read the payload data size
        int hPayloadSize = din.readInt();

        // Finally, read the payload data
        byte[] hPayload = new byte[hPayloadSize];   // TODO Consider fancy Java API? (ByteBuffer / BufferedReader)

        // But first, let's do a consistency check!
        UUID checkUuid = UUID.nameUUIDFromBytes(hPayload);
        if(!checkUuid.equals(hFUuid)) {
            // Consistency check failed!
            throw new FragmentFormatException("Fragment possibly currupted (hash check failed)");
        }

        return new Fragment(hVersion, hAlgorithm, hFUuid, hCUuid, hPayload);
    }

    public void export(OutputStream dest) throws IOException {  // TODO Use Serialization API instead?
        DataOutputStream dout = new DataOutputStream(dest);

        // Write version and algorithm header
        dout.writeShort(version);

        // Write cluster UUID
        dout.writeLong(clusterUuid.getMostSignificantBits());
        dout.writeLong(clusterUuid.getLeastSignificantBits());

        // Write algorithm parameters

        // Write frag UUID
        dout.writeLong(fragUuid.getMostSignificantBits());
        dout.writeLong(fragUuid.getLeastSignificantBits());

        // Write payload size
        dout.writeInt(payload.length);

        // Write payload data
        dout.write(payload);
    }

    /**
     * Recalculates the fragment UUID.
     * This method is used internally to update the fragment UUID hash based on the (new) payload data.
     * Note that the payload data must be updated _before_ invoking this method.
     */
    protected void recalculateFragUUID() {
        fragUuid = UUID.nameUUIDFromBytes(payload);
        //System.gc();
    }

    /**
     * Get the fragment format version.
     * @return Fragment format version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Get the type 3 UUID for this particular fragment.
     * @return Fragment UUID
     */
    public UUID getFragUuid() {
        return fragUuid;
    }

    /**
     * Get the type 3 UUID for the complete cluster data.
     * @return Cluster UUID
     */
    public UUID getClusterUuid() {
        return clusterUuid;
    }

    /**
     * Get the payload data of this fragment.
     * @return Payload data
     */
    public byte[] getPayload() {
        return payload;
    }
}
