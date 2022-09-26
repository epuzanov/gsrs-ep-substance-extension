package gsrs.module.substance.repository;

import gsrs.repository.GsrsRepository;
import ix.ginas.models.v1.SubstanceReference;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface SubstanceReferenceRepository extends GsrsRepository<SubstanceReference, UUID> {
    @Query("select s.uuid from SubstanceReference s")
    List<String> getAllUuids();
}
