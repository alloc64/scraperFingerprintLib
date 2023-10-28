# ScraperFingerprintLib

Java library used for generation of unique HTTP headers for scraping purposes. Avoid getting slammed by 403's or captchas by leaking your fingerprints while scraping.

This library is a transpiled version of
[header-generator](https://github.com/apify/fingerprint-suite/tree/master/packages/header-generator) and 
[generative-bayesian-network](https://github.com/apify/fingerprint-suite/tree/master/packages/generative-bayesian-network) by apify
[fingerprint-suite](https://github.com/apify/fingerprint-suite/tree/master)

The library does not support browser queries supported by the header-generator. 
The functionality was reduced to necessary header generation given specified `HeaderGenerationOptions`

## Usage
Basic usage is shown below. For more examples see [HeaderGeneratorTest](src/test/java/com/alloc64/scraperfingerprintlib/HeaderGeneratorTest.java)

The library is fully compatible with `BayesianNetwork` from `fingerprint-suite`, so you can use directly data from [header-generator/src/data_files](https://github.com/apify/fingerprint-suite/tree/master/packages/header-generator/src/data_files)

The `BayesianNetwork` data must be unzipped and loaded from the `*.json` files. 

```
HeaderGenerator headerGenerator = new HeaderGenerator(
        new FileInputStream("src/main/resources/headers-order.json"),
        new FileInputStream("src/main/resources/browser-helper-file.json"),
        new FileInputStream("src/main/resources/input-network-definition.json"),
        new FileInputStream("src/main/resources/header-network-definition.json")
);

var result = headerGenerator.getHeaders(
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

```

## License
[Apache 2.0](LICENSE.md)