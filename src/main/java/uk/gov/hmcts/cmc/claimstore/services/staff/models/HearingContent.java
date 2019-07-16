package uk.gov.hmcts.cmc.claimstore.services.staff.models;

import lombok.Builder;
import uk.gov.hmcts.cmc.domain.models.directionsquestionnaire.ExpertReport;
import uk.gov.hmcts.cmc.domain.models.directionsquestionnaire.UnavailableDate;
import uk.gov.hmcts.cmc.domain.models.directionsquestionnaire.Witness;

import java.util.List;

@Builder
public class HearingContent {
    List<String> supportRequired;
    String hearingLocation;
    String locationReason;
    String hasExpertReport;
    List<ExpertReport> expertReports;
    String courtPermissionForExpertReport;
    String expertExamineNeeded;
    String whatToExamine;
    String reasonWhyExpertAdvice;
    String evidenceRequired;
    Witness witness;
    List<UnavailableDate> unavailableDates;
}
