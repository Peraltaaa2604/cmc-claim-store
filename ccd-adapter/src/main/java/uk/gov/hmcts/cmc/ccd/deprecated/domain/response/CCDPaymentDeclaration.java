package uk.gov.hmcts.cmc.ccd.deprecated.domain.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class CCDPaymentDeclaration {
    private LocalDate paidDate;
    private String explanation;
}
