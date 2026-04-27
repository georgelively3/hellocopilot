package com.example.hellocopilot.service;

import com.example.hellocopilot.dto.S3ProcessingResult;
import com.example.hellocopilot.exception.S3ProcessingException;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3ServiceImpl Unit Tests")
class S3ServiceImplTest {

    @Mock
    private S3Downloader s3Downloader;

    @InjectMocks
    private S3ServiceImpl s3Service;

    private static InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    // ---- Happy path ----

    @Test
    @DisplayName("processes valid CSV with header and returns all records")
    void processTransactionFile_validCsv_returnsRecords() throws IOException {
        String csv = "userId,timestamp,status\n"
                + "user1,2024-01-15T10:30:00,SUCCESS\n"
                + "user2,2024-01-15T10:31:00,FAILED\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("my-bucket", "txns.csv");

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getRecords().get(0).getUserId()).isEqualTo("user1");
        assertThat(result.getRecords().get(0).getTimestamp()).isEqualTo("2024-01-15T10:30:00");
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getRecords().get(1).getUserId()).isEqualTo("user2");
    }

    @Test
    @DisplayName("processes CSV without header row")
    void processTransactionFile_noHeader_returnsRecords() throws IOException {
        String csv = "user1,2024-01-15T10:30:00,SUCCESS\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("processes empty file and returns no records or errors")
    void processTransactionFile_emptyFile_returnsEmpty() throws IOException {
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(""));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("skips blank lines without adding errors")
    void processTransactionFile_blankLines_skipped() throws IOException {
        String csv = "user1,2024-01-15T10:30:00,SUCCESS\n\n\nuser2,2024-01-15T10:31:00,PENDING\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getErrors()).isEmpty();
    }

    // ---- Malformed record handling ----

    @Test
    @DisplayName("records error for line with wrong number of fields")
    void processTransactionFile_wrongFieldCount_addsError() throws IOException {
        String csv = "user1,2024-01-15T10:30:00\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getLineNumber()).isEqualTo(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).contains("Expected 3 fields");
    }

    @Test
    @DisplayName("records error for blank userId")
    void processTransactionFile_blankUserId_addsError() throws IOException {
        String csv = ",2024-01-15T10:30:00,SUCCESS\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).contains("userId");
    }

    @Test
    @DisplayName("records error for blank timestamp")
    void processTransactionFile_blankTimestamp_addsError() throws IOException {
        String csv = "user1,,SUCCESS\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).contains("timestamp");
    }

    @Test
    @DisplayName("records error for unparseable timestamp")
    void processTransactionFile_badTimestamp_addsError() throws IOException {
        String csv = "user1,not-a-date,SUCCESS\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).contains("Invalid timestamp format");
        assertThat(result.getErrors().get(0).getRawLine()).isEqualTo("user1,not-a-date,SUCCESS");
    }

    @Test
    @DisplayName("records error for blank status")
    void processTransactionFile_blankStatus_addsError() throws IOException {
        String csv = "user1,2024-01-15T10:30:00,\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).contains("status");
    }

    @Test
    @DisplayName("processes mix of valid and malformed lines")
    void processTransactionFile_mixedLines_separatesRecordsAndErrors() throws IOException {
        String csv = "userId,timestamp,status\n"
                + "user1,2024-01-15T10:30:00,SUCCESS\n"
                + "BAD_LINE_ONLY_TWO_FIELDS,oops\n"
                + "user2,2024-01-15T10:31:00,PENDING\n"
                + ",2024-01-15T10:32:00,FAILED\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getErrors()).hasSize(2);
    }

    // ---- Error propagation ----

    @Test
    @DisplayName("throws S3ProcessingException on S3Exception")
    void processTransactionFile_s3Exception_throwsS3ProcessingException() throws IOException {
        S3Exception s3Ex = (S3Exception) S3Exception.builder()
                .message("NoSuchBucket")
                .statusCode(404)
                .build();
        when(s3Downloader.download(anyString(), anyString())).thenThrow(s3Ex);

        assertThatThrownBy(() -> s3Service.processTransactionFile("bad-bucket", "key"))
                .isInstanceOf(S3ProcessingException.class)
                .hasMessageContaining("Failed to retrieve file from S3");
    }

    @Test
    @DisplayName("throws S3ProcessingException on IOException during read")
    void processTransactionFile_ioException_throwsS3ProcessingException() throws IOException {
        when(s3Downloader.download(anyString(), anyString()))
                .thenThrow(new IOException("Simulated read failure"));

        assertThatThrownBy(() -> s3Service.processTransactionFile("bucket", "key"))
                .isInstanceOf(S3ProcessingException.class)
                .hasMessageContaining("Failed to read S3 object content");
    }

    // ---- Edge cases ----

    @Test
    @DisplayName("handles header-only file with no data rows")
    void processTransactionFile_headerOnly_returnsEmpty() throws IOException {
        String csv = "userId,timestamp,status\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("trims whitespace from field values")
    void processTransactionFile_fieldsWithWhitespace_trimmed() throws IOException {
        String csv = "  user1  , 2024-01-15T10:30:00 , SUCCESS \n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getUserId()).isEqualTo("user1");
        assertThat(result.getRecords().get(0).getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("records error when line has too many fields")
    void processTransactionFile_tooManyFields_addsError() throws IOException {
        String csv = "user1,2024-01-15T10:30:00,SUCCESS,EXTRA\n";
        when(s3Downloader.download(anyString(), anyString())).thenReturn(toStream(csv));

        S3ProcessingResult result = s3Service.processTransactionFile("bucket", "key");

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getErrorMessage()).contains("Expected 3 fields but found 4");
    }
}
