package gsrs.module.substance.exporters;

import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class JmespathSpreadsheetExporterConfiguration {
    @Value("${ix.ginas.exporters.JmespathSpreadsheetExporterFactory.columns}")
    private List<Map<String, String>> columnExpressions = (List<Map<String, String>>) Arrays.asList((Map<String, String>) new HashMap<String, String>(){{put("name", "UUID"); put("expression", "uuid");}});
}
