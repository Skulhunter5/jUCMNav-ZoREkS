package seg.jUCMNav.importexport;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;

import seg.jUCMNav.importexport.reports.utils.ReportUtils;

/**
 * Exports as JPEG file. Implementation used in Eclipse 3.1 doesn't allow variable quality.
 * 
 * @author jkealey
 * 
 */
public class ExportImageJPG extends ExportImage {

    /**
     * SWT's GTK implementation writes JPEGs through gdk-pixbuf
     * (gdk_pixbuf_save_to_bufferv) and silently drops the output when no JPEG loader is
     * installed -- it discards the method's return value, so the file comes out empty
     * without any exception. Encode through Java's built-in ImageIO instead, which needs
     * nothing but the JDK.
     */
    public static void saveJPEG(ImageData imageData, FileOutputStream fos) {
        BufferedImage bufferedImage = ReportUtils.SWTimageToAWTImage(imageData);
        try {
            if (!ImageIO.write(bufferedImage, "jpeg", fos)) {
                throw new IOException("No JPEG writer available");
            }
            fos.flush();
        } catch (IOException e) {
            throw jpegError(e);
        }
    }

    private static SWTException jpegError(IOException cause) {
        SWTException ex = new SWTException(SWT.ERROR_IO, cause.getMessage());
        ex.initCause(cause);
        return ex;
    }

    /** Convenience overload matching ImageLoader.save(String, int). */
    public static void saveJPEG(ImageData imageData, String outPath) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outPath);
            saveJPEG(imageData, fos);
        } catch (IOException e) {
            throw jpegError(e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    throw jpegError(e);
                }
            }
        }
    }

    /**
     * Returns SWT.IMAGE_JPEG
     * 
     * @see seg.jUCMNav.importexport.ExportImage#getType()
     */
    public int getType() {
        return SWT.IMAGE_JPEG;
    }

    @Override
    protected void saveImage(ImageData imageData, FileOutputStream fos) {
        saveJPEG(imageData, fos);
    }

}
