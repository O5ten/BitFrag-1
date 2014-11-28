package net.comploud.code.bitfrag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Write a raw "binary" (non-encoded) XOR fragment.
 * Created by tek-ti on 2014-11-12.
 */
public class BinaryXORFragmentWriter implements FragmentWriter {
    private XORFragment frag;

    public BinaryXORFragmentWriter(XORFragment frag) {
        this.frag = frag;
    }

    @Override
    public void writeFragment(ByteBuffer buffer) throws IOException {

    }

    /**
     * Write this fragment to a stream.
     * The stream must be initialized and open.
     * @param stream Destination stream
     * @throws IOException Upon a failed stream operation
     */
    @Override
    public void writeFragment(OutputStream stream) throws IOException {
        DataOutputStream dout = new DataOutputStream(stream);

        // Write version and algorithm header
        dout.writeShort(frag.getVersion());

        // Write cluster UUID
        UUID clusterUuid = frag.getClusterId();
        dout.writeLong(clusterUuid.getMostSignificantBits());
        dout.writeLong(clusterUuid.getLeastSignificantBits());

        // Write frag UUID
        dout.writeLong(frag.getId().getMostSignificantBits());
        dout.writeLong(frag.getId().getLeastSignificantBits());

        // Write cluster parameters
        dout.writeInt(frag.getClusterDataSize());

        // Write fragment parameters
        dout.writeByte(frag.getPiece());

        // Write payload size
        dout.writeInt(frag.getPayload().array().length);

        // Write payload data
        dout.write(frag.getPayload().array());
    }
}
