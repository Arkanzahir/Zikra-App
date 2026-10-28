# Zikra — splash motion reference

28 October2026. Requested animated splash reference, preserving the approved
moon/Quran/spark logo. **Design reference only; APK1.9.22 is unchanged.**

- [Animated GIF](splash-preview.gif) / [MP4](splash-preview.mp4).
- [Storyboard](storyboard.png) / [vector storyboard](storyboard.svg).
- [Interactive preview](preview/index.html): replay, phase buttons, keyboard
  scrubber and reduced-motion control. Serve only `preview/` on loopback with a
  static HTTP server for dependable browser/font loading.
- [Timing/source contract](motion.json), [original vector parts](mark-parts.json),
  [static vector](splash-static.svg), [verification](validation.json).

## Choreography

The original emblem begins centred. Book halves settle, the crescent steadies
and the spark brightens once. The emblem moves left while the editable wordmark
appears. Motion settles in about1.12seconds. The GIF/MP4 includes a short hold
and loops for review; a production splash must not loop or impose that hold.
The background remains the app's pine `#15332F`, with one faint halo, one emblem
and one wordmark. No loading percentage, fake spinner, sound, tagline or large
ghost logo is added. Font: the existing Plus Jakarta Sans app asset.

Reduced motion shows the final static lockup. The prototype runs only during
replay and stops requesting animation frames when finished or hidden. It uses
no backend, external fonts, account details, uploads or persistent user data.

## Figma status — incomplete

[Zikra — Splash Motion](https://www.figma.com/design/sRcHARfqt4E3OrGH13PgX6)
was created. Read-only discovery confirmed the product font and native motion
API. Code Connect/existing screens and relevant libraries were checked; there
were no compatible Zikra brand assets, so a scoped construction script was
prepared from the approved SVG and code colors.

**The construction call was rejected by the Figma MCP Starter call limit.**
No completed editable canvas, rendered Figma animation or working Figma prototype
is claimed. No paid upgrade or quota workaround was performed. The local
animation above is rendered from SVG with resvg and encoded locally; it is
explicitly not a Figma export. The attempted Figma script and state ledger remain
private for resuming after access is available. The vector storyboard is a
portable source, not a flattened screenshot imported as a fake editable canvas.

## Review and future Android handoff

The supplied videos were sampled privately. The first shows Android's large
launch icon followed by a much smaller in-app lockup; the attached still catches
their visual overlap. The second demonstrates an icon-to-wordmark reveal and
expanding color field. No private video frames or third-party branding were
uploaded to Figma or included in Git.

The reference addresses identity/scale continuity. Actual removal of the old
large/small overlap still needs implementation: align Android12+ system splash
and Activity content, transfer one mark through the exit, and navigate to Home
once. Test cold/warm launches, older Android fallback, large fonts, background
re-entry and system animator-disabled behavior. Avoid delaying a ready Home for
a decorative intro. These are handoff requirements, not completed native tests.

Local checks verified the source vector paths and font bytes, sampled the
encoded MP4 phases, inspected the storyboard, tested browser replay/phase/
keyboard/reduced-motion behavior and narrow/desktop overflow. A desktop aspect
ratio defect was corrected and rechecked. The temporary browser viewport was
reset. This does not close F01/F04 or any A–D/public-release gate.
