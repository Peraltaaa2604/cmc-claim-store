package uk.gov.hmcts.cmc.claimstore.controllers;

import feign.FeignException;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.cmc.claimstore.BaseSaveTest;
import uk.gov.hmcts.cmc.domain.models.ClaimData;
import uk.gov.hmcts.cmc.domain.models.sampledata.SampleClaimData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.cmc.ccd.domain.CaseEvent.CLAIM_ISSUE_RECEIPT_UPLOAD;
import static uk.gov.hmcts.cmc.ccd.domain.CaseEvent.CREATE_CASE;
import static uk.gov.hmcts.cmc.ccd.domain.CaseEvent.ISSUE_CASE;
import static uk.gov.hmcts.cmc.ccd.domain.CaseEvent.SEALED_CLAIM_UPLOAD;
import static uk.gov.hmcts.cmc.claimstore.utils.ResourceLoader.successfulCoreCaseDataStoreStartResponse;

@TestPropertySource(
    properties = {
        "feature_toggles.async_event_operations_enabled=false"
    }
)
public class SaveClaimWithCoreCaseDataStoreTest extends BaseSaveTest {

    @Test
    public void shouldStoreRepresentedClaimIntoCCD() throws Exception {
        //given
        ClaimData claimData = SampleClaimData.submittedByLegalRepresentative();
        setupCreateClaimFlowForRepresentative(claimData);

        //when
        makeIssueClaimRequest(claimData, SOLICITOR_AUTHORISATION_TOKEN)
            .andExpect(status().isOk())
            .andReturn();

        //verify
        verifyStartForCaseworker(SOLICITOR_AUTHORISATION_TOKEN, CREATE_CASE);
        verifySubmitForCaseworker(SOLICITOR_AUTHORISATION_TOKEN);
        verifyStartEventForCaseworker(SOLICITOR_AUTHORISATION_TOKEN, ISSUE_CASE);
        verifyStartEventForCaseworker(SOLICITOR_AUTHORISATION_TOKEN, SEALED_CLAIM_UPLOAD);
        verifySubmitEventForCaseworker(SOLICITOR_AUTHORISATION_TOKEN, 2);
    }

    @Test
    public void shouldStoreCitizenClaimIntoCCD() throws Exception {
        //given
        ClaimData claimData = SampleClaimData.submittedByClaimantBuilder().build();
        setupCreateClaimFlowForCitizen(claimData, FEATURES);

        //when
        makeIssueClaimRequest(claimData, AUTHORISATION_TOKEN)
            .andExpect(status().isOk())
            .andReturn();

        //verify
        verifyStartForCitizen(AUTHORISATION_TOKEN, CREATE_CASE);
        verifySubmitForCitizen(AUTHORISATION_TOKEN);
        verifyStartEventForCitizen(AUTHORISATION_TOKEN, ISSUE_CASE);
        verifyStartEventForCitizen(AUTHORISATION_TOKEN, SEALED_CLAIM_UPLOAD);
        verifyStartEventForCitizen(AUTHORISATION_TOKEN, CLAIM_ISSUE_RECEIPT_UPLOAD);
        verifySubmitEventForCitizen(AUTHORISATION_TOKEN, 3);
    }

    @Test
    public void shouldFailIssuingClaimEvenWhenCCDStoreFailsToStartEvent() throws Exception {
        ClaimData claimData = SampleClaimData.submittedByLegalRepresentative();

        given(coreCaseDataApi.startForCaseworker(
            eq(SOLICITOR_AUTHORISATION_TOKEN),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            eq(CREATE_CASE.getValue())
            )
        ).willThrow(FeignException.class);

        given(authTokenGenerator.generate()).willReturn(SERVICE_TOKEN);

        MvcResult result = makeIssueClaimRequest(claimData, SOLICITOR_AUTHORISATION_TOKEN)
            .andExpect(status().isInternalServerError())
            .andReturn();

        assertThat(result.getResolvedException().getMessage())
            .isEqualTo("Failed storing claim in CCD store for case id 000LR001 on event CREATE_CASE");
    }

    @Test
    public void shouldIssueClaimEvenWhenCCDStoreFailsToSubmitEvent() throws Exception {
        ClaimData claimData = SampleClaimData.submittedByLegalRepresentative();

        given(coreCaseDataApi.startForCaseworker(
            eq(SOLICITOR_AUTHORISATION_TOKEN),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            eq(CREATE_CASE.getValue())
            )
        ).willReturn(successfulCoreCaseDataStoreStartResponse());

        given(coreCaseDataApi.submitForCaseworker(
            eq(SOLICITOR_AUTHORISATION_TOKEN),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            eq(IGNORE_WARNING),
            any()
            )
        ).willThrow(FeignException.class);

        given(authTokenGenerator.generate()).willReturn(SERVICE_TOKEN);

        MvcResult result = makeIssueClaimRequest(claimData, SOLICITOR_AUTHORISATION_TOKEN)
            .andExpect(status().isInternalServerError())
            .andReturn();

        assertThat(result.getResolvedException().getMessage())
            .isEqualTo("Failed storing claim in CCD store for case id 000LR001 on event CREATE_CASE");
    }
}
