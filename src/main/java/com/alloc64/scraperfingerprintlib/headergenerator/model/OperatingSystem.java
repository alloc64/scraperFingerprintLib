package com.alloc64.scraperfingerprintlib.headergenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OperatingSystem {
    WINDOWS("windows"),
    MACOS("macos"),
    LINUX("linux"),
    ANDROID("android"),
    IOS("ios");

    private final String value;
}

