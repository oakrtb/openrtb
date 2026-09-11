# Publishing OakRTB SDKs

Authority stays in the monorepo (`schema/jsonschema/`, `proto/`). Language packages **vendor copies** via `make sync-schemas` so they can be published independently.

```bash
make sync-schemas   # refresh Go / Java / Rust vendored schema + proto
git add sdk/go/schema/schemas sdk/java/src/main/resources sdk/java/src/main/proto sdk/rust/schemas sdk/rust/proto
```

## Go (`pkg.go.dev`)

Module path: `github.com/oakrtb/openrtb/sdk/go`

After tagging the **repository root** (already done for `v0.2.0`):

```bash
go get github.com/oakrtb/openrtb/sdk/go@v0.2.0
# force proxy refresh if needed:
curl -s 'https://proxy.golang.org/github.com/oakrtb/openrtb/sdk/go/@v/v0.2.0.info'
```

No extra publish step beyond the Git tag.

## Rust (`crates.io`)

Prerequisites:

1. [crates.io](https://crates.io) account linked to GitHub
2. API token: `cargo login`
3. `protoc` on `PATH` (for local dry-run / consumers’ first build)

```bash
make sync-schemas
make publish-rust-dry          # cargo publish --dry-run
cd sdk/rust && cargo publish   # real upload
```

Crate name: **`oakrtb-sdk`**. Includes vendored `schemas/*.json` and `proto/**/*.proto`.

## Java (Maven Central)

Prerequisites:

1. Sonatype Central Portal account: https://central.sonatype.com/  
   - Register namespace **`com.oakrtb`** (GitHub `oakrtb` org ownership verification)
2. Generate a **user token** on the portal
3. GPG key for signing (`gpg --gen-key`); publish public key to a keyserver

`~/.m2/settings.xml` (do not commit):

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_CENTRAL_USERNAME</username>
      <password>YOUR_CENTRAL_TOKEN</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>gpg</id>
      <properties>
        <gpg.keyname>YOUR_KEY_ID</gpg.keyname>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>gpg</activeProfile>
  </activeProfiles>
</settings>
```

Deploy from monorepo:

```bash
make sync-schemas
cd sdk/java
mvn -Prelease clean deploy
```

Coordinates:

```xml
<groupId>com.oakrtb</groupId>
<artifactId>oakrtb-sdk</artifactId>
<version>0.2.0</version>
```

The `release` profile attaches sources + javadoc, GPG-signs, and uses
`central-publishing-maven-plugin`. The shaded `*-all.jar` is an optional classifier for fat-jar users; Central’s primary artifact is the thin jar.

Dry-run package (no deploy / may skip gpg if unset):

```bash
make publish-java-dry
```

## GitHub Actions (optional)

After secrets exist, add a workflow that on `v*` tags runs:

| Secret | Used for |
|---|---|
| `CRATES_IO_TOKEN` | `cargo publish` |
| `CENTRAL_USERNAME` / `CENTRAL_TOKEN` | Maven Central |
| `GPG_PRIVATE_KEY` / `GPG_PASSPHRASE` | `maven-gpg-plugin` |

Do **not** commit tokens. Prefer manual first publish, then automate.

## Checklist per release

1. Bump `VERSION`, SDK `pom.xml` / `Cargo.toml` / `go.mod` docs, CHANGELOG
2. `make sync-schemas && make validate && make sdk-test`
3. Tag `vX.Y.Z` + GitHub Release
4. `cargo publish` + `mvn -Prelease deploy`
5. Update README install snippets if coordinates changed
