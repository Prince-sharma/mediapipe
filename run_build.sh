#docker cp LICENSE 75fa4b7958e7:/mediapipe/LICENSE_killroy
DOCKER_NAME=ba952d0610e9
docker cp mediapipe/. $DOCKER_NAME:/mediapipe/mediapipe
docker exec -it mediapipe5 bazel build -c opt --config=android_arm64 mediapipe/examples/android/src/java/com/google/mediapipe/apps/edgedetectiongpu
docker cp $DOCKER_NAME:/mediapipe/bazel-bin/mediapipe/examples/android/src/java/com/google/mediapipe/apps/. ~/Desktop/HAND_FACE/mediapipe_working_calc/gen_apps