/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import static org.assertj.core.api.Assertions.fail;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.fetcher.api.FetcherException;
import io.vertx.core.Vertx;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */

public class BitbucketFetcherTest {

    @ClassRule
    public static final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    private BitbucketFetcher fetcher = new BitbucketFetcher(null);

    private Vertx vertx = Vertx.vertx();

    @Before
    public void init() {
        ReflectionTestUtils.setField(fetcher, "vertx", vertx);
    }

    @Test
    public void shouldNotFetchWithoutContent() throws FetcherException {
        stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file"))
                .willReturn(aResponse().withStatus(200).withBody(""))
        );
        BitbucketFetcherConfiguration config = new BitbucketFetcherConfiguration();
        config.setFilepath("path/to/file");
        config.setUsername("MyUserName");
        config.setBitbucketUrl("http://localhost:" + wireMockRule.port() + "/2.0");
        config.setBranchOrTag("MyBranch");
        config.setRepository("MyRepo");
        ReflectionTestUtils.setField(fetcher, "bitbucketFetcherConfiguration", config);
        ReflectionTestUtils.setField(fetcher, "httpClientTimeout", 10_000);

        InputStream fetch = fetcher.fetch().getContent();

        assertThat(fetch).isNull();
    }

    @Test
    public void shouldNotFetchEmptyBody() throws Exception {
        stubFor(get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file")).willReturn(aResponse().withStatus(200)));
        BitbucketFetcherConfiguration config = new BitbucketFetcherConfiguration();
        config.setFilepath("path/to/file");
        config.setUsername("MyUserName");
        config.setBitbucketUrl("http://localhost:" + wireMockRule.port() + "/2.0");
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

        stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file"))
                .willReturn(aResponse().withStatus(200).withBody(content))
        );
        BitbucketFetcherConfiguration config = new BitbucketFetcherConfiguration();
        config.setFilepath("path/to/file");
        config.setUsername("MyUserName");
        config.setBitbucketUrl("http://localhost:" + wireMockRule.port() + "/2.0");
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

    @Test(expected = FetcherException.class)
    public void shouldThrowExceptionWhenStatusNot200() throws Exception {
        stubFor(
            get(urlEqualTo("/2.0/repositories/MyUserName/MyRepo/src/MyBranch/path/to/file"))
                .willReturn(aResponse().withStatus(401).withBody("{\n" + "  \"message\": \"401 Unauthorized\"\n" + "}"))
        );
        BitbucketFetcherConfiguration config = new BitbucketFetcherConfiguration();
        config.setFilepath("path/to/file");
        config.setUsername("MyUserName");
        config.setBitbucketUrl("http://localhost:" + wireMockRule.port() + "/2.0");
        config.setBranchOrTag("MyBranch");
        config.setRepository("MyRepo");
        ReflectionTestUtils.setField(fetcher, "bitbucketFetcherConfiguration", config);

        try {
            fetcher.fetch();
        } catch (FetcherException fe) {
            assertThat(fe.getMessage().contains("Status code: 401"));
            assertThat(fe.getMessage().contains("Message: 401 Unauthorized"));
            throw fe;
        }

        fail("Fetch response with status code != 200 does not throw Exception");
    }
}
