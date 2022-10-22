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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;


/**
 * Created by Egor Puzanov on 10/18/22.
 */
public class GsrsApiExporterFactory implements ExporterFactory {

    private OutputFormat format = new OutputFormat("gsrsapi", "Send to ...");
    private int timeout = 120000;
    private boolean trustAllCerts = false;
    private boolean validate = true;
    private String baseUrl = "http://localhost:8080/api/v1/substances";
    private Map<String, String> headers;

    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout.intValue();
    }

    public void setTrustAllCerts(boolean trustAllCerts) {
        this.trustAllCerts = trustAllCerts;
    }

    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
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
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(timeout)
            .build();
        HttpClientBuilder client = HttpClients.custom()
            .useSystemProperties()
            .setDefaultRequestConfig(requestConfig);
        if (trustAllCerts) {
            try {
                SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustAllStrategy())
                    .useTLS()
                    .build();
                SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, new AllowAllHostnameVerifier());
                client = client.setSSLSocketFactory(connectionFactory);
            } catch (Exception ex) {
            }
        }
        ClientHttpRequestFactory clientFactory = new HttpComponentsClientHttpRequestFactory(client.build());
        RestTemplate restTemplate = new RestTemplate(clientFactory);
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(baseUrl));
        return new GsrsApiExporter(out, restTemplate, headers, validate);
    }

    //@Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        return parameters;
    }
}
