/**
 * 
 */
package seg.jUCMNav.model.commands.create;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.gef.commands.Command;

import grl.Actor;
import grl.ActorRef;
import grl.CollapsedActorRef;
import grl.Contribution;
import grl.Decomposition;
import grl.Dependency;
import grl.ElementLink;
import grl.GRLGraph;
import grl.GrlFactory;
import grl.IntentionalElement;
import grl.IntentionalElementRef;
import grl.LinkRef;
import grl.kpimodel.Indicator;
import grl.kpimodel.KpimodelFactory;
import fm.Feature;
import fm.FmFactory;
import seg.jUCMNav.Messages;
import seg.jUCMNav.model.ModelCreationFactory;
import seg.jUCMNav.model.commands.IGlobalStackCommand;
import seg.jUCMNav.model.commands.JUCMNavCommand;
import seg.jUCMNav.model.util.MetadataHelper;
import seg.jUCMNav.model.util.URNNamingHelper;
import seg.jUCMNav.strategies.EvaluationStrategyManager;
import urn.URNspec;
import urncore.Concern;
import urncore.URNmodelElement;
import urncore.IURNContainerRef;
import urncore.IURNNode;
import urncore.IURNDiagram;

/**
 * Generates a new GRL graph that is an instance model of the given type-model graph.
 * 
 * <p>
 * For every actor of the source graph the user supplies a count; the generated graph contains
 * that many independent copies of the actor and of each of its intentional elements. Each copy
 * is named "i: name" (where {@code i} starts at 1). Links between intentional elements of the
 * same actor are replicated in every copy; links between elements of different actors (GRL
 * dependencies) are not supported yet and are rejected by {@link #analyze(GRLGraph)}.
 * </p>
 * 
 * <p>
 * The generated graph is disconnected from the source model: every definition is a fresh object
 * with its own id, so editing or evaluating one model never affects the other. The copies are
 * laid out in rows, one row per source actor, with each instance placed in a cell the size of
 * its actor; the margin (both horizontal and vertical) is configurable.
 * </p>
 * 
 * @author skuly
 */
public class GenerateInstanceModelCommand extends Command implements JUCMNavCommand, IGlobalStackCommand {

    /**
     * Default spacing between rows and between actor cells. The generate dialog preselects this
     * value; change it here to adjust the default everywhere.
     */
    public static final int DEFAULT_MARGIN = 80;

    /**
     * Metadata key set on every generated graph. A graph carrying it is an instance model, not a
     * type model: instance models cannot be instantiated again.
     */
    public static final String INSTANCE_MODEL = "jucmnav.instanceModel"; //$NON-NLS-1$

    /**
     * Metadata key holding the id of the type-model graph the instance model was generated from.
     * This is what "go to type model" resolves against.
     */
    public static final String INSTANCE_MODEL_SOURCE = "jucmnav.instanceModelSource"; //$NON-NLS-1$

    /**
     * Extra space added around an actor's elements when computing the size of its cell, so the
     * drawn actor (an oval around its bounds) does not clip its children.
     */
    private static final int ACTOR_PADDING = 6;

    public enum GenerationProblem {
        NONE, NESTED_ACTOR, FREE_FLOATING_IE, DEPENDENCY, NO_ACTOR
    }

    private URNspec urn;
    private GRLGraph source;
    private Map<ActorRef, Integer> counts;
    private int margin;

    private GRLGraph graph;
    private int index;

    private List<Actor> createdActors = new ArrayList<Actor>();
    private List<IntentionalElement> createdIntElements = new ArrayList<IntentionalElement>();
    private List<ElementLink> createdLinks = new ArrayList<ElementLink>();

    private Map<ActorRef, List<Actor>> actorCopies = new LinkedHashMap<ActorRef, List<Actor>>();
    private Map<IntentionalElementRef, List<IntentionalElement>> intElementCopies = new LinkedHashMap<IntentionalElementRef, List<IntentionalElement>>();
    private Map<IntentionalElementRef, List<IntentionalElementRef>> refCopies = new LinkedHashMap<IntentionalElementRef, List<IntentionalElementRef>>();

    /**
     * @param urn
     *            the model the generated graph is added to
     * @param source
     *            the type-model graph being instantiated
     * @param counts
     *            how many copies must be generated for each actor of the source graph
     * @param margin
     *            spacing (in pixels) between actor rows and between actor cells
     */
    public GenerateInstanceModelCommand(URNspec urn, GRLGraph source, Map<ActorRef, Integer> counts, int margin) {
        this.urn = urn;
        this.source = source;
        this.counts = counts;
        this.margin = margin;

        // must be created here for getDiagram()/getAffectedDiagram() to work before execute()
        graph = (GRLGraph) ModelCreationFactory.getNewObject(urn, GRLGraph.class);
        setLabel(Messages.getString("GenerateInstanceModelCommand.label")); //$NON-NLS-1$
    }

    /**
     * Static analysis of a GRL graph: is it a valid type model for instantiation?
     * 
     * @return {@link GenerationProblem#NONE} if the graph can be instantiated, otherwise the first
     *         problem found. Precedence: nested actors, free-floating intentional elements,
     *         dependencies (WIP), no actors.
     */
    public static GenerationProblem analyze(GRLGraph graph) {
        if (graph == null)
            return GenerationProblem.NO_ACTOR;

        EList contRefs = graph.getContRefs();

        // Nested (included/collapsed) actors are not supported in type/instance models.
        for (Object o : contRefs) {
            IURNContainerRef ref = (IURNContainerRef) o;
            if (ref instanceof CollapsedActorRef)
                return GenerationProblem.NESTED_ACTOR;
            if (ref.getParent() != null)
                return GenerationProblem.NESTED_ACTOR;
            if (ref.getContDef() instanceof Actor && !((Actor) ref.getContDef()).getIncludedActors().isEmpty())
                return GenerationProblem.NESTED_ACTOR;
        }
        for (Object o : graph.getNodes()) {
            if (o instanceof CollapsedActorRef)
                return GenerationProblem.NESTED_ACTOR;
            if (o instanceof IntentionalElementRef && ((IntentionalElementRef) o).getContRef() == null)
                return GenerationProblem.FREE_FLOATING_IE;
        }

        // Cross-actor links (dependencies) are supported by the model but instantiation is WIP.
        for (Object o : graph.getConnections()) {
            if (!(o instanceof LinkRef))
                continue;
            LinkRef ref = (LinkRef) o;
            IURNNode sourceNode = ref.getSource();
            IURNNode targetNode = ref.getTarget();
            if (!(sourceNode instanceof IntentionalElementRef) || !(targetNode instanceof IntentionalElementRef))
                return GenerationProblem.DEPENDENCY;
            if (((IntentionalElementRef) sourceNode).getContRef() != ((IntentionalElementRef) targetNode).getContRef())
                return GenerationProblem.DEPENDENCY;
        }

        if (contRefs.isEmpty())
            return GenerationProblem.NO_ACTOR;

        return GenerationProblem.NONE;
    }

    /**
     * @return {@code true} if the graph is an instance model (generated by this feature).
     */
    public static boolean isInstanceModel(GRLGraph graph) {
        return graph != null
                && Boolean.parseBoolean(MetadataHelper.getMetaData(graph, INSTANCE_MODEL));
    }

    /**
     * Resolves the type-model graph an instance model was generated from, by the id stored at
     * generation time.
     * 
     * @param urn
     *            the model instance graph lives in
     * @param graph
     *            an instance model generated by {@link GenerateInstanceModelCommand}
     * @return the type model, or {@code null} if {@code graph} is not an instance model or its
     *         source cannot be found
     */
    public static GRLGraph getTypeModel(URNspec urn, GRLGraph graph) {
        if (!isInstanceModel(graph) || urn == null || urn.getUrndef() == null)
            return null;
        String sourceId = MetadataHelper.getMetaData(graph, INSTANCE_MODEL_SOURCE);
        if (sourceId == null)
            return null;
        for (Object o : urn.getUrndef().getSpecDiagrams()) {
            if (!(o instanceof GRLGraph))
                continue;
            GRLGraph candidate = (GRLGraph) o;
            // never resolve to the graph itself, even if a stale id looped back
            if (candidate != graph && sourceId.equals(candidate.getId()))
                return candidate;
        }
        return null;
    }

    /**
     * @see org.eclipse.gef.commands.Command#canExecute()
     */
    public boolean canExecute() {
        if (urn == null || urn.getUrndef() == null || source == null || graph == null)
            return false;
        if (analyze(source) != GenerationProblem.NONE)
            return false;
        if (counts == null || counts.isEmpty())
            return false;
        for (ActorRef actorRef : counts.keySet()) {
            Integer count = counts.get(actorRef);
            if (count == null || count.intValue() < 1)
                return false;
            if (!source.getContRefs().contains(actorRef))
                return false;
        }
        return true;
    }

    /**
     * @see org.eclipse.gef.commands.Command#execute()
     */
    public void execute() {
        index = Math.max(0, urn.getUrndef().getSpecDiagrams().indexOf(source) + 1);
        redo();
    }

    public GRLGraph getDiagram() {
        return graph;
    }

    /**
     * Builds the instance graph from scratch. Undo empties the generated objects so this method
     * can be called again after an undo.
     * 
     * @see org.eclipse.gef.commands.Command#redo()
     */
    public void redo() {
        testPreConditions();

        clearCreatedObjects();

        // fresh id + name for the generated graph
        graph.setId(""); //$NON-NLS-1$
        URNNamingHelper.setElementNameAndID(urn, graph);
        graph.setName(source.getName() + " IM-" + countList()); //$NON-NLS-1$

        buildActorCopies();
        buildLinks();

        // record the origin and mark this as an instance model BEFORE the graph becomes visible;
        // the outline realizes tree nodes synchronously on the specDiagrams.add notification, and
        // its edit part caches the first icon it computes
        MetadataHelper.addMetaData(urn, graph, INSTANCE_MODEL, "true"); //$NON-NLS-1$
        MetadataHelper.addMetaData(urn, graph, INSTANCE_MODEL_SOURCE, source.getId()); //$NON-NLS-1$

        urn.getUrndef().getSpecDiagrams().add(index, graph);

        String value = MetadataHelper.getMetaData(urn, "CoURN"); //$NON-NLS-1$
        if (value != null && value.equals("true") && urn.getUrndef().getConcerns().size() > 0) { //$NON-NLS-1$
            Concern tempConcern = (Concern) urn.getUrndef().getConcerns().get(0);
            tempConcern.getSpecDiagrams().add(getDiagram());
        }

        EvaluationStrategyManager.getInstance().calculateEvaluation();

        testPostConditions();
    }

    /**
     * @see org.eclipse.gef.commands.Command#undo()
     */
    public void undo() {
        testPostConditions();

        urn.getUrndef().getSpecDiagrams().remove(graph);

        // refs, nodes and link refs are contained in the graph; empty it and let the opposite
        // references clean themselves up
        graph.getContRefs().clear();
        graph.getNodes().clear();
        graph.getConnections().clear();

        for (Actor actor : createdActors)
            urn.getGrlspec().getActors().remove(actor);
        for (IntentionalElement element : createdIntElements)
            urn.getGrlspec().getIntElements().remove(element);
        for (ElementLink link : createdLinks)
            urn.getGrlspec().getLinks().remove(link);

        testPreConditions();
    }

    /**
     * @see seg.jUCMNav.model.commands.IGlobalStackCommand#getAffectedDiagram()
     */
    public IURNDiagram getAffectedDiagram() {
        return getDiagram();
    }

    public String countList() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ActorRef actorRef : (EList<ActorRef>) source.getContRefs()) {
            int count = 1;
            if (counts != null && counts.get(actorRef) != null)
                count = counts.get(actorRef).intValue();
            if (!first)
                sb.append("-"); //$NON-NLS-1$
            sb.append(count);
            first = false;
        }
        return sb.toString();
    }

    private void clearCreatedObjects() {
        createdActors.clear();
        createdIntElements.clear();
        createdLinks.clear();
        actorCopies.clear();
        intElementCopies.clear();
        refCopies.clear();
    }

    private void buildActorCopies() {
        int rowY = 0;
        for (ActorRef actorRef : (EList<ActorRef>) source.getContRefs()) {
            int count = 1;
            if (counts != null && counts.get(actorRef) != null)
                count = counts.get(actorRef).intValue();

            // the cell is sized to the actor: the rectangle enclosing its position and elements
            int minX = actorRef.getX();
            int minY = actorRef.getY();
            int maxX = actorRef.getX();
            int maxY = actorRef.getY();
            int ax = actorRef.getX();
            int ay = actorRef.getY();
            for (Object child : actorRef.getNodes()) {
                IURNNode node = (IURNNode) child;
                int width = 0;
                int height = 0;
                if (node instanceof URNmodelElement) {
                    width = MetadataHelper.getIntMetaData((URNmodelElement) node, MetadataHelper.WIDTH, 0);
                    height = MetadataHelper.getIntMetaData((URNmodelElement) node, MetadataHelper.HEIGHT, 0);
                }
                minX = Math.min(minX, node.getX());
                minY = Math.min(minY, node.getY());
                maxX = Math.max(maxX, node.getX() + width);
                maxY = Math.max(maxY, node.getY() + height);
            }
            minX -= ACTOR_PADDING;
            minY -= ACTOR_PADDING;
            maxX += ACTOR_PADDING;
            maxY += ACTOR_PADDING;
            // prefer the actor's own size from the source graph so the copies match the type
            // model; fall back to the bounding box of its elements when the size was never set
            int actorWidth = actorRef.getWidth() > 0 ? actorRef.getWidth() : Math.max(1, maxX - minX);
            int actorHeight = actorRef.getHeight() > 0 ? actorRef.getHeight() : Math.max(1, maxY - minY);
            int cellWidth = actorWidth + margin;
            int cellHeight = actorHeight + margin;

            actorCopies.put(actorRef, new ArrayList<Actor>());

            for (int i = 1; i <= count; i++) {
                int originX = (i - 1) * cellWidth;
                int originY = rowY;

                Actor actor = copyActor((Actor) actorRef.getContDef(), i);
                actorCopies.get(actorRef).add(actor);

                ActorRef newActorRef = (ActorRef) GrlFactory.eINSTANCE.createActorRef();
                newActorRef.setContDef(actor);
                newActorRef.setLabel(urncore.UrncoreFactory.eINSTANCE.createComponentLabel());
                newActorRef.setX(originX);
                newActorRef.setY(originY);
                newActorRef.setWidth(actorWidth);
                newActorRef.setHeight(actorHeight);
                URNNamingHelper.setElementNameAndID(urn, newActorRef);
                newActorRef.setName(i + ": " + URNNamingHelper.getName(actorRef)); //$NON-NLS-1$
                graph.getContRefs().add(newActorRef);

                for (int c = 0; c < actorRef.getNodes().size(); c++) {
                    IURNNode child = (IURNNode) actorRef.getNodes().get(c);
                    if (!(child instanceof IntentionalElementRef))
                        continue;
                    IntentionalElementRef sourceRef = (IntentionalElementRef) child;
                    IntentionalElement element = copyIntentionalElement(sourceRef.getDef(), i);

                    IntentionalElementRef newRef = (IntentionalElementRef) GrlFactory.eINSTANCE.createIntentionalElementRef();
                    newRef.setDef(element);
                    newRef.setX(originX + (sourceRef.getX() - ax));
                    newRef.setY(originY + (sourceRef.getY() - ay));
                    URNNamingHelper.setElementNameAndID(urn, newRef);
                    newRef.setName(i + ": " + URNNamingHelper.getName(sourceRef)); //$NON-NLS-1$
                    copyWidthHeightMetadata(sourceRef, newRef);
                    graph.getNodes().add(newRef);
                    newActorRef.getNodes().add(newRef);

                    List<IntentionalElement> copies = intElementCopies.get(sourceRef);
                    if (copies == null) {
                        copies = new ArrayList<IntentionalElement>();
                        intElementCopies.put(sourceRef, copies);
                    }
                    copies.add(element);

                    List<IntentionalElementRef> refs = refCopies.get(sourceRef);
                    if (refs == null) {
                        refs = new ArrayList<IntentionalElementRef>();
                        refCopies.put(sourceRef, refs);
                    }
                    refs.add(newRef);
                }
            }

            rowY += cellHeight;
        }
    }

    private void buildLinks() {
        for (Object connection : source.getConnections()) {
            if (!(connection instanceof LinkRef))
                continue;
            LinkRef sourceRef = (LinkRef) connection;
            if (!(sourceRef.getSource() instanceof IntentionalElementRef) || !(sourceRef.getTarget() instanceof IntentionalElementRef))
                continue;
            IntentionalElementRef sourceEnd = (IntentionalElementRef) sourceRef.getSource();
            IntentionalElementRef targetEnd = (IntentionalElementRef) sourceRef.getTarget();
            if (sourceEnd.getContRef() != targetEnd.getContRef())
                continue;

            List<IntentionalElement> sourceCopies = intElementCopies.get(sourceEnd);
            List<IntentionalElement> targetCopies = intElementCopies.get(targetEnd);
            List<IntentionalElementRef> sourceRefCopies = refCopies.get(sourceEnd);
            List<IntentionalElementRef> targetRefCopies = refCopies.get(targetEnd);
            if (sourceCopies == null || targetCopies == null || sourceRefCopies == null || targetRefCopies == null
                    || sourceCopies.size() != targetCopies.size() || sourceRefCopies.size() != targetRefCopies.size())
                continue;
            int copyCount = sourceCopies.size();

            for (int i = 0; i < copyCount; i++) {
                ElementLink link = copyElementLink(sourceRef.getLink());
                sourceCopies.get(i).getLinksSrc().add(link);
                targetCopies.get(i).getLinksDest().add(link);
                urn.getGrlspec().getLinks().add(link);
                createdLinks.add(link);

                LinkRef linkRef = (LinkRef) GrlFactory.eINSTANCE.createLinkRef();
                linkRef.setLink(link);
                linkRef.setSource(sourceRefCopies.get(i));
                linkRef.setTarget(targetRefCopies.get(i));
                if (link instanceof Contribution) {
                    // as AddLinkRefCommand does: the connection label is what displays the
                    // contribution strength next to the arrow
                    urncore.ConnectionLabel label = urncore.UrncoreFactory.eINSTANCE.createConnectionLabel();
                    label.setDeltaX(30);
                    label.setDeltaY(-30);
                    linkRef.setLabel(label);
                }
                graph.getConnections().add(linkRef);
            }
        }
    }

    /**
     * Copies the definition-level attributes of an actor into a brand new {@code Actor}.
     */
    private Actor copyActor(grl.GRLLinkableElement sourceDef, int instanceIndex) {
        Actor sourceActor = (Actor) sourceDef;
        Actor actor = GrlFactory.eINSTANCE.createActor();
        actor.setImportance(sourceActor.getImportance());
        actor.setImportanceQuantitative(sourceActor.getImportanceQuantitative());
        actor.setLineColor(sourceActor.getLineColor());
        actor.setFillColor(sourceActor.getFillColor());
        actor.setFilled(sourceActor.isFilled());
        actor.setId(""); //$NON-NLS-1$
        URNNamingHelper.setElementNameAndID(urn, actor);
        actor.setName(instanceIndex + ": " + sourceActor.getName()); //$NON-NLS-1$
        urn.getGrlspec().getActors().add(actor);
        createdActors.add(actor);
        return actor;
    }

    /**
     * Copies the definition-level attributes of an intentional element into a brand new
     * definition of the same concrete type.
     */
    private IntentionalElement copyIntentionalElement(IntentionalElement sourceElement, int instanceIndex) {
        IntentionalElement element;
        if (sourceElement instanceof Indicator)
            element = KpimodelFactory.eINSTANCE.createIndicator();
        else if (sourceElement instanceof Feature)
            element = FmFactory.eINSTANCE.createFeature();
        else
            element = GrlFactory.eINSTANCE.createIntentionalElement();

        element.setType(sourceElement.getType());
        element.setDecompositionType(sourceElement.getDecompositionType());
        element.setImportance(sourceElement.getImportance());
        element.setImportanceQuantitative(sourceElement.getImportanceQuantitative());
        element.setLineColor(sourceElement.getLineColor());
        element.setFillColor(sourceElement.getFillColor());
        element.setFilled(sourceElement.isFilled());
        element.setId(""); //$NON-NLS-1$
        URNNamingHelper.setElementNameAndID(urn, element);
        element.setName(instanceIndex + ": " + sourceElement.getName()); //$NON-NLS-1$
        urn.getGrlspec().getIntElements().add(element);
        createdIntElements.add(element);
        return element;
    }

    /**
     * Creates a fresh definition-side link of the same concrete class, copying every attribute
     * the tool stores on it. The copy gets its own id and name (like {@code ModelCreationFactory}):
     * several parts of the tool call {@code ElementLink.getName()} unconditionally, so an unnamed
     * copy would throw.
     */
    private ElementLink copyElementLink(ElementLink sourceLink) {
        ElementLink copy;
        if (sourceLink instanceof Contribution) {
            Contribution source = (Contribution) sourceLink;
            Contribution contribution = GrlFactory.eINSTANCE.createContribution();
            contribution.setContribution(source.getContribution());
            contribution.setQuantitativeContribution(source.getQuantitativeContribution());
            contribution.setCorrelation(source.isCorrelation());
            copy = contribution;
        } else if (sourceLink instanceof Decomposition) {
            copy = GrlFactory.eINSTANCE.createDecomposition();
        } else if (sourceLink instanceof Dependency) {
            copy = GrlFactory.eINSTANCE.createDependency();
        } else {
            throw new IllegalArgumentException("Unsupported GRL link type: " + sourceLink.getClass().getName()); //$NON-NLS-1$
        }
        copy.setId(""); //$NON-NLS-1$
        URNNamingHelper.setElementNameAndID(urn, copy);
        return copy;
    }

    private void copyWidthHeightMetadata(IntentionalElementRef source, IntentionalElementRef target) {
        int width = MetadataHelper.getIntMetaData(source, MetadataHelper.WIDTH, 0);
        int height = MetadataHelper.getIntMetaData(source, MetadataHelper.HEIGHT, 0);
        if (width > 0)
            MetadataHelper.addMetaData(urn, target, MetadataHelper.WIDTH, String.valueOf(width));
        if (height > 0)
            MetadataHelper.addMetaData(urn, target, MetadataHelper.HEIGHT, String.valueOf(height));
    }

    public void testPreConditions() {
        assert urn != null && urn.getUrndef() != null && urn.getGrlspec() != null : "pre: model missing"; //$NON-NLS-1$
        assert source != null && graph != null : "pre: graph missing"; //$NON-NLS-1$
        assert !urn.getUrndef().getSpecDiagrams().contains(graph) : "pre: graph already in model"; //$NON-NLS-1$
        assert analyze(source) == GenerationProblem.NONE : "pre: source graph cannot be instantiated"; //$NON-NLS-1$
    }

    public void testPostConditions() {
        assert urn != null && urn.getUrndef() != null && urn.getGrlspec() != null : "post: model missing"; //$NON-NLS-1$
        assert graph != null : "post: graph missing"; //$NON-NLS-1$
        assert urn.getUrndef().getSpecDiagrams().contains(graph) : "post: graph not in model"; //$NON-NLS-1$
        assert graph.getContRefs().size() == totalCopies() : "post: wrong number of actor copies"; //$NON-NLS-1$
        assert createdIntElements.size() == totalIntElementCopies() : "post: wrong number of IE copies"; //$NON-NLS-1$
    }

    public int totalCopies() {
        int total = 0;
        for (ActorRef actorRef : (EList<ActorRef>) source.getContRefs()) {
            int count = 1;
            if (counts != null && counts.get(actorRef) != null)
                count = counts.get(actorRef).intValue();
            total += count;
        }
        return total;
    }

    public int totalIntElementCopies() {
        int total = 0;
        for (ActorRef actorRef : (EList<ActorRef>) source.getContRefs()) {
            int count = 1;
            if (counts != null && counts.get(actorRef) != null)
                count = counts.get(actorRef).intValue();
            for (Object child : actorRef.getNodes()) {
                if (child instanceof IntentionalElementRef)
                    total += count;
            }
        }
        return total;
    }
}