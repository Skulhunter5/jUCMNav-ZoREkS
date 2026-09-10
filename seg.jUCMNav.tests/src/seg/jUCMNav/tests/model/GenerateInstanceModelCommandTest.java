package seg.jUCMNav.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.swt.graphics.Image;

import fm.Feature;
import fm.FmFactory;
import grl.Actor;
import grl.ActorRef;
import grl.Contribution;
import grl.ContributionType;
import grl.Decomposition;
import grl.Dependency;
import grl.ElementLink;
import grl.GRLGraph;
import grl.GrlFactory;
import grl.IntentionalElement;
import grl.IntentionalElementRef;
import grl.IntentionalElementType;
import grl.LinkRef;
import grl.kpimodel.Indicator;
import grl.kpimodel.KpimodelFactory;
import seg.jUCMNav.JUCMNavPlugin;
import seg.jUCMNav.editparts.treeEditparts.GrlGraphTreeEditPart;
import seg.jUCMNav.model.commands.create.CreateGrlGraphCommand;
import seg.jUCMNav.model.commands.create.GenerateInstanceModelCommand;
import seg.jUCMNav.model.commands.create.GenerateInstanceModelCommand.GenerationProblem;
import seg.jUCMNav.strategies.util.ReusedElementUtil;
import urn.URNspec;
import urn.UrnFactory;
import urncore.ConnectionLabel;
import urncore.UrncoreFactory;

/**
 * Unit tests for {@link GenerateInstanceModelCommand}. Pure model, no workbench: the command only
 * reads and writes the EMF model, so it is exercised exactly the way {@code StubExtractionScopeTest}
 * exercises {@code StubExtractionScope}.
 *
 * <p>
 * The type model is built programmatically (two actors A and B, each with intentional elements,
 * plus an intra-actor contribution inside A), then instantiated with {@code {A:2, B:1}}. Besides
 * the happy path we verify the {@code analyze} problem detection (free-floating elements, nested
 * actors, cross-actor dependencies) and the undo/redo round-trip.
 * </p>
 *
 * @author skuly
 */
public class GenerateInstanceModelCommandTest {

    private URNspec urn;

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

    private IntentionalElementRef addIE(GRLGraph graph, ActorRef actor, String name, IntentionalElementType type, int dx,
            int dy) {
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

    private LinkRef connect(GRLGraph graph, IntentionalElementRef sourceRef, IntentionalElementRef targetRef, boolean contribution) {
        return connect(graph, sourceRef, targetRef, contribution ? (ElementLink) GrlFactory.eINSTANCE.createContribution()
                : (ElementLink) GrlFactory.eINSTANCE.createDependency());
    }

    private LinkRef connect(GRLGraph graph, IntentionalElementRef sourceRef, IntentionalElementRef targetRef, ElementLink link) {
        link.setSrc(sourceRef.getDef());
        link.setDest(targetRef.getDef());
        urn.getGrlspec().getLinks().add(link);
        LinkRef linkRef = (LinkRef) GrlFactory.eINSTANCE.createLinkRef();
        linkRef.setLink(link);
        linkRef.setSource(sourceRef);
        linkRef.setTarget(targetRef);
        graph.getConnections().add(linkRef);
        return linkRef;
    }

    // ---------------------------------------------------------------- analyze

    @Test
    public void analyzeEmptyGraph() {
        GRLGraph graph = newGraph("Empty");
        assertEquals(GenerationProblem.NO_ACTOR, GenerateInstanceModelCommand.analyze(graph));
    }

    @Test
    public void analyzeCleanGraph() {
        GRLGraph graph = newGraph("G");
        addActor(graph, "A", 100, 100);
        assertEquals(GenerationProblem.NONE, GenerateInstanceModelCommand.analyze(graph));
    }

    @Test
    public void analyzeFreeFloatingIntentionalElement() {
        GRLGraph graph = newGraph("G");
        IntentionalElement def = GrlFactory.eINSTANCE.createIntentionalElement();
        def.setName("Free");
        def.setType(IntentionalElementType.GOAL_LITERAL);
        urn.getGrlspec().getIntElements().add(def);
        IntentionalElementRef ref = GrlFactory.eINSTANCE.createIntentionalElementRef();
        ref.setDef(def);
        ref.setLabel(UrncoreFactory.eINSTANCE.createNodeLabel());
        graph.getNodes().add(ref);
        addActor(graph, "A", 100, 100);
        assertEquals(GenerationProblem.FREE_FLOATING_IE, GenerateInstanceModelCommand.analyze(graph));
    }

    @Test
    public void analyzeNestedActor() {
        GRLGraph graph = newGraph("G");
        ActorRef outer = addActor(graph, "Outer", 100, 100);
        ActorRef inner = addActor(graph, "Inner", 200, 200);
        inner.setParent(outer);
        assertEquals(GenerationProblem.NESTED_ACTOR, GenerateInstanceModelCommand.analyze(graph));
    }

    @Test
    public void analyzeCrossActorDependency() {
        GRLGraph graph = newGraph("G");
        ActorRef a = addActor(graph, "A", 100, 100);
        ActorRef b = addActor(graph, "B", 300, 300);
        IntentionalElementRef aie = addIE(graph, a, "Goal", IntentionalElementType.GOAL_LITERAL, 10, 20);
        IntentionalElementRef bie = addIE(graph, b, "Task", IntentionalElementType.TASK_LITERAL, 10, 20);
        connect(graph, aie, bie, false);
        assertEquals(GenerationProblem.DEPENDENCY, GenerateInstanceModelCommand.analyze(graph));
    }

    // ---------------------------------------------------------------- generation

    @Test
    public void generateCopies() {
        GRLGraph source = newGraph("TypeModel");
        ActorRef actorA = addActor(source, "A", 100, 100);
        ActorRef actorB = addActor(source, "B", 300, 300);
        IntentionalElementRef g1 = addIE(source, actorA, "G1", IntentionalElementType.GOAL_LITERAL, 10, 20);
        IntentionalElementRef g2 = addIE(source, actorA, "G2", IntentionalElementType.GOAL_LITERAL, 60, 20);
        IntentionalElementRef t1 = addIE(source, actorB, "T1", IntentionalElementType.TASK_LITERAL, 10, 20);
        LinkRef contribution = connect(source, g1, g2, true);

        Map<ActorRef, Integer> counts = new LinkedHashMap<ActorRef, Integer>();
        counts.put(actorA, Integer.valueOf(2));
        counts.put(actorB, Integer.valueOf(1));

        GenerateInstanceModelCommand command = new GenerateInstanceModelCommand(urn, source, counts, 20);
        assertTrue(command.canExecute());
        command.execute();

        GRLGraph graph = command.getDiagram();
        assertTrue(urn.getUrndef().getSpecDiagrams().contains(graph));
        // inserted right after the source graph
        assertEquals(1, urn.getUrndef().getSpecDiagrams().indexOf(graph));
        assertEquals("TypeModel IM-2-1", graph.getName());

        // definitions: 2 original actors + 3 copies; 3 original elements + 5 copies
        assertEquals(5, urn.getGrlspec().getActors().size());
        assertEquals(8, urn.getGrlspec().getIntElements().size());

        // actor copies, in order
        assertEquals(3, graph.getContRefs().size());
        assertEquals("1: A", ((ActorRef) graph.getContRefs().get(0)).getName());
        assertEquals("2: A", ((ActorRef) graph.getContRefs().get(1)).getName());
        assertEquals("1: B", ((ActorRef) graph.getContRefs().get(2)).getName());

        // intentional element copies, nested in the actor copies
        ActorRef a1 = (ActorRef) graph.getContRefs().get(0);
        ActorRef a2 = (ActorRef) graph.getContRefs().get(1);
        assertEquals(2, a1.getNodes().size());
        assertEquals(2, a2.getNodes().size());
        IntentionalElementRef a1g1 = (IntentionalElementRef) a1.getNodes().get(0);
        IntentionalElementRef a1g2 = (IntentionalElementRef) a1.getNodes().get(1);
        assertEquals("1: G1", a1g1.getName());
        assertEquals("1: G2", a1g2.getName());
        assertEquals("1: G1", a1g1.getDef().getName());
        assertEquals(IntentionalElementType.GOAL_LITERAL, a1g1.getDef().getType());
        // relative placement inside the actor is preserved (source actor at 100,100, IEs +10/+60,+20)
        assertEquals(10, a1g1.getX());
        assertEquals(20, a1g1.getY());
        assertEquals(60, a1g2.getX());

        // layout: copy size matches the source actor; second A cell is to the right of the first
        // (cell width = source actor width + margin); B is one row below A
        assertEquals(0, a1.getX());
        assertEquals(0, a1.getY());
        assertEquals(200, a1.getWidth());
        assertEquals(150, a1.getHeight());
        assertEquals(220, a2.getX());
        assertEquals(0, a2.getY());
        ActorRef b1 = (ActorRef) graph.getContRefs().get(2);
        assertEquals(0, b1.getX());
        assertEquals(170, b1.getY());

        // intra-actor link replicated per instance, endpoints wired to the matching copies
        assertEquals(2, graph.getConnections().size());
        for (Object o : graph.getConnections()) {
            LinkRef copyRef = (LinkRef) o;
            assertTrue(copyRef.getLink() instanceof Contribution);
            IntentionalElementRef copySource = (IntentionalElementRef) copyRef.getSource();
            IntentionalElementRef copyTarget = (IntentionalElementRef) copyRef.getTarget();
            assertEquals(copySource.getContRef(), copyTarget.getContRef());
            assertTrue(copySource.getName().startsWith("1: ") || copySource.getName().startsWith("2: "));
            assertNotSame(contribution, copyRef.getLink());
            assertNotSame(g1.getDef(), copySource.getDef());
        }

        // original graph untouched
        assertEquals(2, source.getContRefs().size());
        assertEquals(1, source.getConnections().size());

        // undo
        command.undo();
        assertFalse(urn.getUrndef().getSpecDiagrams().contains(graph));
        assertEquals(2, urn.getGrlspec().getActors().size());
        assertEquals(3, urn.getGrlspec().getIntElements().size());

        // redo
        command.redo();
        assertTrue(urn.getUrndef().getSpecDiagrams().contains(graph));
        assertEquals(5, urn.getGrlspec().getActors().size());
        assertEquals(3, graph.getContRefs().size());
    }

    @Test
    public void generatedGraphIsMarkedAndResolvesBackToSource() {
        GRLGraph source = newGraph("TypeModel");
        ActorRef actorA = addActor(source, "A", 100, 100);
        addIE(source, actorA, "G1", IntentionalElementType.GOAL_LITERAL, 10, 20);

        Map<ActorRef, Integer> counts = new LinkedHashMap<ActorRef, Integer>();
        counts.put(actorA, Integer.valueOf(1));
        GenerateInstanceModelCommand command = new GenerateInstanceModelCommand(urn, source, counts, 20);
        command.execute();

        GRLGraph instance = command.getDiagram();
        // the generated graph is an instance model, the source is not
        assertTrue(GenerateInstanceModelCommand.isInstanceModel(instance));
        assertFalse(GenerateInstanceModelCommand.isInstanceModel(source));
        // and it resolves back to its type model, never to itself
        assertEquals(source, GenerateInstanceModelCommand.getTypeModel(urn, instance));

        // a freshly created, unrelated graph is neither
        GRLGraph other = newGraph("Other");
        assertFalse(GenerateInstanceModelCommand.isInstanceModel(other));
        assertEquals(null, GenerateInstanceModelCommand.getTypeModel(urn, other));

        // undo removes the graph from the model; redo brings it and its marker back
        command.undo();
        assertFalse(urn.getUrndef().getSpecDiagrams().contains(instance));
        command.redo();
        assertTrue(urn.getUrndef().getSpecDiagrams().contains(instance));
        assertTrue(GenerateInstanceModelCommand.isInstanceModel(instance));
        assertEquals(source, GenerateInstanceModelCommand.getTypeModel(urn, instance));
    }

    @Test
    public void copiesAreIndependentOfOriginal() {
        GRLGraph source = newGraph("Orig");
        ActorRef actorA = addActor(source, "A", 100, 100);
        addIE(source, actorA, "G1", IntentionalElementType.GOAL_LITERAL, 10, 20);

        Map<ActorRef, Integer> counts = new LinkedHashMap<ActorRef, Integer>();
        counts.put(actorA, Integer.valueOf(1));
        GenerateInstanceModelCommand command = new GenerateInstanceModelCommand(urn, source, counts, 20);
        command.execute();

        // renaming the original definition must not reach the copy
        Actor original = (Actor) actorA.getContDef();
        original.setName("Renamed");
        ActorRef copyRef = (ActorRef) command.getDiagram().getContRefs().get(0);
        Actor copyDef = (Actor) copyRef.getContDef();
        assertEquals("1: A", copyRef.getName());
        assertEquals("1: A", copyDef.getName());
        assertFalse(original == copyDef);
        assertNotNull(copyDef.getId());
    }

    @Test
    public void allIntentionalElementTypesInstantiate() {
        GRLGraph source = newGraph("Types");
        ActorRef actorA = addActor(source, "A", 100, 100);
        addIE(source, actorA, "Sg", IntentionalElementType.SOFTGOAL_LITERAL, 0, 0);
        addIE(source, actorA, "Go", IntentionalElementType.GOAL_LITERAL, 10, 0);
        addIE(source, actorA, "Ta", IntentionalElementType.TASK_LITERAL, 20, 0);
        addIE(source, actorA, "Re", IntentionalElementType.RESSOURCE_LITERAL, 30, 0);

        IntentionalElement ind = KpimodelFactory.eINSTANCE.createIndicator();
        ind.setName("In");
        ind.setType(IntentionalElementType.INDICATOR_LITERAL);
        urn.getGrlspec().getIntElements().add(ind);
        IntentionalElementRef indRef = GrlFactory.eINSTANCE.createIntentionalElementRef();
        indRef.setDef(ind);
        indRef.setLabel(UrncoreFactory.eINSTANCE.createNodeLabel());
        indRef.setX(40);
        indRef.setY(0);
        source.getNodes().add(indRef);
        actorA.getNodes().add(indRef);

        Feature feat = FmFactory.eINSTANCE.createFeature();
        feat.setName("Fe");
        feat.setType(IntentionalElementType.TASK_LITERAL);
        urn.getGrlspec().getIntElements().add(feat);
        IntentionalElementRef featRef = GrlFactory.eINSTANCE.createIntentionalElementRef();
        featRef.setDef(feat);
        featRef.setLabel(UrncoreFactory.eINSTANCE.createNodeLabel());
        featRef.setX(50);
        featRef.setY(0);
        source.getNodes().add(featRef);
        actorA.getNodes().add(featRef);

        Map<ActorRef, Integer> counts = new LinkedHashMap<ActorRef, Integer>();
        counts.put(actorA, Integer.valueOf(1));
        GenerateInstanceModelCommand command = new GenerateInstanceModelCommand(urn, source, counts, 20);
        command.execute();

        ActorRef copy = (ActorRef) command.getDiagram().getContRefs().get(0);
        for (int i = 0; i < copy.getNodes().size(); i++) {
            IntentionalElementRef ieCopy = (IntentionalElementRef) copy.getNodes().get(i);
            IntentionalElement defCopy = ieCopy.getDef();
            assertEquals("1: " + new String[] { "Sg", "Go", "Ta", "Re", "In", "Fe" }[i], ieCopy.getName());
            switch (i) {
            case 0:
                assertEquals(IntentionalElementType.SOFTGOAL_LITERAL, defCopy.getType());
                break;
            case 1:
                assertEquals(IntentionalElementType.GOAL_LITERAL, defCopy.getType());
                break;
            case 2:
                assertEquals(IntentionalElementType.TASK_LITERAL, defCopy.getType());
                break;
            case 3:
                assertEquals(IntentionalElementType.RESSOURCE_LITERAL, defCopy.getType());
                break;
            case 4:
                assertTrue("indicator copy must be an Indicator, was " + defCopy.getClass().getName(),
                        defCopy instanceof Indicator);
                assertEquals(IntentionalElementType.INDICATOR_LITERAL, defCopy.getType());
                break;
            case 5:
                assertTrue("feature copy must be a Feature, was " + defCopy.getClass().getName(),
                        defCopy instanceof Feature);
                assertEquals(IntentionalElementType.TASK_LITERAL, defCopy.getType());
                break;
            }
        }
    }

    @Test
    public void everyLinkTypeReplicatedAndNamed() {
        GRLGraph source = newGraph("Links");
        ActorRef actorA = addActor(source, "A", 100, 100);
        IntentionalElementRef g1 = addIE(source, actorA, "G1", IntentionalElementType.GOAL_LITERAL, 10, 20);
        IntentionalElementRef g2 = addIE(source, actorA, "G2", IntentionalElementType.GOAL_LITERAL, 60, 20);
        IntentionalElementRef g3 = addIE(source, actorA, "G3", IntentionalElementType.GOAL_LITERAL, 110, 20);

        Contribution contrib = GrlFactory.eINSTANCE.createContribution();
        contrib.setContribution(ContributionType.MAKE_LITERAL);
        contrib.setQuantitativeContribution(100);
        contrib.setCorrelation(true);
        LinkRef contribRef = connect(source, g1, g2, contrib);

        LinkRef decompRef = connect(source, g2, g3, GrlFactory.eINSTANCE.createDecomposition());
        LinkRef depRef = connect(source, g1, g3, GrlFactory.eINSTANCE.createDependency());

        Map<ActorRef, Integer> counts = new LinkedHashMap<ActorRef, Integer>();
        counts.put(actorA, Integer.valueOf(1));
        GenerateInstanceModelCommand command = new GenerateInstanceModelCommand(urn, source, counts, 20);
        command.execute();

        GRLGraph graph = command.getDiagram();
        assertEquals(3, graph.getConnections().size());
        for (Object o : graph.getConnections()) {
            LinkRef copyRef = (LinkRef) o;
            IntentionalElementRef copySrc = (IntentionalElementRef) copyRef.getSource();
            IntentionalElementRef copyTgt = (IntentionalElementRef) copyRef.getTarget();
            assertTrue(copyRef.getLink() instanceof ElementLink);
            // same-actor copies stay within one instance
            assertEquals(copySrc.getContRef(), copyTgt.getContRef());
            // the copy got its own id and a name: isReuseLink calls getName().substring(0,5),
            // so an unnamed link would throw
            assertNotNull(copyRef.getLink().getName());
            assertNotNull(copyRef.getLink().getId());
            assertFalse(copyRef.getLink().getId().equals(contrib.getId()));
            assertFalse(ReusedElementUtil.isReuseLink(copyRef.getLink()));

            if (copyRef.getLink() instanceof Contribution) {
                Contribution c = (Contribution) copyRef.getLink();
                assertEquals(contrib.getContribution(), c.getContribution());
                assertEquals(contrib.getQuantitativeContribution(), c.getQuantitativeContribution());
                assertTrue(c.isCorrelation());
                // strength text/icon is rendered from the connection label the editor adds
                assertTrue("contribution LinkRef copy must carry a ConnectionLabel",
                        copyRef.getLabel() instanceof ConnectionLabel);
                assertEquals(30, ((ConnectionLabel) copyRef.getLabel()).getDeltaX());
                assertEquals(-30, ((ConnectionLabel) copyRef.getLabel()).getDeltaY());
            }
            assertNotSame(contribRef.getLink(), copyRef.getLink());
            assertNotSame(decompRef.getLink(), copyRef.getLink());
            assertNotSame(depRef.getLink(), copyRef.getLink());
        }
    }

    @Test
    public void markerIsVisibleWhenGraphEntersSpecDiagrams() {
        GRLGraph source = newGraph("TypeModel");
        ActorRef actorA = addActor(source, "A", 100, 100);
        addIE(source, actorA, "G1", IntentionalElementType.GOAL_LITERAL, 10, 20);

        final boolean[] markedAtAdd = new boolean[1];
        urn.getUrndef().eAdapters().add(new Adapter() {
            public void notifyChanged(Notification notification) {
                if (notification.getEventType() == Notification.ADD && notification.getNewValue() instanceof GRLGraph)
                    markedAtAdd[0] = GenerateInstanceModelCommand.isInstanceModel((GRLGraph) notification.getNewValue());
            }

            public Notifier getTarget() {
                return null;
            }

            public void setTarget(Notifier newTarget) {
            }

            public boolean isAdapterForType(Object type) {
                return false;
            }
        });

        Map<ActorRef, Integer> counts = new LinkedHashMap<ActorRef, Integer>();
        counts.put(actorA, Integer.valueOf(1));
        GenerateInstanceModelCommand command = new GenerateInstanceModelCommand(urn, source, counts, 20);
        command.execute();
        // the Outline realizes the graph's tree node synchronously on this same ADD
        // notification; if the marker is not there yet, the node latches the plain GRL icon
        assertTrue("instance marker must already be set when the graph enters the spec diagrams", markedAtAdd[0]);
    }

    @Test
    public void outlineEditPartSwitchesIconForInstanceModel() {
        GRLGraph source = newGraph("TypeModel");
        ActorRef actorA = addActor(source, "A", 100, 100);
        addIE(source, actorA, "G1", IntentionalElementType.GOAL_LITERAL, 10, 20);

        Map<ActorRef, Integer> counts = new LinkedHashMap<ActorRef, Integer>();
        counts.put(actorA, Integer.valueOf(1));
        GenerateInstanceModelCommand command = new GenerateInstanceModelCommand(urn, source, counts, 20);
        command.execute();
        GRLGraph instance = command.getDiagram();

        // the hierarchical/concerns outline trees render graph nodes through
        // GrlGraphTreeEditPart.getImage(), so the branch must flip on the instance marker
        assertEquals(JUCMNavPlugin.getImage("icons/grl16.gif"), new TestGrlGraphTreeEditPart(source).getImage());
        assertEquals(JUCMNavPlugin.getImage(JUCMNavPlugin.getInstanceIconDescriptor()),
                new TestGrlGraphTreeEditPart(instance).getImage());
    }

    @Test
    public void instanceMarkerSurvivesSaveAndLoad() throws Exception {
        GRLGraph source = newGraph("TypeModel");
        ActorRef actorA = addActor(source, "A", 100, 100);
        addIE(source, actorA, "G1", IntentionalElementType.GOAL_LITERAL, 10, 20);

        Map<ActorRef, Integer> counts = new LinkedHashMap<ActorRef, Integer>();
        counts.put(actorA, Integer.valueOf(1));
        GenerateInstanceModelCommand command = new GenerateInstanceModelCommand(urn, source, counts, 20);
        command.execute();
        String instanceId = command.getDiagram().getId();

        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("jucm", new XMIResourceFactoryImpl()); //$NON-NLS-1$
        Resource resource = rs.createResource(URI.createURI("roundtrip.jucm")); //$NON-NLS-1$
        resource.getContents().add(urn);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        resource.save(bytes, null);

        ResourceSet rs2 = new ResourceSetImpl();
        rs2.getResourceFactoryRegistry().getExtensionToFactoryMap().put("jucm", new XMIResourceFactoryImpl()); //$NON-NLS-1$
        Resource resource2 = rs2.createResource(URI.createURI("roundtrip.jucm")); //$NON-NLS-1$
        resource2.load(new ByteArrayInputStream(bytes.toByteArray()), null);
        URNspec loaded = (URNspec) resource2.getContents().get(0);

        GRLGraph loadedInstance = null;
        for (Object o : loaded.getUrndef().getSpecDiagrams()) {
            if (o instanceof GRLGraph && instanceId.equals(((GRLGraph) o).getId()))
                loadedInstance = (GRLGraph) o;
        }
        assertNotNull("instance graph must survive the roundtrip", loadedInstance);
        // an instance model loaded from disk must still be recognized, otherwise the Outline
        // (and 'Go to type model') would silently show it as a plain GRL graph
        assertTrue(GenerateInstanceModelCommand.isInstanceModel(loadedInstance));
    }

    private static final class TestGrlGraphTreeEditPart extends GrlGraphTreeEditPart {
        TestGrlGraphTreeEditPart(GRLGraph model) {
            super(model);
        }

        public Image getImage() {
            return super.getImage();
        }
    }
}