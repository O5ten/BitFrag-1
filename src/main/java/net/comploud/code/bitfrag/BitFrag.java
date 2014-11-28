package net.comploud.code.bitfrag;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

/**
 * Execution class for net.comploud.code.bitfrag.BitFrag v0.1.
 * Super simple and trivial as of this early version.
 * Created by tek-ti on 2014-09-08.
 */
public class BitFrag {
    private static final String banner = "net.comploud.code.bitfrag.BitFrag v0.1.1";

    public static void main(String argv[]) {
        // Here be spagetti code for now
        if(argv.length < 1) {
            System.out.println(banner);
            System.out.println("Usage: net.comploud.code.bitfrag.BitFrag [-d] <file(s)>");
            System.out.println();
            System.out.println("\t-d\tDefrag (reconstruct) files");
            System.exit(1);
        } else if(argv.length > 2 || argv[0].equals("-d")) {
            // Do reconstruction of input files
            BinaryXORFragmentReader reader = new BinaryXORFragmentReader();
            System.out.println(banner + " - Reconstruction mode");

            // Open and read all input files
            HashSet<Fragment> frags = new HashSet<Fragment>();
            for(int i = 1; i < argv.length; i++) {
                try {
                    File infile = new File(argv[i]);
                    System.out.print("Reading fragment file: " + infile + ": ");
                    System.out.flush();
                    Fragment frag = reader.parseFragment(new FileInputStream(infile));
                    frags.add(frag);
                    System.out.println("OK");
                } catch(IOException e) {
                    System.out.println("Failed (IO): " + e.getMessage());
                } catch(FragmentFormatException e) {
                    System.out.println("Failed: " + e.getMessage());
                }
            }

            // Now try to match these fragments to each other in order to build a cluster (if possible)
            FragmentMapper mapper = new FragmentMapper();
            for(Fragment frag : frags) {
                mapper.map(frag);
            }

            // Attempt to reconstruct all discovered clusters (if possible)
            for(Cluster clust : mapper.getKnownClusters()) {
                System.out.print("Reconstructing data for cluster " + clust.getId() + ": ");
                System.out.flush();
                int dataSize = 1024;    // TODO This is NOT ok!
                ByteBuffer data = ByteBuffer.allocate(dataSize);    // TODO This is uncomfortable. Didn't see that one coming...
                XORAlgorithm algo = new XORAlgorithm(clust);
                try {
                    ReconstructionReport report = algo.reconstruct(data);

                    // TODO The output file is the whole buffer... Fix!

                    // Success
                    if(report.flawless()) {
                        System.out.println("Flawless success!");
                    } else {
                        System.out.println("Restorative success!");
                        System.out.println("Report: [missing=" + report.getMissing() + ", corrupted=" + report.getCorrupted() + ", tampered=" + report.getTampered() + "]");
                    }

                    File outfile = new File(clust.getId() + ".cluster");  // Keep it this simple for now
                    System.out.print("Writing data to file: " + outfile + ": ");
                    System.out.flush();
                    FileOutputStream outstream = new FileOutputStream(outfile);
                    outstream.getChannel().write(data);
                    System.out.println("OK");
                } catch(InsufficientFragmentsException e) {
                    System.out.println("Insufficient fragments: " + e.getMessage());
                } catch(ReconstructionException e) {
                    System.out.println("Reconstruction failed: " + e.getMessage());
                } catch(IOException e) {
                    System.out.println("Failed (IO): " + e.getMessage());
                }
            }
        } else {
            // Do fragmentation of input file
            System.out.println(banner + " - Fragmentation mode");
            try {
                // Open and read the input file
                FileChannel infile = FileChannel.open(new File(argv[0]).toPath(), StandardOpenOption.READ);
                ByteBuffer data = ByteBuffer.allocate((int)infile.size());    // Hmm.. Ugly.
                infile.read(data);
                XORAlgorithm algo = new XORAlgorithm(null); // TODO This is a bad design pattern
                Cluster<XORFragment> cluster = algo.fragment(data);

                System.out.println("Created cluster " + cluster.getId());
                System.out.println("Writing fragments to files:");
                for(XORFragment frag : cluster) {
                    // Write this fragment to the file system (limited to current directory for now)
                    File outfile = new File(frag.getId() + ".frag");
                    System.out.print(outfile + ": ");
                    System.out.flush();
                    FileOutputStream outsream = new FileOutputStream(outfile);
                    BinaryXORFragmentWriter fragWriter = new BinaryXORFragmentWriter(frag);
                    fragWriter.writeFragment(outsream);
                    System.out.println("OK");
                }
            } catch(IOException e) {
                System.out.println("Failed (IO): " + e.getMessage());
                System.exit(2);
            }
        }
    }
}
