package com.alloc64.scraperfingerprintlib.headergenerator.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class HttpBrowserObject {
    private String name; // BrowserName | typeof MISSING_VALUE_DATASET_TOKEN;
    private List<Integer> version;
    private String completeString;
    private HttpVersion httpVersion;
}