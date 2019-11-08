package uk.gov.hmcts.cmc.claimstore.jobs.cron;

import lombok.Getter;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cmc.claimstore.services.ScheduledStateTransitionService;
import uk.gov.hmcts.cmc.claimstore.services.StateTransition;
import uk.gov.hmcts.cmc.scheduler.model.CronJob;

@Component
@Getter
@DisallowConcurrentExecution
public abstract class AbstractStateTransitionJob implements CronJob {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    private ScheduledStateTransitionService scheduledStateTransitionService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            scheduledStateTransitionService.stateChangeTriggered(getStateTransition());
        } catch (Exception e) {
            throw new JobExecutionException(e);
        }
    }

    @Autowired
    public void setScheduledStateTransitionService(ScheduledStateTransitionService scheduledStateTransitionService) {
        this.scheduledStateTransitionService = scheduledStateTransitionService;
    }

    protected abstract StateTransition getStateTransition();
}
