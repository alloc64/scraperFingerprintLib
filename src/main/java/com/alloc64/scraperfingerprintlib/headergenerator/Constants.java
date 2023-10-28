package com.alloc64.scraperfingerprintlib.headergenerator;

import com.alloc64.scraperfingerprintlib.headergenerator.model.OperatingSystem;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Constants {
    public static final List<String> SUPPORTED_BROWSERS = Arrays.stream(OperatingSystem.values())
            .map(OperatingSystem::getValue)
            .collect(Collectors.toList());
    public static final String BROWSER_HTTP_NODE_NAME = "*BROWSER_HTTP";
    public static final String OPERATING_SYSTEM_NODE_NAME = "*OPERATING_SYSTEM";
    public static final String DEVICE_NODE_NAME = "*DEVICE";
    public static final String MISSING_VALUE_DATASET_TOKEN = "*MISSING_VALUE*";
    public static final String SITE = "site";
    public static final String MODE = "mode";
    public static final String USER = "user";
    public static final String DEST = "dest";
    public static final Map<String, String> HTTP1_SEC_FETCH_ATTRIBUTES = Map.of(
            MODE, "Sec-Fetch-Mode",
            DEST, "Sec-Fetch-Dest",
            SITE, "Sec-Fetch-Site",
            USER, "Sec-Fetch-User"
    );
    public static final Map<String, String> HTTP2_SEC_FETCH_ATTRIBUTES = Map.of(
            MODE, "sec-fetch-mode",
            DEST, "sec-fetch-dest",
            SITE, "sec-fetch-site",
            USER, "sec-fetch-user"
    );
}