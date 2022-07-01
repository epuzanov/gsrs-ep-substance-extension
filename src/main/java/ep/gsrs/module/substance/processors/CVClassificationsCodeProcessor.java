package ep.gsrs.module.substance.processors;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.cv.api.GsrsControlledVocabularyDTO;
import gsrs.cv.api.GsrsVocabularyTermDTO;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Code;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This EntityProcessor will create a Classifications comments string
 * for the Code using CV as the source (code.code = vt.value and
 * code.comments = vt.display)
 *
 * @author Egor Puzanov
 */
@Slf4j
public class CVClassificationsCodeProcessor implements EntityProcessor<Code> {

    @Autowired
    private ControlledVocabularyApi api;

    private CachedSupplier initializer = CachedSupplier.runOnceInitializer(this::addCvDomainIfNeeded);

    private CVClassificationsCodeProcessorConfig config;

    public static class CVClassificationsCodeProcessorConfig {
        public String codeSystem = "WHO-ATC";
        public String cvDomain;
        public Long cvVersion;
        public String prefix;
        public int[] masks = {};
        public Map<String, String> terms = new HashMap<String, String>();

        public void setMasks(Map<Integer, Integer> m) {
            masks = m.entrySet().stream()
                                .sorted(Map.Entry.<Integer, Integer>comparingByKey().reversed())
                                .mapToInt(e->e.getValue())
                                .toArray();
        }
    }

    public CVClassificationsCodeProcessor() {
        this(new HashMap<String, Object>());
    }

    public CVClassificationsCodeProcessor(Map m) {
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.convertValue(m, CVClassificationsCodeProcessorConfig.class);
    }

    private void addCvDomainIfNeeded() {
        if (config.cvDomain != null) {
            log.debug("starting to add CV Domain if needed");
            try {
                Optional<GsrsControlledVocabularyDTO> opt = api.findByDomain(config.cvDomain);
                if(!opt.isPresent()){
                    List<GsrsVocabularyTermDTO> list = new ArrayList<>();
                    for (Map.Entry<String, String> entry : config.terms.entrySet()) {
                        list.add(GsrsVocabularyTermDTO.builder()
                                .display(entry.getValue())
                                .value(entry.getKey())
                                .hidden(false)
                                .build());
                    }
                    api.create(GsrsControlledVocabularyDTO.builder()
                            .domain(config.cvDomain)
                            .terms(list)
                            .build());
                    if (config.cvVersion != null) {
                        config.cvVersion = new Long(1);
                    }
                } else {
                    if (config.cvVersion != null) {
                        config.cvVersion = opt.get().getVersion();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally{
                log.debug("finished CV Domain add routine");
            }
        }
    }

    private void updateTermsIfNeeded() {
        if (config.cvDomain != null && config.cvVersion != null) {
            try {
                Optional<GsrsControlledVocabularyDTO> opt = api.findByDomain(config.cvDomain);
                if (opt.isPresent() && config.cvVersion != null && opt.get().getVersion() > config.cvVersion) {
                    Map<String, String> terms = new HashMap<String, String>();
                    for (GsrsVocabularyTermDTO term : new ArrayList<GsrsVocabularyTermDTO>(opt.get().getTerms())) {
                        terms.put(term.getValue(), term.getDisplay());
                    }
                    config.terms = terms;
                    config.cvVersion = opt.get().getVersion();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void initialize() throws FailProcessingException{
        initializer.getSync();
    }

    @Override
    public void prePersist(Code obj) {
        if (config.codeSystem.equals(obj.codeSystem) && obj.code != null && !obj.code.isEmpty() && !obj.isClassification()) {
            updateTermsIfNeeded();
            String comments = config.terms.get(obj.code);
            if (comments != null) {
                if (config.masks.length > 0) {
                    for (int mask: config.masks) {
                        if (obj.code.length() > mask) {
                            String display = config.terms.get(obj.code.substring(0, mask));
                            if (display != null) {
                                comments = display + " | " + comments;
                            }
                        }
                    }
                }
                if (config.prefix != null) {
                    comments = config.prefix + " | " + comments;
                }
                obj.comments = comments;
            }
        }
    }

    @Override
    public void preUpdate(Code obj) {
        prePersist(obj);
    }

    @Override
    public Class<Code> getEntityClass() {
        return Code.class;
    }
}
