package uk.gov.hmcts.cmc.domain.models.claimantresponse;

import org.junit.Test;
import uk.gov.hmcts.cmc.domain.models.sampledata.SampleClaimantResponse;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.cmc.domain.BeanValidator.validate;

public class ResponseRejectionTest {

    @Test
    public void shouldBeSuccessfulValidationForValidResponse() {
        ClaimantResponse claimantResponse = SampleClaimantResponse.validDefaultRejection();

        Set<String> response = validate(claimantResponse);

        assertThat(response).hasSize(0);
    }

    @Test
    public void shouldBeInvalidWhenAmountNotPresent() {
        ClaimantResponse claimantResponse = SampleClaimantResponse.ClaimantResponseRejection.builder()
            .withAmountPaid(null)
            .build();

        Set<String> response = validate(claimantResponse);

        assertThat(response).hasSize(1);
    }

    @Test
    public void shouldBeInvalidWhenReasonNotPresent() {
        ClaimantResponse claimantResponse = SampleClaimantResponse.ClaimantResponseRejection.builder()
            .withReason(null)
            .build();

        Set<String> response = validate(claimantResponse);

        assertThat(response).hasSize(1);
    }
}