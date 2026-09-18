package seg.jUCMNav.tests.figures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import grl.GrlFactory;
import grl.IntentionalElement;
import seg.jUCMNav.JUCMNavPlugin;
import seg.jUCMNav.figures.util.UrnMetadata;
import seg.jUCMNav.strategies.EvaluationStrategyManager;
import seg.jUCMNav.views.preferences.GeneralPreferencePage;
import urn.URNspec;
import urn.UrnFactory;
import urncore.Metadata;
import urncore.UrncoreFactory;

/**
 * The pilcrow (¶) that jUCMNav appends to labels marks the presence of
 * non-stereotype, non-runtime metadata. The Switch Model Language action stores
 * every element's previous name and description in {@code AltName} /
 * {@code AltDescription} metadata, so those must not count as "other metadata"
 * or every element would be labelled with a ¶ after one toggle -- and switching
 * back never removes the metadata, so the ¶ would be permanent.
 *
 * <p>
 * Pure model, no workbench: {@link UrnMetadata} only reads the metadata list and
 * a preference, so an {@link IntentionalElement} created straight through the
 * GRL factory is enough to exercise both label functions.
 *
 * @author Claude
 */
public class UrnMetadataIndicatorTest {

    private URNspec urn;
    private IntentionalElement elem;
    private IPreferenceStore prefStore;
    private boolean originalIndicatorVisible;

    @Before
    public void setUp() {
        urn = UrnFactory.eINSTANCE.createURNspec();
        urn.setUrndef(UrncoreFactory.eINSTANCE.createURNdefinition());
        urn.setGrlspec(GrlFactory.eINSTANCE.createGRLspec());
        elem = GrlFactory.eINSTANCE.createIntentionalElement();
        urn.getGrlspec().getIntElements().add(elem);

        prefStore = JUCMNavPlugin.getDefault().getPreferenceStore();
        originalIndicatorVisible = prefStore.getBoolean(GeneralPreferencePage.PREF_METADATAINDVISIBLE);
        prefStore.setValue(GeneralPreferencePage.PREF_METADATAINDVISIBLE, true);
    }

    @After
    public void tearDown() {
        prefStore.setValue(GeneralPreferencePage.PREF_METADATAINDVISIBLE, originalIndicatorVisible);
    }

    private void addMetadata(String name, String value) {
        Metadata metadata = UrncoreFactory.eINSTANCE.createMetadata();
        metadata.setName(name);
        metadata.setValue(value);
        elem.getMetadata().add(metadata);
    }

    private String allStereotypes() {
        return UrnMetadata.getAllStereotypes(elem, elem);
    }

    // ------------------------------------------------------------------ the fix

    /** AltName on its own is bilingual-model bookkeeping, not metadata to signal. */
    @Test
    public void altNameAloneNeverShowsPilcrow() {
        addMetadata("AltName", "mon nom fran\u00e7ais");
        assertEquals("", UrnMetadata.getStereotypes(elem));
        assertEquals("", allStereotypes());
    }

    /** AltDescription likewise. */
    @Test
    public void altDescriptionAloneNeverShowsPilcrow() {
        addMetadata("AltDescription", "autre texte");
        assertEquals("", UrnMetadata.getStereotypes(elem));
        assertEquals("", allStereotypes());
    }

    /** Both together -- the full state left by a Switch Model Language toggle. */
    @Test
    public void bothBilingualMetadataNeverShowsPilcrow() {
        addMetadata("AltName", "a");
        addMetadata("AltDescription", "b");
        assertEquals("", UrnMetadata.getStereotypes(elem));
        assertEquals("", allStereotypes());
    }

    // ------------------------------------------------- preserved label behaviour

    /** Runtime evaluation metadata was already excluded from the indicator. */
    @Test
    public void runtimeEvalMetadataAloneNeverShowsPilcrow() {
        addMetadata(EvaluationStrategyManager.METADATA_NUMEVAL, "0");
        assertEquals("", UrnMetadata.getStereotypes(elem));
        assertEquals("", allStereotypes());
    }

    /** Stereotypes render as guillemets, and alone cause no pilcrow. */
    @Test
    public void stereotypeAloneShowsGuillemetsButNoPilcrow() {
        addMetadata("STQuality", "good");
        String labels = UrnMetadata.getStereotypes(elem);
        assertTrue("stereotype should render in guillemets", labels.contains("good"));
        assertFalse("a lone stereotype is not an 'other metadata' indicator", labels.contains(UrnMetadata.METADATA_PRESENCE));
    }

    // ------------------------------------------------------ the indicator itself

    /** Real, arbitrary metadata is exactly what the pilcrow exists to signal. */
    @Test
    public void ordinaryMetadataShowsPilcrow() {
        addMetadata("anythingTheUserWrote", "hello");
        assertTrue(UrnMetadata.getStereotypes(elem).contains(UrnMetadata.METADATA_PRESENCE));
        assertTrue(allStereotypes().contains(UrnMetadata.METADATA_PRESENCE));
    }

    /** Bilingual bookkeeping must not mask genuine metadata the user added. */
    @Test
    public void genuineMetadataAlongsideBilingualStillShowsPilcrow() {
        addMetadata("AltName", "a");
        addMetadata("anythingTheUserWrote", "hello");
        assertTrue(UrnMetadata.getStereotypes(elem).contains(UrnMetadata.METADATA_PRESENCE));
        assertTrue(allStereotypes().contains(UrnMetadata.METADATA_PRESENCE));
    }

    /** The "Show metadata indicators" preference must still gate everything. */
    @Test
    public void pilcrowSuppressedWhenIndicatorPreferenceDisabled() {
        addMetadata("anythingTheUserWrote", "hello");
        prefStore.setValue(GeneralPreferencePage.PREF_METADATAINDVISIBLE, false);
        assertEquals("", UrnMetadata.getStereotypes(elem));
        assertEquals("", allStereotypes());
    }
}