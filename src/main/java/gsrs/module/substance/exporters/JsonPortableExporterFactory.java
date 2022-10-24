package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.ginas.models.v1.Substance;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import lombok.Data;

/**
 * Created by epuzanov on 8/30/21.
 */
@Data
public class JsonPortableExporterFactory implements ExporterFactory{

    private OutputFormat format = new OutputFormat("gsrsp", "Json Portable Export (gsrsp) File");
    private List<String> fieldsToRemove  = Arrays.asList("_name","_nameHTML","_formulaHTML","_approvalIDDisplay","_isClassification","_self","self","approvalID","approved","approvedBy","changeReason","created","createdBy","lastEdited","lastEditedBy","deprecated","uuid","refuuid","originatorUuid","linkingID","id","documentDate","status","version");
    private boolean shouldCompress = true;
    private String gsrsVersion = "3.0.2";
    private boolean sign = false;

    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }

    public void setShouldCompress(boolean shouldCompress) {
        this.shouldCompress = shouldCompress;
    }

    public void setSign(boolean sign) {
        this.sign = sign;
    }

    public void setGsrsVersion(String gsrsVersion) {
        this.gsrsVersion = gsrsVersion;
    }

    public void setFieldsToRemove(Map<String, String> fieldsToRemove) {
        this.fieldsToRemove = (List<String>) fieldsToRemove.values();
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
        if(shouldCompress) {
            return new JsonPortableExporter(new GZIPOutputStream(out), fieldsToRemove, sign, gsrsVersion);
        }
        return new JsonPortableExporter(out, fieldsToRemove, sign, gsrsVersion);
    }

    //@Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        return parameters;
    }
}
