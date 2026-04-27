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

    @NotBlank(message = "Bucket name is required")
    private String bucket;

    @NotBlank(message = "Object key is required")
    private String key;
}
