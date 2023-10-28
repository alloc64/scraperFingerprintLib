package com.alloc64.scraperfingerprintlib;


import com.alloc64.scraperfingerprintlib.headergenerator.HeaderGenerator;
import com.alloc64.scraperfingerprintlib.headergenerator.model.*;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.util.List;

public class HeaderGeneratorTest {
    @Test
    public void test() throws Exception {
        HeaderGenerator headerGenerator = new HeaderGenerator(
                new FileInputStream("src/main/resources/headers-order.json"),
                new FileInputStream("src/main/resources/browser-helper-file.json"),
                new FileInputStream("src/main/resources/input-network-definition.json"),
                new FileInputStream("src/main/resources/header-network-definition.json")
        );

        var result = headerGenerator.getHeadersBatch(16,
                new HeaderGeneratorOptions()
                        .setBrowsers(new BrowserSpecification()
                                .setName(BrowserName.CHROME)
                                .setMaxVersion(118)
                                .setMinVersion(84)
                                .setHttpVersion(HttpVersion.HTTP2))
                        .setOperatingSystems(List.of(OperatingSystem.WINDOWS))
                        .setDevices(List.of(Device.DESKTOP))
                        .setLocales(List.of("en-US", "en-GB"))
                        .setHttpVersion(HttpVersion.HTTP2),
                new Headers()
        );

        System.currentTimeMillis();

        Headers defaultHeaders = new Headers();
        defaultHeaders.put("X-Test", "test");

        var result2 = headerGenerator.getHeaders(
                new HeaderGeneratorOptions()
                        .setBrowsers(new BrowserSpecification()
                                .setName(BrowserName.CHROME)
                                .setMaxVersion(118)
                                .setMinVersion(84)
                                .setHttpVersion(HttpVersion.HTTP2))
                        .setOperatingSystems(List.of(OperatingSystem.WINDOWS))
                        .setDevices(List.of(Device.DESKTOP))
                        .setLocales(List.of("en-US", "en-GB"))
                        .setHttpVersion(HttpVersion.HTTP2),
                defaultHeaders
        );

        System.currentTimeMillis();
    }
}
