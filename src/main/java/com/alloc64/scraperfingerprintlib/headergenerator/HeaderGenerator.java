package com.alloc64.scraperfingerprintlib.headergenerator;

import com.alloc64.scraperfingerprintlib.bayesiannet.BayesianNetwork;
import com.alloc64.scraperfingerprintlib.headergenerator.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.alloc64.scraperfingerprintlib.headergenerator.Constants.*;

public class HeaderGenerator {
    private final BayesianNetwork inputGeneratorNetwork;
    private final BayesianNetwork headerGeneratorNetwork;
    private final List<HttpBrowserObject> uniqueBrowsers = new ArrayList<>();
    private final Map<String, List<String>> headersOrder;

    public HeaderGenerator(InputStream headersOrderStream,
                           InputStream browserHelperFileStream,
                           InputStream inputGeneratorNetworkStream,
                           InputStream headerGeneratorNetworkStream) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        this.headersOrder = objectMapper.readValue(headersOrderStream, new TypeReference<>() {
        });
        List<String> uniqueBrowserStrings = objectMapper.readValue(browserHelperFileStream, new TypeReference<>() {
        });

        for (String browserString : uniqueBrowserStrings) {
            // There are headers without user agents in the datasets we used to configure the generator. They should be disregarded.
            if (!browserString.equals(MISSING_VALUE_DATASET_TOKEN))
                this.uniqueBrowsers.add(this.prepareHttpBrowserObject(browserString));
        }

        this.inputGeneratorNetwork = new BayesianNetwork(inputGeneratorNetworkStream);
        this.headerGeneratorNetwork = new BayesianNetwork(headerGeneratorNetworkStream);
    }

    public List<Headers> getHeadersBatch(int batchSize,
                                         HeaderGeneratorOptions options,
                                         Headers requestDependentHeaders) {
        List<Headers> headersBatch = new ArrayList<>();
        for (int i = 0; i < batchSize; i++)
            headersBatch.add(getHeaders(options, requestDependentHeaders));

        return headersBatch;
    }

    public Headers getHeaders(HeaderGeneratorOptions options, Headers requestDependentHeaders) {
        Map<String, List<String>> possibleAttributeValues = getPossibleAttributeValues(options);
        Map<String, String> inputSample
                = inputGeneratorNetwork.generateConsistentSampleWhenPossible(possibleAttributeValues);

        if (inputSample.isEmpty())
            throw new IllegalStateException("Could not generate a consistent sample for the following options. " +
                    "Relax your settings and try again.");

        Map<String, String> generatedSample = headerGeneratorNetwork.generateSample(inputSample);
        HttpBrowserObject generatedHttpAndBrowser = prepareHttpBrowserObject(generatedSample.get(BROWSER_HTTP_NODE_NAME));
        Map<String, String> secFetchAttributeNames = HTTP2_SEC_FETCH_ATTRIBUTES;
        String acceptLanguageFieldName = "accept-language";
        if (!generatedHttpAndBrowser.getHttpVersion().equals(HttpVersion.HTTP2)) {
            acceptLanguageFieldName = "Accept-Language";
            secFetchAttributeNames = HTTP1_SEC_FETCH_ATTRIBUTES;
        }

        generatedSample.put(acceptLanguageFieldName, getAcceptLanguageField(options.getLocales()));

        boolean isChrome = generatedHttpAndBrowser.getName().equals(BrowserName.CHROME.getValue());
        boolean isFirefox = generatedHttpAndBrowser.getName().equals(BrowserName.FIREFOX.getValue());
        boolean isEdge = generatedHttpAndBrowser.getName().equals(BrowserName.EDGE.getValue());

        boolean hasSecFetch = (isChrome && generatedHttpAndBrowser.getVersion().get(0) >= 76)
                || (isFirefox && generatedHttpAndBrowser.getVersion().get(0) >= 90)
                || (isEdge && generatedHttpAndBrowser.getVersion().get(0) >= 79);

        if (hasSecFetch) {
            generatedSample.put(secFetchAttributeNames.get(Constants.SITE), "same-site");
            generatedSample.put(secFetchAttributeNames.get(Constants.MODE), "navigate");
            generatedSample.put(secFetchAttributeNames.get(Constants.USER), "?1");
            generatedSample.put(secFetchAttributeNames.get(Constants.DEST), "document");
        }

        generatedSample.entrySet()
                .removeIf(entry -> entry.getKey().equalsIgnoreCase("connection") && entry.getValue().equals("close")
                        || entry.getKey().startsWith("*") || entry.getValue().equals(MISSING_VALUE_DATASET_TOKEN));

        var map = new Headers();
        map.putAll(generatedSample);
        map.putAll(requestDependentHeaders);

        return orderHeaders(map, headersOrder.get(generatedHttpAndBrowser.getName()));
    }

    private Map<String, List<String>> getPossibleAttributeValues(HeaderGeneratorOptions headerOptions) {
        List<BrowserSpecification> browsers = prepareBrowsersConfig(
                headerOptions.getBrowsers(), headerOptions.getHttpVersion()
        );

        List<String> browserHttpOptions = getBrowserHttpOptions(browsers);
        Map<String, List<String>> possibleAttributeValues = new HashMap<>();

        possibleAttributeValues.put(BROWSER_HTTP_NODE_NAME, browserHttpOptions);
        possibleAttributeValues.put(OPERATING_SYSTEM_NODE_NAME, headerOptions.getOperatingSystems()
                .stream()
                .map(OperatingSystem::getValue)
                .collect(Collectors.toList()));

        if (headerOptions.getDevices() != null) {
            possibleAttributeValues.put(DEVICE_NODE_NAME, headerOptions.getDevices()
                    .stream()
                    .map(Device::getValue)
                    .collect(Collectors.toList()));
        }

        return possibleAttributeValues;
    }

    private List<String> getBrowserHttpOptions(List<BrowserSpecification> browsers) {
        List<String> browserHttpOptions = new ArrayList<>();
        for (BrowserSpecification browser : browsers) {
            uniqueBrowsers.stream()
                    .filter(browserOption -> browser.getName().getValue().equals(browserOption.getName())
                            && ((browser.getMinVersion() == null || browserVersionIsLesserOrEquals(List.of(browser.getMinVersion()), browserOption.getVersion()))
                            && (browser.getMaxVersion() == null || browserVersionIsLesserOrEquals(browserOption.getVersion(), List.of(browser.getMaxVersion())))
                            && browser.getHttpVersion() == browserOption.getHttpVersion()))
                    .forEach(browserOption -> browserHttpOptions.add(browserOption.getCompleteString()));
        }
        return browserHttpOptions;
    }

    private boolean browserVersionIsLesserOrEquals(List<Integer> browserVersionL, List<Integer> browserVersionR) {
        return browserVersionL.get(0) <= browserVersionR.get(0);
    }

    public Headers orderHeaders(Headers headers, List<String> order) {
        Headers orderedSample = new Headers();

        order.stream()
                .filter(headers::containsKey)
                .forEach(attribute -> orderedSample.put(attribute, headers.get(attribute)));

        headers.keySet()
                .stream()
                .filter(attribute -> !order.contains(attribute))
                .forEach(attribute -> orderedSample.put(attribute, headers.get(attribute)));

        return orderedSample;
    }

    private String getAcceptLanguageField(List<String> localesFromOptions) {
        List<String> locales = new ArrayList<>(localesFromOptions);
        List<String> highLevelLocales = new ArrayList<>();

        locales.forEach(locale -> {
            if (!locale.contains("-"))
                highLevelLocales.add(locale);
        });

        locales.forEach(locale -> {
            if (!highLevelLocales.contains(locale)) {
                boolean highLevelEquivalentPresent = highLevelLocales.stream()
                        .anyMatch(locale::contains);

                if (!highLevelEquivalentPresent)
                    highLevelLocales.add(locale);
            }
        });

        Collections.shuffle(highLevelLocales);
        Collections.shuffle(locales);

        List<String> localesInAddingOrder = new ArrayList<>();

        highLevelLocales.forEach(highLevelLocale -> {
            locales.forEach(locale -> {
                if (locale.contains(highLevelLocale) && !highLevelLocales.contains(locale))
                    localesInAddingOrder.add(locale);
            });
            localesInAddingOrder.add(highLevelLocale);
        });

        StringBuilder acceptLanguageFieldValue = new StringBuilder(localesInAddingOrder.get(0));

        for (int x = 1; x < localesInAddingOrder.size(); x++)
            acceptLanguageFieldValue.append(",").append(localesInAddingOrder.get(x)).append(";q=").append(1.0 - x * 0.1);

        return acceptLanguageFieldValue.toString();
    }

    private HttpBrowserObject prepareHttpBrowserObject(String httpBrowserString) {
        String[] parts = httpBrowserString.split("\\|");
        String browserString = parts[0];
        String httpVersion = parts[1];
        HttpBrowserObject browserObject;

        if (browserString.equals(MISSING_VALUE_DATASET_TOKEN)) {
            browserObject = new HttpBrowserObject()
                    .setName(MISSING_VALUE_DATASET_TOKEN)
                    .setVersion(new ArrayList<>())
                    .setCompleteString(httpBrowserString)
                    .setHttpVersion(HttpVersion.from(httpVersion));
        } else {
            HttpBrowserObject preparedBrowser = prepareBrowserObject(browserString);
            browserObject = new HttpBrowserObject()
                    .setName(preparedBrowser.getName())
                    .setVersion(preparedBrowser.getVersion())
                    .setCompleteString(httpBrowserString)
                    .setHttpVersion(HttpVersion.from(httpVersion));
        }

        return browserObject;
    }

    private HttpBrowserObject prepareBrowserObject(String browserString) {
        String[] nameVersionSplit = browserString.split("/");
        String[] versionSplit = nameVersionSplit[1].split("\\.");
        List<Integer> preparedVersion = new ArrayList<>();

        for (String s : versionSplit)
            preparedVersion.add(Integer.parseInt(s));

        return new HttpBrowserObject().setName(nameVersionSplit[0])
                .setVersion(preparedVersion)
                .setCompleteString(browserString);
    }

    private List<BrowserSpecification> prepareBrowsersConfig(BrowserSpecification browsers, HttpVersion httpVersion) {
        List<BrowserSpecification> finalBrowsers = List.of(browsers);

        for (BrowserSpecification browser : finalBrowsers)
            browser.setHttpVersion(httpVersion);

        return finalBrowsers;
    }
}
