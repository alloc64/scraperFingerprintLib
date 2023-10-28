package com.alloc64.scraperfingerprintlib.headergenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HttpVersion {
    HTTP1("1"),
    HTTP2("2");

    private final String value;

    public static HttpVersion from(String httpVersion) {
        HttpVersion result = null;
        if (httpVersion != null) {
            switch (httpVersion) {
                case "1" -> result = HTTP1;
                case "2" -> result = HTTP2;
            }
        }

        return result;
    }
}
