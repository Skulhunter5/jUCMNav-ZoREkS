package seg.jUCMNav.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import grl.Actor;
import grl.ActorRef;
import grl.Decomposition;
import grl.DecompositionType;
import grl.Evaluation;
import grl.EvaluationStrategy;
import grl.GRLGraph;
import grl.GrlFactory;
import grl.GroupedDependency;
import grl.GroupedDependencyRef;
import grl.IntentionalElement;
import grl.IntentionalElementRef;
import grl.IntentionalElementType;
import grl.LinkRef;
import seg.jUCMNav.model.commands.create.CreateGrlGraphCommand;
import seg.jUCMNav.model.commands.create.GenerateInstanceModelCommand;
import seg.jUCMNav.strategies.EvaluationStrategyManager;
import urn.URNspec;
import urn.UrnFactory;
import urncore.UrncoreFactory;

/**
 * Regression tests for the grouped-dependency evaluation in the (default, quantitative) strategy
 * algorithm: the hub is evaluated through the {@code getGroupedDependencyEvaluation} hook using its
 * {@code destMultiplicity} as a hard constraint on how many of its target-side instances must be
 * satisfied. Following the convention of ordinary dependencies, source goals are restricted based on
 * target goals: when the box denies, the source-side (drawing-start) copies are clipped to the box's
 * value while the target-side copies keep their own evaluation.
 *
 * <p>
 * Pure model, no workbench ({@code EvaluationStrategyManager.getInstance(false)} skips diagram
 * refresh) - exactly the pattern {@code StubExtractionScopeTest} uses for pure queries.
 * </p>
 *
 * @author skuly
 */
public class GroupedDependencyEvaluationTest {

    private URNspec urn;
    private GRLGraph instance;
    private GroupedDependency box;

    @Before
    public void setUp() {
        urn = UrnFactory.eINSTANCE.createURNspec();
        urn.setUrndef(UrncoreFactory.eINSTANCE.createURNdefinition());
        urn.setGrlspec(GrlFactory.eINSTANCE.createGRLspec());
    }

    private GRLGraph newGraph(String name) {
        CreateGrlGraphCommand create = new CreateGrlGraphCommand(urn);
        create.execute();
        GRLGraph graph = create.getDiagram();
        graph.setName(name);
        return graph;
    }

    private ActorRef addActor(GRLGraph graph, String name, int x, int y) {
        Actor actor = GrlFactory.eINSTANCE.createActor();
        actor.setName(name);
        urn.getGrlspec().getActors().add(actor);
        ActorRef ref = GrlFactory.eINSTANCE.createActorRef();
        ref.setContDef(actor);
        ref.setLabel(UrncoreFactory.eINSTANCE.createComponentLabel());
        ref.setX(x);
        ref.setY(y);
        ref.setWidth(200);
        ref.setHeight(150);
        graph.getContRefs().add(ref);
        return ref;
    }

    private IntentionalElementRef addIE(GRLGraph graph, ActorRef actor, String name, IntentionalElementType type, int dx, int dy) {
        IntentionalElement def = GrlFactory.eINSTANCE.createIntentionalElement();
        def.setName(name);
        def.setType(type);
        urn.getGrlspec().getIntElements().add(def);
        IntentionalElementRef ref = GrlFactory.eINSTANCE.createIntentionalElementRef();
        ref.setDef(def);
        ref.setLabel(UrncoreFactory.eINSTANCE.createNodeLabel());
        ref.setX(actor.getX() + dx);
        ref.setY(actor.getY() + dy);
        graph.getNodes().add(ref);
        actor.getNodes().add(ref);
        return ref;
    }

    private void connect(GRLGraph graph, IntentionalElementRef sourceRef, IntentionalElementRef targetRef, LinkRef link) {
        urn.getGrlspec().getLinks().add(link.getLink());
        graph.getConnections().add(link);
        link.setSource(sourceRef);
        link.setTarget(targetRef);
    }

    /**
     * Builds an instance model {@code {A:2, B:2}} from a type model where a super goal
     * OR-decomposes into a single goal that depends on a task of the other actor, with a hard lower
     * bound ({@code 1..*}) on the target multiplicity.
     */
    private void buildGroupedDependency() {
        GRLGraph source = newGraph("TypeModel");
        ActorRef actorA = addActor(source, "A", 100, 100);
        ActorRef actorB = addActor(source, "B", 300, 300);
        IntentionalElementRef parent = addIE(source, actorA, "Main", IntentionalElementType.GOAL_LITERAL, 0, 0);
        IntentionalElementRef aie = addIE(source, actorA, "Goal", IntentionalElementType.GOAL_LITERAL, 30, 40);
        IntentionalElementRef bie = addIE(source, actorB, "Task", IntentionalElementType.TASK_LITERAL, 30, 40);

        Decomposition decomp = GrlFactory.eINSTANCE.createDecomposition();
        // as in a real model the parent is the decomposition dest (it carries the type) and the
        // children are the src ends; the connection is drawn child -> parent
        decomp.setSrc(aie.getDef());
        decomp.setDest(parent.getDef());
        parent.getDef().setDecompositionType(DecompositionType.OR_LITERAL);
        LinkRef decompRef = (LinkRef) GrlFactory.eINSTANCE.createLinkRef();
        decompRef.setLink(decomp);
        connect(source, aie, parent, decompRef);

        grl.Dependency dep = GrlFactory.eINSTANCE.createDependency();
        dep.setDestMultiplicity("1..*"); //$NON-NLS-1$
        // as for a hand-drawn dependency, src is the drawing-end (target def) and dest the
        // drawing-start (source def); the generation keys off the LinkRef direction only
        dep.setSrc(bie.getDef());
        dep.setDest(aie.getDef());
        LinkRef depRef = (LinkRef) GrlFactory.eINSTANCE.createLinkRef();
        depRef.setLink(dep);
        connect(source, aie, bie, depRef);

        Map<ActorRef, Integer> counts = new LinkedHashMap<ActorRef, Integer>();
        counts.put(actorA, Integer.valueOf(2));
        counts.put(actorB, Integer.valueOf(2));
        GenerateInstanceModelCommand command = new GenerateInstanceModelCommand(urn, source, counts, 20);
        assertTrue(command.canExecute());
        command.execute();
        instance = command.getDiagram();
        for (Object o : instance.getNodes())
            if (o instanceof GroupedDependencyRef) {
                box = ((GroupedDependencyRef) o).getDef();
                return;
            }
        throw new IllegalStateException("no grouped dependency generated"); //$NON-NLS-1$
    }

    private EvaluationStrategy newStrategy() {
        EvaluationStrategy strategy = GrlFactory.eINSTANCE.createEvaluationStrategy();
        strategy.setName("S"); //$NON-NLS-1$
        strategy.setGrlspec(urn.getGrlspec());
        urn.getGrlspec().getStrategies().add(strategy);
        return strategy;
    }

    private List<IntentionalElement> sourceCopies() {
        // the source-side (restricted, drawing-start) copies are the dest of the box fans
        List<IntentionalElement> out = new ArrayList<IntentionalElement>();
        for (Object o : instance.getConnections()) {
            LinkRef fan = (LinkRef) o;
            if (box.equals(fan.getLink().getSrc()))
                out.add((IntentionalElement) fan.getLink().getDest());
        }
        return out;
    }

    private List<IntentionalElement> targetCopies() {
        // the target-side (counted, drawing-end) copies are the src of the box fans
        List<IntentionalElement> out = new ArrayList<IntentionalElement>();
        for (Object o : instance.getConnections()) {
            LinkRef fan = (LinkRef) o;
            if (box.equals(fan.getLink().getDest()))
                out.add((IntentionalElement) fan.getLink().getSrc());
        }
        return out;
    }

    private List<IntentionalElement> sourceSideGates() {
        // the remaining instance copies (the "Main" gates above the box-restricted sources)
        List<IntentionalElement> out = new ArrayList<IntentionalElement>();
        List<IntentionalElement> sources = sourceCopies();
        List<IntentionalElement> targets = targetCopies();
        for (Object o : instance.getNodes()) {
            if (!(o instanceof IntentionalElementRef))
                continue;
            IntentionalElement def = ((IntentionalElementRef) o).getDef();
            if (!sources.contains(def) && !targets.contains(def))
                out.add(def);
        }
        return out;
    }

    private int boxEvaluation(EvaluationStrategyManager manager) {
        try {
            Field f = EvaluationStrategyManager.class.getDeclaredField("evaluations"); //$NON-NLS-1$
            f.setAccessible(true);
            @SuppressWarnings("rawtypes")
            Map evals = (Map) f.get(manager);
            Evaluation eval = (Evaluation) evals.get(box);
            return eval == null ? Integer.MIN_VALUE : eval.getEvaluation();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deniedBoxRestrictsSourceGoalsAndLeavesNoWhiteNodes() {
        buildGroupedDependency();
        assertEquals("1..2", box.getDestMultiplicity()); //$NON-NLS-1$
        EvaluationStrategyManager manager = EvaluationStrategyManager.getInstance(false);
        manager.setStrategy(newStrategy());
        manager.calculateEvaluation();

        // with no satisfied target the 1..2 lower bound is unmet: the box denies
        assertEquals(-100, boxEvaluation(manager));
        // every intentional element must have received an evaluation (no "white" nodes after the CCE fix)
        for (Object o : urn.getGrlspec().getIntElements()) {
            IntentionalElement ie = (IntentionalElement) o;
            assertNotNull("element " + ie.getName() + " must have an evaluation", //$NON-NLS-1$ //$NON-NLS-2$
                    manager.getDisplayEvaluationObject(ie));
        }
        // the box restricts its source-side (drawing-start) copies: they are clipped to the box value
        // (dependency semantics: result is clipped down to the source's value)
        for (IntentionalElement source : sourceCopies())
            assertEquals(-100, manager.getDisplayEvaluationObject(source).getEvaluation());
        // the source-side gates above the restricted sources follow their OR-decomposition
        for (IntentionalElement gate : sourceSideGates())
            assertEquals(-100, manager.getDisplayEvaluationObject(gate).getEvaluation());
        // the target-side copies are NOT restricted: they keep their own (default) value
        for (IntentionalElement target : targetCopies())
            assertEquals(0, manager.getDisplayEvaluationObject(target).getEvaluation());
    }

    @Test
    public void satisfiedTargetsDoNotRestrictSources() {
        buildGroupedDependency();
        EvaluationStrategy strategy = newStrategy();
        // mark both target copies satisfied
        for (IntentionalElement target : targetCopies()) {
            Evaluation ev = GrlFactory.eINSTANCE.createEvaluation();
            ev.setIntElement(target);
            ev.setEvaluation(100);
            strategy.getEvaluations().add(ev);
        }

        EvaluationStrategyManager manager = EvaluationStrategyManager.getInstance(false);
        manager.setStrategy(strategy);
        manager.calculateEvaluation();

        // both targets satisfied >= lower bound 1 -> the box evaluates to SATISFICED
        assertEquals(100, boxEvaluation(manager));
        // URN semantics: dependencies set upper bounds, not contributions.
        // the source-side copies have no own evaluation and stay at 0; the satisfied box does not
        // raise them (the dependency only constrains them, 0 < 100 so no clipping occurs).
        for (IntentionalElement source : sourceCopies())
            assertEquals(0, manager.getDisplayEvaluationObject(source).getEvaluation());
        // the target copies keep their explicitly-assigned value
        for (IntentionalElement target : targetCopies())
            assertEquals(100, manager.getDisplayEvaluationObject(target).getEvaluation());
    }
}