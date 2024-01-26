package qilin.android.manifest;

import qilin.android.axml.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseProcessManifest implements IManifestHandler {

    /**
     * Handler for android xml files
     */
    protected AXmlHandler axml;

    protected AXmlNode manifest;

    protected AXmlNode application;

    /**
     * Gets the application's package name
     *
     * @return The package name of the application
     */
    private String cache_PackageName = null;

    protected List<AXmlNode> activities = null;


    public BaseProcessManifest(File apkFile) throws IOException {
        if (!apkFile.exists())
            throw new RuntimeException(
                    String.format("The given APK file %s does not exist", apkFile.getCanonicalPath()));
        try (ApkHandler apk = new ApkHandler(apkFile)) {
            try (InputStream is = apk.getInputStream("AndroidManifest.xml")) {
                if (is == null)
                    throw new FileNotFoundException(String.format("The file %s does not contain an Android Manifest",
                            apkFile.getAbsolutePath()));
                this.handle(is);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handle(InputStream inputStream) throws IOException {
        this.axml = new AXmlHandler(inputStream, new AXmlParser());
        // get manifest node
        AXmlDocument document = this.axml.getDocument();
        this.manifest = document.getRootNode();
        if (!this.manifest.getTag().equals("manifest"))
            throw new RuntimeException("Root node is not a manifest node");
        List<AXmlNode> applications = this.manifest.getChildrenWithTag("application");
        if (applications.isEmpty())
            throw new RuntimeException("Manifest contains no application node");
        else if (applications.size() > 1)
            throw new RuntimeException("Manifest contains more than one application node");
        this.application = applications.get(0);
        this.activities = this.axml.getNodesWithTag("activity");
    }

    @Override
    public void close() {

    }

    public String getPackageName() {
        if (cache_PackageName == null) {
            AXmlAttribute<?> attr = this.manifest.getAttribute("package");
            if (attr != null && attr.getValue() != null)
                cache_PackageName = (String) attr.getValue();
        }
        return cache_PackageName;
    }

    public String expandClassName(String className) {
        String packageName = getPackageName();
        if (className.startsWith("."))
            return packageName + className;
        else if (!className.contains("."))
            return packageName + "." + className;
        else
            return className;
    }

    @Override
    public List<String> getActivities() {
        return this.activities.stream().map(activity -> activity.getAttribute("name").getValue().toString()).collect(Collectors.toList());
    }
}
