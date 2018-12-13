package uk.gov.hmcts.cmc.ccd.deprecated.domain.claimantresponse;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class CCDClaimantResponse {
    private CCDClaimantResponseType claimantResponseType;
    private CCDResponseAcceptation responseAcceptation;
    private CCDResponseRejection responseRejection;
}
