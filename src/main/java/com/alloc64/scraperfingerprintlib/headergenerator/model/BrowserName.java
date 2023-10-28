package com.alloc64.scraperfingerprintlib.headergenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BrowserName {
    CHROME("chrome"),
    FIREFOX("firefox"),
    SAFARI("safari"),
    EDGE("edge");

    private final String value;
}
