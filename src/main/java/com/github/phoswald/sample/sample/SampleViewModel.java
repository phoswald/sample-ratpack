package com.github.phoswald.sample.sample;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

public class SampleViewModel {

    public String now = ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    public Map<String, String> env = new TreeMap<>(System.getenv());
    public Map<Object, Object> props = new TreeMap<>(System.getProperties());
}
