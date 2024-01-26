package qilin.android.axml;

import java.util.HashMap;
import java.util.Map;

public class AXmlDocument {

    /**
     * The root node of the document
     */
    private AXmlNode rootNode;

    /**
     * The namespaces registered on this element.
     */
    Map<String, AXmlNamespace> namespaces = null;

    /**
     * Adds a namespace that is defined in this node.
     *
     * @param ns The namespace defined in this node.
     */
    public void addNamespace(AXmlNamespace ns) {
        // Do not add the default namespace
        if (ns.getUri() == null || ns.getUri().isEmpty())
            return;

        if (this.namespaces == null)
            this.namespaces = new HashMap<>();
        this.namespaces.put(ns.getPrefix(), ns);
    }

    /**
     * Sets the root node of this document
     *
     * @param rootNode The new root node of this document
     */
    public void setRootNode(AXmlNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Gets the root node of this Android XML document
     *
     * @return The root node of this Android XML document
     */
    public AXmlNode getRootNode() {
        return this.rootNode;
    }
}
