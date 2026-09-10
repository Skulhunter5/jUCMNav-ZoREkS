package seg.jUCMNav;

import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import seg.jUCMNav.views.preferences.GeneralPreferencePage;

/**
 * The main plugin class to be used in the desktop.
 */
public class JUCMNavPlugin extends AbstractUIPlugin {
    // The shared instance.
    private static JUCMNavPlugin plugin;

    // Resource bundle.
    private ResourceBundle resourceBundle;

    private static HashMap imgDescriptorFactory;
    private static HashMap imgFactory;
    private static ImageDescriptor instanceIconDescriptor;

    public static final String PLUGIN_ID = "seg.jUCMNav"; //$NON-NLS-1$

    /**
     * The constructor.
     */
    public JUCMNavPlugin() {
        super();
        plugin = this;
        try {
            resourceBundle = ResourceBundle.getBundle("seg.jUCMNav.JUCMNavPluginResources"); //$NON-NLS-1$
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
    }

    /**
     * This method is called upon plug-in activation
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }

    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     */
    public static JUCMNavPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = JUCMNavPlugin.getDefault().getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    public static ImageDescriptor getImageDescriptor(String path) {
        if (imgDescriptorFactory == null)
            imgDescriptorFactory = new HashMap();

        if (!imgDescriptorFactory.containsKey(path)) {
            imgDescriptorFactory.put(path, ImageDescriptor.createFromFile(JUCMNavPlugin.class, path));
        }

        return (ImageDescriptor) imgDescriptorFactory.get(path);
    }

    /**
     * Returns the instance-model icon: a copy of the classic GRL icon whose green fill has been
     * recolored to amber/orange so instance and type model graphs are visually distinct. The icon
     * is derived in memory from the proven-resolvable {@code grl16.gif} (green pixels only; the
     * dark outline, white seams and source transparency are preserved) instead of shipping a
     * separate resource, which keeps it working regardless of how the bundle is packaged (workspace
     * vs. prebuilt jar).
     */
    public static synchronized ImageDescriptor getInstanceIconDescriptor() {
        if (instanceIconDescriptor == null) {
            ImageData src = getImageDescriptor("icons/grl16.gif").getImageData(); //$NON-NLS-1$
            RGB orange = new RGB(255, 180, 0);
            ImageData icon = new ImageData(src.width, src.height, 32,
                    new PaletteData(0xFF0000, 0x00FF00, 0x0000FF));
            for (int y = 0; y < src.height; y++) {
                for (int x = 0; x < src.width; x++) {
                    RGB rgb = src.palette.getRGB(src.getPixel(x, y));
                    boolean greenFill = rgb.green > 110 && rgb.green > rgb.red + 20 && rgb.green > rgb.blue + 20;
                    int v = greenFill ? (orange.red << 16 | orange.green << 8 | orange.blue)
                            : (rgb.red << 16 | rgb.green << 8 | rgb.blue);
                    icon.setPixel(x, y, v);
                    icon.setAlpha(x, y, src.getAlpha(x, y));
                }
            }
            instanceIconDescriptor = ImageDescriptor.createFromImageData(icon);
        }
        return instanceIconDescriptor;
    }

    public static Image getImage(String path) {
        if (imgFactory == null)
            imgFactory = new HashMap();

        if (!imgFactory.containsKey(path)) {
            imgFactory.put(path, ImageDescriptor.createFromFile(JUCMNavPlugin.class, path).createImage());
        }

        return ((Image) imgFactory.get(path));
    }

    public static Image getImage(ImageDescriptor descriptor) {
        if (imgFactory == null)
            imgFactory = new HashMap();
        if (!imgFactory.containsKey(descriptor)) {
            imgFactory.put(descriptor, descriptor.createImage());
        }

        return ((Image) imgFactory.get(descriptor));
    }

    public static boolean isInDebug() {
        return (GeneralPreferencePage.getAuthor() != null && "debug".equalsIgnoreCase(GeneralPreferencePage.getAuthor())); //$NON-NLS-1$
    }

}
