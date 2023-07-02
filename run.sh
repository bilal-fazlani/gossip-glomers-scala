set -ex

java -jar -agentlib:native-image-agent=config-output-dir=src/main/resources/META-INF/native-image -jar $1
