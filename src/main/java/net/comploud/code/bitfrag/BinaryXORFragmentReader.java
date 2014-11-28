package net.comploud.code.bitfrag;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Read a raw "binary" (non-encoded) XOR fragment.
 * Created by tek-ti on 2014-11-12.
 */
public class BinaryXORFragmentReader implements FragmentReader {

    // TODO Remember: Do not check digest here, this is done during reconstruction

    @Override
    public Fragment parseFragment(ByteBuffer buffer) throws FragmentFormatException {
        return null;
    }

    /**
     * Parse a possible fragment from an open input stream.
     * The input stream have to be initialized and readable.
     * This method will read an arbitrary amount of data from the stream (hence advancing its position).
     * @param src Open and readable input stream to read fragment from
     * @return A verified fragment
     * @throws java.io.IOException Upon a failed stream operation
     * @throws FragmentFormatException If fragment UUID (digest) mismatches with the payload data
     */
    @Override
    public XORFragment parseFragment(InputStream src) throws IOException, FragmentFormatException {
        DataInputStream din = new DataInputStream(src);

        // Read version header
        short hVersion = din.readShort();
        if(hVersion != XORAlgorithm.SUPPORTED_FRAGMENT_VERSION) {     // The only supported/expected version as of now
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
        int hDataSize = din.readInt();

        // Read fragment parameters
        byte hPart = din.readByte();

        // Read the payload data size
        int hPayloadSize = din.readInt();

        // Finally, read the payload data
        byte[] hPayload = new byte[hPayloadSize];   // TODO Protect against aggressive mallocs?
        int hPayloadRead = din.read(hPayload);
        if(hPayloadRead != hPayloadSize) {
            throw new FragmentFormatException("Insufficient payload data: " + hPayloadSize + " != " + hPayloadRead);
        }

        // But first, let's do a consistency check!
        UUID checkUuid = UUID.nameUUIDFromBytes(hPayload);
        if(!checkUuid.equals(hFUuid)) {
            // Consistency check failed!
            throw new FragmentFormatException("Fragment possibly corrupted (digest check failed)");
        }

        // Create fragment
        XORFragment frag = new XORFragment(hVersion, hCUuid, hFUuid, hDataSize, hPart, ByteBuffer.wrap(hPayload));
        return frag;


        /* Hey! There's room for some optimization here! The way a new cluster is created for each fragment can be
         * refactored to using some sort of FragmentParser that holds discovered clusters - in, say, a HashMap - and
         * sorts the fragments directly into their appropriate clusters.
         * This way, most of these (cluster) memory allocations could be replaced by a respective log(N) lookup.
         * But this would require a different design pattern. Maybe consider in some future?
         */
    }
}
