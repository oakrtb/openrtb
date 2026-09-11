# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| 0.2.x | Yes |
| 0.1.x | Best-effort (breaking SDK renames in 0.2.0) |

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security-sensitive reports.

Prefer one of:

1. **[GitHub Security Advisories](https://github.com/oakrtb/openrtb/security/advisories/new)**（private disclosure）
2. Email the maintainers via the contact listed on the [GitHub org / repo](https://github.com/oakrtb/openrtb) if Advisories are unavailable

Include: affected component (schema / transport / Go / Java / Rust), version or commit, reproduction steps, and impact.

We aim to acknowledge within **7 days** and coordinate a fix / advisory before public disclosure.

## Scope

In scope: protocol validation bypasses that incorrectly accept malicious or broken OpenRTB payloads when using this project's schema / SDK validators; supply-chain issues in published artifacts.

Out of scope: vulnerabilities in third-party SSP/DSP implementations that merely consume OakRTB; general IAB OpenRTB design questions.
