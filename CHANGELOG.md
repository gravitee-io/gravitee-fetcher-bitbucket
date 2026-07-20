## [3.0.2](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/compare/3.0.1...3.0.2) (2026-07-20)


### Bug Fixes

* normalize filepath and clarify not-found error ([2cf3a1f](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/2cf3a1fa62c1097326b05e9d9f24e4ac2a78c622))

## [3.0.1](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/compare/3.0.0...3.0.1) (2026-07-16)


### Bug Fixes

* encode basic auth credentials in UTF-8 ([32a5608](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/32a56085aa6415fbeba1c07cc16d87bfade95cd3))
* guide users to Atlassian API tokens in form labels and auth errors ([e8fa7e7](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/e8fa7e7b05b4dd6bab243ed457c96f66b6940c77))
* reject configuration when filepath is missing ([b055206](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/b055206a2815c9c799814bf14339aa11f3cfe813))

# [3.0.0](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/compare/2.1.1...3.0.0) (2026-07-15)


### Bug Fixes

* fail fetch when the connection stalls while reading the response ([4bd2c55](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/4bd2c55ec267d465de6555acf938825e05d047c7))
* log fetch errors once and unwrap the async exception wrapper ([5aa2577](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/5aa2577c64e8d270cb0f8a4bc3513f2c47074314))


### Features

* upgrade to vertx 5 ([d615328](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/d6153286073252b5eee174c3f4fa5db6613d53f1))


### BREAKING CHANGES

* compiled against Vert.x 5 (gravitee-bom 9.x), requires an APIM runtime on Vert.x 5

## [2.1.1](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/compare/2.1.0...2.1.1) (2024-09-11)


### Bug Fixes

* improve schema form ([2175e45](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/2175e4596fab3847212d9bb46196356fb5d42520))

# [2.1.0](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/compare/2.0.1...2.1.0) (2024-09-03)


### Features

* improve fetchCron field ([b0e4059](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/b0e4059b756658535b93f4a2997e1ee6e2cc7573))

## [2.0.1](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/compare/2.0.0...2.0.1) (2024-06-10)


### Bug Fixes

* **deps:** update dependency org.wiremock:wiremock-standalone to v3.6.0 ([942e8b2](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/942e8b261305d75d59d187fc3ce30cffff4c3d65))
* use right scope for lib ([9b53dfe](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/9b53dfe9578ee1942ca3cc58e5ac40ad54a8036f))

# [2.0.0](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/compare/1.7.1...2.0.0) (2024-06-05)


### chore

* bump dependencies ([89805b8](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/89805b8ef0af6d48644e2ff14666631da1b079ad))


### BREAKING CHANGES

* require JDK 17

## [1.7.1](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/compare/1.7.0...1.7.1) (2023-03-30)


### Bug Fixes

* use url path in client options ([9374568](https://github.com/gravitee-io/gravitee-fetcher-bitbucket/commit/9374568c5a8d6adce7d244c9b43fa2e79c5b2dbc))
