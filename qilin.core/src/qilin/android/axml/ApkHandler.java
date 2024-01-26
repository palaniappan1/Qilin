package qilin.android.axml;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Provides access to the files within an APK and can add and replace files.
 *
 * @author Palaniappan Muthuraman
 */

public class ApkHandler implements AutoCloseable{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The handled APK file.
     */
    protected File apk;

    /**
     * Pointer to the ZipFile. If an InputStream for a file within the ZipFile is
     * returned by {@link ApkHandler#getInputStream(String)} the ZipFile object has
     * to remain available in order to read the InputStream.
     */
    protected ZipFile zip;

    public ApkHandler(File apk) {
        this.apk = apk;
    }

    /**
     * Returns an {@link InputStream} for a file within the APK.<br />
     * The given filename has to be the relative path within the APK, e.g.
     * <code>res/menu/main.xml</code>
     *
     * @param filename the file's path
     * @return {@link InputStream} for the searched file, if not found null
     * @throws IOException if an I/O error occurs.
     */
    public InputStream getInputStream(String filename) throws IOException {
        InputStream is = null;

        // check if zip file is already opened
        if (this.zip == null)
            this.zip = new ZipFile(this.apk);

        // search for file with given filename
        Enumeration<?> entries = this.zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String entryName = entry.getName();
            if (entryName.equals(filename)) {
                is = this.zip.getInputStream(entry);
                break;
            }
        }

        return is;
    }

    @Override
    public void close() {

    }
}
