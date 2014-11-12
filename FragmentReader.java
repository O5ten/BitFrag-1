package net.comploud.code.bitfrag;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Interface for a class that reads (parses) fragments.
 * Created by tek-ti on 2014-11-11.
 */
public interface FragmentReader {
    public Fragment parseFragment(ByteBuffer buffer) throws FragmentFormatException;
    public Fragment parseFragment(InputStream stream) throws FragmentFormatException;
}
