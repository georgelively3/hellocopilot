package com.example.hellocopilot.service;

import com.example.hellocopilot.dto.S3ProcessingResult;

public interface S3Service {

    S3ProcessingResult processTransactionFile(String bucket, String key);
}
