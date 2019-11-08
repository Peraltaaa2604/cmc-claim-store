package uk.gov.hmcts.cmc.claimstore.jobs.cron;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobExecutionException;
import uk.gov.hmcts.cmc.claimstore.services.ScheduledStateTransitionService;
import uk.gov.hmcts.cmc.claimstore.services.StateTransition;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class IntentionToProceedJobTest {

    @Mock
    private ScheduledStateTransitionService scheduledStateTransitionService;

    private IntentionToProceedJob intentionToProceedJob;

    @Before
    public void setup() {
        intentionToProceedJob = new IntentionToProceedJob();
        intentionToProceedJob.setScheduledStateTransitionService(scheduledStateTransitionService);
    }

    @Test
    public void executeShouldTriggerIntentionToProceed() throws Exception {
        intentionToProceedJob.execute(null);

        verify(scheduledStateTransitionService).stateChangeTriggered(eq(StateTransition.STAY_CLAIM));
    }

    @Test(expected = JobExecutionException.class)
    public void shouldThrowJobExecutionException() throws Exception {
        doThrow(new RuntimeException()).when(scheduledStateTransitionService).stateChangeTriggered(any());

        intentionToProceedJob.execute(null);
    }
}
