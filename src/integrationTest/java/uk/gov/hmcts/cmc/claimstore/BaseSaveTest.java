package uk.gov.hmcts.cmc.claimstore;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import uk.gov.hmcts.cmc.ccd.domain.CCDCase;
import uk.gov.hmcts.cmc.claimstore.idam.models.GeneratePinResponse;
import uk.gov.hmcts.cmc.claimstore.idam.models.User;
import uk.gov.hmcts.cmc.claimstore.idam.models.UserDetails;
import uk.gov.hmcts.cmc.claimstore.services.notifications.fixtures.SampleUserDetails;
import uk.gov.hmcts.cmc.domain.models.Claim;
import uk.gov.hmcts.cmc.domain.models.ClaimData;
import uk.gov.hmcts.cmc.domain.models.ClaimSubmissionOperationIndicators;
import uk.gov.hmcts.cmc.domain.models.sampledata.SampleClaim;
import uk.gov.hmcts.cmc.domain.utils.LocalDateTimeFactory;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.document.domain.Classification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.cmc.ccd.domain.CaseEvent.MORE_TIME_REQUESTED_ONLINE;
import static uk.gov.hmcts.cmc.claimstore.utils.ResourceLoader.successfulDocumentManagementUploadResponse;
import static uk.gov.hmcts.cmc.domain.models.ClaimState.CREATE;

public abstract class BaseSaveTest extends BaseIntegrationTest {
    public static final String ANONYMOUS_BEARER_TOKEN = "Anonymous Bearer token";
    public static final String ANONYMOUS_USER_ID = "3";

    @Before
    public void setup() {
        UserDetails userDetails = SampleUserDetails.builder().build();
        given(userService.getUserDetails(AUTHORISATION_TOKEN)).willReturn(userDetails);
        given(userService.getUser(AUTHORISATION_TOKEN)).willReturn(new User(AUTHORISATION_TOKEN, userDetails));

        UserDetails solicitorDetails = SampleUserDetails.builder().withRoles("solicitor").build();
        given(userService.getUserDetails(SOLICITOR_AUTHORISATION_TOKEN))
            .willReturn(solicitorDetails);
        given(userService.getUser(SOLICITOR_AUTHORISATION_TOKEN))
            .willReturn(new User(SOLICITOR_AUTHORISATION_TOKEN, solicitorDetails));

        given(userService.generatePin("Dr. John Smith", AUTHORISATION_TOKEN))
            .willReturn(new GeneratePinResponse("my-pin", "2"));

        given(pdfServiceClient.generateFromHtml(any(byte[].class), anyMap()))
            .willReturn(PDF_BYTES);

        given(userService.authenticateAnonymousCaseWorker())
            .willReturn(new User(ANONYMOUS_BEARER_TOKEN,
                SampleUserDetails.builder().withUserId(ANONYMOUS_USER_ID).build()));

        given(referenceNumberRepository.getReferenceNumberForLegal()).willReturn("000LR001");
        given(referenceNumberRepository.getReferenceNumberForCitizen()).willReturn("000MC001");

        given(authTokenGenerator.generate()).willReturn(SERVICE_TOKEN);
    }
}
