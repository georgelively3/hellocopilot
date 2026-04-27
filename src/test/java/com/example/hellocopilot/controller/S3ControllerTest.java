package com.example.hellocopilot.controller;

import com.example.hellocopilot.dto.S3ProcessingRequest;
import com.example.hellocopilot.dto.S3ProcessingResult;
import com.example.hellocopilot.dto.TransactionParseError;
import com.example.hellocopilot.dto.TransactionRecord;
import com.example.hellocopilot.exception.GlobalExceptionHandler;
import com.example.hellocopilot.exception.S3ProcessingException;
import com.example.hellocopilot.service.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { S3Controller.class, GlobalExceptionHandler.class })
@DisplayName("S3Controller Unit Tests")
class S3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3Service s3Service;

    private S3ProcessingRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = S3ProcessingRequest.builder()
                .bucket("my-bucket")
                .key("transactions/txns.csv")
                .build();
    }

    // ---- 200 responses ----

    @Test
    @DisplayName("POST /api/s3/process returns 200 with records and no errors")
    void processFile_returns200_withRecords() throws Exception {
        TransactionRecord record = TransactionRecord.builder()
                .userId("user1")
                .timestamp("2024-01-15T10:30:00")
                .status("SUCCESS")
                .build();
        S3ProcessingResult result = S3ProcessingResult.builder()
                .records(List.of(record))
                .errors(List.of())
                .build();
        when(s3Service.processTransactionFile("my-bucket", "transactions/txns.csv")).thenReturn(result);

        mockMvc.perform(post("/api/s3/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].userId").value("user1"))
                .andExpect(jsonPath("$.records[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.errors.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/s3/process returns 200 with empty records when file is empty")
    void processFile_returns200_emptyResult() throws Exception {
        S3ProcessingResult result = S3ProcessingResult.builder()
                .records(List.of())
                .errors(List.of())
                .build();
        when(s3Service.processTransactionFile(anyString(), anyString())).thenReturn(result);

        mockMvc.perform(post("/api/s3/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(0))
                .andExpect(jsonPath("$.errors.length()").value(0));
    }

    @Test
    @DisplayName("POST /api/s3/process returns 200 with mixed records and errors")
    void processFile_returns200_mixedResult() throws Exception {
        TransactionRecord record = TransactionRecord.builder()
                .userId("user1").timestamp("2024-01-15T10:30:00").status("SUCCESS").build();
        TransactionParseError error = TransactionParseError.builder()
                .lineNumber(3).rawLine("bad,data").errorMessage("Expected 3 fields but found 2").build();
        S3ProcessingResult result = S3ProcessingResult.builder()
                .records(List.of(record))
                .errors(List.of(error))
                .build();
        when(s3Service.processTransactionFile(anyString(), anyString())).thenReturn(result);

        mockMvc.perform(post("/api/s3/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].lineNumber").value(3))
                .andExpect(jsonPath("$.errors[0].errorMessage").value("Expected 3 fields but found 2"));
    }

    // ---- 400 validation ----

    @Test
    @DisplayName("POST /api/s3/process returns 400 when bucket is blank")
    void processFile_returns400_whenBucketBlank() throws Exception {
        S3ProcessingRequest bad = S3ProcessingRequest.builder().bucket("").key("key.csv").build();

        mockMvc.perform(post("/api/s3/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/s3/process returns 400 when key is blank")
    void processFile_returns400_whenKeyBlank() throws Exception {
        S3ProcessingRequest bad = S3ProcessingRequest.builder().bucket("my-bucket").key("").build();

        mockMvc.perform(post("/api/s3/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/s3/process returns 400 when body is missing")
    void processFile_returns400_whenBodyMissing() throws Exception {
        mockMvc.perform(post("/api/s3/process")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---- 502 S3 failure ----

    @Test
    @DisplayName("POST /api/s3/process returns 502 when S3ProcessingException is thrown")
    void processFile_returns502_onS3ProcessingException() throws Exception {
        when(s3Service.processTransactionFile(anyString(), anyString()))
                .thenThrow(new S3ProcessingException("Failed to retrieve file from S3: NoSuchBucket"));

        mockMvc.perform(post("/api/s3/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Failed to retrieve file from S3: NoSuchBucket"));
    }
}
