# oakrtb-sdk (Java)

OpenRTB-compatible models, builders, hot-path `view` (LightGate), optional `fit`, and off-path `schema` validation for [OakRTB](https://github.com/oakrtb/openrtb).

```xml
<dependency>
  <groupId>com.oakrtb</groupId>
  <artifactId>oakrtb-sdk</artifactId>
  <version>0.2.0</version>
</dependency>
```

Requires **JDK 21+**. JSON Schema resources are vendored under `src/main/resources/schema/jsonschema/` (refreshed by `make sync-schemas`).

Build from the monorepo:

```bash
make jar   # → gen/java/dist/oakrtb-sdk-0.2.0*.jar
```

Publishing to Maven Central: see [docs/publishing.md](https://github.com/oakrtb/openrtb/blob/main/docs/publishing.md).
