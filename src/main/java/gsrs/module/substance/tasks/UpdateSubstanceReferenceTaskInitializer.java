package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.EntityProcessorFactory;
import gsrs.module.substance.repository.SubstanceReferenceRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.SubstanceReference;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Used to restore broken SubstanceReference objects
 *
 * @author Egor Puzanov
 *
 */
@Slf4j
@Data
public class UpdateSubstanceReferenceTaskInitializer extends ScheduledTaskInitializer {

    @Autowired
    private EntityProcessorFactory entityProcessorFactory;

    @Autowired
    private SubstanceReferenceRepository substanceReferenceRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {

        l.message("Initializing SubstanceReference Updater");
        log.trace("Initializing SubstanceReference Updater");

        l.message("Initializing SubstanceReference Updater: acquiring list");

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        l.message("Initializing SubstanceReference Updater: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing SubstanceReference Updater: starting process");
        log.trace("starting process");

        try {
            adminService.runAs(adminAuth, (Runnable) () -> {
                TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                log.trace("got outer tx " + tx);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    processSubstanceReferences(l);
                } catch (Exception ex) {
                    l.message("SubstanceReference processing error: " + ex.getMessage());
                }
            });
        } catch (Exception ee) {
            log.error("SubstanceReference processing error: ", ee);
            l.message("ERROR:" + ee.getMessage());
            throw new RuntimeException(ee);
        }

        l.message("Shutting down executor service");
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            //should never happen
            log.error("Interrupted exception!");
        }
        l.message("Task finished");
        l.complete();
        //listen.doneProcess();
    }

    @Override
    public String getDescription() {
        return "Update all SubstanceReferences in the database";
    }

    private void processSubstanceReferences(SchedulerPlugin.TaskListener l){
        log.trace("starting in SubstanceReferences");

        List<String> srIds = substanceReferenceRepository.getAllUuids();
        log.trace("total substance referencs: {}", srIds.size());
        int soFar = 0;
        for (String srId: srIds) {
            soFar++;
            log.trace("going to fetch substance reference with ID {}", srId);
            UUID srUuid = UUID.fromString(srId);
            Optional<SubstanceReference> srOpt = substanceReferenceRepository.findById(srUuid);
            if( !srOpt.isPresent()){
                log.info("No substance reference found with ID {}", srId);
                continue;
            }
            SubstanceReference sr = srOpt.get();
            String srStr = sr.toString();
            log.trace("processing substance reference with ID {}", sr.uuid.toString());
            try {
                entityProcessorFactory.getCombinedEntityProcessorFor(sr).preUpdate(sr);
            } catch (Exception ex) {
                log.error("Error during processing: {}", ex.getMessage());
            }
            if(!srStr.equals(sr.toString())) {
                try {
                    log.trace("resaving substance reference {}", sr.toString());
                    TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    tx.setReadOnly(false);
                    tx.executeWithoutResult(c-> {
                        log.trace("before saveAndFlush");
                        SubstanceReference sr2 = StaticContextAccessor.getEntityManagerFor(SubstanceReference.class).merge(sr);
                        sr2.forceUpdate();
                        substanceReferenceRepository.saveAndFlush(sr2);
                    });

                    log.trace("saved substance reference {}", sr.toString());
                } catch (Exception ex) {
                    log.error("Error during save: {}", ex.getMessage());
                }
            }
            l.message(String.format("Processed %d of %d substance references", soFar, srIds.size()));
        }
    }
}
