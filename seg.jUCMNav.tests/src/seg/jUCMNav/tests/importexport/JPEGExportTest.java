package seg.jUCMNav.tests.importexport;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.junit.Test;

import seg.jUCMNav.importexport.ExportImageJPG;

/**
 * Regression test for the silent-empty-JPEG bug: SWT's GTK implementation encodes
 * JPEG through gdk-pixbuf (gdk_pixbuf_save_to_bufferv) and silently drops the
 * output when the platform has no JPEG loader -- it ignores the return value, so
 * the target file is created but stays 0 bytes with no exception. ExportImageJPG
 * now writes through Java's built-in javax.imageio instead, which depends only on
 * the JDK.
 *
 * Pure query-level test: builds an ImageData (no Display), encodes it to a temp
 * file and asserts the bytes are a real JPEG (SOI/EOI markers).
 */
public class JPEGExportTest {

    @Test
    public void testJPEGExportProducesNonEmptyValidFile() throws IOException {
        int w = 80, h = 60;
        ImageData data = new ImageData(w, h, 32, new PaletteData(0xFF0000, 0xFF00, 0xFF));
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                data.setPixel(x, y, ((x * 255 / w) << 16) | ((y * 255 / h) << 8) | 128);
            }
        }

        File out = File.createTempFile("jUCMNav-jpeg-export-", ".jpg");
        out.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(out)) {
            ExportImageJPG.saveJPEG(data, fos);
        }

        byte[] bytes = Files.readAllBytes(out.toPath());
        assertTrue("JPEG file must not be empty", bytes.length > 0);
        assertTrue("must start with JPEG SOI marker", bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8);
        assertTrue("must end with JPEG EOI marker", bytes[bytes.length - 2] == (byte) 0xFF && bytes[bytes.length - 1] == (byte) 0xD9);
    }
}