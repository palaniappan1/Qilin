package qilin.android.axml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractBinaryXMLFileParser implements IBinaryXmlParser{
    /**
     * Map containing lists of nodes sharing the same <code>tag</code>.
     * The <code>tag</code> is the key to access the list.
     */
    protected HashMap<String, ArrayList<AXmlNode>> nodesWithTag = new HashMap<>();

    /**
     * The xml document.
     */
    protected AXmlDocument document = new AXmlDocument();

    /**
     * Adds a pointer to the given <code>node</code> with the key <code>tag</code>.
     *
     * @param	tag		the node's tag
     * @param	node	the node being pointed to
     */
    protected void addPointer(String tag, AXmlNode node) {
        if(!this.nodesWithTag.containsKey(tag)) this.nodesWithTag.put(tag, new ArrayList<AXmlNode>());
        this.nodesWithTag.get(tag).add(node);
    }

    @Override
    public AXmlDocument getDocument() {
        return this.document;
    }

    @Override
    public List<AXmlNode> getNodesWithTag(String tag) {
        if(this.nodesWithTag.containsKey(tag))
            return new ArrayList<>(this.nodesWithTag.get(tag));
        else
            return Collections.emptyList();
    }
}
