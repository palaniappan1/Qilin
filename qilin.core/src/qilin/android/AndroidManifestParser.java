package qilin.android;

import qilin.android.manifest.IManifestHandler;
import qilin.android.manifest.ProcessManifest;
import qilin.core.PTAScene;
import soot.SootClass;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AndroidManifestParser {

    protected IManifestHandler manifest = null;

    protected Set<SootClass> entrypoints = new HashSet<>();

    public Set<SootClass> parseAppAndResourcesAndReturnEntryPoints(String APP_PATH) throws IOException {
        parseAppResources(APP_PATH);
        return entrypoints;
    }


    public void parseAppResources(String APP_PATH) throws IOException {
        manifest = createManifestParser(new File(APP_PATH));
        Set<String> entryPoints = manifest.getEntryPointClasses();
        entrypoints = new HashSet<>(entryPoints.size());
//        for (String className : entryPoints) {
//            SootClass sc = PTAScene.v().createSootClassUnsafe(className);
//            if (sc != null)
//               entrypoints.add(sc);
//        }
    }

    private IManifestHandler createManifestParser(File file) throws IOException {
        return new ProcessManifest(file);
    }
}
