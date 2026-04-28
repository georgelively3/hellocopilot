package com.example.hellocopilot.service;

import com.example.hellocopilot.dto.S3ProcessingResult;
import com.example.hellocopilot.dto.TransactionParseError;
import com.example.hellocopilot.dto.TransactionRecord;
import com.example.hellocopilot.exception.S3ProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private static final int EXPECTED_FIELDS = 3;
    private static final String HEADER_INDICATOR = "userid";

    private final S3Downloader s3Downloader;

    @Override
    public S3ProcessingResult processTransactionFile(String bucket, String key) {
        log.info("Processing transaction file from S3: bucket={}, key={}", bucket, key);

        List<TransactionRecord> records = new ArrayList<>();
        List<TransactionParseError> errors = new ArrayList<>();

        try (var inputStream = s3Downloader.download(bucket, key);
             var reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.toLowerCase().contains(HEADER_INDICATOR)) {
                    log.debug("Skipping header row at line {}", lineNumber);
                    continue;
                }
                parseLine(line, lineNumber, records, errors);
            }

        } catch (S3Exception e) {
            log.error("S3 error reading bucket={}, key={}: {}", bucket, key, e.getMessage());
            throw new S3ProcessingException("Failed to retrieve file from S3: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("IO error reading S3 object bucket={}, key={}: {}", bucket, key, e.getMessage());
            throw new S3ProcessingException("Failed to read S3 object content", e);
        }

        log.info("Completed processing: {} records, {} errors from bucket={}, key={}",
                records.size(), errors.size(), bucket, key);

        return S3ProcessingResult.builder()
                .records(records)
                .errors(errors)
                .build();
    }

    private void parseLine(String line, int lineNumber,
            List<TransactionRecord> records, List<TransactionParseError> errors) {

        if (line.isBlank()) {
            log.debug("Skipping blank line {}", lineNumber);
            return;
        }

        String[] fields = line.split(",", -1);

        if (fields.length != EXPECTED_FIELDS) {
            String msg = String.format("Expected %d fields but found %d", EXPECTED_FIELDS, fields.length);
            recordError(errors, lineNumber, line, msg);
            return;
        }

        String userId = fields[0].trim();
        String timestamp = fields[1].trim();
        String status = fields[2].trim();

        if (userId.isEmpty()) {
            recordError(errors, lineNumber, line, "userId must not be blank");
            return;
        }

        if (timestamp.isEmpty()) {
            recordError(errors, lineNumber, line, "timestamp must not be blank");
            return;
        }

        try {
            LocalDateTime.parse(timestamp);
        } catch (DateTimeParseException e) {
            recordError(errors, lineNumber, line, "Invalid timestamp format: " + timestamp);
            return;
        }

        if (status.isEmpty()) {
            recordError(errors, lineNumber, line, "status must not be blank");
            return;
        }

        records.add(TransactionRecord.builder()
                .userId(userId)
                .timestamp(timestamp)
                .status(status)
                .build());
    }

    private void recordError(List<TransactionParseError> errors, int lineNumber,
            String rawLine, String message) {
        log.error("Malformed record at line {}: [{}] - {}", lineNumber, rawLine, message);
        errors.add(TransactionParseError.builder()
                .lineNumber(lineNumber)
                .rawLine(rawLine)
                .errorMessage(message)
                .build());
    }
}
