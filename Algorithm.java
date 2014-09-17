package net.comploud.code.bitfrag;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Created by tek-ti on 2014-09-11.
 */
public abstract class Algorithm {
    public abstract Set<Fragment> frag(byte[] data);
    public abstract ByteBuffer deFrag(Cluster cluster);
}
