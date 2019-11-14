package uk.gov.hmcts.cmc.claimstore.jobs.cron;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.cmc.claimstore.services.IntentionToProceedService;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class IntentionToProceedJobTest {

    @Mock
    private IntentionToProceedService intentionToProceedService;

    private IntentionToProceedJob intentionToProceedJob;

    @Before
    public void setup() {
        intentionToProceedJob = new IntentionToProceedJob();
        intentionToProceedJob.setIntentionToProceedService(intentionToProceedService);
    }

    @Test
    public void executeShouldTriggerIntentionToProceed() throws Exception {
        intentionToProceedJob.execute(null);

        verify(intentionToProceedService).scheduledTrigger();
    }
}
