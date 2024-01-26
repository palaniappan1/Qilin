package qilin.android.axml;

import pxb.android.axml.AxmlReader;
import pxb.android.axml.AxmlVisitor;
import pxb.android.axml.NodeVisitor;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootResolver;
import soot.tagkit.IntegerConstantValueTag;
import soot.tagkit.Tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AXmlParser extends AbstractBinaryXMLFileParser{

    private final Map<Integer, String> idToNameMap = new HashMap<>();
    private static final Map<Integer, String> ANDROID_CONSTANTS = new HashMap<>();

    public class XmlVisitor extends AxmlVisitor {

        public final AXmlNode node;

        @Override
        public void attr(String ns, String name, int resourceId, int type, Object obj) {
            if (this.node == null) {
                throw new RuntimeException("NULL nodes cannot have attributes");
            }

            String tname = name;

            // If we have no node name, we use the resourceId to look up the
            // attribute in the android.R.attr class.
            if (tname == null || tname.isEmpty())
                tname = idToNameMap.get(resourceId);

            if (tname == null) {
                try {
                    SootClass rClass = Scene.v().forceResolve("android.R$attr", SootClass.BODIES);
                    if (rClass != null && !rClass.isPhantom()) {
                        outer: for (SootField sf : rClass.getFields())
                            for (Tag t : sf.getTags())
                                if (t instanceof IntegerConstantValueTag) {
                                    IntegerConstantValueTag cvt = (IntegerConstantValueTag) t;
                                    if (cvt.getIntValue() == resourceId) {
                                        tname = sf.getName();
                                        idToNameMap.put(resourceId, tname);
                                        // fake the Android namespace
                                        ns = "http://schemas.android.com/apk/res/android";
                                        break outer;
                                    }
                                    break;
                                }
                    }
                } catch (SootResolver.SootClassNotFoundException ex) {
                    // We try the next option
                }
            }

            // If we have nothing better, we use the resource ID
            if (tname == null && resourceId > 0)
                tname = String.valueOf(resourceId);

            if (tname == null) {
                // Without a tag name, we cannot continue
                return;
            }

            // Avoid whitespaces
            tname = tname.trim();

            if (type == AXmlConstants.TYPE_STRING) {
                if (obj instanceof String)
                    this.node.addAttribute(new AXmlAttribute<String>(tname, resourceId, type, (String) obj));
            }

            super.attr(ns, name, resourceId, type, obj);

        }

        public XmlVisitor() {
            this.node = new AXmlNode("dummy", "", null,true, "null");
        }

        public XmlVisitor(AXmlNode node) {
            this.node = node;
        }

        @Override
        public void ns(String prefix, String uri, int line) {
            document.addNamespace(new AXmlNamespace(prefix, uri, line));
        }

        @Override
        public NodeVisitor child(String ns, String name) {
            AXmlNode childNode = new AXmlNode(name == null ? null : name.trim(), ns == null ? null : ns.trim(), node);
            if (name != null)
                addPointer(name, childNode);
            return new XmlVisitor(childNode);
        }

        @Override
        public void text(int lineNumber, String value) {
            node.setText(value);
            super.text(lineNumber, value);
        }

        @Override
        public void end() {
            document.setRootNode(node);
        }
    }

    @Override
    public void parseFile(byte[] buffer) throws IOException {
        AxmlReader axmlReader = new AxmlReader(buffer);
        axmlReader.accept(new XmlVisitor());
    }

    private class AXmlConstants {

        public static final int TYPE_STRING = 0x03;
    }
}
