package com.example.hellocopilot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3DownloaderImpl implements S3Downloader {

    private final S3Client s3Client;

    @Override
    public InputStream download(String bucket, String key) {
        log.debug("Downloading s3://{}/{}", bucket, key);
        return s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
