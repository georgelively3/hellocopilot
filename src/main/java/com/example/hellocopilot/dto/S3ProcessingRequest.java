package com.example.hellocopilot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class S3ProcessingRequest {

    @NotBlank(message = "bucket must not be blank")
    private String bucket;

    @NotBlank(message = "key must not be blank")
    private String key;
}
