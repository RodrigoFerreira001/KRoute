.PHONY: publish-local

publish-local:
	./gradlew :kroute:publishToMavenLocal :kroute-google-cloud-extension:publishToMavenLocal
