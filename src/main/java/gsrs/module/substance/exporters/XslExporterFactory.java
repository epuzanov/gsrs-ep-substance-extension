package gsrs.module.substance.exporters;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gsrs.module.substance.utils.XslParameterUtils;

import ix.ginas.exporters.*;
import ix.ginas.models.v1.Substance;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Created by Egor Puzanov.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class XslExporterFactory implements ExporterFactory<Substance> {

    private OutputFormat format = new OutputFormat("gsrsp", "Json Portable Export (gsrsp) File");
    private String templateFile = "";
    private String header = "";
    private String footer = "";
    private String delimiter = "";
    private Map<String, String> xslParameters = new LinkedHashMap<>();
    private Map<String, String> xslNamespaces = new LinkedHashMap<>();
    private boolean shouldCompress = false;

    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }

    public void setTemplateFile(String templateFile) {
        this.templateFile = templateFile;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Sets the additional parameters passed to the XSL template on every record.
     *
     * @param xslParameters map of template parameter names to their values
     */
    public void setXslParameters(Map<String, String> xslParameters) {
        this.xslParameters = xslParameters;
    }

    public Map<String, String> getXslParameters() {
        return xslParameters;
    }

    /**
     * Sets the namespace prefix mappings used to resolve prefixed parameter names.
     *
     * @param xslNamespaces mapping of namespace prefixes to namespace URIs
     */
    public void setXslNamespaces(Map<String, String> xslNamespaces) {
        this.xslNamespaces = xslNamespaces;
    }

    public Map<String, String> getXslNamespaces() {
        return xslNamespaces;
    }

    public void setShouldCompress(boolean shouldCompress) {
        this.shouldCompress = shouldCompress;
    }

    @Override
    public boolean supports(Parameters params) {
        return params.getFormat().equals(format);
    }

    @Override
    public Set<OutputFormat> getSupportedFormats() {
        return Collections.singleton(format);
    }

    @Override
    public Exporter<Substance> createNewExporter(OutputStream out, Parameters params) throws IOException {
        JsonNode detailedParameters = params == null ? null : params.detailedParameters();
        Map<String, String> parameters = XslParameterUtils.merge(
            xslParameters,
            XslParameterUtils.fromJson(detailedParameters, XslParameterUtils.PARAMETERS_PROPERTY));
        Map<String, String> namespaces = XslParameterUtils.merge(
            xslNamespaces,
            XslParameterUtils.namespacesFromJson(detailedParameters));
        if(shouldCompress) {
            try {
                return new XslExporter(new GZIPOutputStream(out), templateFile, header, footer, delimiter,
                    parameters, namespaces);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        try {
            return new XslExporter(out, templateFile, header, footer, delimiter, parameters, namespaces);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        ObjectNode xslParameters = parameters.putObject(XslParameterUtils.PARAMETERS_PROPERTY);
        xslParameters.put("type", "object")
            .put("title", "XSL Parameters")
            .put("description", "Additional parameters passed to the XSL template");
        xslParameters.putObject("additionalProperties").put("type", "string");
        ObjectNode namespaces = parameters.putObject(XslParameterUtils.NAMESPACES_PROPERTY);
        namespaces.put("type", "object")
            .put("title", "XSL Parameter Namespaces")
            .put("description", "Namespace prefixes used by the XSL parameter names");
        namespaces.putObject("additionalProperties").put("type", "string");
        return generateSchemaNode(getClass().getSimpleName(), parameters);
    }
}
