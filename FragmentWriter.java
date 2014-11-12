package net.comploud.code.bitfrag;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Interface for a class that writes (exports) fragments.
 * Created by tek-ti on 2014-11-11.
 */
public interface FragmentWriter {
    public void writeFragment(ByteBuffer buffer) throws IOException;    // TODO Really?
    public void writeFragment(OutputStream stream) throws IOException;
}
