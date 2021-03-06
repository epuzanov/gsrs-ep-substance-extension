package ep.gsrs.module.substance.indexers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.burt.jmespath.JmesPath;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;

import ix.core.controllers.EntityFactory;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by Egor Puzanov on 9/10/2021.
 */

public class JmespathIndexValueMaker implements IndexValueMaker<Substance> {

    private List<Expression<JsonNode>> expressions = new ArrayList<Expression<JsonNode>>();
    private final ObjectWriter writer = EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().writer();

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
            for (Expression<JsonNode> expression: expressions) {
                JsonNode results = expression.search(tree);
                if (!results.isArray()) {
                    results = mapper.createArrayNode().add(results);
                }
                for (JsonNode result: results) {
                    Iterator<Map.Entry<String, JsonNode>> fields = result.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        String key = field.getKey();
                        String value = field.getValue().asText(null);
                        if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
                            continue;
                        } else if (key.startsWith("root_")) {
                            consumer.accept(IndexableValue.simpleStringValue(key, value));
                        } else {
                            consumer.accept(IndexableValue.simpleFacetStringValue(key, value));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Expression<JsonNode>> getExpressions() {
        return this.expressions;
    }

    public void setExpressions(LinkedHashMap<Integer, String> expressions) {
        JmesPath<JsonNode> jmespath = new JacksonRuntime();
        this.expressions.clear();
        for (String expression: expressions.values()) {
            try {
                this.expressions.add((Expression<JsonNode>) jmespath.compile(expression));
            } catch (Exception e) {
                e.printStackTrace();
            }
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
