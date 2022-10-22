package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.ObjectWriter;
import ix.core.controllers.EntityFactory;
import ix.core.validator.GinasProcessingMessage;
import ix.core.validator.ValidationResponse;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Egor Puzanov on 10/18/22.
 */
public class GsrsApiExporter implements Exporter<Substance> {

    private final HttpHeaders headers;
    private final BufferedWriter out;
    private final RestTemplate restTemplate;
    private final boolean validate;
    private final ObjectWriter writer = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();

    public GsrsApiExporter(OutputStream out, RestTemplate restTemplate, Map<String, String> headers, boolean validate) throws IOException {
        Objects.requireNonNull(out);
        this.out = new BufferedWriter(new OutputStreamWriter(out));
        Objects.requireNonNull(restTemplate);
        this.restTemplate = restTemplate;
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            h.set(entry.getKey(), entry.getValue());
        }
        this.headers = h;
        this.validate = validate;
    }

    @Override
    public void export(Substance obj) throws IOException {
        Date date = new Date();
        boolean update = true;
        try {
            obj.version = restTemplate.getForObject("/{uuid}/version", String.class, obj.getUuid().toString()).replace("\"", "");
        } catch (HttpClientErrorException.NotFound ex) {
            obj.version = "1";
            update = false;
        } catch (Exception ex) {
            out.write(String.format("%tF %tT Substance: %s %s - ERROR: %s", date, date, obj.getUuid().toString(), obj.getName(), ex.getMessage()));
            out.newLine();
            return;
        }
        HttpEntity<String> request = new HttpEntity<String>(writer.writeValueAsString(obj), headers);
        try {
            if (validate) {
                ValidationResponse vr = restTemplate.postForObject("/@validate", request, ValidationResponse.class);
                if (vr.hasError()) {
                    throw new Exception(vr.toString());
                }
            }
            if (update) {
                restTemplate.put("/", request);
            } else {
                obj = restTemplate.postForObject("/", request, Substance.class);
            }
            out.write(String.format("%tF %tT Substance: %s %s - SUCCESS", date, date, obj.getUuid().toString(), obj.getName()));
        } catch (Exception ex) {
            out.write(String.format("%tF %tT Substance: %s %s - ERROR: %s", date, date, obj.getUuid().toString(), obj.getName(), ex.getMessage()));
        }
        out.newLine();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
