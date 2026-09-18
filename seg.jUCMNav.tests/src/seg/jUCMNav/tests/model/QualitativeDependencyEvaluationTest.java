package seg.jUCMNav.tests.model;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import grl.Dependency;
import grl.Decomposition;
import grl.DecompositionType;
import grl.Evaluation;
import grl.EvaluationStrategy;
import grl.GrlFactory;
import grl.IntentionalElement;
import grl.IntentionalElementType;
import seg.jUCMNav.strategies.EvaluationStrategyManager;
import seg.jUCMNav.views.preferences.StrategyEvaluationPreferences;
import urn.URNspec;
import urn.UrnFactory;
import urncore.UrncoreFactory;

/**
 * Regression tests for dependency evaluation in the qualitative GRL strategy algorithm. Per the URN
 * paper, a depender's qualitative evaluation is the minimum between its own (just computed) value
 * and the qualitative values of the elements it depends on, using the label order
 * {@code Denied < (Conflict = Undecided) < WeaklyDenied < None < WeaklySatisfied < Satisfied}; a
 * Conflict in the chain is substituted with Undecided (conflicts are not propagated). The current
 * value of the depender must never leak into its own result.
 *
 * <p>
 * Pure model, no workbench ({@code EvaluationStrategyManager.getInstance(false)} skips diagram
 * refresh), same pattern as {@code GroupedDependencyEvaluationTest}.
 * </p>
 *
 * @author skuly
 */
public class QualitativeDependencyEvaluationTest {

    private URNspec urn;
    private EvaluationStrategy strategy;
    private EvaluationStrategyManager manager;
    private String originalAlgorithm;

    @Before
    public void setUp() {
        urn = UrnFactory.eINSTANCE.createURNspec();
        urn.setUrndef(UrncoreFactory.eINSTANCE.createURNdefinition());
        urn.setGrlspec(GrlFactory.eINSTANCE.createGRLspec());
        originalAlgorithm = StrategyEvaluationPreferences.getAlgorithm();
        StrategyEvaluationPreferences.setAlgorithm(String.valueOf(StrategyEvaluationPreferences.QUALITATIVE_ALGORITHM));
        manager = EvaluationStrategyManager.getInstance(false);
    }

    @After
    public void tearDown() {
        StrategyEvaluationPreferences.setAlgorithm(originalAlgorithm);
        manager.setStrategy(null);
    }

    private IntentionalElement newElement(String name, IntentionalElementType type) {
        IntentionalElement element = GrlFactory.eINSTANCE.createIntentionalElement();
        element.setName(name);
        element.setType(type);
        urn.getGrlspec().getIntElements().add(element);
        return element;
    }

    private void decomposeOr(IntentionalElement parent, IntentionalElement child) {
        parent.setDecompositionType(DecompositionType.OR_LITERAL);
        Decomposition decomp = GrlFactory.eINSTANCE.createDecomposition();
        decomp.setSrc(child);
        decomp.setDest(parent);
        urn.getGrlspec().getLinks().add(decomp);
    }

    private void dependOn(IntentionalElement depender, IntentionalElement dependum) {
        // as in a real model and in GroupedDependencyEvaluationTest, the dependency's src is the
        // drawing-end (dependum) and its dest the drawing-start (depender)
        Dependency dep = GrlFactory.eINSTANCE.createDependency();
        dep.setSrc(dependum);
        dep.setDest(depender);
        urn.getGrlspec().getLinks().add(dep);
    }

    private void preset(IntentionalElement element, int value) {
        Evaluation eval = GrlFactory.eINSTANCE.createEvaluation();
        eval.setIntElement(element);
        eval.setEvaluation(value);
        strategy.getEvaluations().add(eval);
    }

    private void newStrategy() {
        strategy = GrlFactory.eINSTANCE.createEvaluationStrategy();
        strategy.setName("S"); //$NON-NLS-1$
        strategy.setGrlspec(urn.getGrlspec());
        urn.getGrlspec().getStrategies().add(strategy);
    }

    private int evaluate(IntentionalElement target) {
        manager.setStrategy(strategy);
        manager.calculateEvaluation();
        return manager.getDisplayEvaluationObject(target).getEvaluation();
    }

    /**
     * A depender determined fully satisfied by an OR decomposition over a satisfied sub-goal must
     * keep that value when the goal it depends on is fully satisfied, instead of being stuck at
     * None. Regression for the stale-value baseline in the qualitative dependency calculation.
     */
    @Test
    public void satisfiedDependumDoesNotCapSatisfiedDepender() {
        IntentionalElement parent = newElement("Depender", IntentionalElementType.GOAL_LITERAL);
        IntentionalElement child = newElement("SubGoal", IntentionalElementType.GOAL_LITERAL);
        IntentionalElement dependum = newElement("Dependum", IntentionalElementType.TASK_LITERAL);
        decomposeOr(parent, child);
        dependOn(parent, dependum);

        newStrategy();
        preset(child, 100);
        preset(dependum, 100);

        assertEquals(100, evaluate(parent));
    }

    /**
     * A depender with an unsatisfied (None) dependum is capped to None even though its own
     * decomposition would yield Satisfied (paper example (a): None is weaker than Satisfied).
     */
    @Test
    public void noneDependumCapsSatisfiedDepender() {
        IntentionalElement parent = newElement("Depender", IntentionalElementType.GOAL_LITERAL);
        IntentionalElement child = newElement("SubGoal", IntentionalElementType.GOAL_LITERAL);
        IntentionalElement dependum = newElement("Dependum", IntentionalElementType.TASK_LITERAL);
        decomposeOr(parent, child);
        dependOn(parent, dependum);

        newStrategy();
        preset(child, 100);

        assertEquals(0, evaluate(parent));
    }

    /**
     * A weakly satisfied dependum caps the depender to its weakly satisfied value (50), no higher.
     */
    @Test
    public void weaklySatisfiedDependumCapsDependerToItsOwnValue() {
        IntentionalElement parent = newElement("Depender", IntentionalElementType.GOAL_LITERAL);
        IntentionalElement child = newElement("SubGoal", IntentionalElementType.GOAL_LITERAL);
        IntentionalElement dependum = newElement("Dependum", IntentionalElementType.TASK_LITERAL);
        decomposeOr(parent, child);
        dependOn(parent, dependum);

        newStrategy();
        preset(child, 100);
        preset(dependum, 50);

        assertEquals(50, evaluate(parent));
    }

    /**
     * A Conflict in a dependum must propagate as Undecided to the depender, not as Conflict (paper
     * example (b): conflicts are substituted with Undecided as they are not propagated).
     */
    @Test
    public void conflictDependumPropagatesUndecided() {
        IntentionalElement parent = newElement("Depender", IntentionalElementType.GOAL_LITERAL);
        IntentionalElement child = newElement("SubGoal", IntentionalElementType.GOAL_LITERAL);
        IntentionalElement dependum = newElement("Dependum", IntentionalElementType.TASK_LITERAL);
        decomposeOr(parent, child);
        dependOn(parent, dependum);

        newStrategy();
        preset(child, 100);
        preset(dependum, seg.jUCMNav.extensionpoints.IGRLStrategyAlgorithm.CONFLICT);

        assertEquals(seg.jUCMNav.extensionpoints.IGRLStrategyAlgorithm.UNDECIDED, evaluate(parent));
    }

    /**
     * The depender must follow the dependum across evaluations instead of being pinned to a
     * previously computed value. Regression for the stale-value baseline: a depender first capped
     * to None must recover once its dependum becomes satisfied in a later evaluation pass.
     */
    @Test
    public void dependerRecoversWhenDependumImproves() {
        IntentionalElement parent = newElement("Depender", IntentionalElementType.GOAL_LITERAL);
        IntentionalElement child = newElement("SubGoal", IntentionalElementType.GOAL_LITERAL);
        IntentionalElement dependum = newElement("Dependum", IntentionalElementType.TASK_LITERAL);
        decomposeOr(parent, child);
        dependOn(parent, dependum);

        newStrategy();
        preset(child, 100);

        // first pass: unset dependum means None, so the depender is capped to None
        assertEquals(0, evaluate(parent));
        // second pass: the dependum becomes fully satisfied, the depender must recover
        preset(dependum, 100);
        assertEquals(100, evaluate(parent));
    }
}