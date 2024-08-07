# Change Log

### Unreleased

* Update the `org.realityforge.javax.annotation` artifact to version `1.1.1`.
* Upgrade the `au.com.stocksoftware.idea.codestyle` artifact to version `1.17`.

### [v0.02](https://github.com/realityforge/gwt-serviceworker-linker/tree/v0.02) (2020-03-27) · [Full Changelog](https://github.com/realityforge/gwt-serviceworker-linker/compare/v0.01...v0.02)

* Change the default value of the configuration property `serviceworker_optimize_serviceworker` from `false` to `true`. As a result serviceworkers will be optimized and obfuscated by default.
* If the `serviceworker_log_level` configuration property ise set to `0` then use an alternative service worker template that has logging statements explicitly stripped out. It seems that the optimizer used by the linker is not smart enough to apply the transform automatically.

### [v0.01](https://github.com/realityforge/gwt-serviceworker-linker/tree/v0.01) (2020-03-12) · [Full Changelog](https://github.com/realityforge/gwt-serviceworker-linker/compare/0a38a8ee451ea957d59e7f67fb71455b0f123199...v0.01)

 ‎🎉	Initial super-alpha release ‎🎉.
