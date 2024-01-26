package qilin.core.builder;

import qilin.core.ArtificialMethod;
import soot.*;
import soot.jimple.Jimple;

import java.util.Collections;

public class FakeAndroidMainFactory extends ArtificialMethod {
    private SootClass fakeClass;

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
        this.localStart = 0;
        fakeClass = getOrCreateDummyMainClass();
        Body body;
        /**
         * Default name of the dummy main method
         */
        String dummyMethodName = "fakeAndroidMainMethod";
        mainMethod = fakeClass.getMethodByNameUnsafe(dummyMethodName);
        Type stringArrayType = ArrayType.v(RefType.v("java.lang.String"), 1);
        mainMethod = Scene.v().makeSootMethod(dummyMethodName, Collections.singletonList(stringArrayType), VoidType.v());
        body = Jimple.v().newBody();
        body.setMethod(mainMethod);
        mainMethod.setActiveBody(body);
        fakeClass.addMethod(mainMethod);
        fakeClass.setApplicationClass();
        mainMethod.setModifiers(java.lang.reflect.Modifier.PUBLIC | java.lang.reflect.Modifier.STATIC);
        LocalGenerator lg = Scene.v().createLocalGenerator(body);
        Local paramLocal = lg.generateLocal(stringArrayType);
        body.getUnits()
                .addFirst(Jimple.v().newIdentityStmt(paramLocal, Jimple.v().newParameterRef(stringArrayType, 0)));
    }

    protected SootClass getOrCreateDummyMainClass() {
        /**
         * Default name of the class containing the dummy main method
         */
        String dummyClassName = "FakeAndroidMain";
        SootClass mainClass = Scene.v().getSootClassUnsafe(dummyClassName);
        if (mainClass == null) {
            mainClass = Scene.v().makeSootClass(dummyClassName);
            mainClass.setResolvingLevel(SootClass.BODIES);
            Scene.v().addClass(mainClass);
        }
        return mainClass;
    }
}
