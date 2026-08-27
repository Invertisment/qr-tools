# QR tools

A small Android utility built around two features:

- A keyboard (input method) with a "Scan QR code" button that opens a camera popup in place, without hiding the keyboard or losing focus on the field you're typing into.
- A share-target: share any text from any app and get a QR code for it in a popup.

No ads, no analytics, no network access. Camera permission is only used while actively scanning.

## Building

```
./gradlew :app:assembleDebug
```

Run the test suite with:

```
make test
```

## License

GPL-3.0-or-later. See [LICENSE](LICENSE).
