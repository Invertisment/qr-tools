ANDROID_HOME ?= $(HOME)/Android/Sdk
BUILD_TOOLS := $(shell ls -d $(ANDROID_HOME)/build-tools/*/ 2>/dev/null | sort -V | tail -1)
APKSIGNER := $(BUILD_TOOLS)apksigner

RELEASE_KEY_ALIAS ?= fdroid-qrtools

UNSIGNED_APK := app/build/outputs/apk/release/app-release-unsigned.apk
SIGNED_APK := app-release-signed.apk

.PHONY: all build test release

all: build test

build:
	./gradlew :app:assembleDebug

test:
	./gradlew :app:testDebugUnitTest

# Builds and signs a release APK for the reproducible-build / F-Droid Binaries upload. Reads
# the keystore from the RELEASE_KEYSTORE env var (never a Makefile default — there's no safe
# default for a signing key) and refuses to run without it.
#
# Deliberately does NOT run a separate zipalign pass: AGP has built zipalign directly into the
# packaging task since AGP 2.2.0, so assembleRelease's output is already aligned. Re-aligning
# it here rewrites the zip archive and produces different bytes than F-Droid's own rebuild
# (which just uses Gradle's direct output) — confirmed the hard way, this broke the
# reproducible-build byte comparison the first time around.
release:
	@if [ -z "$(RELEASE_KEYSTORE)" ]; then \
		echo "Error: RELEASE_KEYSTORE is not set." >&2; \
		echo "Usage: RELEASE_KEYSTORE=/path/to/release.p12 make release" >&2; \
		exit 1; \
	fi
	./gradlew :app:assembleRelease
	"$(APKSIGNER)" sign --ks "$(RELEASE_KEYSTORE)" --ks-key-alias "$(RELEASE_KEY_ALIAS)" --out "$(SIGNED_APK)" "$(UNSIGNED_APK)"
	"$(APKSIGNER)" verify --verbose "$(SIGNED_APK)"
	@echo "Signed release APK: $(SIGNED_APK)"
