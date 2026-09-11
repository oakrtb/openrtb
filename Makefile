.PHONY: validate install-dev sync-schemas proto-check proto-go proto-java proto-rust proto \
	sdk-test sdk-test-go sdk-test-java sdk-test-rust jar \
	publish-rust-dry publish-java-dry

PROTO := proto/oakrtb/v2/openrtb.proto
VERSION := $(shell cat VERSION)
JAVA_HOME ?= $(shell echo $$JAVA_HOME)

install-dev:
	python3 -m pip install -r scripts/requirements.txt

validate:
	python3 scripts/validate.py

# 权威 schema/proto → 各语言 vendored 副本（包发布 / go get / crates.io / Maven 需要）。
sync-schemas:
	@mkdir -p sdk/go/schema/schemas
	@mkdir -p sdk/java/src/main/resources/schema/jsonschema
	@mkdir -p sdk/java/src/main/proto/oakrtb/v2
	@mkdir -p sdk/rust/schemas sdk/rust/proto/oakrtb/v2
	cp schema/jsonschema/*.json sdk/go/schema/schemas/
	cp schema/jsonschema/*.json sdk/java/src/main/resources/schema/jsonschema/
	cp schema/jsonschema/*.json sdk/rust/schemas/
	cp $(PROTO) sdk/java/src/main/proto/oakrtb/v2/
	cp $(PROTO) sdk/rust/proto/oakrtb/v2/
	@echo "synced schema + proto into sdk/{go,java,rust}"

# Syntax-check protobuf (no language plugins required).
proto-check:
	protoc -I proto --descriptor_set_out=/dev/null $(PROTO)

# Go models into sdk/go (committed) and mirror under gen/go.
proto-go: proto-check
	@mkdir -p sdk/go gen/go
	protoc -I proto \
		--go_out=sdk/go --go_opt=module=github.com/oakrtb/openrtb/sdk/go \
		$(PROTO)
	@rm -rf gen/go/oakrtb && mkdir -p gen/go/oakrtb && cp -R sdk/go/oakrtb/v2 gen/go/oakrtb/

# Java models via Maven protobuf plugin into target/generated-sources, also copy to gen/java.
proto-java: proto-check
	@mkdir -p gen/java
	cd sdk/java && mvn -q protobuf:compile
	@rm -rf gen/java/com && cp -R sdk/java/target/generated-sources/protobuf/java/com gen/java/

# Rust models are generated at compile-time (prost). This target only mirrors a dry-run build.
proto-rust: proto-check
	cd sdk/rust && cargo build -q
	@mkdir -p gen/rust
	@echo "Rust stubs are emitted to OUT_DIR during cargo build; see sdk/rust" > gen/rust/README.txt

proto: proto-go proto-java proto-rust

sdk-test-go: sync-schemas
	cd sdk/go && go test ./...

sdk-test-java:
	cd sdk/java && mvn -q test

sdk-test-rust:
	cd sdk/rust && cargo test -q

sdk-test: sdk-test-go sdk-test-java sdk-test-rust

# Thin jar + shaded all-in-one jar under gen/java/dist/
jar:
	cd sdk/java && mvn -q package -DskipTests
	@mkdir -p gen/java/dist
	cp sdk/java/target/oakrtb-sdk-$(VERSION).jar gen/java/dist/
	cp sdk/java/target/oakrtb-sdk-$(VERSION)-all.jar gen/java/dist/
	@ls -la gen/java/dist/

# Dry-run package publish (no upload). See docs/publishing.md
publish-rust-dry: sync-schemas
	cd sdk/rust && cargo publish --dry-run --allow-dirty

publish-java-dry: sync-schemas
	cd sdk/java && mvn -q -Prelease package -DskipTests
	@echo "Built release artifacts under sdk/java/target (sources/javadoc/gpg need keys for full deploy)"

