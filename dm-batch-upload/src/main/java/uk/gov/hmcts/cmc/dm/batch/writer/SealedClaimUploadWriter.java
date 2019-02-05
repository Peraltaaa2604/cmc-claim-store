package uk.gov.hmcts.cmc.dm.batch.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.hmcts.cmc.dm.batch.model.Claim;

import java.util.List;
import java.util.Optional;

@Component
public class SealedClaimUploadWriter implements ItemWriter<Claim> {

    private static final Logger log = LoggerFactory.getLogger(SealedClaimUploadWriter.class);

    private static final String SEALED_CLAIM_URL_PATH = "documents/sealedClaim/";

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public SealedClaimUploadWriter(RestTemplate restTemplate, @Value("${claimstore.api.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    // TODO: remove this before merging to master.This is temporary and will be replaced with a
    // token fetched from userservice
    private final String tempAuth = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJvOTFxMXM4aXVvazlwNDVl"
        + "ZDdlbHE1dTdoMSIsInN1YiI6IjI4IiwiaWF0IjoxNTQ5Mzc1NDE2LCJleHAiOjE1NDk0MDQyMTYsImRhdGEiOiJ"
        + "jaXRpemVuLGNsYWltYW50LGNpdGl6ZW4tbG9hMSxjbGFpbWFudC1sb2ExIiwidHlwZSI6IkFDQ0VTUyIsImlkIjoiM"
        + "jgiLCJmb3JlbmFtZSI6IkpvaG4iLCJzdXJuYW1lIjoiU21pdGgiLCJkZWZhdWx0LXNlcnZpY2UiOiJjbWMiLCJsb2E"
        + "iOjEsImRlZmF1bHQtdXJsIjoiaHR0cHM6Ly93d3ctY2l0aXplbi5tb25leWNsYWltLnJlZm9ybS5obWN0cy5uZXQ6M"
        + "zAwMC9jbWMiLCJncm91cCI6ImNpdGl6ZW5zIn0.2tHfr3L608DdNEjHmx5ZpYq9E6XvjlZPPrU28zNB984";

    @Override
    public void write(List<? extends Claim> items) throws Exception {
        if (items.size() == 0) {
            log.info("There are no claims with missing sealed claim documents");
        }
        items.forEach(claim -> {
            try {
                log.info("Trying to upload sealed claim form for claim: {}", claim.getReferenceNumber());
                HttpHeaders headers = new HttpHeaders();
                headers.add("Authorization", tempAuth);
                HttpEntity<String> httpEntity = new HttpEntity<>(headers);
                UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(SEALED_CLAIM_URL_PATH)
                    .path(claim.getExternalId());
                ResponseEntity<ByteArrayResource> entity = restTemplate.exchange(builder.toUriString(),
                    HttpMethod.GET,
                    httpEntity,
                    ByteArrayResource.class);
                Optional body = Optional.ofNullable(entity.getBody());
                if (entity.getStatusCode() == HttpStatus.OK && body.isPresent()) {
                    log.info("Sealed claim upload for claim: {} was successful", claim.getReferenceNumber());
                } else {
                    log.error("Sealed claim upload for claim: {} failed", claim.getReferenceNumber());
                }
            } catch (Exception ex) {
                log.error("Sealed claim upload writer failure", ex);
                throw ex;
            }
        });
    }
}
