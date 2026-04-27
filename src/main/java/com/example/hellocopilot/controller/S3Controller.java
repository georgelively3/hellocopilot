package com.example.hellocopilot.controller;

import com.example.hellocopilot.dto.S3ProcessingRequest;
import com.example.hellocopilot.dto.S3ProcessingResult;
import com.example.hellocopilot.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    /**
     * POST /api/s3/process
     * Downloads and parses a CSV transaction file from S3.
     * Returns parsed records and any malformed-line errors.
     */
    @PostMapping("/process")
    public ResponseEntity<S3ProcessingResult> processFile(
            @Valid @RequestBody S3ProcessingRequest request) {
        log.info("Received S3 process request: bucket={}, key={}", request.getBucket(), request.getKey());
        S3ProcessingResult result = s3Service.processTransactionFile(request.getBucket(), request.getKey());
        return ResponseEntity.ok(result);
    }
}
