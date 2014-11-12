package net.comploud.code.bitfrag;

import java.io.InputStream;
import java.nio.ByteBuffer;

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

    @Override
    public Fragment parseFragment(InputStream stream) throws FragmentFormatException {
        return null;
    }
}
