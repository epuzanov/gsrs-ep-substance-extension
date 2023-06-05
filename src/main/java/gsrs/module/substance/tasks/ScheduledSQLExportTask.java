package gsrs.module.substance.tasks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.scheduledTasks.SchedulerPlugin.TaskListener;
import gsrs.springUtils.StaticContextAccessor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

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
    private List<ZipEntryConfig> zipEntries = new ArrayList<ZipEntryConfig>();
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
    private class ZipEntryConfig {
        @JsonProperty(value="name")
        private final String name;
        @JsonProperty(value="msg")
        private final String msg;
        @JsonProperty(value="sql")
        private final String sql;
        @JsonProperty(value="encoding")
        private final String encoding;

        public String getEncoding() {
            if (encoding != null) {
                return encoding;
            }
            return "ISO-8859-1";
        }
    }

    @Data
    private class DestinationConfig {
        @JsonProperty(value="user")
        private final String user;
        @JsonProperty(value="password")
        private final String password;
        @JsonProperty(value="uri")
        private String uri;

        public String getUri() {
            if (uri != null) {
                return uri;
            }
            return "exports/substances.zip";
        }
    }

    public void setFieldConverter(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        this.fieldConverter = (FieldConverter) Class.forName(className).newInstance();
    }

    private Connection getConnection() throws SQLException {
        DataSource dataSource = StaticContextAccessor.getBean(DataSource.class);
        if (dataSource == null) {
            log.error("data source is null!");
            return null;
        }
        return dataSource.getConnection();
    }

    private PrintStream makePrintStream(File writeFile, String encoding) throws IOException {
        return new PrintStream(
            new BufferedOutputStream(new FileOutputStream(writeFile)),
            false, encoding);
    }

    private File makeCsvFile(String msg, String encoding, ResultSet rs, TaskListener l) throws IOException {
        ResultSetMetaData metadata = null;
        Clob clob_value = null;
        String value = "";
        String part_value = "";
        double denom = 0;
        int total = 0;
        int rnum = 0;
        int ccount = 0;
        List<String> cast_fields = new ArrayList<String>();
        File tmpFile = File.createTempFile("export", ".tmp");

        l.message("Get " + msg);
        //log.debug("Get " + msg);

        try (PrintStream out = makePrintStream(tmpFile, encoding)) {
            metadata = rs.getMetaData();
            ccount = metadata.getColumnCount();
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
                if (i != 1) {
                    out.print(csvDelimiter);
                }
                out.print(value);
            }
            out.println();

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
                    l.message("Exporting " + msg + " " + rnum + " of " + total);
                }
            }
        } catch (Exception e) {
            log.error("Exception:", e);
        }
        return tmpFile;
    }

    private FileSystemOptions getFileSystemOptions(URI uri, String user, String password) throws FileSystemException {
        FileSystemOptions opts = new FileSystemOptions();
        if (user != null && user.length() > 0 && password != null && password.length() > 0) {
            UserAuthenticator auth = new StaticUserAuthenticator("", user, password);
            DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, auth);
        }
        switch (uri.getScheme().toLowerCase()) {
            case "ftp":
                FtpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);
                FtpFileSystemConfigBuilder.getInstance().setPassiveMode(opts, true);
                break;
            case "sftp":
                SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(opts, false);
                SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
                SftpFileSystemConfigBuilder.getInstance().setSessionTimeoutMillis(opts, 10000);
                break;
            default:
                break;
        }
        return opts;
    }

    private void uploadFile(File localFile, TaskListener l) {
        String user = null;
        String password = null;
        URI uri = null;
        StandardFileSystemManager manager = new StandardFileSystemManager();
        try {
            manager.init();
            FileObject lfo = manager.resolveFile(localFile.getAbsolutePath());
            if (lfo.exists()) {
                for (DestinationConfig dst : destinations) {
                    user = dst.getUser();
                    password = dst.getPassword();
                    try {
                        uri = new URI(dst.getUri());
                    } catch (URISyntaxException e) {
                        uri = null;
                    }
                    if (uri == null || uri.getScheme() == null) {
                        File f = new File(dst.getUri());
                        uri = f.toURI();
                    }
                    l.message("Uploading file to " + uri.toString());
                    log.info("Uploading file to " + uri.toString());
                    try {
                        //log.debug("URI:" + uri.toString() + " user:" + user + " password:" + password);
                        FileSystemOptions opts = getFileSystemOptions(uri, user, password);
                        FileObject rfo = manager.resolveFile(uri.toString(), opts);
                        if (!rfo.getParent().exists()) {
                            rfo.getParent().createFolder();
                        }
                        log.debug("Source URI: " + lfo.getPublicURIString() + " Destination URI: " + rfo.getPublicURIString() + " Options: " + opts.toString());
                        rfo.copyFrom(lfo, Selectors.SELECT_SELF);
                    } catch (Exception e) {
                        log.error("Exception", e);
                    }
                }
            } else {
                log.error("Local file " + localFile.getName() + " not found.");
            }
        } catch (Exception e) {
            log.error("Exception", e);
        } finally {
            manager.close();
        }
    }

    public void run(SchedulerPlugin.JobStats stats, TaskListener l) {
        File tf = null;
        File zipFile = null;
        byte[] buffer = new byte[1024];
        int len;

        try {
            lock.lock();
            l.message("Establishing connection");
            log.info("SQL export started.");
            zipFile = File.createTempFile("export", ".zip");
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
                try (Connection c = getConnection()) {
                    for (ZipEntryConfig ze : zipEntries) {
                        //log.debug("ZipEntryConfig: " + ze.getName() + " " + ze.getMsg() + " " + ze.getSql());
                        out.putNextEntry(new ZipEntry(ze.getName()));
                        try (PreparedStatement s = c.prepareStatement(ze.getSql(),
                                                                      ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                                      ResultSet.CONCUR_READ_ONLY)) {
                            try (ResultSet rs = s.executeQuery()) {
                                tf = makeCsvFile(ze.getMsg(), ze.getEncoding(), rs, l);
                                try (FileInputStream in = new FileInputStream(tf)) {
                                    while (( len = in.read(buffer)) > 0) {
                                        out.write(buffer, 0, len);
                                    }
                                    out.closeEntry();
                                }
                                tf.delete();
                            }
                        }
                    }
                } finally {
                    l.message("Closed Connection");
                }
            } catch (Exception e) {
                log.error("Error writing SQL export", e);
            }
            uploadFile(zipFile, l);
        } catch (Exception e) {
            log.error("Exception", e);
        } finally {
            lock.unlock();
            zipFile.delete();
        }
    }

    @Override
    public String getDescription() {
        return "Full SQL Export to " + name;
    }
}

