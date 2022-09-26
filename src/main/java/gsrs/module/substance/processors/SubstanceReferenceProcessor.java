package gsrs.module.substance.processors;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.SubstanceReference;
import ix.utils.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Egor Puzanov
 */

@Data
@Slf4j
public class SubstanceReferenceProcessor implements EntityProcessor<SubstanceReference>{

    @Autowired
    private SubstanceRepository substanceRepository;

    private static Pattern fakeIdPattern = Pattern.compile("FAKE_ID:([0-9A-Z]{10})");
    private SubstanceReferenceProcessorConfig config;

    public static class SubstanceReferenceProcessorConfig {
        private Map<Pattern, String> codeSystemPatterns = new HashMap<Pattern, String>();
        public void setCodeSystemPatterns(Map<String, String> m) {
            for (Map.Entry<String, String> entry : m.entrySet()) {
                codeSystemPatterns.put(Pattern.compile(entry.getKey()), entry.getValue());
            }
        }
        public Map<Pattern, String> getCodeSystemPatterns() {
            return codeSystemPatterns;
        }
    }

    public SubstanceReferenceProcessor() {
        this(new HashMap<String, Object>());
    }

    public SubstanceReferenceProcessor(Map m) {
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.convertValue(m, SubstanceReferenceProcessorConfig.class);
    }

    @Override
    public void prePersist(SubstanceReference obj) throws EntityProcessor.FailProcessingException {
        String codeSystem = null;
        SubstanceRepository.SubstanceSummary relatedSubstanceSummary = null;
        if (relatedSubstanceSummary == null) {
            if (obj.refuuid != null && !obj.refuuid.isEmpty()) {
                Matcher matcher = fakeIdPattern.matcher(obj.refuuid);
                if (matcher.find()) {
                    obj.approvalID = matcher.group(1);
                    obj.refuuid = UUID.randomUUID().toString();
                } else {
                    relatedSubstanceSummary = substanceRepository.findSummaryBySubstanceReference(obj).orElse(null);
                }
            } else {
                obj.refuuid = UUID.randomUUID().toString();
            }
        }
        if (relatedSubstanceSummary == null) {
            for (Map.Entry<Pattern, String> entry : config.codeSystemPatterns.entrySet()) {
                Matcher m = entry.getKey().matcher(obj.approvalID);
                if (m.find()) {
                    relatedSubstanceSummary = substanceRepository.findByCodes_CodeAndCodes_CodeSystem(m.group(1), entry.getValue()).stream().findFirst().orElse(null);
                    if (relatedSubstanceSummary != null) {
                        break;
                    }
                }
            }
        }
        if (relatedSubstanceSummary == null) {
            relatedSubstanceSummary = substanceRepository.findByNames_NameIgnoreCase(obj.refPname).stream().findFirst().orElse(null);
        }
        if (relatedSubstanceSummary instanceof SubstanceRepository.SubstanceSummary) {
            SubstanceReference sr = relatedSubstanceSummary.toSubstanceReference();
            obj.refuuid = sr.refuuid;
            obj.refPname = sr.refPname;
            obj.approvalID = sr.approvalID;
            obj.substanceClass = sr.substanceClass;
        }
    }

    @Override
    public void postPersist(SubstanceReference obj) throws EntityProcessor.FailProcessingException {
        // TODO Auto-generated method stub

    }

    @Override
    public void preRemove(SubstanceReference obj) throws EntityProcessor.FailProcessingException {
        // TODO Auto-generated method stub

    }

    @Override
    public void postRemove(SubstanceReference obj) throws EntityProcessor.FailProcessingException {
        // TODO Auto-generated method stub

    }

    @Override
    public void preUpdate(SubstanceReference obj) throws EntityProcessor.FailProcessingException {
        prePersist(obj);
    }

    @Override
    public void postUpdate(SubstanceReference obj) throws EntityProcessor.FailProcessingException {
        // TODO Auto-generated method stub

    }

    @Override
    public void postLoad(SubstanceReference obj) throws ix.core.EntityProcessor.FailProcessingException {
        // TODO Auto-generated method stub

    }

    @Override
    public Class<SubstanceReference> getEntityClass() {
        return SubstanceReference.class;
    }
}
