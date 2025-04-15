set -e
export IS_INTERNAL_BUILD=false
./gradlew clean -p okhttp-applicationstream build -PversionName=$1
./gradlew -p okhttp-applicationstream publishGithub -PversionName=$1