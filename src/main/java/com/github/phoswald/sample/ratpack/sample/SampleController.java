package com.github.phoswald.sample.ratpack.sample;

public class SampleController {

    public String getSamplePage() {
        return new SampleView().render(new SampleViewModel());
    }
}
