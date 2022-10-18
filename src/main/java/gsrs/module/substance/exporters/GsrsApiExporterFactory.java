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
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import javax.net.ProxySelector;
import javax.net.http.HttpClient;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


/**
 * Created by Egor Puzanov on 10/18/22.
 */
public class GsrsApiExporterFactory implements ExporterFactory {

    private OutputFormat format = new OutputFormat("gsrsapi", "Send to the other GSRS instance");
    private int timeout = 50;
    private boolean trustAllCerts = false;
    private Map<String, String> headers = new HashMap<String, String>();
    private final TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(
               X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(
               X509Certificate[] certs, String authType) {
           }
        }
    };

    public void setFormat(Map<String, String> m) {
        this.format = new OutputFormat(m.get("extension"), m.get("displayName"));
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout.intValue();
    }

    public void setTrustAllCerts(boolean trustAllCerts) {
        this.trustAllCerts = trustAllCerts;
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
        SSLContext sslContext = SSLContext.getDefault();
        SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
        if (trustAllCerts) {
            sslContext.init(null, trustAllCerts, new SecureRandom());
            sslParameters.setEndpointIdentificationAlgorithm(null);
        }
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(timeout))
            .proxy(ProxySelector.getDefault())
            .sslContext(sslContext)
            .sslParameters(sslParameters)
            .build();
        ClientHttpRequestFactory clientFactory = new HttpComponentsClientHttpRequestFactory(client);
        RestTemplate restTemplate = new RestTemplate(clientFactory).getInterceptors().add(new ClientHttpRequestInterceptor(){
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                for (Map.Entry<String, String> entry, headers.entrySet()) {
                    request.getHeaders().set(entry.getKey(), entry.getValue());
                }
                return execution.execute(request, body);
            }
        });
        return new GsrsApiExporter(out, restTemplate);
    }

    //@Override
    public JsonNode getSchema() {
        ObjectNode parameters = JsonNodeFactory.instance.objectNode();
        return parameters;
    }
}
