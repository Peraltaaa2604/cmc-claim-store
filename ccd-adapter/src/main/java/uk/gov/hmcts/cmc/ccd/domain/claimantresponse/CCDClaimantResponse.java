package uk.gov.hmcts.cmc.ccd.domain.claimantresponse;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "claimantResponseType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CCDResponseAcceptation.class, name = "ACCEPTATION"),
    @JsonSubTypes.Type(value = CCDResponseRejection.class, name = "REJECTION")
})
@Getter
@EqualsAndHashCode
public abstract class CCDClaimantResponse {
    private BigDecimal amountPaid;
    private final LocalDateTime submittedOn;

    public CCDClaimantResponse(BigDecimal amountPaid, LocalDateTime submittedOn) {
        this.amountPaid = amountPaid;
        this.submittedOn = submittedOn;
    }

    public abstract CCDClaimantResponseType getClaimantResponseType();

}