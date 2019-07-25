package uk.gov.hmcts.cmc.claimstore;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import uk.gov.hmcts.cmc.claimstore.idam.models.GeneratePinResponse;
import uk.gov.hmcts.cmc.claimstore.idam.models.User;
import uk.gov.hmcts.cmc.claimstore.idam.models.UserDetails;
import uk.gov.hmcts.cmc.claimstore.services.notifications.fixtures.SampleUserDetails;
import uk.gov.hmcts.cmc.domain.models.Claim;
import uk.gov.hmcts.cmc.domain.models.ClaimData;
import uk.gov.hmcts.cmc.domain.models.ClaimDocument;
import uk.gov.hmcts.cmc.domain.models.ClaimDocumentCollection;
import uk.gov.hmcts.cmc.domain.models.ClaimDocumentType;
import uk.gov.hmcts.cmc.domain.models.ClaimSubmissionOperationIndicators;
import uk.gov.hmcts.cmc.domain.models.response.YesNoOption;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.cmc.ccd.domain.CaseEvent.CLAIM_ISSUE_RECEIPT_UPLOAD;
import static uk.gov.hmcts.cmc.ccd.domain.CaseEvent.CREATE_CASE;
import static uk.gov.hmcts.cmc.ccd.domain.CaseEvent.ISSUE_CASE;
import static uk.gov.hmcts.cmc.ccd.domain.CaseEvent.SEALED_CLAIM_UPLOAD;

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

    protected void setupCreateClaimFlowForCitizen(ClaimData claimData, List<String> features) {
        startForCitizen(AUTHORISATION_TOKEN, CREATE_CASE);
        Claim claim = submitForCitizen(claimData, features, AUTHORISATION_TOKEN);
        uploadDocument(AUTHORISATION_TOKEN);
        uploadDocument(AUTHORISATION_TOKEN);

        ClaimDocumentCollection collection = new ClaimDocumentCollection();
        collection.addClaimDocument(getClaimDocument(ClaimDocumentType.SEALED_CLAIM, "claim-form.pdf"));
        collection.addClaimDocument(getClaimDocument(ClaimDocumentType.CLAIM_ISSUE_RECEIPT, "claim-form-claimant-copy.pdf"));

        Claim updated = claim.toBuilder()
            .claimSubmissionOperationIndicators(ClaimSubmissionOperationIndicators.builder()
                .sealedClaimUpload(YesNoOption.YES)
                .staffNotification(YesNoOption.YES)
                .claimantNotification(YesNoOption.YES)
                .bulkPrint(YesNoOption.YES)
                .rpa(YesNoOption.YES)
                .defendantNotification(YesNoOption.YES)
                .claimIssueReceiptUpload(YesNoOption.YES)
                .build())
            .claimDocumentCollection(collection)
            .build();

        startUpdateForCitizen(updated, AUTHORISATION_TOKEN, SEALED_CLAIM_UPLOAD);
        submitUpdateForCitizen(updated, AUTHORISATION_TOKEN);

        startUpdateForCitizen(updated, AUTHORISATION_TOKEN, CLAIM_ISSUE_RECEIPT_UPLOAD);
        submitUpdateForCitizen(updated, AUTHORISATION_TOKEN);

        startUpdateForCitizen(updated, AUTHORISATION_TOKEN, ISSUE_CASE);
        submitUpdateForCitizen(updated, AUTHORISATION_TOKEN);
    }

    protected void setupCreateClaimFlowForRepresentative(ClaimData claimData) {
        startForCaseWorker(SOLICITOR_AUTHORISATION_TOKEN, CREATE_CASE);
        Claim claim = submitForCaseWorker(claimData, SOLICITOR_AUTHORISATION_TOKEN);
        uploadDocument(SOLICITOR_AUTHORISATION_TOKEN);

        ClaimDocumentCollection collection = new ClaimDocumentCollection();
        collection.addClaimDocument(getClaimDocument(ClaimDocumentType.SEALED_CLAIM, "claim-form.pdf"));

        Claim updated = claim.toBuilder()
            .claimSubmissionOperationIndicators(ClaimSubmissionOperationIndicators.builder()
                .sealedClaimUpload(YesNoOption.YES)
                .staffNotification(YesNoOption.YES)
                .claimantNotification(YesNoOption.YES)
                .bulkPrint(YesNoOption.YES)
                .build())
            .claimDocumentCollection(collection)
            .build();

        startEventForCaseworker(updated, SOLICITOR_AUTHORISATION_TOKEN, ISSUE_CASE);
        submitEventForCaseworker(updated, SOLICITOR_AUTHORISATION_TOKEN);
        startEventForCaseworker(updated, SOLICITOR_AUTHORISATION_TOKEN, SEALED_CLAIM_UPLOAD);
        submitEventForCaseworker(updated, SOLICITOR_AUTHORISATION_TOKEN);
    }

    protected void setupGetClaimHavingDocumentByExternalId(
        ClaimData claimData,
        ClaimDocumentType documentType,
        String documentName
    ) {
        ClaimDocumentCollection collection = new ClaimDocumentCollection();
        collection.addClaimDocument(getClaimDocument(documentType, documentName));

        Claim claim = getClaim(claimData, FEATURES, "1").toBuilder()
            .claimDocumentCollection(collection)
            .build();

        searchForCitizen(claim, AUTHORISATION_TOKEN, getSearchCrieteria(claimData));
    }

    protected void setupGetClaimHavingDocumentByExternalIdForRepresentative(
        ClaimData claimData,
        ClaimDocumentType documentType,
        String documentName
    ) {
        ClaimDocumentCollection collection = new ClaimDocumentCollection();
        collection.addClaimDocument(getClaimDocument(documentType, documentName));

        Claim claim = getClaim(claimData, FEATURES, "1").toBuilder()
            .claimDocumentCollection(collection)
            .build();
        searchForRepresentative(claim, SOLICITOR_AUTHORISATION_TOKEN, getSearchCrieteria(claimData));
    }

    @NotNull
    private ImmutableMap<String, String> getSearchCrieteria(ClaimData claimData) {
        return ImmutableMap
            .of("case.externalId", claimData.getExternalId().toString())
            .of("sortDirection", "desc")
            .of("page", "1");
    }

    private ClaimDocument getClaimDocument(ClaimDocumentType sealedClaim, String documentName) {
        return ClaimDocument.builder()
            .documentManagementUrl(URI.create("http://localhost:8085/documents/85d97996-22a5-40d7-882e-3a382c8ae1b4"))
            .documentManagementBinaryUrl(URI.create("someBinaryUrl"))
            .documentType(sealedClaim)
            .documentName(documentName)
            .build();
    }
}
