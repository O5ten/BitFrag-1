package net.comploud.code.bitfrag;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum to map algorithm names to their fragment format header values.
 * Created by tek-ti on 2014-09-11.
 * @deprecated Wrote this enum as part of another idea. Don't use it. Might still come in handy someday...
 */
@Deprecated
public enum AlgorithmSpec {
    XOR ((short)1),
    HAMMING ((short)2),         // http://en.wikipedia.org/wiki/Hamming_code
    REED_SOLOMON ((short)3),    // http://en.wikipedia.org/wiki/Reed%E2%80%93Solomon_error_correction
    LDPC ((short)4),            // http://en.wikipedia.org/wiki/Low-density_parity-check_code
    TURBO_CODE ((short)5);      // http://en.wikipedia.org/wiki/Turbo_code

    /**
     * Header value for a specific algorithm.
     * This value is what's stored in a fragment.
     */
    private final short header;

    private AlgorithmSpec(short header) {
        this.header = header;
    }

    // Some cred for the "enum reverse lookup" trick: http://howtodoinjava.com/2012/12/07/guide-for-understanding-enum-in-java/

    /**
     * Reverse lookup table for header values.
     */
    private static final Map<Short, AlgorithmSpec> lookup = new HashMap<Short, AlgorithmSpec>();

    // Initialize reverse lookup table
    static {
        for(AlgorithmSpec spec : EnumSet.allOf(AlgorithmSpec.class)) {
            lookup.put(spec.headerValue(), spec);
        }
    }

    /**
     * Do a "reverse lookup" of header value (a short int) to an enum item.
     */
    public static AlgorithmSpec lookup(short header) {
        return lookup.get(header);
    }

    /**
     * Get the header value representing this algorithm.
     * @return Header value
     */
    public short headerValue() { return header; }

    // TODO Figure out a fantastic way of overriding getter for a sub-class of net.comploud.code.bitfrag.Algorithm and all that jacked-up OO-magic
    //public abstract Class<net.comploud.code.bitfrag.Algorithm> getAlgorithm();
}
