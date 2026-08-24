.PHONY: all build test

all: build test

build:
	./gradlew :app:assembleDebug

test:
	./gradlew :app:testDebugUnitTest
