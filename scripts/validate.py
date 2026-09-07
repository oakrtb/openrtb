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
FULL = ROOT / "testdata" / "full"
OPENAPI = ROOT / "openapi" / "openrtb.yaml"

SCHEMA_BY_KIND = {
    "bid-request": "bid-request.schema.json",
    "bid-response": "bid-response.schema.json",
}

# OpenAPI embedded examples → schema kind (must stay schema-valid).
OPENAPI_EXAMPLES = {
    "BannerBidRequest": "bid-request.schema.json",
    "BannerBidResponse": "bid-response.schema.json",
    "NoBidResponse": "bid-response.schema.json",
}


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


def schema_for_path(path: Path) -> str:
    """Resolve schema from typed directory: .../bid-request/*.json or .../bid-response/*.json."""
    parts = {p.lower() for p in path.parts}
    for kind, schema_file in SCHEMA_BY_KIND.items():
        if kind in parts:
            return schema_file
    raise ValueError(
        f"{path.relative_to(ROOT)}: expected under bid-request/ or bid-response/"
    )


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
    example_paths = sorted(EXAMPLES.rglob("*.json"))
    if not example_paths:
        print("FAIL  no examples found under examples/")
        return 1
    for path in example_paths:
        try:
            schema_file = schema_for_path(path)
        except ValueError as exc:
            failed += 1
            print(f"FAIL  {exc}")
            continue
        payload = load_json(path)
        if not isinstance(payload, dict):
            failed += 1
            print(f"FAIL  {path.relative_to(ROOT)}  payload must be a JSON object")
            continue
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

    print("== full-field fixtures ==")
    full_paths = sorted(FULL.rglob("*.json")) if FULL.is_dir() else []
    if not full_paths:
        failed += 1
        print("FAIL  no full fixtures under testdata/full/")
    for path in full_paths:
        try:
            schema_file = schema_for_path(path)
        except ValueError as exc:
            failed += 1
            print(f"FAIL  {exc}")
            continue
        payload = load_json(path)
        v = validator_for(schema_file, registry)
        errors = [e.message for e in v.iter_errors(payload)]
        if schema_file == "bid-request.schema.json" and isinstance(payload, dict):
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
        try:
            schema_file = schema_for_path(path)
        except ValueError as exc:
            failed += 1
            print(f"FAIL  {exc}")
            continue
        payload = load_json(path)
        v = validator_for(schema_file, registry)
        errors = list(v.iter_errors(payload))
        if errors:
            print(f"ok    {path.relative_to(ROOT)}  rejected as expected")
        else:
            failed += 1
            print(f"FAIL  {path.relative_to(ROOT)}  should have been rejected")

    print("== openapi embedded examples ==")
    try:
        import yaml  # type: ignore
    except ImportError:
        failed += 1
        print("FAIL  PyYAML missing; run: pip install -r scripts/requirements.txt")
    else:
        if not OPENAPI.is_file():
            failed += 1
            print(f"FAIL  missing {OPENAPI.relative_to(ROOT)}")
        else:
            doc = yaml.safe_load(OPENAPI.read_text(encoding="utf-8"))
            examples = (doc or {}).get("components", {}).get("examples", {}) or {}
            for name, schema_file in OPENAPI_EXAMPLES.items():
                entry = examples.get(name)
                if not isinstance(entry, dict) or "value" not in entry:
                    failed += 1
                    print(f"FAIL  openapi example {name} missing value")
                    continue
                payload = entry["value"]
                v = validator_for(schema_file, registry)
                errors = [e.message for e in v.iter_errors(payload)]
                if errors:
                    failed += 1
                    print(f"FAIL  openapi/{name}  [{schema_file}]")
                    for message in errors:
                        print(f"      - {message}")
                else:
                    print(f"ok    openapi/{name}  [{schema_file}]")

    if failed:
        print(f"\n{failed} check(s) failed")
        return 1
    print("\nall schema checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
