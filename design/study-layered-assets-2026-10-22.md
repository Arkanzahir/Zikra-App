# Aset Study Hub — 22 Oktober 2026

Dibuat terlebih dahulu dengan built-in `image_gen.imagegen`, lalu dipasang ke Android native.
Referensi gaya: [konsep pilihan pengguna](approved-2026-10-21/study-focus-break-concept.png).
PNG asli disalin ke `android/app/src/main/res/drawable-nodpi/` tanpa pengeditan raster eksternal.
Atlas dibagi melalui source rectangle saat digambar; alfa asli dipertahankan. Android mendecode
empat aset sekali dengan inSampleSize2 dan berbagi bitmap lewat cache weak reference.

Ruangan diam. Karakter mempunyai dua pose. Lengan menulis berputar pada siku dan pergelangan;
lengan minum mengangkat cangkir ke bibir, menahan/memiringkan, lalu turun dalam siklus delapan detik.
Gerak mengikuti waktu adegan tersendiri, tidak mengubah deadline timer. Pause/animasi mati/
latar belakang menghentikan callback. Ini rig 2D dengan beberapa titik putar, bukan simulasi tangan 3D.

## Room plate

Aset: [study_room_v2.png](../android/app/src/main/res/drawable-nodpi/study_room_v2.png).
Transparan: tidak (background plate).

Prompt final:

Use case: illustration-story. Production background plate for a layered animated Android study timer, not a UI mockup. The input is STYLE AND ROOM REFERENCE ONLY, especially the middle focus illustration. Generate ONLY the empty study room and desk, NO PERSON, no hands, no arms. Landscape composition 3:2. Cozy detailed hand-painted editorial illustration, delicate ink outlines, warm sunlight, sage green walls, oak desk, softly rendered foliage outside a tall wood-framed window on the RIGHT, leafy indoor plant at LEFT, simple dark olive study chair centered. Eye-level front three-quarter camera, match reference. An open blank notebook lies on the desktop at lower center, its left page around x48%, y83%; a graphite laptop with screen angled toward the center at lower RIGHT; two closed unlabelled books at bottom LEFT; pencil cup at far bottom RIGHT. Desk surface starts near y78% and extends to bottom edge. Keep central wall/chair region x30%-68%, y28%-78% clear for a separate seated student overlay. No letters, words, Arabic, numbers, logos, UI, phones, frames, watermarks, people or human shapes. A small blank cream wall note is okay. The desk and all objects remain fixed during animation. Fill canvas to edges, no border, no rounded mask (app applies it). Aim for the rich illustrated finish of the supplied focus scene, not flat geometric clip art, not glossy 3D, not photorealism.

## Two character poses

Aset: [study_student_poses_v2.png](../android/app/src/main/res/drawable-nodpi/study_student_poses_v2.png).
Transparan: ya.

Prompt final:

Use case: illustration-story. Create ONE transparent 2-cell character sprite atlas for a layered study animation. Reference image 1 is the Zikra character/style reference: the SAME young male student as in its middle and right phone illustrations: wavy dark brown/black hair, warm medium skin, calm face, sage green long-sleeved crewneck shirt. Reference image 2 is the room lighting/style the character will be composited into (warm daylight from viewer RIGHT). Output landscape canvas containing exactly TWO equally sized VERTICAL cells side by side, no divider, no text, no scene. Genuine transparent background. In each cell depict the same waist-up seated student, same body scale, waist and right elbow at identical positions; head near top center, shoulders near cell middle, torso to bottom. Left cell FOCUS pose: head tilted slightly down toward notebook, attentive relaxed expression. Right cell BREAK pose: head lifted a little, eyes relaxed/softly closed, lips neutral for sipping. Detailed painted storybook/editorial rendering with fine dark outlines and soft watercolor shading, match reference; NOT simplified geometric clipart. This is a CHARACTER BODY layer for a 2D puppet rig: include the left arm (viewer RIGHT) resting low across the bottom as if on a desk, with a relaxed supporting hand. The animated RIGHT forearm (viewer LEFT) is a separate layer: show its sage upper sleeve down to the rolled elbow cuff near lower LEFT, but DO NOT draw the right forearm, right hand, pen, or cup. The rolled elbow cuff is a clean clothing boundary for layering, not an injury. Keep both cell body silhouettes consistent and aligned, no cropped hair, no legs, no chair, no desk, no notebook, no laptop, no objects, no ground shadow, no background, no labels. Each character must fit entirely in its own half of the canvas with a little transparent margin; do not overlap cells. High-quality illustration closely matching the user reference student's age, hair, clothes and proportions.

## Writing arm and pen

Aset: [study_writing_arm_v2.png](../android/app/src/main/res/drawable-nodpi/study_writing_arm_v2.png).
Transparan: ya.

Prompt final:

Use case: illustration-story. Production puppet-animation layer on a real transparent background. The supplied character atlas is the identity, rendering, skin-tone and clothing reference. Generate ONLY his RIGHT FOREARM, HAND AND BLACK PEN, viewed from the same front three-quarter camera as the left focus character. Long horizontal composition, approximately 2.5:1. The forearm starts at viewer LEFT with a softly rounded elbow/attachment region (its first 15% will tuck behind the existing open sage sleeve cuff); warm tan bare forearm runs down gently toward viewer RIGHT; natural five-finger right hand at the right end holding a slim black pen in a correct tripod writing grip. Pen nib points DOWN and slightly RIGHT below the hand, as if touching a notebook. Hand and wrist relaxed, believable anatomy, no extra fingers. Match the detailed watercolor/ink editorial character exactly; gentle warm daylight highlights from upper RIGHT. Include only a tiny edge of sage green fabric at the left attachment if needed, no full upper arm, no body, no face, no second hand, no desk, no paper, no cup, no writing marks, no background or shadow. Keep entire forearm/hand/pen inside canvas with a modest clear margin. At rest, elbow is near 10% width 35% height, pen tip near 86% width 85% height. This layer will rotate a few degrees about its elbow and wrist to animate real writing. No labels, guides, cells, text, logos, border, watermark or checkerboard.

## Drinking arm and mug

Aset: [study_drinking_arm_v2.png](../android/app/src/main/res/drawable-nodpi/study_drinking_arm_v2.png).
Transparan: ya.

Prompt final:

Use case: illustration-story. A single isolated RIGHT FOREARM/HAND HOLDING A MUG puppet-animation layer on true transparent background. Match the supplied student's sage-green shirt, warm tan skin, detailed hand-painted ink/watercolor rendering, and daylight from upper RIGHT. No character face or body. This is the student's right arm as seen on viewer LEFT of his seated body, bent upward to bring a cup to his mouth on viewer RIGHT. Composition approximately 4:3: a small rolled sage sleeve/elbow attachment at lower LEFT (around 12% width, 86% height); bare forearm diagonally rises toward upper RIGHT; relaxed right hand naturally grips the handle of a simple warm ivory ceramic mug at upper RIGHT (mug rim around 77% width, 20% height). Mug handle toward viewer LEFT beside the hand, open mug rim clearly visible and level, simple tiny sage/gold band only, no writing/logo. Anatomically believable hand/fingers, entire arm and mug in frame with clear margins. The first part of the sleeve tucks under a separate body layer. The scene is sipping a calm drink during a study break. App will rotate the elbow to lift cup, hold it at the lips, slightly tilt it, and lower it. Do NOT include steam (app adds native steam), second arm, notebook, desk, chair, face, torso, background, shadow, text, labels, guides, border, watermark or checkerboard. Preserve exact illustrated style of the reference atlas, NOT clipart or photorealism.
