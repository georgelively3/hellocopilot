package com.example.hellocopilot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionParseError {

    private int lineNumber;
    private String rawLine;
    private String errorMessage;
}
