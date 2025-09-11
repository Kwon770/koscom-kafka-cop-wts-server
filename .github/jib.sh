#!/bin/bash

# KOSCOM Kafka COP Producer Jib Build Script
imageName=$1
imageTag=$2
profile=${3:-prod}

echo "==== Jib 빌드 시작 ===="
echo "대상 이미지 이름: $imageName"
echo "대상 이미지 태그: $imageTag"
echo "대상 프로파일: $profile"

# 환경변수 설정
export IMAGE_TAG=$imageTag
export ACTIVE_PROFILE=$profile
export IMAGE_NAME=$imageName

# Jib 빌드 실행
echo "Gradle Jib 빌드 실행 중..."
./gradlew jib \
  -Djib.to.image="$imageName:$imageTag" \
  -Djib.to.tags=latest \
  -Dspring.profiles.active="$profile" \
  --no-daemon

if [ $? -eq 0 ]; then
  echo "✅ Jib 빌드 성공!"
  echo "이미지: $imageName:$imageTag"
else
  echo "❌ Jib 빌드 실패!"
  exit 1
fi

echo "==== Jib 빌드 완료 ===="