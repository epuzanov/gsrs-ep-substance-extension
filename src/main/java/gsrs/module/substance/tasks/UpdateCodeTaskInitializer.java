package gsrs.module.substance.tasks;

import gov.nih.ncats.common.executors.BlockingSubmitExecutor;
import gsrs.EntityProcessorFactory;
import gsrs.module.substance.repository.CodeRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.security.AdminService;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.Code;

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
 * Used to update Code objects
 *
 * @author Egor Puzanov
 *
 */
@Slf4j
@Data
public class UpdateCodeTaskInitializer extends ScheduledTaskInitializer {

    @Autowired
    private EntityProcessorFactory entityProcessorFactory;

    @Autowired
    private CodeRepository CodeRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {

        l.message("Initializing Code Updater");
        log.trace("Initializing Code Updater");

        l.message("Initializing Code Updater: acquiring list");

        ExecutorService executor = BlockingSubmitExecutor.newFixedThreadPool(5, 10);
        l.message("Initializing Code Updater: acquiring user account");
        Authentication adminAuth = adminService.getAnyAdmin();
        l.message("Initializing Code Updater: starting process");
        log.trace("starting process");

        try {
            adminService.runAs(adminAuth, (Runnable) () -> {
                TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                log.trace("got outer tx " + tx);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    processCodes(l);
                } catch (Exception ex) {
                    l.message("Code processing error: " + ex.getMessage());
                }
            });
        } catch (Exception ee) {
            log.error("Code processing error: ", ee);
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
        return "Update all Codes in the database";
    }

    private static int codeHash(Code code) {
        int hash = 7;
        hash = 31 * hash + (code.code == null ? 0 : code.code.hashCode());
        hash = 31 * hash + (code.comments == null ? 0 : code.comments.hashCode());
        hash = 31 * hash + (code.url == null ? 0 : code.url.hashCode());
        hash = 31 * hash + (code.getAccess() == null ? 0 : code.getAccess().toString().hashCode());
        return hash;
    }

    private void processCodes(SchedulerPlugin.TaskListener l){
        log.trace("starting in Codes");

        List<Code> codes= CodeRepository.findAll();
        log.trace("total Codes: {}", codes.size());
        int soFar =0;
        for (Code code: codes) {
            soFar++;
            log.trace("processing code with ID {}", code.uuid.toString());
            int hash = codeHash(code);
            try {
                entityProcessorFactory.getCombinedEntityProcessorFor(code).preUpdate(code);
            } catch (Exception ex) {
                log.error("Error during processing: {}", ex.getMessage());
            }
            if(codeHash(code) != hash) {
                try {
                    log.trace("resaving code {}", code.toString());
                    TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                    tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    tx.setReadOnly(false);
                    tx.executeWithoutResult(c -> {
                        log.trace("before saveAndFlush");
                        Code c2 = StaticContextAccessor.getEntityManagerFor(Code.class).merge(code);
                        c2.forceUpdate();
                        CodeRepository.saveAndFlush(c2);
                    });

                    log.trace("saved code {}", code.toString());
                } catch (Exception ex) {
                    log.error("Error during save: {}", ex.getMessage());
                }
            }
            l.message(String.format("Processed %d of %d code", soFar, codes.size()));
        }
    }
}
