package gsrs.module.substance.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.common.JoseType;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.KeyAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.ContentEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryption;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweHeaders;
import org.apache.cxf.rs.security.jose.jwe.JweJsonConsumer;
import org.apache.cxf.rs.security.jose.jwe.JweJsonEncryptionEntry;
import org.apache.cxf.rs.security.jose.jwe.JweJsonProducer;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jwe.KeyEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKeys;
import org.apache.cxf.rs.security.jose.jwk.JwkUtils;
import org.apache.cxf.rs.security.jose.jws.JwsCompactConsumer;
import org.apache.cxf.rs.security.jose.jws.JwsCompactProducer;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Egor Puzanov
 */

@Slf4j
@Component
public class JoseUtil {

    @Autowired
    private JoseUtilConfiguration config;
    private static JoseUtil INSTANCE = new JoseUtil();

    private JoseUtil() {
    }

    public static JoseUtil getInstance() {
        return INSTANCE;
    }

    public String sign(String str) {
        JwsHeaders protectedHeaders = new JwsHeaders();
        JsonWebKey privateKey = config.getPrivateKey();
        try {
            protectedHeaders.setKeyId(privateKey.getKeyId());
            protectedHeaders.setContentType("application/json");
            JwsCompactProducer jwsProducer = new JwsCompactProducer(protectedHeaders, str);
            JwsSignatureProvider jwsp = JwsUtils.getSignatureProvider(privateKey, config.getSig());
            str = jwsProducer.signWith(jwsp);
        } catch (Exception e) {
        }
        return str;
    }

    public ObjectNode verify(String jwsCompactStr) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        JwsCompactConsumer jwsConsumer = new JwsCompactConsumer(jwsCompactStr);
        try {
            JwsHeaders headers = jwsConsumer.getJwsHeaders();
            result = (ObjectNode) mapper.readTree(jwsConsumer.getDecodedJwsPayloadBytes());
            if (result.has("_metadata")) {
                boolean verified = jwsConsumer.verifySignatureWith(config.getKey(headers.getKeyId()), headers.getSignatureAlgorithm());
                ((ObjectNode) result.get("_metadata")).set("verified", JsonNodeFactory.instance.booleanNode(verified));
            }
        } catch (Exception e) {
        }
        return result;
    }

    public void encrypt(ObjectNode node) {
        JsonWebKey key;
        KeyEncryptionProvider keyEncryption;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode result = JsonNodeFactory.instance.objectNode();
        JweHeaders protectedHeaders = new JweHeaders(config.getEnc());
        protectedHeaders.setType(JoseType.JOSE_JSON);
        JweHeaders sharedUnprotectedHeaders = new JweHeaders();
        sharedUnprotectedHeaders.setKeyEncryptionAlgorithm(config.getAlg());
        ContentEncryptionProvider contentEncryption = JweUtils.getContentEncryptionProvider(config.getEnc(), true);
        List<JweEncryptionProvider> jweProviders = new LinkedList<JweEncryptionProvider>();
        List<JweHeaders> perRecipientHeades = new LinkedList<JweHeaders>();
        JsonWebKey privateKey = config.getPrivateKey();
        if (privateKey != null && node.get("access").findValue(privateKey.getKeyId()) == null) {
            keyEncryption = JweUtils.getKeyEncryptionProvider(privateKey, config.getAlg());
            jweProviders.add(new JweEncryption(keyEncryption, contentEncryption));
            perRecipientHeades.add(new JweHeaders(privateKey.getKeyId()));
        }
        Iterator<JsonNode> it = node.get("access").elements();
        while (it.hasNext()) {
            key = config.getKey(it.next().asText());
            if (key != null) {
                keyEncryption = JweUtils.getKeyEncryptionProvider(key, config.getAlg());
                jweProviders.add(new JweEncryption(keyEncryption, contentEncryption));
                perRecipientHeades.add(new JweHeaders(key.getKeyId()));
            }
        }
        if (!jweProviders.isEmpty()) {
            JweJsonProducer p = new JweJsonProducer(protectedHeaders,
                                        sharedUnprotectedHeaders,
                                        StringUtils.toBytesUTF8(node.toString()));
            try {
                result = mapper.readTree(p.encryptWith(jweProviders, perRecipientHeades));
            } catch (Exception e) {
            }
        }
        node.removeAll();
        Iterator<Map.Entry<String, JsonNode>> fields = result.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            node.set(field.getKey(), field.getValue());
        }
    }

    public void decrypt(ObjectNode node) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        JweJsonConsumer consumer = new JweJsonConsumer(node.toString());
        node.removeAll();
        JsonWebKey privateKey = config.getPrivateKey();
        try {
            KeyAlgorithm keyAlgo = consumer.getSharedUnprotectedHeader().getKeyEncryptionAlgorithm();
            ContentAlgorithm ctAlgo = consumer.getProtectedHeader().getContentEncryptionAlgorithm();
            JweDecryptionProvider jwe = JweUtils.createJweDecryptionProvider(JweUtils.getKeyDecryptionProvider(privateKey, keyAlgo), ctAlgo);
            for (JweJsonEncryptionEntry encEntry : consumer.getRecipients()) {
                if (privateKey.getKeyId().equals(encEntry.getUnprotectedHeader().getKeyId())) {
                    result = (ObjectNode) mapper.readTree(consumer.decryptWith(jwe, encEntry).getContent());
                    break;
                }
            }
        } catch (Exception e) {
        }
        Iterator<Map.Entry<String, JsonNode>> fields = result.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            node.set(field.getKey(), field.getValue());
        }
    }
}
