/* Qilin - a Java Pointer Analysis Framework
 * Copyright (C) 2021-2030 Qilin developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3.0 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <https://www.gnu.org/licenses/lgpl-3.0.en.html>.
 */

package driver;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qilin.CoreConfig;
import qilin.android.AndroidManifestParser;
import qilin.core.PTA;
import qilin.core.PTAScene;
import qilin.core.builder.FakeAndroidMainFactory;
import qilin.pta.PTAConfig;
import qilin.util.MemoryWatcher;
import qilin.util.PTAUtils;
import qilin.util.Stopwatch;
import soot.PackManager;
import soot.Scene;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static PTA run(String[] args) {
        PTA pta;
        new PTAOption().parseCommandLine(args);
        setupSoot();
        if (PTAConfig.v().getOutConfig().dumpJimple) {
            String jimplePath = PTAConfig.v().getAppConfig().APP_PATH.replace(".jar", "");
            PTAUtils.dumpJimple(jimplePath);
            System.out.println("Jimple files have been dumped to: " + jimplePath);
        }
//        logger.info("Constructing the callgraph...");
//        PackManager.v().getPack("cg").apply();
//        CallGraph cg = PTAScene.v().getCallGraph();
//        System.out.println("#CALLGRAPH:" + cg.size());
        pta = PTAFactory.createPTA(PTAConfig.v().getPtaConfig().ptaPattern);
        pta.run(PTAConfig.v().getAppConfig().sootScene);
        System.out.println("Number of call graph edges " + pta.getCgb().calledges.size());
        return pta;
    }

    public static PTA getPTA(Scene scene, String[] args){
        /* Example arguments for this method
        java driver.Main  -pae -pe -clinit=ONFLY -lcs -mh -pta=insens -apppath /Users/palaniappanmuthuraman/Desktop/Qilin_Example
        -mainclass Example  -jre=artifact/benchmarks/JREs/jre1.6.0_45 -se -dumppts
         */
        new PTAOption().parseCommandLine(args);
        if(scene != null) {
            PTAScene.v(scene);
        }
        else{
            setupSoot();
        }
        PTA pta;
        pta = PTAFactory.createPTA(PTAConfig.v().getPtaConfig().ptaPattern);
        pta.run(PTAConfig.v().getAppConfig().sootScene);
        return pta;
    }

    public static void mainRun(String[] args) {
        Stopwatch ptaTimer = Stopwatch.newAndStart("Main PTA (including pre-analysis)");
        long pid = ProcessHandle.current().pid();
        MemoryWatcher memoryWatcher = new MemoryWatcher(pid, "Main PTA");
        memoryWatcher.start();
        run(args);
        ptaTimer.stop();
        System.out.println(ptaTimer);
        memoryWatcher.stop();
        System.out.println(memoryWatcher);
    }

    public static void setupSoot() {
        setSootOptions(PTAConfig.v());
        setSootClassPath(PTAConfig.v());
        PTAScene.v().addBasicClasses();
        PTAScene.v().loadNecessaryClasses();
    }

    public static void setupSoot(Scene scene){
        setSootOptions(PTAConfig.v());
        setSootClassPath(PTAConfig.v());
        PTAScene.v(scene);
    }

    /**
     * Set command line options for soot.
     */
    private static void setSootOptions(PTAConfig config) {
        List<String> dirs = new ArrayList<>();
        PTAConfig.ApplicationConfiguration appConfig = config.getAppConfig();
        dirs.add(appConfig.APP_PATH);
        Options.v().set_process_dir(dirs);

        if(appConfig.APP_PATH.endsWith(".apk")){
            Options.v().set_no_bodies_for_excluded(true);
            Options.v().set_whole_program(true);
            Options.v().set_src_prec(Options.src_prec_apk);
            Options.v().set_process_multiple_dex(true);
            Options.v().set_keep_offset(false);
            Options.v().set_android_jars("/Users/palaniappanmuthuraman/Documents/android-platforms");
            Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
            Options.v().set_ignore_resolution_errors(true);
        }
        else{
            Options.v().set_src_prec(Options.src_prec_only_class);
        }

        if (appConfig.MAIN_CLASS == null) {
            appConfig.MAIN_CLASS = PTAUtils.findMainFromMetaInfo(appConfig.APP_PATH);
        }

        if (appConfig.MAIN_CLASS != null) {
            Options.v().set_main_class(appConfig.MAIN_CLASS);
        }

        if (appConfig.INCLUDE_ALL) {
            Options.v().set_include_all(true);
        }

        if (appConfig.INCLUDE != null) {
            Options.v().set_include(appConfig.INCLUDE);
        }

        if(appConfig.WHOLE_PROGRAM_ANALYSIS){
            Options.v().set_whole_program(true);
            Options.v().set_no_bodies_for_excluded(true);
            Options.v().setPhaseOption("jb", "use-original-names:true");
            Options.v().set_prepend_classpath(false);
            Options.v().setPhaseOption("cg", "all-reachable:true");
        }

        if (appConfig.EXCLUDE != null) {
            Options.v().set_no_bodies_for_excluded(true);
            Options.v().set_exclude(appConfig.EXCLUDE);
        }

        // configure callgraph construction algorithm
//        configureCallgraphAlg(config.callgraphAlg);

        Options.v().setPhaseOption("jb", "use-original-names:true");
        Options.v().setPhaseOption("jb", "model-lambdametafactory:false");
        Options.v().set_output_format(Options.output_format_jimple);

        if (appConfig.REFLECTION_LOG != null) {
            Options.v().setPhaseOption("cg", "reflection-log:" + appConfig.REFLECTION_LOG);
        }

        Options.v().set_keep_line_number(true);
        Options.v().set_full_resolver(true);
        Options.v().set_ignore_resolving_levels(true);

        // Options.v().set_src_prec(Options.src_prec_class);
        Options.v().set_allow_phantom_refs(true);

    }

    /**
     * Set the soot class path to point to the default class path appended with the
     * app path (the classes dir or the application jar) and jar files in the
     * library dir of the application.
     */
    private static void setSootClassPath(PTAConfig config) {
        List<String> cps = new ArrayList<>();
        PTAConfig.ApplicationConfiguration appConfig = config.getAppConfig();
        // note that the order is important!
        cps.add(appConfig.APP_PATH);
        if(!appConfig.APP_PATH.endsWith(".apk")) {
            cps.addAll(getLibJars(appConfig.LIB_PATH));
            cps.addAll(getJreJars(appConfig.JRE));
        }
        // TODO : Once it works fine, need to change the logic
        cps.add("/Users/palaniappanmuthuraman/Library/Android/sdk/platforms");
        final String classpath = String.join(File.pathSeparator, cps);
        logger.info("Setting Soot ClassPath: {}", classpath);
        System.setProperty("soot.class.path", classpath);
    }

    private static Collection<String> getJreJars(String JRE) {
        if (JRE == null) {
            return Collections.emptySet();
        }
        final String jreLibDir = JRE + File.separator + "lib";
        return FileUtils.listFiles(new File(jreLibDir), new String[]{"jar"}, false).stream().map(File::toString)
                .collect(Collectors.toList());
    }

    /**
     * Returns a collection of files, one for each of the jar files in the app's lib
     * folder
     */
    private static Collection<String> getLibJars(String LIB_PATH) {
        if (LIB_PATH == null) {
            return Collections.emptySet();
        }
        File libFile = new File(LIB_PATH);
        if (libFile.exists()) {
            if (libFile.isDirectory()) {
                return FileUtils.listFiles(libFile, new String[]{"jar"}, true).stream().map(File::toString)
                        .collect(Collectors.toList());
            } else if (libFile.isFile()) {
                if (libFile.getName().endsWith(".jar")) {
                    return Collections.singletonList(LIB_PATH);
                }
                logger.error("Project not configured properly. Application library path {} is not a jar file.",
                        libFile);
                System.exit(1);
            }
        }
        logger.error("Project not configured properly. Application library path {} is not correct.", libFile);
        System.exit(1);
        return null;
    }

    // callgraph relevant APIs
    // refer to https://github.com/secure-software-engineering/FlowDroid/blob/develop/soot-infoflow-android/src/soot/jimple/infoflow/android/SetupApplication.java
    private static void configureCallgraphAlg(PTAConfig.CallgraphAlgorithm cgAlg) {
        switch (cgAlg) {
            case CHA -> {
                Options.v().setPhaseOption("cg.cha", "on");
                break;
            }
            case VTA -> {
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "vta:true");
                break;
            }
            case RTA -> {
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "rta:true");
                Options.v().setPhaseOption("cg.spark", "on-fly-cg:false");
                break;
            }
            case GEOM -> {
                Options.v().setPhaseOption("cg.spark", "on");
                Options.v().setPhaseOption("cg.spark", "geom-pta:true");
                // Those are default options, not sure whether removing them works.
                Options.v().setPhaseOption("cg.spark", "geom-encoding:Geom");
                Options.v().setPhaseOption("cg.spark", "geom-worklist:PQ");
                break;
            }
            case SPARK -> {
                Options.v().setPhaseOption("cg.spark", "on");
                break;
            }
            case QILIN -> {
            }
            default -> {
                break;
            }
        }
    }

    public static CallGraph runCallgraphAlg(String[] args) {
        CallGraph cg;
        new PTAOption().parseCommandLine(args);
        setupSoot();
        if (PTAConfig.v().getOutConfig().dumpJimple) {
            String jimplePath = PTAConfig.v().getAppConfig().APP_PATH.replace(".jar", "");
            PTAUtils.dumpJimple(jimplePath);
            System.out.println("Jimple files have been dumped to: " + jimplePath);
        }
        logger.info("Constructing the callgraph using " + PTAConfig.v().callgraphAlg + "...");
        if (PTAConfig.v().callgraphAlg == PTAConfig.CallgraphAlgorithm.QILIN) {
            PTA pta = PTAFactory.createPTA(PTAConfig.v().getPtaConfig().ptaPattern);
//            pta.run(PTAConfig.v().getAppConfig().sootScene);
        } else {
            PackManager.v().getPack("cg").apply();
        }
        cg = PTAScene.v().getCallGraph();
        System.out.println("#CALLGRAPH:" + cg.size());
        return cg;
    }

    public static void mainCG(Scene scene, String[] args) {
        new PTAOption().parseCommandLine(args);
        if(scene != null) {
            PTAScene.v(scene);
        }
        else{
            setupSoot();
        }
        Stopwatch cgTimer = Stopwatch.newAndStart("Main CG");
        long pid = ProcessHandle.current().pid();
        MemoryWatcher memoryWatcher = new MemoryWatcher(pid, "Main CG");
        memoryWatcher.start();
        logger.info("Constructing the callgraph using " + PTAConfig.v().callgraphAlg + "...");
//        runCallgraphAlg(args);
        PackManager.v().getPack("cg").apply();
        System.out.println("#CALLGRAPH:" + PTAScene.v().getCallGraph().size());
        cgTimer.stop();
        System.out.println(cgTimer);
        memoryWatcher.stop();
        System.out.println(memoryWatcher);
    }

    public static void main(String[] args) {
        mainRun(args);
//        mainDummyRun();
//        System.out.println(new FakeAndroidMainFactory().getFakeMain().getActiveBody());
//         mainCG(args);
    }

    private static void mainDummyRun() {
        getPTA(CreateDummyScene.getDummySootScene(), generateArgs().split("\n"));
    }

    private static String generateArgs() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("-sootSceneProvided\n");
//        stringBuilder.append("-pae\n");
//        stringBuilder.append("-pe\n");
        stringBuilder.append("-clinit=ONFLY\n");
//        stringBuilder.append("-lcs\n");
//        stringBuilder.append("-mh\n");
        // This is the place where we should, add the proper pta algorithm we need
        stringBuilder.append("-pta=insens" + "\n");
//        stringBuilder.append("-se\n");
//        stringBuilder.append("-cg\n");
        return stringBuilder.toString();
    }
}
