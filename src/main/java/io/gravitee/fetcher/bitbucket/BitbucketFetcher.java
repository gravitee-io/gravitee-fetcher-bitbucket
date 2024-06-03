/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.fetcher.bitbucket;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.utils.UUID;
import io.gravitee.fetcher.api.Fetcher;
import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.FetcherException;
import io.gravitee.fetcher.api.Resource;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.utils.NodeUtils;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.support.CronExpression;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class BitbucketFetcher implements Fetcher {

    private static final Logger logger = LoggerFactory.getLogger(BitbucketFetcher.class);

    private static final String HTTPS_SCHEME = "https";

    private BitbucketFetcherConfiguration bitbucketFetcherConfiguration;

    @Autowired
    private Vertx vertx;

    @Autowired
    private Node node;

    @Value("${httpClient.timeout:10000}")
    private int httpClientTimeout;

    @Value("${httpClient.proxy.type:HTTP}")
    private String httpClientProxyType;

    @Value("${httpClient.proxy.http.host:#{systemProperties['http.proxyHost'] ?: 'localhost'}}")
    private String httpClientProxyHttpHost;

    @Value("${httpClient.proxy.http.port:#{systemProperties['http.proxyPort'] ?: 3128}}")
    private int httpClientProxyHttpPort;

    @Value("${httpClient.proxy.http.username:#{null}}")
    private String httpClientProxyHttpUsername;

    @Value("${httpClient.proxy.http.password:#{null}}")
    private String httpClientProxyHttpPassword;

    @Value("${httpClient.proxy.https.host:#{systemProperties['https.proxyHost'] ?: 'localhost'}}")
    private String httpClientProxyHttpsHost;

    @Value("${httpClient.proxy.https.port:#{systemProperties['https.proxyPort'] ?: 3128}}")
    private int httpClientProxyHttpsPort;

    @Value("${httpClient.proxy.https.username:#{null}}")
    private String httpClientProxyHttpsUsername;

    @Value("${httpClient.proxy.https.password:#{null}}")
    private String httpClientProxyHttpsPassword;

    public BitbucketFetcher(BitbucketFetcherConfiguration bitbucketFetcherConfiguration) {
        this.bitbucketFetcherConfiguration = bitbucketFetcherConfiguration;
    }

    @Override
    public Resource fetch() throws FetcherException {
        checkRequiredFields();
        try {
            Buffer buffer = fetchContent().join();
            final Resource resource = new Resource();
            if (buffer == null || buffer.length() == 0) {
                logger.warn("Something goes wrong, Bitbucket responds with a status 200 but the content is empty.");
            } else {
                resource.setContent(new ByteArrayInputStream(buffer.getBytes()));
                final HashMap<String, Object> metadata = new HashMap<>(1);
                metadata.put(EDIT_URL_PROPERTY_KEY, buildEditUrl());
                metadata.put(PROVIDER_NAME_PROPERTY_KEY, "Bitbucket");
                resource.setMetadata(metadata);
            }
            return resource;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new FetcherException("Unable to fetch Bitbucket content (" + ex.getMessage() + ")", ex);
        }
    }

    @Override
    public FetcherConfiguration getConfiguration() {
        return bitbucketFetcherConfiguration;
    }

    private void checkRequiredFields() throws FetcherException {
        if (
            bitbucketFetcherConfiguration.getBranchOrTag() == null ||
            bitbucketFetcherConfiguration.getBitbucketUrl() == null ||
            bitbucketFetcherConfiguration.getRepository() == null ||
            bitbucketFetcherConfiguration.getUsername() == null ||
            (
                bitbucketFetcherConfiguration.isAutoFetch() &&
                (bitbucketFetcherConfiguration.getFetchCron() == null || bitbucketFetcherConfiguration.getFetchCron().isEmpty())
            )
        ) {
            throw new FetcherException("Some required configuration attributes are missing.", null);
        }

        if (bitbucketFetcherConfiguration.isAutoFetch() && bitbucketFetcherConfiguration.getFetchCron() != null) {
            try {
                CronExpression.parse(bitbucketFetcherConfiguration.getFetchCron());
            } catch (IllegalArgumentException e) {
                throw new FetcherException("Cron expression is invalid", e);
            }
        }
    }

    private String buildEditUrl() throws FetcherException {
        checkRequiredFields();
        final String bitbucketUrl = bitbucketFetcherConfiguration.getBitbucketUrl().replace("api.", "");
        return (
            bitbucketUrl.substring(0, bitbucketUrl.lastIndexOf('/')) +
            '/' +
            bitbucketFetcherConfiguration.getUsername() +
            '/' +
            bitbucketFetcherConfiguration.getRepository() +
            "/src/" +
            (bitbucketFetcherConfiguration.getBranchOrTag() == null ? "master" : bitbucketFetcherConfiguration.getBranchOrTag()) +
            '/' +
            bitbucketFetcherConfiguration.getFilepath() +
            "?spa=0&mode=edit"
        );
    }

    private String getEncodedRequestUrl() throws UnsupportedEncodingException {
        String ref =
            (
                (bitbucketFetcherConfiguration.getBranchOrTag() == null || bitbucketFetcherConfiguration.getBranchOrTag().trim().isEmpty())
                    ? "master"
                    : bitbucketFetcherConfiguration.getBranchOrTag().trim()
            );

        return (
            bitbucketFetcherConfiguration.getBitbucketUrl().trim() +
            "/repositories/" +
            bitbucketFetcherConfiguration.getUsername() +
            "/" +
            bitbucketFetcherConfiguration.getRepository() +
            "/src/" +
            ref +
            "/" +
            bitbucketFetcherConfiguration.getFilepath()
        );
    }

    private CompletableFuture<Buffer> fetchContent() throws Exception {
        Promise<Buffer> promise = Promise.promise();

        String url = getEncodedRequestUrl();

        URI requestUri = URI.create(url);
        boolean ssl = HTTPS_SCHEME.equalsIgnoreCase(requestUri.getScheme());

        final HttpClientOptions options = new HttpClientOptions()
            .setSsl(ssl)
            .setTrustAll(true)
            .setMaxPoolSize(1)
            .setKeepAlive(false)
            .setTcpKeepAlive(false)
            .setConnectTimeout(httpClientTimeout);

        if (bitbucketFetcherConfiguration.isUseSystemProxy()) {
            ProxyOptions proxyOptions = new ProxyOptions();
            proxyOptions.setType(ProxyType.valueOf(httpClientProxyType));
            if (HTTPS_SCHEME.equals(requestUri.getScheme())) {
                proxyOptions.setHost(httpClientProxyHttpsHost);
                proxyOptions.setPort(httpClientProxyHttpsPort);
                proxyOptions.setUsername(httpClientProxyHttpsUsername);
                proxyOptions.setPassword(httpClientProxyHttpsPassword);
            } else {
                proxyOptions.setHost(httpClientProxyHttpHost);
                proxyOptions.setPort(httpClientProxyHttpPort);
                proxyOptions.setUsername(httpClientProxyHttpUsername);
                proxyOptions.setPassword(httpClientProxyHttpPassword);
            }
            options.setProxyOptions(proxyOptions);
        }

        final HttpClient httpClient = vertx.createHttpClient(options);

        final int port = requestUri.getPort() != -1 ? requestUri.getPort() : (HTTPS_SCHEME.equals(requestUri.getScheme()) ? 443 : 80);

        try {
            final RequestOptions reqOptions = new RequestOptions()
                .setMethod(HttpMethod.GET)
                .setPort(port)
                .setHost(requestUri.getHost())
                .setURI(requestUri.getPath())
                .putHeader(io.gravitee.common.http.HttpHeaders.USER_AGENT, NodeUtils.userAgent(node))
                .putHeader("X-Gravitee-Request-Id", UUID.toString(UUID.random()))
                .setTimeout(httpClientTimeout)
                .setFollowRedirects(true);

            if (bitbucketFetcherConfiguration.getLogin() != null && bitbucketFetcherConfiguration.getPassword() != null) {
                String encoding = Base64
                    .getEncoder()
                    .encodeToString(
                        (bitbucketFetcherConfiguration.getLogin() + ":" + bitbucketFetcherConfiguration.getPassword()).getBytes()
                    );
                reqOptions.putHeader("Authorization", "Basic " + encoding);
            }

            httpClient
                .request(reqOptions)
                .onFailure(throwable -> {
                    promise.fail(throwable);

                    // Close client
                    httpClient.close();
                })
                .onSuccess(request -> {
                    request.response(asyncResponse -> {
                        if (asyncResponse.failed()) {
                            promise.fail(asyncResponse.cause());

                            // Close client
                            httpClient.close();
                        } else {
                            HttpClientResponse response = asyncResponse.result();
                            if (response.statusCode() == HttpStatusCode.OK_200) {
                                response.bodyHandler(buffer -> {
                                    promise.complete(buffer);

                                    // Close client
                                    httpClient.close();
                                });
                            } else {
                                promise.fail(
                                    new FetcherException(
                                        "Unable to fetch '" +
                                        url +
                                        "'. Status code: " +
                                        response.statusCode() +
                                        ". Message: " +
                                        response.statusMessage(),
                                        null
                                    )
                                );

                                // Close client
                                httpClient.close();
                            }
                        }
                    });

                    request.exceptionHandler(throwable -> {
                        try {
                            promise.fail(throwable);

                            // Close client
                            httpClient.close();
                        } catch (IllegalStateException ise) {
                            // Do not take care about exception when closing client
                        }
                    });

                    request.end();
                });
        } catch (Exception ex) {
            logger.error("Unable to fetch content using HTTP", ex);
            promise.fail(ex);
        }

        return promise.future().toCompletionStage().toCompletableFuture();
    }
}
