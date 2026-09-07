#!/usr/bin/env python3
"""Validate OakRTB examples and negative fixtures against JSON Schema."""

from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    from jsonschema import Draft202012Validator
    from referencing import Registry, Resource
    from referencing.jsonschema import DRAFT202012
except ImportError as exc:  # pragma: no cover
    raise SystemExit(
        "Missing dependency. Run: pip install -r scripts/requirements.txt"
    ) from exc

ROOT = Path(__file__).resolve().parents[1]
SCHEMA_DIR = ROOT / "schema" / "jsonschema"
EXAMPLES = ROOT / "examples"
INVALID = ROOT / "testdata" / "invalid"


def load_json(path: Path) -> object:
    with path.open(encoding="utf-8") as fh:
        return json.load(fh)


def build_registry() -> Registry:
    registry = Registry()
    for path in SCHEMA_DIR.glob("*.json"):
        contents = load_json(path)
        resource = Resource.from_contents(contents, default_specification=DRAFT202012)
        registry = registry.with_resource(path.name, resource)
        schema_id = contents.get("$id")
        if isinstance(schema_id, str):
            registry = registry.with_resource(schema_id, resource)
    return registry


def validator_for(schema_file: str, registry: Registry) -> Draft202012Validator:
    schema = load_json(SCHEMA_DIR / schema_file)
    return Draft202012Validator(schema, registry=registry)


def infer_schema(path: Path, payload: dict) -> str:
    parts = {p.lower() for p in path.parts}
    name = path.name.lower()
    if "bid-response" in parts or "response" in name or "no-bid" in name:
        return "bid-response.schema.json"
    if "bid-request" in parts or "request" in name or "imp" in payload:
        return "bid-request.schema.json"
    return "bid-response.schema.json"


def validate_native_embedded(payload: dict, native_validator: Draft202012Validator) -> list[str]:
    errors: list[str] = []
    for imp in payload.get("imp") or []:
        native = imp.get("native") or {}
        request = native.get("request")
        if not request:
            continue
        try:
            inner = json.loads(request)
        except json.JSONDecodeError as exc:
            errors.append(f"native.request is not JSON: {exc}")
            continue
        for err in native_validator.iter_errors(inner):
            errors.append(f"native.request: {err.message}")
    return errors


def main() -> int:
    registry = build_registry()
    native_v = validator_for("native.schema.json", registry)
    failed = 0

    print("== valid examples ==")
    for path in sorted(EXAMPLES.glob("*.json")):
        payload = load_json(path)
        schema_file = infer_schema(path, payload)
        v = validator_for(schema_file, registry)
        errors = [e.message for e in v.iter_errors(payload)]
        if schema_file == "bid-request.schema.json":
            errors.extend(validate_native_embedded(payload, native_v))
        if errors:
            failed += 1
            print(f"FAIL  {path.relative_to(ROOT)}  [{schema_file}]")
            for message in errors:
                print(f"      - {message}")
        else:
            print(f"ok    {path.relative_to(ROOT)}  [{schema_file}]")

    print("== invalid fixtures (must fail) ==")
    invalid_paths = sorted(INVALID.rglob("*.json"))
    if not invalid_paths:
        print("FAIL  no invalid fixtures found under testdata/invalid")
        return 1
    for path in invalid_paths:
        payload = load_json(path)
        schema_file = infer_schema(path, payload)
        v = validator_for(schema_file, registry)
        errors = list(v.iter_errors(payload))
        if errors:
            print(f"ok    {path.relative_to(ROOT)}  rejected as expected")
        else:
            failed += 1
            print(f"FAIL  {path.relative_to(ROOT)}  should have been rejected")

    if failed:
        print(f"\n{failed} check(s) failed")
        return 1
    print("\nall schema checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
