set -ex

rm -rf "$PROJECT_DIR"/src/main/resources/META-INF/native-image

java -jar \
"-agentlib:native-image-agent=config-merge-dir=$PROJECT_DIR/src/main/resources/META-INF/native-image" \
-jar "$PROJECT_DIR/target/$PROJECT_NAME.jar"