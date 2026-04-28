package com.example.hellocopilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class S3ProcessingResult {

    private List<TransactionRecord> records;
    private List<TransactionParseError> errors;
}
