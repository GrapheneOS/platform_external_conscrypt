#!/bin/bash

if [ -z "$ANDROID_BUILD_TOP" ]; then
    echo "Missing environment variables. Did you run build/envsetup.sh and lunch?" 1>&2
    exit 1
fi

CLASSPATH=${ANDROID_HOST_OUT}/framework/currysrc.jar:${ANDROID_HOST_OUT}/framework/android_conscrypt_srcgen.jar
CONSCRYPT_DIR=${ANDROID_BUILD_TOP}/external/conscrypt

cd ${ANDROID_BUILD_TOP}
make -j15 currysrc android_conscrypt_srcgen

CORE_PLATFORM_API_FILE=${CONSCRYPT_DIR}/srcgen/core-platform-api.txt

function do_transform() {
  local SRC_IN_DIR=$1
  local SRC_OUT_DIR=$2

  if [ ! -d $SRC_OUT_DIR ]; then
    echo ${SRC_OUT_DIR} does not exist >&2
    exit 1
  fi
  rm -rf ${SRC_OUT_DIR}
  mkdir -p ${SRC_OUT_DIR}

  java -cp ${CLASSPATH} com.android.conscrypt.srcgen.ConscryptTransform ${SRC_IN_DIR} ${SRC_OUT_DIR} ${CORE_PLATFORM_API_FILE}
}

REPACKAGED_DIR=${CONSCRYPT_DIR}/repackaged
for i in $(ls ${REPACKAGED_DIR})
do
  for s in src/main/java src/test/java
  do
    IN=${CONSCRYPT_DIR}/$i/$s
    if [ -d $IN ]; then
      do_transform ${IN} ${REPACKAGED_DIR}/$i/$s
    fi
  done
done

# Remove some unused test files:
rm -fr ${REPACKAGED_DIR}/openjdk-integ-tests/src/test/java/com/android/org/conscrypt/ConscryptSuite.java
rm -fr ${REPACKAGED_DIR}/openjdk-integ-tests/src/test/java/com/android/org/conscrypt/ConscryptJava6Suite.java

# Remove the libcore package as that is provided by libcore.
rm -fr ${REPACKAGED_DIR}/testing/src/main/java/libcore
