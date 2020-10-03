package com.github.phoswald.sample.sample;

public class SampleController {

    public String getSamplePage() {
        return new SampleView().render(new SampleViewModel());
    }
}
