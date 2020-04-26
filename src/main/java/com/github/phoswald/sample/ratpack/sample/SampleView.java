package com.github.phoswald.sample.ratpack.sample;

import com.github.phoswald.sample.ratpack.AbstractView;

public class SampleView extends AbstractView<SampleViewModel> {

    public SampleView() {
        super("sample", "model");
    }
}
