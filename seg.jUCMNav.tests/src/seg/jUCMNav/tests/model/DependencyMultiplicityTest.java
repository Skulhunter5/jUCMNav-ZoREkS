package seg.jUCMNav.tests.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.Before;
import org.junit.Test;

import grl.Dependency;
import grl.GrlFactory;
import grl.IntentionalElement;
import grl.IntentionalElementType;
import seg.jUCMNav.model.commands.transformations.ChangeDependencyMultiplicityCommand;
import seg.jUCMNav.model.util.DependencyMultiplicity;
import urn.URNspec;
import urn.UrnFactory;
import urncore.UrncoreFactory;

/**
 * Unit tests for dependency multiplicities: parsing/validation/formatting
 * ({@link DependencyMultiplicity}), the undoable model command
 * ({@link ChangeDependencyMultiplicityCommand}) and the XMI save/load round-trip of the new
 * {@code srcMultiplicity}/{@code destMultiplicity} attributes.
 *
 * <p>
 * Everything is pure model (no workbench), on purpose: the multiplicity feature can be exercised
 * for near-free, exactly like {@code StubExtractionScopeTest} exercises {@code StubExtractionScope}.
 * </p>
 *
 * @author skuly
 */
public class DependencyMultiplicityTest {

    private URNspec urn;

    @Before
    public void setUp() {
        urn = UrnFactory.eINSTANCE.createURNspec();
        urn.setUrndef(UrncoreFactory.eINSTANCE.createURNdefinition());
        urn.setGrlspec(GrlFactory.eINSTANCE.createGRLspec());
    }

    private Dependency newDependency() {
        IntentionalElement src = GrlFactory.eINSTANCE.createIntentionalElement();
        src.setName("Src");
        src.setType(IntentionalElementType.GOAL_LITERAL);
        IntentionalElement dst = GrlFactory.eINSTANCE.createIntentionalElement();
        dst.setName("Dst");
        dst.setType(IntentionalElementType.TASK_LITERAL);
        urn.getGrlspec().getIntElements().add(src);
        urn.getGrlspec().getIntElements().add(dst);
        Dependency dep = GrlFactory.eINSTANCE.createDependency();
        dep.setSrc(src);
        dep.setDest(dst);
        urn.getGrlspec().getLinks().add(dep);
        return dep;
    }

    // ---------------------------------------------------------------- validation matrix

    @Test
    public void validMultiplicities() {
        String[] valid = { "", "[1..2]", "1..*", "*..*", "*..3", "0..2", "1..1", " *..* ", "1 .. 5",
                "2..2" };
        for (String v : valid)
            assertTrue("expected valid: \"" + v + "\"", DependencyMultiplicity.isValid(v));
    }

    @Test
    public void invalidMultiplicities() {
        String[] invalid = { null, "-1..2", "3..1", "1..x", "1", "1..", "..2", "x", "1..-2", "a",
                "1.5..3", "1..2..3" };
        for (String v : invalid)
            assertFalse("expected invalid: \"" + v + "\"", DependencyMultiplicity.isValid(v));
    }

    @Test
    public void starBoundAllowedInEitherPosition() {
        assertTrue(DependencyMultiplicity.isValid("*..3"));
        assertTrue(DependencyMultiplicity.isValid("3..*"));
        assertTrue(DependencyMultiplicity.isValid("*..*"));
    }

    // ---------------------------------------------------------------- formatting

    @Test
    public void normalizeStoredProducesCanonicalForm() {
        assertEquals("", DependencyMultiplicity.normalizeStored(""));
        assertEquals("", DependencyMultiplicity.normalizeStored(null));
        assertEquals("1..2", DependencyMultiplicity.normalizeStored("[1..2]"));
        assertEquals("1..2", DependencyMultiplicity.normalizeStored(" 1 .. 2 "));
        assertEquals("1..*", DependencyMultiplicity.normalizeStored("1..*"));
        assertEquals("*..3", DependencyMultiplicity.normalizeStored(" *..3 "));
        // invalid input is returned unchanged (trimmed)
        assertEquals("invalid", DependencyMultiplicity.normalizeStored(" invalid "));
    }

    @Test
    public void toDisplayRendersBrackets() {
        assertEquals("", DependencyMultiplicity.toDisplay(""));
        assertEquals("", DependencyMultiplicity.toDisplay(null));
        assertEquals("[1..2]", DependencyMultiplicity.toDisplay("1..2"));
        assertEquals("[1..2]", DependencyMultiplicity.toDisplay("[1..2]"));
        assertEquals("[*..*]", DependencyMultiplicity.toDisplay(" * .. * "));
    }

    // ---------------------------------------------------------------- command

    @Test
    public void commandSetsUndoesAndRedoesSourceEnd() {
        Dependency dep = newDependency();
        ChangeDependencyMultiplicityCommand cmd = new ChangeDependencyMultiplicityCommand(dep,
                ChangeDependencyMultiplicityCommand.SOURCE, "[1..2]");
        assertTrue(cmd.canExecute());
        cmd.execute();
        assertEquals("1..2", dep.getSrcMultiplicity());
        cmd.undo();
        assertNull("unset multiplicity defaults to null", dep.getSrcMultiplicity());
        cmd.redo();
        assertEquals("1..2", dep.getSrcMultiplicity());
    }

    @Test
    public void commandTouchesOnlyTheChosenEnd() {
        Dependency dep = newDependency();
        dep.setSrcMultiplicity("1..*");

        ChangeDependencyMultiplicityCommand cmd = new ChangeDependencyMultiplicityCommand(dep,
                ChangeDependencyMultiplicityCommand.TARGET, "0..3");
        cmd.execute();
        assertEquals("1..*", dep.getSrcMultiplicity());
        assertEquals("0..3", dep.getDestMultiplicity());

        cmd.undo();
        assertEquals("1..*", dep.getSrcMultiplicity());
        assertNull("unset multiplicity defaults to null", dep.getDestMultiplicity());
    }

    @Test
    public void commandWithEmptyValueClearsMultiplicity() {
        Dependency dep = newDependency();
        dep.setSrcMultiplicity("2..2");

        ChangeDependencyMultiplicityCommand cmd = new ChangeDependencyMultiplicityCommand(dep,
                ChangeDependencyMultiplicityCommand.SOURCE, "");
        cmd.execute();
        assertEquals("", dep.getSrcMultiplicity());
        cmd.undo();
        assertEquals("2..2", dep.getSrcMultiplicity());
    }

    @Test
    public void commandRejectsUnknownEnd() {
        Dependency dep = newDependency();
        ChangeDependencyMultiplicityCommand cmd = new ChangeDependencyMultiplicityCommand(dep, 42, "[1..2]");
        assertFalse(cmd.canExecute());
    }

    // ---------------------------------------------------------------- save / load round-trip

    @Test
    public void multiplicitySurvivesSaveAndLoad() throws Exception {
        Dependency dep = newDependency();
        dep.setSrcMultiplicity("1..*");
        dep.setDestMultiplicity("*..3");

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

        java.util.List links = loaded.getGrlspec().getLinks();
        assertEquals(1, links.size());
        assertNotNull(links.get(0));
        assertTrue("loaded link must still be a dependency", links.get(0) instanceof Dependency);
        Dependency loadedDep = (Dependency) links.get(0);
        assertEquals("1..*", loadedDep.getSrcMultiplicity());
        assertEquals("*..3", loadedDep.getDestMultiplicity());
    }

    @Test
    public void emptyMultiplicitySurvivesSaveAndLoad() throws Exception {
        Dependency dep = newDependency();

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

        Dependency loadedDep = (Dependency) loaded.getGrlspec().getLinks().get(0);
        assertEquals("", DependencyMultiplicity.normalizeStored(loadedDep.getSrcMultiplicity()));
        assertEquals("", DependencyMultiplicity.normalizeStored(loadedDep.getDestMultiplicity()));
    }
}