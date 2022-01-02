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

package qilin.core.solver;

import qilin.CoreConfig;
import qilin.core.PTA;
import qilin.core.PTAScene;
import qilin.core.builder.CallGraphBuilder;
import qilin.core.builder.ExceptionHandler;
import qilin.core.builder.MethodNodeFactory;
import qilin.core.pag.*;
import qilin.core.sets.P2SetVisitor;
import qilin.core.sets.PointsToSetInternal;
import qilin.util.PTAUtils;
import qilin.util.Pair;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.util.NumberedString;
import soot.util.queue.ChunkedQueue;
import soot.util.queue.QueueReader;

import java.util.*;

public final class Solver extends Propagator {
    private final TreeSet<ValNode> valNodeWorkList = new TreeSet<>();
    private final PAG pag;
    private final PTA pta;
    private final CallGraphBuilder cgb;
    private final ExceptionHandler eh;
    private final ChunkedQueue<ExceptionThrowSite> throwSiteQueue = new ChunkedQueue<>();
    private final ChunkedQueue<VirtualCallSite> virtualCallSiteQueue = new ChunkedQueue<>();

    public Solver(PTA pta) {
        this.cgb = pta.getCgb();
        this.pag = pta.getPag();
        this.eh = pta.getExceptionHandler();
        this.pta = pta;
    }

    @Override
    public void propagate() {
        final QueueReader<MethodOrMethodContext> newRMs = cgb.reachMethodsReader();
        final QueueReader<Node> newPAGEdges = pag.edgeReader();
        final QueueReader<ExceptionThrowSite> newThrows = throwSiteQueue.reader();
        final QueueReader<VirtualCallSite> newCalls = virtualCallSiteQueue.reader();
        cgb.initReachableMethods();
        processStmts(newRMs);
        pag.getAlloc().forEach((a, set) -> set.forEach(v -> propagatePTS(v, a)));
        while (!valNodeWorkList.isEmpty()) {
            ValNode curr = valNodeWorkList.pollFirst();
            // Step 1: Resolving Direct Constraints
            assert curr != null;
            final PointsToSetInternal pts = curr.getP2Set();
            final PointsToSetInternal newset = pts.getNewSet();
            pag.simpleLookup(curr).forEach(to -> propagatePTS(to, newset));

            if (curr instanceof VarNode mSrc) {
                // Step 1 continues.
                Collection<ExceptionThrowSite> throwSites = eh.throwSitesLookUp(mSrc);
                for (ExceptionThrowSite site : throwSites) {
                    eh.exceptionDispatch(newset, site);
                }
                // Step 2: Resolving Indirect Constraints.
                handleStoreAndLoadOnBase(mSrc);
                // Step 3: Collecting New Constraints.
                Collection<VirtualCallSite> sites = cgb.callSitesLookUp(mSrc);
                for (VirtualCallSite site : sites) {
                    cgb.virtualCallDispatch(newset, site);
                }
                processStmts(newRMs);
            }
            curr.getP2Set().flushNew();
            // Step 4: Activating New Constraints.
            activateConstraints(newCalls, newRMs, newThrows, newPAGEdges);
        }
    }

    public void processStmts(Iterator<MethodOrMethodContext> newRMs) {
        while (newRMs.hasNext()) {
            MethodOrMethodContext momc = newRMs.next();
            SootMethod method = momc.method();
            if (method.isPhantom()) {
                continue;
            }
            MethodPAG mpag = pag.getMethodPAG(method);
            addToPAG(mpag, momc.context());
            // !FIXME in a context-sensitive pointer analysis, clinits in a method maybe added multiple times.
            if (CoreConfig.v().getPtaConfig().clinitMode == CoreConfig.ClinitMode.ONFLY) {
                // add <clinit> find in the method to reachableMethods.
                Iterator<SootMethod> it = mpag.triggeredClinits();
                while (it.hasNext()) {
                    SootMethod sm = it.next();
                    cgb.injectCallEdge(sm.getDeclaringClass().getType(), pta.parameterize(sm, pta.emptyContext()), Kind.CLINIT);
                }
            }
            recordCallStmts(momc, mpag.invokeStmts);
            recordThrowStmts(momc, mpag.stmt2wrapperedTraps.keySet());
        }
    }

    private void recordCallStmts(MethodOrMethodContext m, Collection<Unit> units) {
        for (final Unit u : units) {
            final Stmt s = (Stmt) u;
            if (s.containsInvokeExpr()) {
                InvokeExpr ie = s.getInvokeExpr();
                if (ie instanceof InstanceInvokeExpr iie) {
                    Local receiver = (Local) iie.getBase();
                    VarNode recNode = cgb.getReceiverVarNode(receiver, m);
                    NumberedString subSig = iie.getMethodRef().getSubSignature();
                    VirtualCallSite virtualCallSite = new VirtualCallSite(recNode, s, m, iie, subSig, Edge.ieToKind(iie));
                    if (cgb.recordVirtualCallSite(recNode, virtualCallSite)) {
                        virtualCallSiteQueue.add(virtualCallSite);
                    }
                } else {
                    SootMethod tgt = ie.getMethod();
                    if (tgt != null) { // static invoke or dynamic invoke
                        VarNode recNode = pag.getMethodPAG(m.method()).nodeFactory().caseThis();
                        recNode = (VarNode) pta.parameterize(recNode, m.context());
                        cgb.recordStaticCallSite(recNode, new Pair<>(m, s));
                        if (ie instanceof DynamicInvokeExpr) {
                            // !TODO dynamicInvoke is provided in JDK after Java 7.
                            // currently, PTA does not handle dynamicInvokeExpr.
                        } else {
                            cgb.addStaticEdge(m, s, tgt, Edge.ieToKind(ie));
                        }
                    } else if (!Options.v().ignore_resolution_errors()) {
                        throw new InternalError("Unresolved target " + ie.getMethod()
                                + ". Resolution error should have occured earlier.");
                    }
                }
            }
        }
    }

    private void recordThrowStmts(MethodOrMethodContext m, Collection<Stmt> stmts) {
        for (final Stmt stmt : stmts) {
            SootMethod sm = m.method();
            MethodPAG mpag = pag.getMethodPAG(sm);
            MethodNodeFactory nodeFactory = mpag.nodeFactory();
            Node src;
            if (stmt.containsInvokeExpr()) {
                src = pag.makeInvokeStmtThrowVarNode(stmt, sm);
            } else {
                assert stmt instanceof ThrowStmt;
                ThrowStmt ts = (ThrowStmt) stmt;
                src = nodeFactory.getNode(ts.getOp());
            }
            VarNode throwNode = (VarNode) pta.parameterize(src, m.context());
            ExceptionThrowSite throwSite = new ExceptionThrowSite(throwNode, stmt, m);
            if (eh.addThrowSite(throwNode, throwSite)) {
                throwSiteQueue.add(throwSite);
            }
        }
    }

    private void addToPAG(MethodPAG mpag, Context cxt) {
        Set<Context> contexts = pag.getMethod2ContextsMap().computeIfAbsent(mpag, k1 -> new HashSet<>());
        if (!contexts.add(cxt)) {
            return;
        }
        for (QueueReader<Node> reader = mpag.getInternalReader().clone(); reader.hasNext(); ) {
            Node from = reader.next();
            Node to = reader.next();
            from = pta.parameterize(from, cxt);
            to = pta.parameterize(to, cxt);
            if (from instanceof AllocNode) {
                handleImplicitCallToFinalizerRegister((AllocNode) from);
            }
            pag.addEdge(from, to);
        }
    }

    // handle implicit calls to java.lang.ref.Finalizer.register by the JVM.
    // please refer to library/finalization.logic in doop.
    private void handleImplicitCallToFinalizerRegister(AllocNode heap) {
        if (PTAUtils.supportFinalize(heap)) {
            SootMethod rm = PTAScene.v().getMethod("<java.lang.ref.Finalizer: void register(java.lang.Object)>");
            MethodPAG tgtmpag = pag.getMethodPAG(rm);
            MethodNodeFactory tgtnf = tgtmpag.nodeFactory();
            Node parm = tgtnf.caseParm(0);
            Context calleeCtx = pta.emptyContext();
            AllocNode baseHeap = heap.base();
            parm = pta.parameterize(parm, calleeCtx);
            pag.addEdge(heap, parm);
            cgb.injectCallEdge(baseHeap, pta.parameterize(rm, calleeCtx), Kind.STATIC);
        }
    }

    private void handleStoreAndLoadOnBase(VarNode base) {
        for (final FieldRefNode fr : base.getAllFieldRefs()) {
            final FieldValNode fvn = pag.makeFieldValNode(fr.getField());
            for (final VarNode v : pag.storeInvLookup(fr)) {
                handleStoreEdge(base.getP2Set().getNewSet(), fvn, v);
            }
            for (final VarNode to : pag.loadLookup(fr)) {
                handleLoadEdge(base.getP2Set().getNewSet(), fvn, to);
            }
        }
    }

    private void handleStoreEdge(PointsToSetInternal baseHeaps, FieldValNode fvn, ValNode from) {
        baseHeaps.forall(new P2SetVisitor() {
            public void visit(Node n) {
                if (disallowStoreOrLoadOn((AllocNode) n)) {
                    return;
                }
                final ValNode oDotF = (ValNode) pta.parameterize(fvn, PTAUtils.plusplusOp((AllocNode) n));
                pag.addEdge(from, oDotF);
            }
        });
    }

    private void handleLoadEdge(PointsToSetInternal baseHeaps, FieldValNode fvn, ValNode to) {
        baseHeaps.forall(new P2SetVisitor() {
            public void visit(Node n) {
                if (disallowStoreOrLoadOn((AllocNode) n)) {
                    return;
                }
                final ValNode oDotF = (ValNode) pta.parameterize(fvn, PTAUtils.plusplusOp((AllocNode) n));
                pag.addEdge(oDotF, to);
            }
        });
    }

    private void activateConstraints(QueueReader<VirtualCallSite> newCalls, QueueReader<MethodOrMethodContext> newRMs, QueueReader<ExceptionThrowSite> newThrows, QueueReader<Node> addedEdges) {
        while (newCalls.hasNext()) {
            final VirtualCallSite site = newCalls.next();
            final VarNode receiver = site.recNode();
            cgb.virtualCallDispatch(receiver.getP2Set().getOldSet(), site);
        }
        processStmts(newRMs);

        while (newThrows.hasNext()) {
            final ExceptionThrowSite ets = newThrows.next();
            final VarNode throwNode = ets.getThrowNode();
            eh.exceptionDispatch(throwNode.getP2Set().getOldSet(), ets);
        }
        /*
         * there are some actual parameter to formal parameter edges whose source nodes are not in the worklist.
         * For this case, we should use the following loop to update the target nodes and insert the
         * target nodes into the worklist if nesseary.
         * */
        while (addedEdges.hasNext()) {
            final Node addedSrc = addedEdges.next();
            final Node addedTgt = addedEdges.next();
            if (addedSrc instanceof VarNode && addedTgt instanceof VarNode
                    || addedSrc instanceof ContextField || addedTgt instanceof ContextField
            ) { // x = y; x = o.f; o.f = y;
                final ValNode srcv = (ValNode) addedSrc;
                final ValNode tgtv = (ValNode) addedTgt;
                propagatePTS(tgtv, srcv.getP2Set().getOldSet());
            } else if (addedSrc instanceof final FieldRefNode srcfrn) { // b = a.f
                final FieldValNode fvn = pag.makeFieldValNode(srcfrn.getField());
                handleLoadEdge(srcfrn.getBase().getP2Set().getOldSet(), fvn, (ValNode) addedTgt);
            } else if (addedTgt instanceof final FieldRefNode tgtfrn) { // a.f = b;
                final FieldValNode fvn = pag.makeFieldValNode(tgtfrn.getField());
                handleStoreEdge(tgtfrn.getBase().getP2Set().getOldSet(), fvn, (ValNode) addedSrc);
            } else if (addedSrc instanceof AllocNode) { // alloc x = new T;
                propagatePTS((VarNode) addedTgt, (AllocNode) addedSrc);
            }
        }
    }

    private void propagatePTS(final ValNode pointer, PointsToSetInternal other) {
        final PointsToSetInternal addTo = pointer.makeP2Set();
        if (addTo.addAll(other, null)) {
            valNodeWorkList.add(pointer);
        }
    }

    private void propagatePTS(final ValNode pointer, AllocNode heap) {
        if (pointer.makeP2Set().add(heap)) {
            valNodeWorkList.add(pointer);
        }
    }

    // we do not allow store to and load from constant heap/empty array.
    private boolean disallowStoreOrLoadOn(AllocNode heap) {
        AllocNode base = heap.base();
        // return base instanceof StringConstantNode || PTAUtils.isEmptyArray(base);
        return PTAUtils.isEmptyArray(base);
    }
}