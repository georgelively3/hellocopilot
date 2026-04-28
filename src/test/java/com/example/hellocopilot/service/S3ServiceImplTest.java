package com.example.hellocopilot.service;

import com.example.hellocopilot.dto.S3ProcessingResult;
import com.example.hellocopilot.exception.S3ProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceImplTest {

    @Mock
    private S3Downloader s3Downloader;

    @InjectMocks
    private S3ServiceImpl s3Service;

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void processTransactionFile_parsesValidCsvWithHeader() throws IOException {
        String csv = "userId,timestamp,status\n"
                + "user1,2024-01-15T10:30:00,SUCCESS\n"
                + "user2,2024-01-15T11:00:00,FAILED\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getRecords().get(0).getUserId()).isEqualTo("user1");
        assertThat(result.getRecords().get(0).getTimestamp()).isEqualTo("2024-01-15T10:30:00");
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void processTransactionFile_parsesValidCsvWithoutHeader() throws IOException {
        String csv = "user1,2024-01-15T10:30:00,SUCCESS\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void processTransactionFile_returnsEmptyResultForEmptyFile() throws IOException {
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(""));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void processTransactionFile_skipsBlankLines() throws IOException {
        String csv = "user1,2024-01-15T10:30:00,SUCCESS\n\nuser2,2024-01-15T11:00:00,FAILED\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void processTransactionFile_recordsErrorForWrongFieldCount() throws IOException {
        String csv = "user1,2024-01-15T10:30:00\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).contains("Expected 3 fields but found 2");
    }

    @Test
    void processTransactionFile_recordsErrorForTooManyFields() throws IOException {
        String csv = "user1,2024-01-15T10:30:00,SUCCESS,EXTRA\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).contains("Expected 3 fields but found 4");
    }

    @Test
    void processTransactionFile_recordsErrorForBlankUserId() throws IOException {
        String csv = ",2024-01-15T10:30:00,SUCCESS\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).isEqualTo("userId must not be blank");
    }

    @Test
    void processTransactionFile_recordsErrorForBlankTimestamp() throws IOException {
        String csv = "user1,,SUCCESS\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).isEqualTo("timestamp must not be blank");
    }

    @Test
    void processTransactionFile_recordsErrorForInvalidTimestampFormat() throws IOException {
        String csv = "user1,not-a-date,SUCCESS\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).contains("Invalid timestamp format");
    }

    @Test
    void processTransactionFile_recordsErrorForBlankStatus() throws IOException {
        String csv = "user1,2024-01-15T10:30:00,\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).isEqualTo("status must not be blank");
    }

    @Test
    void processTransactionFile_continuesAfterMalformedRows() throws IOException {
        String csv = "userId,timestamp,status\n"
                + "user1,2024-01-15T10:30:00,SUCCESS\n"
                + "BAD_LINE_ONLY_TWO_FIELDS,oops\n"
                + "user2,2024-01-15T10:31:00,PENDING\n"
                + ",2024-01-15T10:32:00,FAILED\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getErrors()).hasSize(2);
    }

    @Test
    void processTransactionFile_throwsS3ProcessingExceptionOnS3Error() throws IOException {
        S3Exception s3ex = (S3Exception) S3Exception.builder().message("NoSuchBucket").build();
        when(s3Downloader.download("bad-bucket", "key")).thenThrow(s3ex);

        assertThatThrownBy(() -> s3Service.processTransactionFile("bad-bucket", "key"))
                .isInstanceOf(S3ProcessingException.class)
                .hasMessageContaining("Failed to retrieve file from S3");
    }

    @Test
    void processTransactionFile_throwsS3ProcessingExceptionOnIOError() throws IOException {
        when(s3Downloader.download("bucket", "key")).thenThrow(new IOException("Simulated read failure"));

        assertThatThrownBy(() -> s3Service.processTransactionFile("bucket", "key"))
                .isInstanceOf(S3ProcessingException.class)
                .hasMessage("Failed to read S3 object content");
    }

    @Test
    void processTransactionFile_trimsWhitespaceAroundFields() throws IOException {
        String csv = " user1 , 2024-01-15T10:30:00 , SUCCESS \n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getUserId()).isEqualTo("user1");
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void processTransactionFile_headerOnlyFileReturnsEmpty() throws IOException {
        String csv = "userId,timestamp,status\n";
        when(s3Downloader.download("my-bucket", "tx.csv")).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "tx.csv");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).isEmpty();
    }
}
