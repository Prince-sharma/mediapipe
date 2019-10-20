// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include <cmath>
#include <vector>

#include "mediapipe/framework/calculator_framework.h"
#include "mediapipe/framework/formats/detection.pb.h"
#include "mediapipe/framework/formats/location.h"

// #include "mediapipe/framework/formats/location_data.pb.h"
// #include "mediapipe/framework/formats/rect.pb.h" 

#include "mediapipe/framework/port/ret_check.h"
#include "mediapipe/framework/port/status.h"

namespace mediapipe {

namespace {

constexpr char kDetectionsTag[] = "DETECTIONS";
constexpr char kLetterboxPaddingTag[] = "LETTERBOX_PADDING";

}  // namespace

// Adjusts detection locations on a letterboxed image to the corresponding
// locations on the same image with the letterbox removed. This is useful to map
// the detections inferred from a letterboxed image, for example, output of
// the ImageTransformationCalculator when the scale mode is FIT, back to the
// corresponding input image before letterboxing.
//
// Input:
//   DETECTIONS: An std::vector<Detection> representing detections on an
//   letterboxed image.
//
//   LETTERBOX_PADDING: An std::array<float, 4> representing the letterbox
//   padding from the 4 sides ([left, top, right, bottom]) of the letterboxed
//   image, normalized to [0.f, 1.f] by the letterboxed image dimensions.
//
// Output:
//   DETECTIONS: An std::vector<Detection> representing detections with their
//   locations adjusted to the letterbox-removed (non-padded) image.
//
// Usage example:
// node {
//   calculator: "DetectionLetterboxRemovalCalculator"
//   input_stream: "DETECTIONS:detections"
//   input_stream: "LETTERBOX_PADDING:letterbox_padding"
//   output_stream: "DETECTIONS:adjusted_detections"
// }
class DetectionsToFloatCalculator : public CalculatorBase {
 public:
  static ::mediapipe::Status GetContract(CalculatorContract* cc) {
    RET_CHECK(cc->Inputs().HasTag(kDetectionsTag) &&
              cc->Inputs().HasTag(kLetterboxPaddingTag))
        << "Missing one or more input streams.";

    cc->Inputs().Tag(kDetectionsTag).Set<std::vector<Detection>>();
    cc->Inputs().Tag(kLetterboxPaddingTag).Set<std::array<float, 4>>();
//     if(cc->Inputs().HasTag("NORM_RECT")){
//         cc->Inputs().Tag("NORM_RECT").Set<std::vector<NormalizedRect>>();
//     }
    cc->Outputs().Tag(kDetectionsTag).Set<std::vector<Detection>>();
//     if (cc->Outputs().HasTag("DET_TO_FLOAT")) {
//         cc->Outputs().Tag("DET_TO_FLOAT").Set<std::vector<float>>();
//     }
      
    return ::mediapipe::OkStatus();
  }

  ::mediapipe::Status Open(CalculatorContext* cc) override {
    cc->SetOffset(TimestampDiff(0));

    return ::mediapipe::OkStatus();
  }

  ::mediapipe::Status Process(CalculatorContext* cc) override {
    // Only process if there's input detections.
    if (cc->Inputs().Tag(kDetectionsTag).IsEmpty()) {
      return ::mediapipe::OkStatus();
    }

    const auto& input_detections =
        cc->Inputs().Tag(kDetectionsTag).Get<std::vector<Detection>>();
    const auto& letterbox_padding =
        cc->Inputs().Tag(kLetterboxPaddingTag).Get<std::array<float, 4>>();
      

    const float left = letterbox_padding[0];
    const float top = letterbox_padding[1];
    const float left_and_right = letterbox_padding[0] + letterbox_padding[2];
    const float top_and_bottom = letterbox_padding[1] + letterbox_padding[3];

    auto output_detections = absl::make_unique<std::vector<Detection>>();
    for (const auto& detection : input_detections) {
      Detection new_detection;
      new_detection.CopyFrom(detection);
      LocationData::RelativeBoundingBox* relative_bbox =
          new_detection.mutable_location_data()
              ->mutable_relative_bounding_box();

      relative_bbox->set_xmin(
          (detection.location_data().relative_bounding_box().xmin() - left) /
          (1.0f - left_and_right));
      relative_bbox->set_ymin(
          (detection.location_data().relative_bounding_box().ymin() - top) /
          (1.0f - top_and_bottom));
      // The size of the bounding box will change as well.
      relative_bbox->set_width(
          detection.location_data().relative_bounding_box().width() /
          (1.0f - left_and_right));
      relative_bbox->set_height(
          detection.location_data().relative_bounding_box().height() /
          (1.0f - top_and_bottom));

      // Adjust keypoints as well.
      for (int i = 0;
           i < new_detection.mutable_location_data()->relative_keypoints_size();
           ++i) {
        auto* keypoint =
            new_detection.mutable_location_data()->mutable_relative_keypoints(
                i);
        const float new_x = (keypoint->x() - left) / (1.0f - left_and_right);
        const float new_y = (keypoint->y() - top) / (1.0f - top_and_bottom);
        keypoint->set_x(new_x);
        keypoint->set_y(new_y);
      }

      output_detections->emplace_back(new_detection);
    }

    cc->Outputs()
        .Tag("DETECTIONS")
        .Add(output_detections.release(), cc->InputTimestamp());
      
//     if (cc->Outputs().HasTag("DET_TO_FLOAT")) {
//     auto output_vecs =
//         absl::make_unique<std::vector<float>>(5,0);

// //     const auto& location_data = detections[0].location_data();
// //     const float x0 = location_data.relative_keypoints(start_keypoint_index_).x();
// //     const float y0 = location_data.relative_keypoints(start_keypoint_index_).y();
// //     const float x1 = location_data.relative_keypoints(end_keypoint_index_).x();
// //     const float y1 = location_data.relative_keypoints(end_keypoint_index_).y();
// //     output_vecs->at(0)=x0;
// //     output_vecs->at(1)=y0;
// //     output_vecs->at(2)=x1;
// //     output_vecs->at(3)=y1;
// //     float rotation = target_angle_ - std::atan2(-(y1 - y0), x1 - x0);
// //     output_vecs->at(4)=rotation; 
//     cc->Outputs().Tag("DET_TO_FLOAT").Add(output_vecs.release(),
//                                          cc->InputTimestamp());
//     }
      
    return ::mediapipe::OkStatus();
  }
};
REGISTER_CALCULATOR(DetectionsToFloatCalculator);

}  // namespace mediapipe
