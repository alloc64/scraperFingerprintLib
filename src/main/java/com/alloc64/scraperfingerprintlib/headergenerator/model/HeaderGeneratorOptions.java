package com.alloc64.scraperfingerprintlib.headergenerator.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class HeaderGeneratorOptions {
    private BrowserSpecification browsers;
    private List<OperatingSystem> operatingSystems;
    private List<Device> devices;
    private List<String> locales;
    private HttpVersion httpVersion;
}