package uk.gov.hmcts.cmc.ccd.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.cmc.ccd.domain.CCDCase;
import uk.gov.hmcts.cmc.ccd.domain.CCDClaimDocument;
import uk.gov.hmcts.cmc.ccd.domain.CCDCollectionElement;
import uk.gov.hmcts.cmc.domain.models.Claim;
import uk.gov.hmcts.cmc.domain.models.ClaimDocument;
import uk.gov.hmcts.cmc.domain.models.ClaimDocumentCollection;
import uk.gov.hmcts.cmc.domain.models.ClaimDocumentType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ClaimDocumentCollectionMapper {

    private final ClaimDocumentMapper claimDocumentMapper;

    @Autowired
    public ClaimDocumentCollectionMapper(ClaimDocumentMapper claimDocumentMapper) {
        this.claimDocumentMapper = claimDocumentMapper;
    }

    public void to(ClaimDocumentCollection claimDocumentCollection, CCDCase.CCDCaseBuilder builder) {
        if (claimDocumentCollection == null
            || claimDocumentCollection.getClaimDocuments() == null
            || claimDocumentCollection.getClaimDocuments().isEmpty()) {
            return;
        }

        builder.caseDocuments(
            claimDocumentCollection
                .getClaimDocuments()
                .stream()
                .filter(this::isNotPin)
                .filter(this::isNotCCJ)
                .map(claimDocumentMapper::to)
                .collect(Collectors.toList())
        );
    }

    private boolean isNotPin(ClaimDocument claimDocument) {
        return !claimDocument.getDocumentType().equals(ClaimDocumentType.DEFENDANT_PIN_LETTER);
    }

    private boolean isNotCCJ(ClaimDocument claimDocument) {
        return !claimDocument.getDocumentType().equals(ClaimDocumentType.CCJ_REQUEST);
    }

    public void from(CCDCase ccdCase, Claim.ClaimBuilder builder) {
        Objects.requireNonNull(ccdCase, "ccdCase must not be null");

        if (CollectionUtils.isEmpty(ccdCase.getCaseDocuments())) {
            return;
        }

        ClaimDocumentCollection claimDocumentCollection = new ClaimDocumentCollection();

        buildClaimDocumentCollection(claimDocumentCollection, ccdCase.getCaseDocuments());
        buildClaimDocumentCollection(claimDocumentCollection, ccdCase.getStaffUploadedDocuments());

        builder.claimDocumentCollection(claimDocumentCollection);
    }

    private void buildClaimDocumentCollection(ClaimDocumentCollection claimDocumentCollection,
                                              List<CCDCollectionElement<CCDClaimDocument>> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return;
        }
        documents.stream()
            .map(claimDocumentMapper::from)
            .forEach(claimDocumentCollection::addClaimDocument);
    }

}
