package uk.gov.hmcts.cmc.claimstore.jobs.cron;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.cmc.claimstore.services.StateTransition;

@Getter
public class IntentionToProceedJob extends AbstractStateTransitionJob {

    private final StateTransition stateTransition = StateTransition.STAY_CLAIM;

    @Value("${stateTransition.stayClaim}")
    private String cronExpression;
}