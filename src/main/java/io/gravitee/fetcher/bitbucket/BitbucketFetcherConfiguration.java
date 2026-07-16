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

import io.gravitee.fetcher.api.FetcherConfiguration;
import io.gravitee.fetcher.api.Sensitive;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class BitbucketFetcherConfiguration implements FetcherConfiguration {

    private String bitbucketUrl;

    /**
     * Workspace (or username) owning the repository, used to build the repository URL — not an authentication credential.
     * Field names below no longer match what they hold ({@code login} = Atlassian account email, {@code password} = Atlassian
     * API token since app passwords were removed); they are kept as-is because they are the JSON keys of configurations
     * already stored in the APIM database. The user-facing labels live in schemas/schema-form.json.
     */
    private String username;

    private String repository;
    private String branchOrTag;
    private String filepath;
    private String login;

    @Sensitive
    private String password;

    private boolean useSystemProxy;
    private String editLink;

    private String fetchCron;

    private boolean autoFetch = false;
}
