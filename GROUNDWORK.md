# Groundwork decisions

How this app applies [Invertisment/scaffolding](https://github.com/Invertisment/scaffolding) (guideline commit `30b63ff`), and the assumptions made where the guideline is silent or ambiguous.

- **`MainActivity` lives in `keyboard/glue`.** Its only job is sending the user to enable the keyboard, so it belongs to that component rather than a separate launcher one.
- **Pure third-party libraries are allowed in core.** `QrCodec` uses ZXing, which is deterministic and does no I/O.
- **Camera permission checks live in glue.** Treated as OS capability state wired to one UI action, not a business permission decision.
- **IME `InputConnection` calls live in glue,** although INFRA_LAYER lists them as infra: the `InputMethodService` entry point *is* the platform API, so the commit happens where the framework hands over the connection.
- **Round-trip property is statistical.** `decode(encode(text)) == text` doesn't hold universally with ZXing (~0.4% detector misses), so the property test asserts a failure-rate ceiling instead.
