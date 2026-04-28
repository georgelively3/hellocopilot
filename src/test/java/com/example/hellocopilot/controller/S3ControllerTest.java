package com.example.hellocopilot.controller;

import com.example.hellocopilot.dto.S3ProcessingResult;
import com.example.hellocopilot.dto.TransactionParseError;
import com.example.hellocopilot.dto.TransactionRecord;
import com.example.hellocopilot.exception.GlobalExceptionHandler;
import com.example.hellocopilot.exception.S3ProcessingException;
import com.example.hellocopilot.service.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {S3Controller.class, GlobalExceptionHandler.class})
class S3ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3Service s3Service;

    @Test
    void processFile_returns200WithRecords() throws Exception {
        S3ProcessingResult result = S3ProcessingResult.builder()
                .records(List.of(TransactionRecord.builder()
                        .userId("user1")
                        .timestamp("2024-01-15T10:30:00")
                        .status("SUCCESS")
                        .build()))
                .errors(List.of())
                .build();

        when(s3Service.processTransactionFile("my-bucket", "tx.csv")).thenReturn(result);

        mockMvc.perform(post("/api/s3/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bucket\":\"my-bucket\",\"key\":\"tx.csv\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].userId").value("user1"))
                .andExpect(jsonPath("$.records[0].timestamp").value("2024-01-15T10:30:00"))
                .andExpect(jsonPath("$.records[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void processFile_returns200WithEmptyResult() throws Exception {
        S3ProcessingResult result = S3ProcessingResult.builder()
                .records(List.of())
                .errors(List.of())
                .build();

        when(s3Service.processTransactionFile("my-bucket", "empty.csv")).thenReturn(result);

        mockMvc.perform(post("/api/s3/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bucket\":\"my-bucket\",\"key\":\"empty.csv\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isEmpty())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void processFile_returns200WithMixedRecordsAndErrors() throws Exception {
        S3ProcessingResult result = S3ProcessingResult.builder()
                .records(List.of(TransactionRecord.builder()
                        .userId("user1").timestamp("2024-01-15T10:30:00").status("SUCCESS").build()))
                .errors(List.of(TransactionParseError.builder()
                        .lineNumber(3).rawLine("bad,line").errorMessage("Expected 3 fields but found 2").build()))
                .build();

        when(s3Service.processTransactionFile("my-bucket", "mixed.csv")).thenReturn(result);

        mockMvc.perform(post("/api/s3/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bucket\":\"my-bucket\",\"key\":\"mixed.csv\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isNotEmpty())
                .andExpect(jsonPath("$.errors[0].lineNumber").value(3))
                .andExpect(jsonPath("$.errors[0].errorMessage").value("Expected 3 fields but found 2"));
    }

    @Test
    void processFile_returns400WhenBucketIsBlank() throws Exception {
        mockMvc.perform(post("/api/s3/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bucket\":\"\",\"key\":\"tx.csv\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processFile_returns400WhenKeyIsBlank() throws Exception {
        mockMvc.perform(post("/api/s3/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bucket\":\"my-bucket\",\"key\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processFile_returns400WhenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/s3/process")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processFile_returns502OnS3ProcessingException() throws Exception {
        when(s3Service.processTransactionFile("bad-bucket", "tx.csv"))
                .thenThrow(new S3ProcessingException("Failed to retrieve file from S3: NoSuchBucket"));

        mockMvc.perform(post("/api/s3/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bucket\":\"bad-bucket\",\"key\":\"tx.csv\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Failed to retrieve file from S3: NoSuchBucket"));
    }

    @Test
    void processFile_returns400WhenBucketIsMissing() throws Exception {
        mockMvc.perform(post("/api/s3/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\":\"tx.csv\"}"))
                .andExpect(status().isBadRequest());
    }
}
