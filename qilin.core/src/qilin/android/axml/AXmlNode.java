package qilin.android.axml;

import java.util.*;

public class AXmlNode{
    /**
     * The node's tag.
     */
    protected String tag;

    /**
     * List containing all children.
     */
    ArrayList<AXmlNode> children = null;

    /**
     * The textual contents of the node
     */
    protected String text;

    /**
     * Map containing all attributes. The key matches the attribute's name.
     */
    Map<String, AXmlAttribute<?>> attributes = null;

    /**
     * The parent node.
     */
    protected AXmlNode parent;

    /**
     * Returns the tag of this node.
     *
     * @return the node's tag
     */
    public String getTag() {
        return tag;
    }


    public AXmlNode(String tag, String ns, AXmlNode parent) {
        this(tag, ns, parent,true, null);
    }

    public AXmlNode(String tag, String ns, AXmlNode parent, boolean added, String text){
        this.tag = tag;
        this.parent = parent;
        if (parent != null)
            parent.addChild(this);
        this.text = text;
    }

    /**
     * List containing all children of this node which have the given
     * <code>tag</code>.
     *
     * @param tag the children's tag
     * @return list with all children with <code>tag</code>
     */
    public List<AXmlNode> getChildrenWithTag(String tag) {
        if (this.children == null)
            return Collections.emptyList();

        ArrayList<AXmlNode> children = new ArrayList<AXmlNode>();
        for (AXmlNode child : this.children) {
            if (child.getTag().equals(tag))
                children.add(child);
        }

        return children;
    }

    public void addAttribute(AXmlAttribute<?> attr) {
        if (attr == null)
            throw new NullPointerException("AXmlAttribute is null");

        if (this.attributes == null)
            this.attributes = new HashMap<>();
        this.attributes.put(attr.getName(), attr);
    }

    /**
     * Returns the attribute with the given <code>name</code>.
     *
     * @param name the attribute's name.
     * @return attribute with <code>name</code>.
     */
    public AXmlAttribute<?> getAttribute(String name) {
        if (this.attributes == null)
            return null;
        return this.attributes.get(name);
    }

    /**
     * Adds the given node as child.
     *
     * @param child a new child for this node
     */
    public void addChild(AXmlNode child) {
        if (this.children == null)
            this.children = new ArrayList<AXmlNode>();
        this.children.add(child);
    }

    public void setText(String text) {
        this.text = text;
    }

}
