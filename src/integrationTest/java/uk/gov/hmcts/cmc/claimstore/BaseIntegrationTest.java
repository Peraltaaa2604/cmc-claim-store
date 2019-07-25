package uk.gov.hmcts.cmc.claimstore;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.cmc.ccd.domain.CCDCase;
import uk.gov.hmcts.cmc.ccd.domain.CaseEvent;
import uk.gov.hmcts.cmc.domain.models.Claim;
import uk.gov.hmcts.cmc.domain.models.ClaimData;
import uk.gov.hmcts.cmc.domain.models.ClaimSubmissionOperationIndicators;
import uk.gov.hmcts.cmc.domain.models.sampledata.SampleClaim;
import uk.gov.hmcts.cmc.domain.utils.LocalDateTimeFactory;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Classification;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.cmc.claimstore.utils.ResourceLoader.successfulDocumentManagementUploadResponse;
import static uk.gov.hmcts.cmc.domain.models.ClaimState.CREATE;
import static uk.gov.hmcts.cmc.domain.utils.LocalDateTimeFactory.nowInLocalZone;

@DirtiesContext
@TestExecutionListeners(listeners = {BaseIntegrationTest.CleanDatabaseListener.class}, mergeMode = MERGE_WITH_DEFAULTS)
public abstract class BaseIntegrationTest extends MockSpringTest {
    protected static final String SUBMITTER_ID = "123";
    protected static final String DEFENDANT_ID = "555";
    protected static final String DEFENDANT_EMAIL = "j.smith@example.com";
    protected static final String BEARER_TOKEN = "Bearer let me in";
    protected static final String SERVICE_TOKEN = "S2S token";

    protected static final String AUTHORISATION_TOKEN = "Bearer token";
    protected static final String SOLICITOR_AUTHORISATION_TOKEN = "Solicitor Bearer token";
    protected static final String CCD_TRANSACTION_TOKEN = "CCD Transaction token";

    protected static final byte[] PDF_BYTES = new byte[] {1, 2, 3, 4};

    protected static final String USER_ID = "1";
    protected static final String JURISDICTION_ID = "CMC";
    protected static final String CASE_TYPE_ID = "MoneyClaimCase";
    protected static final boolean IGNORE_WARNING = true;
    protected static final List<String> FEATURES = ImmutableList.of("admissions");

    @Autowired
    protected ClaimStore claimStore;

    public static class CleanDatabaseListener extends AbstractTestExecutionListener {
        @Override
        public void beforeTestClass(TestContext testContext) {
            ApplicationContext applicationContext = testContext.getApplicationContext();
            DataSource dataSource = applicationContext.getBean("claimStoreDataSource", DataSource.class);
            JdbcTestUtils.deleteFromTables(new JdbcTemplate(dataSource), "claim");
        }
    }

    protected ResultActions makeIssueClaimRequest(ClaimData claimData, String authorization) throws Exception {
        return webClient
            .perform(post("/claims/" + USER_ID)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("Features", FEATURES)
                .content(jsonMapper.toJson(claimData))
            );
    }

    protected ImmutableMap<String, String> searchCriteria(String externalId) {
        return ImmutableMap.of(
            "page", "1",
            "sortDirection", "desc",
            "case.externalId", externalId
        );
    }

    protected ResultActions makeGetRequest(String urlTemplate, String authorisation) throws Exception {
        return webClient.perform(
            get(urlTemplate)
                .header(HttpHeaders.AUTHORIZATION, authorisation)
        );
    }


    protected void startForCitizen(String authorisation, CaseEvent caseEvent) {
        given(coreCaseDataApi.startForCitizen(
            eq(authorisation),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            eq(caseEvent.getValue())
            )
        ).willReturn(getStartEventResponse(getCaseDetails(null)));
    }

    public Claim submitForCitizen(ClaimData claimData, List<String> features, String authorisation) {
        return submitForCitizen(claimData, features, "1", authorisation);
    }

    public Claim submitForCitizen(
        ClaimData claimData,
        List<String> features,
        String submitterId,
        String authorisation
    ) {
        Claim claim = getClaim(claimData, features, submitterId);

        given(coreCaseDataApi.submitForCitizen(
            eq(authorisation),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            eq(IGNORE_WARNING),
            any()
            )
        ).willReturn(buildCaseDetails(claim));

        return claim;
    }

    protected Claim getClaim(ClaimData claimData, List<String> features, String submitterId) {
        LocalDate issueDate = issueDateCalculator.calculateIssueDay(LocalDateTimeFactory.nowInLocalZone());
        LocalDate responseDeadline = responseDeadlineCalculator.calculateResponseDeadline(issueDate);

        return Claim.builder()
            .claimData(claimData)
            .submitterId(submitterId)
            .issuedOn(issueDate)
            .serviceDate(issueDate.plusDays(5))
            .responseDeadline(responseDeadline)
            .externalId(claimData.getExternalId().toString())
            .submitterEmail(SampleClaim.SUBMITTER_EMAIL)
            .createdAt(nowInLocalZone())
            .letterHolderId(SampleClaim.LETTER_HOLDER_ID)
            .features(features)
            .state(CREATE)
            .claimSubmissionOperationIndicators(ClaimSubmissionOperationIndicators.builder().build())
            .referenceNumber(getReferenceNumber(claimData.isClaimantRepresented()))
            .build();
    }

    protected void uploadDocument(String authorisation) {
        given(documentUploadClient
            .upload(eq(authorisation), anyString(), anyString(), anyList(),
                any(uk.gov.hmcts.reform.document.domain.Classification.class),
                anyList()))
            .willReturn(successfulDocumentManagementUploadResponse());
    }

    protected void startForCaseWorker(String authorisation, CaseEvent caseEvent) {
        given(coreCaseDataApi.startForCaseworker(
            eq(authorisation),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            eq(caseEvent.getValue())
            )
        ).willReturn(getStartEventResponse(getCaseDetails(null)));
    }

    protected Claim submitForCaseWorker(ClaimData claimData, String authorisation) {
        return submitForCaseWorker(claimData, "1", authorisation);
    }

    protected Claim submitForCaseWorker(
        ClaimData claimData,
        String submitterId,
        String authorisation
    ) {
        Claim claim = getClaim(claimData, FEATURES, submitterId);

        given(coreCaseDataApi.submitForCaseworker(
            eq(authorisation),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            eq(IGNORE_WARNING),
            any()
            )
        ).willReturn(buildCaseDetails(claim));

        return claim;
    }

    protected String getReferenceNumber(boolean isRepresented) {
        return isRepresented
            ? referenceNumberRepository.getReferenceNumberForLegal()
            : referenceNumberRepository.getReferenceNumberForCitizen();
    }

    protected void startUpdateForCitizen(Claim claim, String authorisation, CaseEvent caseEvent) {
        given(coreCaseDataApi.startEventForCitizen(
            eq(authorisation),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            any(),
            eq(caseEvent.getValue())
            )
        ).willReturn(getStartEventResponse(buildCaseDetails(claim)));
    }

    protected void submitUpdateForCitizen(Claim claim, String authorisation) {
        given(coreCaseDataApi.submitEventForCitizen(
            eq(authorisation),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            any(),
            eq(IGNORE_WARNING),
            any()
            )
        ).willReturn(buildCaseDetails(claim));
    }

    protected void startEventForCaseworker(Claim claim, String authorisation, CaseEvent caseEvent) {
        given(coreCaseDataApi.startEventForCaseWorker(
            eq(authorisation),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            any(),
            eq(caseEvent.getValue())
            )
        ).willReturn(getStartEventResponse(buildCaseDetails(claim)));
    }

    protected void submitEventForCaseworker(Claim claim, String authorisation) {
        given(coreCaseDataApi.submitEventForCaseWorker(
            eq(authorisation),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            any(),
            eq(IGNORE_WARNING),
            any()
            )
        ).willReturn(buildCaseDetails(claim));
    }

    protected void searchForCitizen(Claim claim, String authorisation, Map<String, String> searchCriteria) {
        given(coreCaseDataApi.searchForCitizen(
            eq(authorisation),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            eq(searchCriteria)
            )
        ).willReturn(ImmutableList.of(buildCaseDetails(claim)));
    }

    protected void searchForRepresentative(Claim claim, String authorisation, Map<String, String> searchCriteria) {
        given(coreCaseDataApi.searchForCaseworker(
            eq(authorisation),
            eq(SERVICE_TOKEN),
            eq(USER_ID),
            eq(JURISDICTION_ID),
            eq(CASE_TYPE_ID),
            eq(searchCriteria)
            )
        ).willReturn(ImmutableList.of(buildCaseDetails(claim)));
    }

    protected void verifyStartForCaseworker(String authorisation, CaseEvent caseEvent) {
        verify(coreCaseDataApi)
            .startForCaseworker(
                eq(authorisation),
                eq(SERVICE_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION_ID),
                eq(CASE_TYPE_ID),
                eq(caseEvent.getValue())
            );
    }

    protected void verifySubmitForCaseworker(String authorisation) {
        verify(coreCaseDataApi)
            .submitForCaseworker(
                eq(authorisation),
                eq(SERVICE_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION_ID),
                eq(CASE_TYPE_ID),
                eq(IGNORE_WARNING),
                any()
            );
    }

    protected void verifyStartEventForCaseworker(String authorisation, CaseEvent caseEvent) {
        verify(coreCaseDataApi)
            .startEventForCaseWorker(
                eq(authorisation),
                eq(SERVICE_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION_ID),
                eq(CASE_TYPE_ID),
                any(),
                eq(caseEvent.getValue())
            );
    }

    protected void verifySubmitEventForCaseworker(String authorisation, int numberOftimes) {
        verify(coreCaseDataApi, atLeast(numberOftimes))
            .submitEventForCaseWorker(
                eq(authorisation),
                eq(SERVICE_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION_ID),
                eq(CASE_TYPE_ID),
                any(),
                eq(IGNORE_WARNING),
                any()
            );
    }

    protected void verifyStartForCitizen(String authorisation, CaseEvent caseEvent) {
        verify(coreCaseDataApi)
            .startForCitizen(
                eq(authorisation),
                eq(SERVICE_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION_ID),
                eq(CASE_TYPE_ID),
                eq(caseEvent.getValue())
            );
    }

    protected void verifySubmitForCitizen(String authorisation) {
        verify(coreCaseDataApi)
            .submitForCitizen(
                eq(authorisation),
                eq(SERVICE_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION_ID),
                eq(CASE_TYPE_ID),
                eq(IGNORE_WARNING),
                any()
            );
    }

    protected void verifyStartEventForCitizen(String authorisation, CaseEvent caseEvent) {
        verify(coreCaseDataApi)
            .startEventForCitizen(
                eq(authorisation),
                eq(SERVICE_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION_ID),
                eq(CASE_TYPE_ID),
                any(),
                eq(caseEvent.getValue())
            );
    }

    protected void verifySubmitEventForCitizen(String authorisation, int numberOfTimes) {
        verify(coreCaseDataApi, atLeast(numberOfTimes))
            .submitEventForCitizen(
                eq(authorisation),
                eq(SERVICE_TOKEN),
                eq(USER_ID),
                eq(JURISDICTION_ID),
                eq(CASE_TYPE_ID),
                any(),
                eq(IGNORE_WARNING),
                any()
            );
    }

    private StartEventResponse getStartEventResponse(CaseDetails caseDetails) {
        return StartEventResponse.builder()
            .caseDetails(caseDetails)
            .token(CCD_TRANSACTION_TOKEN)
            .build();
    }

    private CaseDetails buildCaseDetails(Claim claim) {
        CCDCase ccdCase = caseMapper.to(claim).toBuilder()
            .id(1516189555935242L)
            .build();

        Map data = jsonMapper.convertValue(ccdCase, Map.class);
        return getCaseDetails(data);
    }

    private CaseDetails getCaseDetails(Map data) {
        return CaseDetails.builder()
            .id(1516189555935243L)
            .jurisdiction(JURISDICTION_ID)
            .caseTypeId(CASE_TYPE_ID)
            .state("open")
            .createdDate(LocalDateTime.now())
            .lastModified(LocalDateTime.now())
            .securityClassification(Classification.PUBLIC)
            .data(data)
            .build();
    }
}
