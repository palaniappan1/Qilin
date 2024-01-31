package qilin.core.builder;

import qilin.core.ArtificialMethod;
import qilin.util.PTAUtils;
import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;

import java.util.Collections;

public class FakeAndroidMainFactory extends ArtificialMethod {
    private SootClass fakeClass;

    /**
     * Default name of the class containing the dummy main method
     */
    private String dummyClassName = "FakeAndroidMain";
    /**
     * Default name of the dummy main method
     */
    private String dummyMethodName = "fakeAndroidMainMethod";

    private SootMethod mainMethod = null;


    public FakeAndroidMainFactory(){
    }

    public SootMethod getFakeMain() {
        if (body == null) {
            synchronized (this) {
                if (body == null) {
                    makeFakeMain();
                }
            }
        }
        return mainMethod;
    }

    private void makeFakeMain() {
        int implicitCallEdges = 0;
        this.localStart = 0;
        fakeClass = getOrCreateDummyMainClass();
        Body body;
        mainMethod = fakeClass.getMethodByNameUnsafe(dummyMethodName);
        Type stringArrayType = ArrayType.v(RefType.v("java.lang.String"), 1);
        mainMethod = Scene.v().makeSootMethod(dummyMethodName, Collections.singletonList(stringArrayType), VoidType.v());
        body = Jimple.v().newBody();
        body.setMethod(mainMethod);
        mainMethod.setActiveBody(body);
        fakeClass.addMethod(mainMethod);
        fakeClass.setApplicationClass();
        mainMethod.setModifiers(java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC);
//        LocalGenerator lg = Scene.v().createLocalGenerator(body);
//        Local paramLocal = lg.generateLocal(stringArrayType);
//        body.getUnits()
//                .addFirst(Jimple.v().newIdentityStmt(paramLocal, Jimple.v().newParameterRef(stringArrayType, 0)));
    }

    protected SootClass getOrCreateDummyMainClass() {
        SootClass mainClass = Scene.v().getSootClassUnsafe(dummyClassName);
        if (mainClass == null) {
            mainClass = Scene.v().makeSootClass(dummyClassName);
            mainClass.setResolvingLevel(SootClass.BODIES);
            Scene.v().addClass(mainClass);
        }
        return mainClass;
    }
}
