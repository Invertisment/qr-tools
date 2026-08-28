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
#
# Passes --alignment-preserved true: apksigner from build-tools >= 35.0.0-rc1 defaults this
# to false, which makes it silently re-pad every zip entry's alignment (4-byte -> 16k page
# alignment for native libs) as part of signing, even when the input is already aligned.
# That rewrites the exact bytes the v2/v3 signature digest is computed over, so F-Droid's
# apksigcopier — which splices our signature onto its own independently-built unsigned APK
# without ever invoking apksigner's realignment — would otherwise see a CHUNKED_SHA256
# digest mismatch.
#
# Passes --v1-signing-enabled false: apksigner also adds a v1/JAR signature (META-INF/
# MANIFEST.MF, .SF, .RSA) by default, which only exists for pre-Android-7.0 (API < 24)
# compatibility. minSdk here is already 24, so it adds nothing but extra zip entries —
# and apksigcopier splices those back on at a different byte position than apksigner's own
# signing pass used, which is enough to break the whole-file v2/v3 digest even though the
# entry contents are identical. Disabling v1 removes those entries entirely, so there's
# nothing for the splice to place differently.
#
# Confirmed both of the above the hard way, in the same buildserver container F-Droid's CI
# uses: splicing our own signature back onto the exact unsigned APK it was generated from
# failed with a CHUNKED_SHA256 mismatch until both flags were applied. (JDK version, once
# suspected too, turned out not to matter — the unsigned APK was proven byte-identical
# across JDK 21 and JDK 26 builds.)
release:
	@if [ -z "$(RELEASE_KEYSTORE)" ]; then \
		echo "Error: RELEASE_KEYSTORE is not set." >&2; \
		echo "Usage: RELEASE_KEYSTORE=/path/to/release.p12 make release" >&2; \
		exit 1; \
	fi
	./gradlew :app:assembleRelease
	"$(APKSIGNER)" sign --ks "$(RELEASE_KEYSTORE)" --ks-key-alias "$(RELEASE_KEY_ALIAS)" --alignment-preserved true --v1-signing-enabled false --out "$(SIGNED_APK)" "$(UNSIGNED_APK)"
	"$(APKSIGNER)" verify --verbose "$(SIGNED_APK)"
	@echo "Signed release APK: $(SIGNED_APK)"
