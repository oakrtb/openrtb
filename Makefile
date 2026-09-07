.PHONY: validate install-dev sync-schemas proto-check proto-go proto-java proto-rust proto \
	sdk-test sdk-test-go sdk-test-java sdk-test-rust jar

PROTO := proto/oakrtb/v2/openrtb.proto
VERSION := $(shell cat VERSION)
JAVA_HOME ?= $(shell echo $$JAVA_HOME)

install-dev:
	python3 -m pip install -r scripts/requirements.txt

validate:
	python3 scripts/validate.py

sync-schemas:
	@mkdir -p sdk/go/validate/schemas \
		sdk/java/src/main/resources/schema/jsonschema \
		sdk/rust/schemas
	cp schema/jsonschema/*.json sdk/go/validate/schemas/
	cp schema/jsonschema/*.json sdk/java/src/main/resources/schema/jsonschema/
	cp schema/jsonschema/*.json sdk/rust/schemas/

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

sdk-test-java: sync-schemas
	cd sdk/java && mvn -q test

sdk-test-rust: sync-schemas
	cd sdk/rust && cargo test -q

sdk-test: sdk-test-go sdk-test-java sdk-test-rust

# Thin jar + shaded all-in-one jar under gen/java/dist/
jar: sync-schemas
	cd sdk/java && mvn -q package -DskipTests
	@mkdir -p gen/java/dist
	cp sdk/java/target/oakrtb-sdk-$(VERSION).jar gen/java/dist/
	cp sdk/java/target/oakrtb-sdk-$(VERSION)-all.jar gen/java/dist/
	@ls -la gen/java/dist/
