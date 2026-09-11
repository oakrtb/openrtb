## Summary

<!-- 1–3 bullets: what & why -->

## Breaking?

- [ ] No
- [ ] Yes — described in CHANGELOG / this PR

## Test plan

- [ ] `make validate`
- [ ] `make proto-check`（若动 proto）
- [ ] `make sync-schemas` + commit Go schema copies（若动 jsonschema）
- [ ] `make sdk-test`（或相关语言子集）
