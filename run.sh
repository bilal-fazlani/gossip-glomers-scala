set -ex

rm -rf $BASE_PATH/$PROJECT_NAME/src/main/resources/META-INF/native-image

java -jar \
"-agentlib:native-image-agent=config-merge-dir=$BASE_PATH/$PROJECT_NAME/src/main/resources/META-INF/native-image" \
-jar "$BASE_PATH/$PROJECT_NAME/target/$PROJECT_NAME.jar"