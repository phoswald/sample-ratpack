package com.github.phoswald.sample.ratpack.sample;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SampleResource {

    private final String sampleConfig = System.getProperty("app.sample.config");

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
