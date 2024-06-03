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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.fetcher.api.FetcherException;
import io.vertx.core.Vertx;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(fetcher, "vertx", vertx);
    }

    @Test
    public void shouldNotFetchWithoutContent() throws FetcherException {
        wiremock.stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file"))
                .willReturn(aResponse().withStatus(200).withBody(""))
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
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file"))
                .willReturn(aResponse().withStatus(200).withBody(content))
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
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file"))
                .willReturn(aResponse().withStatus(401).withBody("{\n" + "  \"message\": \"401 Unauthorized\"\n" + "}"))
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
}
