# Groundwork decisions

How this app applies [Invertisment/scaffolding](https://github.com/Invertisment/scaffolding) (guideline commit `d6e2d35`), and the assumptions made where the guideline is silent or ambiguous.

- **`MainActivity` lives in `keyboard/glue`.** Its only job is sending the user to enable the keyboard, so it belongs to that component rather than a separate launcher one.
- **Pure third-party libraries are allowed in core.** `QrCodec` uses ZXing, which is deterministic and does no I/O, so it's core — not infra, despite the guideline's QR round-trip example under INFRA_LAYER.
- **Core purity check is a denylist of `android*` imports only.** No clock, random, or filesystem use exists today; the check doesn't ban them yet.
- **Camera permission checks live in glue.** Treated as OS capability state wired to one UI action, not a business permission decision.
- **IME `InputConnection` calls live in glue.** The `InputMethodService` entry point *is* the platform API, so the commit happens where the framework hands over the connection.
- **Round-trip property is statistical.** `decode(encode(text)) == text` doesn't hold universally with ZXing (~0.4% detector misses), so the property test asserts a failure-rate ceiling instead.
- **Camera infra is deliberately untested.** A camera can't run headless, so `QrScanner` problems are found on real devices, the same way an API outage is found in production.
