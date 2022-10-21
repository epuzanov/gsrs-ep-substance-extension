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
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Egor Puzanov on 10/18/22.
 */
public class GsrsApiExporter implements Exporter<Substance> {

    private final HttpHeaders headers;
    private final BufferedWriter out;
    private final RestTemplate restTemplate;
    private final ObjectWriter writer = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();

    public GsrsApiExporter(OutputStream out, RestTemplate restTemplate, Map<String, String> headers) throws IOException {
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
    }

    @Override
    public void export(Substance obj) throws IOException {
        try {
            obj.version = restTemplate.getForObject("/{uuid}/version", String.class, obj.getUuid().toString());
        } catch (Exception ex) {
            obj.version = "1";
        }
        HttpEntity<String> request = new HttpEntity<String>(writer.writeValueAsString(obj), headers);
        ValidationResponse vr = restTemplate.postForObject("/@validate", request, ValidationResponse.class);
        try {
            restTemplate.put("/", request);
            out.write(obj.getUuid().toString() + " " + obj.getName() + " - SUCCESS");
        } catch (Exception ex) {
            out.write(obj.getUuid().toString() + " " + obj.getName() + " - ERROR: " + ex.getMessage());
        }
        out.newLine();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
