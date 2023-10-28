package com.alloc64.scraperfingerprintlib.headergenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Device {
    DESKTOP("desktop"),
    MOBILE("mobile");

    private final String value;
}
