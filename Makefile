ANDROID_HOME ?= $(HOME)/Android/Sdk
BUILD_TOOLS := $(shell ls -d $(ANDROID_HOME)/build-tools/*/ 2>/dev/null | sort -V | tail -1)
ZIPALIGN := $(BUILD_TOOLS)zipalign
APKSIGNER := $(BUILD_TOOLS)apksigner

RELEASE_KEY_ALIAS ?= fdroid-qrtools

UNSIGNED_APK := app/build/outputs/apk/release/app-release-unsigned.apk
ALIGNED_APK := app/build/outputs/apk/release/app-release-aligned.apk
SIGNED_APK := app-release-signed.apk

.PHONY: all build test release

all: build test

build:
	./gradlew :app:assembleDebug

test:
	./gradlew :app:testDebugUnitTest

# Builds, zipaligns, and signs a release APK for the reproducible-build / F-Droid Binaries
# upload. Reads the keystore from the RELEASE_KEYSTORE env var (never a Makefile default —
# there's no safe default for a signing key) and refuses to run without it.
release:
	@if [ -z "$(RELEASE_KEYSTORE)" ]; then \
		echo "Error: RELEASE_KEYSTORE is not set." >&2; \
		echo "Usage: RELEASE_KEYSTORE=/path/to/release.p12 make release" >&2; \
		exit 1; \
	fi
	./gradlew :app:assembleRelease
	"$(ZIPALIGN)" -v -p 4 "$(UNSIGNED_APK)" "$(ALIGNED_APK)"
	"$(APKSIGNER)" sign --ks "$(RELEASE_KEYSTORE)" --ks-key-alias "$(RELEASE_KEY_ALIAS)" --out "$(SIGNED_APK)" "$(ALIGNED_APK)"
	"$(APKSIGNER)" verify --verbose "$(SIGNED_APK)"
	@echo "Signed release APK: $(SIGNED_APK)"
