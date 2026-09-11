---
name: Schema / protocol change
about: Propose a change to JSON Schema, OpenAPI, or protobuf
title: "[schema] "
labels: ["schema"]
---

## Motivation

## Proposed change

- [ ] `schema/jsonschema/`
- [ ] `proto/oakrtb/v2/openrtb.proto`
- [ ] `openapi/openrtb.yaml`
- [ ] docs / examples / testdata
- [ ] Breaking? (optional → required, delete field, type change)

## Compatibility

Aligned OpenRTB snapshot (e.g. 2.6-202606):

## Test plan

- [ ] `make validate`
- [ ] `make sync-schemas` + `make sdk-test`（若改 schema）
