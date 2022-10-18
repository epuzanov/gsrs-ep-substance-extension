package gsrs.module.substance.exporters;

import com.fasterxml.jackson.databind.ObjectWriter;
import ix.core.controllers.EntityFactory;
import ix.ginas.exporters.Exporter;
import ix.ginas.models.v1.Substance;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Egor Puzanov on 10/18/22.
 */
public class GsrsApiExporter implements Exporter<Substance> {
    private final BufferedWriter out;
    private final RestTemplate restTemplate;
    private final ObjectWriter writer =  EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();

    public GsrsApiExporter(OutputStream out, RestTemplate restTemplate) throws IOException{
        Objects.requireNonNull(out);
        this.out = new BufferedWriter(new OutputStreamWriter(out));
        Objects.requireNonNull(restTemplate);
        this.restTemplate = restTemplate;
    }

    @Override
    public void export(Substance obj) throws IOException {
        out.write(writer.writeValueAsString(obj));
        out.newLine();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
