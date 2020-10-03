package com.github.phoswald.sample.sample;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class SampleResource {

    private final String sampleConfig = Optional.ofNullable(System.getenv("APP_SAMPLE_CONFIG")).orElse("Undefined");

    public String getTime() {
        return ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }

    public String getConfig() {
        return sampleConfig;
    }

    public EchoResponse postEcho(EchoRequest request) {
        EchoResponse response = new EchoResponse();
        response.setOuput("Received " + request.getInput());
        return response;
    }
}
