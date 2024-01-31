package qilin.android.axml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link AXmlHandler} provides functionality to parse a byte compressed android xml file and access all nodes.
 *
 * @author Palaniappan Muthuraman
 */
public class AXmlHandler {

    /**
     * Contains the byte compressed xml which was parsed by this {@link AXmlHandler}.
     */
    protected byte[] xml;

    /**
     * The parser used for actually reading out the binary XML file
     */
    protected final IBinaryXmlParser parser;

    /**
     * Returns a list containing all nodes of the xml document which have the given tag.
     *
     * @param	tag		the tag being search for
     * @return	list pointing on all nodes which have the given tag.
     */
    public List<AXmlNode> getNodesWithTag(String tag) {
        return parser.getNodesWithTag(tag);
    }

    public AXmlHandler(InputStream aXmlIs, IBinaryXmlParser parser) throws IOException {
        // wrap the InputStream within a BufferedInputStream
        // to have mark() and reset() methods
        BufferedInputStream buffer = new BufferedInputStream(aXmlIs);

        // read xml one time for writing the output later on
        {
            List<byte[]> chunks = new ArrayList<>();
            int bytesRead = 0;
            while (aXmlIs.available() > 0) {
                byte[] nextChunk = new byte[aXmlIs.available()];
                int chunkSize = buffer.read(nextChunk);
                if (chunkSize < 0)
                    break;
                chunks.add(nextChunk);
                bytesRead += chunkSize;
            }

            // Create the full array
            this.xml = new byte[bytesRead];
            int bytesCopied = 0;
            for (byte[] chunk : chunks) {
                int toCopy = Math.min(chunk.length, bytesRead - bytesCopied);
                System.arraycopy(chunk, 0, this.xml, bytesCopied, toCopy);
                bytesCopied += toCopy;
            }
        }

        parser.parseFile(this.xml);
        this.parser = parser;
    }

    /**
     * Returns the Android xml document.
     *
     * @return	the Android xml document
     */
    public AXmlDocument getDocument() {
        return parser.getDocument();
    }
}
