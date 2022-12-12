package gsrs.module.substance.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.UrlResource;

import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;

@Configuration
@ConfigurationProperties("gsrs.crypto")
@Data
public class JoseUtilConfiguration {
    private JsonWebKeys jwks;
    private String privateKeyId;
    private ContentAlgorithm enc;
    private KeyAlgorithm alg;
    private SignatureAlgorithm sig;

    public void setJwks(Map<String, ?> m) {
        if (m.containsKey("filename")) {
            String filename = (String) m.get("filename");
            if (!filename.contains(":")) {
                filename = "classpath:" + filename;
            }
            try (InputStream is = new UrlResource(filename).getInputStream();) {
                this.jwks = JwkUtils.readJwkSet(is);
            } catch (Exception e) {
                this.jwks = new JsonWebKeys();
            }
        } else {
            try {
                this.jwks = JwkUtils.readJwkSet(new ObjectMapper().writeValueAsString(m));
            } catch (Exception e) {
                this.jwks = new JsonWebKeys();
            }
        }
    }

    public void setEnc(String enc) {
        this.enc = ContentAlgorithm.valueOf(enc);
    }

    public void setAlg(String alg) {
        this.alg = KeyAlgorithm.valueOf(alg);
    }

    public void setSig(String sig) {
        this.sig = SignatureAlgorithm.valueOf(sig);
    }

    public String getPrivateKeyId() {
        if (this.privateKeyId != null) {
            return this.privateKeyId;
        }
        JsonWebKey key = getPrivateKey();
        if (key != null) {
            return key.getKeyId();
        }
        return null;
    }

    public JsonWebKey getPrivateKey() {
        if (this.privateKeyId != null) {
            return this.jwks.getKey(this.privateKeyId);
        }
        if (this.jwks.size() == 0) {
            return null;
        }
        return  this.jwks
                    .getKeys()
                    .stream()
                    .filter(k->(k.getKeyProperty(JsonWebKey.RSA_PRIVATE_EXP) != null || k.getKeyProperty(JsonWebKey.EC_PRIVATE_KEY) != null))
                    .findFirst()
                    .orElse(null);
    }

    public JsonWebKey getKey(String keyId) {
        return this.jwks.getKey(keyId);
    }
}
