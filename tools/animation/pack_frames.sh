#!/usr/bin/env bash
# Resource encoding only: frames come from the original Blender scene, not edited references.
set -euo pipefail
scene_name=$1
frames_dir=$2
asset_dir=$3
mkdir -p "$asset_dir"
ffmpeg -y -v error -framerate 24 -start_number 1 -i "$frames_dir/$scene_name-%04d.png" -vf 'tile=8x6:padding=0:margin=0' -frames:v 1 -c:v libwebp -quality 86 -compression_level 6 "$asset_dir/onboard_${scene_name}_motion.webp"
cp "$frames_dir/$scene_name-end.png" "$asset_dir/onboard_${scene_name}_end.png"
