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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.fetcher.api.FetcherException;
import io.vertx.core.Vertx;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */

class BitbucketFetcherTest {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final BitbucketFetcher fetcher = new BitbucketFetcher(null);

    private final Vertx vertx = Vertx.vertx();

    private Vertx testVertx;

    @BeforeEach
    public void init() {
        testVertx = Vertx.vertx();
        ReflectionTestUtils.setField(fetcher, "vertx", vertx);
    }

    @AfterEach
    void tearDown() throws Exception {
        testVertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    @Test
    public void shouldNotFetchWithoutContent() throws FetcherException {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(
                aResponse().withStatus(200).withBody("")
            )
        );
        BitbucketFetcherConfiguration config = new BitbucketFetcherConfiguration();
        config.setFilepath("path/to/file");
        config.setUsername("MyUserName");
        config.setBitbucketUrl("http://localhost:" + wiremock.getPort() + "/2.0");
        config.setBranchOrTag("MyBranch");
        config.setRepository("MyRepo");
        ReflectionTestUtils.setField(fetcher, "bitbucketFetcherConfiguration", config);
        ReflectionTestUtils.setField(fetcher, "httpClientTimeout", 10_000);

        InputStream fetch = fetcher.fetch().getContent();

        assertThat(fetch).isNull();
    }

    @Test
    public void shouldNotFetchEmptyBody() throws Exception {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(aResponse().withStatus(200))
        );
        BitbucketFetcherConfiguration config = new BitbucketFetcherConfiguration();
        config.setFilepath("path/to/file");
        config.setUsername("MyUserName");
        config.setBitbucketUrl("http://localhost:" + wiremock.getPort() + "/2.0");
        config.setBranchOrTag("MyBranch");
        config.setRepository("MyRepo");
        ReflectionTestUtils.setField(fetcher, "bitbucketFetcherConfiguration", config);
        ReflectionTestUtils.setField(fetcher, "httpClientTimeout", 10_000);

        InputStream fetch = fetcher.fetch().getContent();

        assertThat(fetch).isNull();
    }

    @Test
    public void shouldFetchContent() throws Exception {
        String content = "Gravitee.io is awesome!";

        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(
                aResponse().withStatus(200).withBody(content)
            )
        );
        BitbucketFetcherConfiguration config = new BitbucketFetcherConfiguration();
        config.setFilepath("path/to/file");
        config.setUsername("MyUserName");
        config.setBitbucketUrl("http://localhost:" + wiremock.getPort() + "/2.0");
        config.setBranchOrTag("MyBranch");
        config.setRepository("MyRepo");
        ReflectionTestUtils.setField(fetcher, "bitbucketFetcherConfiguration", config);
        ReflectionTestUtils.setField(fetcher, "httpClientTimeout", 10_000);

        InputStream fetch = fetcher.fetch().getContent();

        assertThat(fetch).isNotNull();
        int n = fetch.available();
        byte[] bytes = new byte[n];
        fetch.read(bytes, 0, n);
        String decoded = new String(bytes, StandardCharsets.UTF_8);
        assertThat(decoded).isEqualTo(content);
    }

    @Test
    public void shouldThrowExceptionWhenStatusNot200() throws Exception {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(
                aResponse().withStatus(401).withBody("{\n" + "  \"message\": \"401 Unauthorized\"\n" + "}")
            )
        );
        BitbucketFetcherConfiguration config = new BitbucketFetcherConfiguration();
        config.setFilepath("path/to/file");
        config.setUsername("MyUserName");
        config.setBitbucketUrl("http://localhost:" + wiremock.getPort() + "/2.0");
        config.setBranchOrTag("MyBranch");
        config.setRepository("MyRepo");
        ReflectionTestUtils.setField(fetcher, "bitbucketFetcherConfiguration", config);

        assertThatThrownBy(fetcher::fetch)
            .isInstanceOf(FetcherException.class)
            .hasMessageContaining("Status code: 401")
            .hasMessageContaining("Message: Unauthorized");
    }

    @Test
    void should_expose_original_cause_instead_of_async_wrapper_when_fetch_fails() {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(aResponse().withStatus(404))
        );

        BitbucketFetcher bitbucketFetcher = bitbucketFetcher(10_000);

        assertThatThrownBy(bitbucketFetcher::fetch)
            .isInstanceOf(FetcherException.class)
            .hasCauseInstanceOf(FetcherException.class)
            .cause()
            .isNotInstanceOf(CompletionException.class);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void should_fail_fast_when_connection_is_closed_while_reading_response() {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(
                aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)
            )
        );

        BitbucketFetcher bitbucketFetcher = bitbucketFetcher(10_000);

        assertThatThrownBy(bitbucketFetcher::fetch).isInstanceOf(FetcherException.class);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void should_fail_when_connection_stalls_while_reading_body() {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(
                aResponse().withStatus(200).withBody("Gravitee.io is awesome!").withChunkedDribbleDelay(20, 20_000)
            )
        );

        BitbucketFetcher bitbucketFetcher = bitbucketFetcher(500);

        assertThatThrownBy(bitbucketFetcher::fetch).isInstanceOf(FetcherException.class);
    }

    @Test
    void should_send_basic_auth_header_built_from_login_and_password() throws Exception {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(
                aResponse().withStatus(200).withBody("content")
            )
        );

        BitbucketFetcher bitbucketFetcher = bitbucketFetcher(10_000);
        BitbucketFetcherConfiguration config = (BitbucketFetcherConfiguration) bitbucketFetcher.getConfiguration();
        config.setLogin("john.doe@example.com");
        config.setPassword("my-api-token");

        bitbucketFetcher.fetch();

        String expectedBasicAuth =
            "Basic " + Base64.getEncoder().encodeToString("john.doe@example.com:my-api-token".getBytes(StandardCharsets.UTF_8));
        wiremock.verify(
            getRequestedFor(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).withHeader(
                "Authorization",
                equalTo(expectedBasicAuth)
            )
        );
    }

    @Test
    void should_give_actionable_hint_when_bitbucket_returns_401() {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(aResponse().withStatus(401))
        );

        BitbucketFetcher bitbucketFetcher = bitbucketFetcher(10_000);

        assertThatThrownBy(bitbucketFetcher::fetch)
            .isInstanceOf(FetcherException.class)
            .hasMessageContaining("Status code: 401")
            .hasMessageContaining("Atlassian account email")
            .hasMessageContaining("Atlassian API token");
    }

    @Test
    void should_give_scope_hint_when_bitbucket_returns_403() {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(aResponse().withStatus(403))
        );

        BitbucketFetcher bitbucketFetcher = bitbucketFetcher(10_000);

        assertThatThrownBy(bitbucketFetcher::fetch)
            .isInstanceOf(FetcherException.class)
            .hasMessageContaining("Status code: 403")
            .hasMessageContaining("read:repository:bitbucket");
    }

    @Test
    void should_never_include_credentials_in_error_message() {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(aResponse().withStatus(401))
        );

        BitbucketFetcher bitbucketFetcher = bitbucketFetcher(10_000);
        BitbucketFetcherConfiguration config = (BitbucketFetcherConfiguration) bitbucketFetcher.getConfiguration();
        config.setLogin("john.doe@example.com");
        config.setPassword("my-secret-api-token");
        String base64Credentials = Base64.getEncoder().encodeToString(
            "john.doe@example.com:my-secret-api-token".getBytes(StandardCharsets.UTF_8)
        );

        Throwable thrown = catchThrowable(bitbucketFetcher::fetch);

        assertThat(thrown).isInstanceOf(FetcherException.class);
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            assertThat(current.getMessage()).doesNotContain("my-secret-api-token").doesNotContain(base64Credentials);
        }
    }

    @Test
    void should_fail_with_clear_message_when_filepath_is_missing() {
        BitbucketFetcher bitbucketFetcher = bitbucketFetcher(10_000);
        BitbucketFetcherConfiguration config = (BitbucketFetcherConfiguration) bitbucketFetcher.getConfiguration();
        config.setFilepath(null);

        assertThatThrownBy(bitbucketFetcher::fetch)
            .isInstanceOf(FetcherException.class)
            .hasMessageContaining("Some required configuration attributes are missing");
    }

    @Test
    void should_fail_with_clear_message_when_filepath_is_blank() {
        BitbucketFetcher bitbucketFetcher = bitbucketFetcher(10_000);
        BitbucketFetcherConfiguration config = (BitbucketFetcherConfiguration) bitbucketFetcher.getConfiguration();
        config.setFilepath("   ");

        assertThatThrownBy(bitbucketFetcher::fetch)
            .isInstanceOf(FetcherException.class)
            .hasMessageContaining("Some required configuration attributes are missing");
    }

    private BitbucketFetcher bitbucketFetcher(int timeoutMs) {
        BitbucketFetcherConfiguration config = new BitbucketFetcherConfiguration();
        config.setFilepath("path/to/file");
        config.setUsername("MyUserName");
        config.setBitbucketUrl("http://localhost:" + wiremock.getPort() + "/2.0");
        config.setBranchOrTag("MyBranch");
        config.setRepository("MyRepo");
        BitbucketFetcher bitbucketFetcher = new BitbucketFetcher(config);
        ReflectionTestUtils.setField(bitbucketFetcher, "httpClientTimeout", timeoutMs);
        ReflectionTestUtils.setField(bitbucketFetcher, "vertx", testVertx);
        return bitbucketFetcher;
    }
}
