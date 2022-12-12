package gsrs.module.substance.indexers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import ix.core.controllers.EntityFactory;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

/**
 * Created by Egor Puzanov on 11/22/2022.
 */

public class XPathIndexValueMaker implements IndexValueMaker<Substance> {

    private List<XPathIndexConfig> expressions = new ArrayList<XPathIndexConfig>();
    private final ObjectWriter writer = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();
    private final ObjectWriter xmlWriter = new XmlMapper(new XmlFactory()
        .configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)
        .configure(ToXmlGenerator.Feature.WRITE_XML_1_1, true)).writer().withRootName("root");

    public class XPathIndexConfig {
        private static final String name;
        private static final XPathExpression expression;
        private static final String replacement = "$1";
        private static final Pattern regex;
        public XPathIndexConfig (Map<String, String> m) {
            this.name = m.get("name");
            this.replacement = m.getOrDefault("replacement", "$1");
            try {
                this.expression = XPathFactory.newInstance().newXPath().compile(m.get("expression"));
            } catch (Exception e) {
                this.expression = null;
            }
            try {
                this.regex = Pattern.compile(m.get("regex"));
            } catch (Exception e) {
                this.regex = null;
            }
        }
        public void evaluate(Document document, Consumer<IndexableValue> consumer) {
            if (document == null || expression == null) {
                return;
            }
            String value = expression.evaluate(document);
            if (regex != null) {
                try {
                    value = regex.matcher(value).replaceAll(replacement);
                } catch (Exception e) {}
            }
            if (name == null || name.isEmpty() || value == null || value.isEmpty()) {
                return;
            } else if (name.startsWith("root_")) {
                consumer.accept(IndexableValue.simpleStringValue(name, value));
            } else {
                consumer.accept(IndexableValue.simpleFacetStringValue(name, value));
            }
        }
    }

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode tree = mapper.readTree(writer.writeValueAsString(substance));
            updateReferences(tree);
            Document xmlDocument = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(xmlWriter.writeValueAsString(tree));
            for (XPathIndexConfig entry: expressions) {
                entry.evaluate(xmlDocument, consumer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<XPathIndexConfig> getExpressions() {
        return this.expressions;
    }

    public void setExpressions(HashMap<String, Map<String, String>> expressions) {
        this.expressions.clear();
        for (Map<String, String> entry: expressions.values()) {
            this.expressions.put(new XPathIndexConfig(entry));
        }
    }

    private void updateReferences(JsonNode tree) {
        ArrayNode references = (ArrayNode)tree.at("/references");
        Map<String, Integer> refMap = new HashMap<>();
        for (int i = 0; i < references.size(); i++) {
            refMap.put(references.get(i).get("uuid").textValue(), i);
        }
        for (JsonNode refsNode: tree.findValues("references")) {
            if (refsNode.isArray()) {
                ArrayNode refs = (ArrayNode) refsNode;
                for (int i = 0; i < refs.size(); i++) {
                    JsonNode ref = refs.get(i);
                    if (ref.isTextual()) {
                        Integer index = refMap.get(ref.asText());
                        if(index !=null) {
                            refs.set(i, references.get(index));
                        }
                    }
                }
            }
        }
    }
}
