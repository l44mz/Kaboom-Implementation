// Camera mode on the website: finds the user's face in the camera video with
// MediaPipe Face Landmarker (Google), inside the browser. No picture leaves the computer.
//
// The Flutter app (lib/features/camera/services/web_focus_detector.dart) calls:
//   await kaboomFace.load()       downloads MediaPipe and the face model (once)
//   kaboomFace.detect(video)      the face in the current video frame, or {present: false}
//
// detect() gives the same numbers ML Kit gives on the phone, so the same focus rules work:
//   yaw / pitch / roll  head angles in degrees
//   eyeOpen             0 (closed) to 1 (open), from the eye-blink blendshapes
(function () {
  const VISION = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14';
  const MODEL =
    'https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task';

  let landmarker = null;
  let loading = null;
  let lastTime = 0;

  async function create(vision, files, delegate) {
    return vision.FaceLandmarker.createFromOptions(files, {
      baseOptions: { modelAssetPath: MODEL, delegate: delegate },
      runningMode: 'VIDEO',
      numFaces: 2, // someone behind the user is ignored (smaller face)
      outputFaceBlendshapes: true, // eye blinks
      outputFacialTransformationMatrixes: true, // head angles
    });
  }

  async function load() {
    if (landmarker) return;
    if (!loading) {
      loading = (async () => {
        const vision = await import(VISION + '/vision_bundle.mjs');
        const files = await vision.FilesetResolver.forVisionTasks(VISION + '/wasm');
        try {
          landmarker = await create(vision, files, 'GPU');
        } catch (e) {
          // Some browsers have no usable GPU: the CPU is slower but works.
          landmarker = await create(vision, files, 'CPU');
        }
      })();
    }
    try {
      await loading;
    } catch (e) {
      loading = null; // allow another try (for example after the network came back)
      throw e;
    }
  }

  const deg = (r) => (r * 180) / Math.PI;

  function faceArea(points) {
    let minX = 1, minY = 1, maxX = 0, maxY = 0;
    for (const p of points) {
      if (p.x < minX) minX = p.x;
      if (p.x > maxX) maxX = p.x;
      if (p.y < minY) minY = p.y;
      if (p.y > maxY) maxY = p.y;
    }
    return (maxX - minX) * (maxY - minY);
  }

  function score(blendshapes, name) {
    const c = blendshapes.categories.find((x) => x.categoryName === name);
    return c ? c.score : null;
  }

  function detect(video) {
    if (!landmarker || video.readyState < 2) return null; // not ready yet
    // MediaPipe needs a time that always goes up.
    const now = Math.max(performance.now(), lastTime + 1);
    lastTime = now;
    const result = landmarker.detectForVideo(video, now);
    const faces = result.faceLandmarks || [];
    if (faces.length === 0) return { present: false };

    // The biggest face is the user.
    let best = 0;
    for (let i = 1; i < faces.length; i++) {
      if (faceArea(faces[i]) > faceArea(faces[best])) best = i;
    }

    // Head angles from the 4x4 transformation matrix (column-major).
    const m = result.facialTransformationMatrixes[best].data;
    const r00 = m[0], r10 = m[1], r20 = m[2], r21 = m[6], r22 = m[10];
    const pitch = deg(Math.atan2(r21, r22));
    const yaw = deg(Math.atan2(-r20, Math.hypot(r00, r10)));
    const roll = deg(Math.atan2(r10, r00));

    // Eyes: the blink scores go from 0 (open) to 1 (closed).
    let eyeOpen = null;
    const shapes = result.faceBlendshapes && result.faceBlendshapes[best];
    if (shapes) {
      const left = score(shapes, 'eyeBlinkLeft');
      const right = score(shapes, 'eyeBlinkRight');
      if (left !== null && right !== null) eyeOpen = 1 - (left + right) / 2;
    }
    return { present: true, yaw: yaw, pitch: pitch, roll: roll, eyeOpen: eyeOpen };
  }

  window.kaboomFace = { load: load, detect: detect };
})();
