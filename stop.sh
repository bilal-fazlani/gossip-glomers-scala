set -e

lock_file="$PROJECT_DIR/src/main/resources/META-INF/native-image/.lock"
if test -f $lock_file; then
  file_contents=$(cat "$lock_file")
  kill $file_contents
else
  echo "No lock file found at $lock_file"
fi  