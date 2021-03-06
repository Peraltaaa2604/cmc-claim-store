package uk.gov.hmcts.cmc.email;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;

import static java.util.Objects.requireNonNull;

public class EmailAttachment {

    private final InputStreamSource data;
    private final String contentType;
    private final String filename;

    public EmailAttachment(InputStreamSource data, String contentType, String filename) {
        requireNonNull(data);
        requireNonNull(contentType);
        requireNonNull(filename);
        this.data = data;
        this.contentType = contentType;
        this.filename = filename;
    }

    public static EmailAttachment pdf(byte[] content, String fileName) {
        return create("application/pdf", content, fileName);
    }

    public static EmailAttachment json(byte[] content, String fileName) {
        return create("application/json", content, fileName);
    }

    public static EmailAttachment csv(byte[] content, String fileName) {
        return create("text/csv", content, fileName);
    }

    private static EmailAttachment create(String contentType, byte[] content, String filename) {
        return new EmailAttachment(
            new ByteArrayResource(content),
            contentType,
            filename
        );
    }

    public InputStreamSource getData() {
        return data;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String toString() {
        return String.format("EmailAttachment{contentType='%s', filename='%s'}", filename, contentType);
    }
}
