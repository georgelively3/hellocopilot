package com.example.hellocopilot.service;

import java.io.IOException;
import java.io.InputStream;

public interface S3Downloader {

    InputStream download(String bucket, String key) throws IOException;
}
