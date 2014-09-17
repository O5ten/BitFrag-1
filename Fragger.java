package net.comploud.code.bitfrag;

import java.io.*;
import java.util.UUID;

/**
 * Chops a stream or block of bytes into fragments.
 * Created by tek-ti on 2014-09-08.
 */
public class Fragger {
    public static void main(String argv[]) {
        // Here be spagetti code for now

        if(argv.length < 2) {
            System.err.println("Usage: Fragger <source file> <output file prefix>");
            System.exit(1);
        } else {
            try {
                File infile = new File(argv[0]);
                DataInputStream din = new DataInputStream(new FileInputStream(infile));
                byte[] data = new byte[(int)infile.length()];  // Hehe, lol :P
                din.read(data);     // Old-school (and possible suboptimal?)
                Cluster cluster = Cluster.createCluster(data);

                for(Fragment frag : cluster) {
                    System.out.println(frag.getClusterUuid() + " / " + frag.getFragUuid());
                }
            } catch(IOException e) {
                System.err.println("IO Error: " + e.getMessage());
                System.exit(2);
            }
        }
    }
}
