package driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.Main;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CreateDummyScene {

    private static final Logger logger = LoggerFactory.getLogger(CreateDummyScene.class);

    private static final String APP_PATH = "/Users/palaniappanmuthuraman/Documents/FlowDroid/DroidBench/apk/Callbacks/RegisterGlobal1.apk";

    private static final String ANDROID_JAR = "/Users/palaniappanmuthuraman/Library/Android/sdk/platforms";

    private static Scene scene;

    public static Scene getDummySootScene(){
        setUpSoot();
        return scene;
    }

    private static void setUpSoot(){
        // Clean up any old Soot instance we may have
        G.reset();
        scene = Scene.v();
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(true);
        Options.v().set_process_dir(Collections.singletonList(APP_PATH));
        Options.v().set_android_jars(ANDROID_JAR);
        Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
        Options.v().set_keep_offset(false);
        Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
        Options.v().set_ignore_resolution_errors(true);

        // Set the Soot configuration options. Note that this will needs to be
        // done before we compute the classpath.
        // explicitly include packages for shorter runtime:
        List<String> excludeList = new LinkedList<String>();
        excludeList.add("java.*");
        excludeList.add("javax.*");

        excludeList.add("sun.*");
        excludeList.add("android.*");
        excludeList.add("androidx.*");

        excludeList.add("org.apache.*");
        excludeList.add("org.eclipse.*");
        excludeList.add("soot.*");
        Options.v().set_exclude(excludeList);
        Options.v().set_no_bodies_for_excluded(true);

        Options.v().set_soot_classpath(getClasspath());
        Main.v().autoSetOptions();

        // Load whatever we need
        logger.info("Loading dex files...");
        scene.loadNecessaryClasses();
        // Make sure that we have valid Jimple bodies
        PackManager.v().getPack("wjpp").apply();
        logger.info("Soot Setup is successful and loaded {} classes", scene.getClasses().size());
    }

    private static String getClasspath() {
        String classpath =  scene.getAndroidJarPath(ANDROID_JAR, APP_PATH);
        logger.info("soot classpath: " + classpath);
        return classpath;
    }

}