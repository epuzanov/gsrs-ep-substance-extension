package gsrs.module.substance.tasks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.scheduledTasks.SchedulerPlugin.TaskListener;
import gsrs.springUtils.StaticContextAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.DataSource;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.FileTypeSelector;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.util.DelegatingFileSystemOptionsBuilder;

/**
 * Used to schedule SQL export to CSV files
 *
 * @author Egor Puzanov
 *
 */

@Slf4j
@Data
public class ScheduledSQLExportTask extends ScheduledTaskInitializer {

    private String name = "substances";
    private FieldConverter fieldConverter = new DefaultFieldConverter();
    private String csvDelimiter = ";";
    private String csvQuoteChar = "\"";
    private String csvEscapeChar = "";
    @JsonProperty(value="files")
    private List<EntryConfig> files = new ArrayList<EntryConfig>();
    @JsonProperty(value="destinations")
    private List<DestinationConfig> destinations = new ArrayList<DestinationConfig>();
    @JsonIgnore
    private Lock lock = new ReentrantLock();

    public interface FieldConverter {
        String toFormat(String fmt, String value) throws Exception;
    }

    private class DefaultFieldConverter implements FieldConverter {
        public String toFormat(String fmt, String value) throws Exception {
            return value;
        }
    }

    @Data
    private class EntryConfig {
        @JsonProperty(value="name")
        private final String name;
        @JsonProperty(value="msg")
        private final String msg;
        @JsonProperty(value="sql")
        private final String sql;
        @JsonProperty(value="encoding")
        private String encoding = "ISO-8859-1";
        @JsonProperty(value="addHeader")
        private Boolean addHeader;

        public boolean getAddHeader() {
            return addHeader != null ? addHeader.booleanValue() : true;
        }
    }

    private class DestinationConfig {
        private final URI uri;
        private final FileSystemOptions options;

        @JsonCreator
        public DestinationConfig(Map<String, String> dst) throws FileSystemException, URISyntaxException {
            FileSystemOptions opts = new FileSystemOptions();
            this.uri = new URI(dst.remove("uri"));
            String scheme = this.uri.getScheme().toLowerCase();
            String domain =  dst.remove("domain");
            String user =  dst.remove("name");
            String password = dst.remove("password");
            if (user != null && user.length() > 0 && password != null && password.length() > 0) {
                UserAuthenticator auth = new StaticUserAuthenticator(domain, user, password);
                DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
            }
            if (dst.size() > 0) {
                FileSystemManager manager = new StandardFileSystemManager();
                ((StandardFileSystemManager) manager).init();
                DelegatingFileSystemOptionsBuilder delegate = new DelegatingFileSystemOptionsBuilder(manager);
                for (Map.Entry<String, String> entry : dst.entrySet()) {
                    delegate.setConfigString(opts, scheme, entry.getKey(), (String) entry.getValue());
                }
                manager.close();
            }
            this.options = opts;
        }

        public URI getUri() {
            return uri;
        }

        public FileSystemOptions getOptions() {
            return options;
        }

        public FileObject getFileObject(FileSystemManager manager) throws FileSystemException {
            return manager.resolveFile(uri.toString(), options);
        }
    }

    public void setFieldConverter(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        this.fieldConverter = (FieldConverter) Class.forName(className).getDeclaredConstructor().newInstance();
    }

    private Connection getConnection() throws SQLException {
        DataSource dataSource = StaticContextAccessor.getBean(DataSource.class);
        if (dataSource == null) {
            log.error("data source is null!");
            return null;
        }
        return dataSource.getConnection();
    }

    private void makeCsvFile(EntryConfig entry, PrintStream out, ResultSet rs, TaskListener l) throws IOException {
        ResultSetMetaData metadata = null;
        Clob clob_value = null;
        String value = "";
        String part_value = "";
        double denom = 0;
        int total = 0;
        int rnum = 0;
        int ccount = 0;
        List<String> cast_fields = new ArrayList<String>();

        l.message("Get " + entry.getMsg());
        //log.debug("Get " + entry.getMsg());

        try {
            metadata = rs.getMetaData();
            ccount = metadata.getColumnCount();
            // Output header
            for (int i = 1; i <= ccount; i++) {
                value = metadata.getColumnName(i).toUpperCase();
                if (value.startsWith("CAST_TO_PART")) {
                    cast_fields.add("PART");
                    continue;
                } else if (value.startsWith("CAST_TO_")) {
                    cast_fields.add(value.substring(8, 12));
                    value = value.substring(13);
                } else {
                    cast_fields.add("NONE");
                }
                if (!entry.getAddHeader()) continue;
                if (i != 1) {
                    out.print(csvDelimiter);
                }
                out.print(value);
            }
            if (entry.getAddHeader()) {
                out.println();
            }

            // Get total
            rs.last();
            total = rs.getRow();
            rs.beforeFirst();
            denom = 1 / (total * 100.0);

            // Output each row
            while (rs.next()) {
                part_value = "";
                // log.debug("Processing row: " + rs.getString(1));
                for (int i = 0; i < ccount; i++) {
                    value = rs.getString(i + 1);
                    if (cast_fields.get(i) == "PART") {
                        if (value == null) {
                            part_value = "";
                        } else {
                            part_value = value;
                        }
                        continue;
                    }
                    if (!"".equals(part_value)) {
                        if (value == null) {
                            value = "";
                        }
                        value = value + part_value;
                        part_value = "";
                    }
                    if (i != 0) {
                        out.print(csvDelimiter);
                    }
                    if (value != null && !"".equals(value)) {
                        value = fieldConverter.toFormat(cast_fields.get(i), value);
                        if (value != null && !"".equals(value)) {
                            if (!"".equals(csvQuoteChar) && value.contains(csvQuoteChar)) {
                                if ("".equals(csvEscapeChar)) {
                                    value = value.replace(csvQuoteChar, csvQuoteChar + csvQuoteChar);
                                } else {
                                    value = value.replace(csvQuoteChar, csvEscapeChar + csvQuoteChar);
                                }
                            }
                            out.print(csvQuoteChar + value + csvQuoteChar);
                        }
                    }
                }
                out.println();

                rnum = rs.getRow();
                l.progress(rnum * denom);
                if (rnum % 10 == 0) {
                    l.message("Exporting " + entry.getMsg() + " " + rnum + " of " + total);
                }
            }
        } catch (Exception e) {
            log.error("Exception:", e);
        } finally {
            out.close();
        }
    }

    private static FileObject makeArchive(FileSystemManager manager, FileObject tmpFs, FileObject dst) throws ArchiveException, CompressorException, FileSystemException, IOException {
        byte[] buffer = new byte[1024];
        int len;
        String archiverName = null;
        String compressorName = null;

        switch (dst.getName().getExtension().toLowerCase()) {
            case "zip":
                archiverName = ArchiveStreamFactory.ZIP;
                break;
            case "jar":
                archiverName = ArchiveStreamFactory.JAR;
                break;
            case "7z":
                archiverName = ArchiveStreamFactory.SEVEN_Z;
                break;
            case "ar":
                archiverName = ArchiveStreamFactory.AR;
                break;
            case "arj":
                archiverName = ArchiveStreamFactory.ARJ;
                break;
            case "tar":
                archiverName = ArchiveStreamFactory.TAR;
                break;
            case "cpio":
                archiverName = ArchiveStreamFactory.CPIO;
                break;
            case "gz": case "gzip":
                compressorName = CompressorStreamFactory.GZIP;
                break;
            case "bz2": case "bzip2":
                compressorName = CompressorStreamFactory.BZIP2;
                break;
            case "br":
                compressorName = CompressorStreamFactory.BROTLI;
                break;
            case "xz":
                compressorName = CompressorStreamFactory.XZ;
                break;
            case "z":
                compressorName = CompressorStreamFactory.Z;
                break;
            case "cpgz":
                archiverName = ArchiveStreamFactory.CPIO;
                compressorName = CompressorStreamFactory.GZIP;
                break;
            case "tgz":
                archiverName = ArchiveStreamFactory.TAR;
                compressorName = CompressorStreamFactory.GZIP;
                break;
            case "cpbz2":
                archiverName = ArchiveStreamFactory.CPIO;
                compressorName = CompressorStreamFactory.BZIP2;
                break;
            case "tbz2":
                archiverName = ArchiveStreamFactory.TAR;
                compressorName = CompressorStreamFactory.BZIP2;
                break;
            default:
                return tmpFs;
        }

        if (archiverName == null) {
            if (dst.getName().getBaseName().toLowerCase().endsWith(".tar." + dst.getName().getExtension().toLowerCase())) {
                archiverName = ArchiveStreamFactory.TAR;
            } else if (dst.getName().getBaseName().toLowerCase().endsWith(".cpio." + dst.getName().getExtension().toLowerCase())){
                archiverName = ArchiveStreamFactory.TAR;
            } else {
                return tmpFs;
            }
        }

        FileObject arcFile = manager.resolveFile("tmp:///export.arcTmp");
        try (ArchiveOutputStream aos = new ArchiveStreamFactory().createArchiveOutputStream(archiverName, arcFile.getContent().getOutputStream())) {
            for (FileObject entryFile : tmpFs.findFiles(new FileTypeSelector(FileType.FILE))) {
                aos.putArchiveEntry(
                    aos.createArchiveEntry(
                        tmpFs.getFileSystem().replicateFile(entryFile, new FileTypeSelector(FileType.FILE)),
                        tmpFs.getName().getRelativeName(entryFile.getName())));
                InputStream is = entryFile.getContent().getInputStream();
                while (( len = is.read(buffer)) > 0) {
                    aos.write(buffer, 0, len);
                }
                aos.closeArchiveEntry();
            }
        }

        if(compressorName == null) {
            arcFile.close();
            return arcFile;
        }

        FileObject comprFile = manager.resolveFile("tmp:///export.comprTmp");
        try (CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream(compressorName, comprFile.getContent().getOutputStream())) {
            InputStream is = arcFile.getContent().getInputStream();
            while (( len = is.read(buffer)) > 0) {
                cos.write(buffer, 0, len);
            }
        }
        arcFile.close();
        comprFile.close();
        return comprFile;
    }

    private static void uploadFile(FileSystemManager manager, FileObject tmpFs, DestinationConfig dst) throws ArchiveException, CompressorException, FileSystemException, IOException {
        FileObject rfo = dst.getFileObject(manager);
        FileSelector selector = Selectors.SELECT_SELF;
        if (!rfo.getParent().exists()) {
            rfo.getParent().createFolder();
        }
        FileObject lfo = makeArchive(manager, tmpFs, rfo);
        if (tmpFs.equals(lfo)) {
            selector = Selectors.EXCLUDE_SELF;
        }
        rfo.copyFrom(lfo, selector);
    }

    public void run(SchedulerPlugin.JobStats stats, TaskListener l) {
        StandardFileSystemManager manager = new StandardFileSystemManager();
        OutputStream outputStream = null;
        CompressorStreamFactory csf = new CompressorStreamFactory();

        try {
            manager.init();
            lock.lock();
            l.message("Establishing connection");
            log.info("SQL export started.");
            FileObject tmpFs = manager.resolveFile("tmp:///export");
            try (Connection c = getConnection()) {
                for (EntryConfig entry : files) {
                    //log.debug("ZipEntryConfig: " + entry.getName() + " " + entry.getMsg() + " " + entry.getSql());
                    FileObject csvFile = tmpFs.resolveFile(entry.getName());
                    String extension = csvFile.getName().getExtension().toLowerCase();
                    if (csf.getOutputStreamCompressorNames().contains(extension)) {
                        outputStream = csf.createCompressorOutputStream(extension, csvFile.getContent().getOutputStream());
                    } else if ("bz2".equals(extension)) {
                        outputStream = csf.createCompressorOutputStream("bzip2", csvFile.getContent().getOutputStream());
                    } else {
                        outputStream = csvFile.getContent().getOutputStream();
                    }
                    PrintStream out = new PrintStream(outputStream, false, entry.getEncoding());
                    try (PreparedStatement s = c.prepareStatement(entry.getSql(),
                                                                  ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                                  ResultSet.CONCUR_READ_ONLY)) {
                        try (ResultSet rs = s.executeQuery()) {
                            makeCsvFile(entry, out, rs, l);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error writing SQL export", e);
            } finally {
                l.message("Closed Connection");
            }
            if (tmpFs.findFiles(new FileTypeSelector(FileType.FILE)).length > 0) {
                for (DestinationConfig dst : destinations) {
                    l.message("Uploading file to " + dst.getUri().toString());
                    log.debug("Destination URI: " + dst.getUri().toString() + " Options: " + dst.getOptions().toString());
                    uploadFile(manager, tmpFs, dst);
                }
            }
        } catch (Exception e) {
            log.error("Exception", e);
        } finally {
            lock.unlock();
            manager.close();
        }
    }

    @Override
    public String getDescription() {
        return "Full SQL Export to " + name;
    }
}

