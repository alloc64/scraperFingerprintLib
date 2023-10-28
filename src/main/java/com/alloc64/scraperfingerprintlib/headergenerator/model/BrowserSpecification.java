package com.alloc64.scraperfingerprintlib.headergenerator.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BrowserSpecification {
    private BrowserName name;
    private Integer minVersion;
    private Integer maxVersion;
    private HttpVersion httpVersion;
}