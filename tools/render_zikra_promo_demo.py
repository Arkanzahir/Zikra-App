"""Cut a landscape teaser around the pauses in the approved Putri narration."""
import argparse
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageOps

from render_zikra_promo_landscape import W, H, PINE, background, between, device
from render_zikra_promo_teaser import ROOT, font, load_recording

FPS = 30
VALIDATION = ROOT / "android/validation"


def demo_still(path):
    with Image.open(path) as original:
        screen = ImageOps.contain(original.convert("RGB"), (326, 724), Image.Resampling.LANCZOS)
        canvas = Image.new("RGB", (326, 724), screen.getpixel((0, 0)))
        canvas.paste(screen, (0, (724-screen.height)//2))
        return canvas


def render(ffmpeg, footage, voice, output):
    if output.exists():
        raise FileExistsError(output)
    home = load_recording(ffmpeg, footage/"home.mp4", 0, 4)
    reader = load_recording(ffmpeg, footage/"quran.mp4", 4.1, 3.6)
    stills = [
        demo_still(VALIDATION/"refined-ui-2026-10-28/native/native-normal/REFINED-hifz-target.png"),
        demo_still(ROOT/".local/global-theme-20261028/screens-final-standard/light-QiblaActivity.png"),
        demo_still(VALIDATION/"prayer-adhan-d02-2026-10-27/native/native-adhan-main-D02-settings-100.png"),
    ]
    brand = Image.open(ROOT/"android/app/src/main/res/drawable-nodpi/zikra_symbol_white.png").convert("RGBA")
    mark = Image.new("RGBA", brand.size, PINE)
    mark.putalpha(brand.getchannel("A"))
    mark.thumbnail((90, 90), Image.Resampling.LANCZOS)
    base = background()
    intro = base.copy()
    intro.alpha_composite(mark, (489, 312))
    ImageDraw.Draw(intro).text((600, 350), "zikra", font=font(80, serif=True), fill=PINE, anchor="lm")
    # Lock the phone to its mark. Only the opening entrance moves the hardware.
    def shot(screen, right, lines, offset=0):
        x = 930 if right else 335
        state = (x+offset, 359, .86, 0 if right else -5, 0, 0)
        phone, (px, py) = device(screen, state)
        frame = base.copy()
        shadow = phone.getchannel("A").point(lambda a: a//6)
        layer = Image.new("L", (W, H))
        layer.paste(shadow, (px+8, py+10))
        frame.paste("#244d41", (0, 0, W, H), layer.filter(ImageFilter.GaussianBlur(12)))
        frame.alpha_composite(phone, (px, py))
        draw = ImageDraw.Draw(frame)
        y = 355 - len(lines)*31
        for line in lines:
            draw.text((108 if right else 608, y), line, font=font(52, True), fill=PINE)
            y += 66
        return frame.convert("RGB")

    routine = [shot(screen, True, copy) for screen, copy in zip(stills, [
        ["Lanjutkan", "hafalanmu."], ["Cek arah", "kiblat."], ["Atur pengingat", "salatmu."],
    ])]
    intro = intro.convert("RGB")
    previous = None
    cmd = [str(ffmpeg), "-hide_banner", "-loglevel", "error", "-n", "-f", "rawvideo",
           "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-", "-i", str(voice),
           "-map", "0:v", "-map", "1:a", "-c:v", "libx264", "-threads", "2", "-crf", "18",
           "-pix_fmt", "yuv420p", "-af", "loudnorm=I=-16:TP=-1.5:LRA=9,apad",
           "-c:a", "aac", "-ar", "48000", "-b:a", "160k", "-t", "15", "-movflags", "+faststart", str(output)]
    with subprocess.Popen(cmd, stdin=subprocess.PIPE) as proc:
        for i in range(15*FPS):
            t = i/FPS
            if t < 1.55:
                frame = intro
            elif t < 5.48:
                elapsed = t-1.55
                frame = shot(home[min(int(elapsed*FPS), len(home)-1)], False,
                             ["Ibadah dan belajar", "dalam satu tempat."], -24*(1-between(elapsed, 0, .34)))
                if elapsed < .34:
                    frame = Image.blend(intro, frame, between(elapsed, 0, .34))
            elif t < 8.98:
                elapsed = t-5.48
                frame = shot(reader[min(int(elapsed*FPS), len(reader)-1)], True,
                             ["Baca Al-Quran", "dengan terjemahan", "dan audio."])
                if elapsed < .2:
                    frame = Image.blend(previous, frame, between(elapsed, 0, .2))
            elif t < 10.58:
                frame = routine[0]
            elif t < 11.76:
                frame = routine[1]
            else:
                frame = routine[2]
            if t < 5.48:
                previous = frame
            proc.stdin.write(frame.tobytes())
        proc.stdin.close()
        if proc.wait():
            raise RuntimeError("Promo encoding failed")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("ffmpeg", "footage", "voice", "output"):
        parser.add_argument("--"+name, type=Path, required=True)
    render(**vars(parser.parse_args()))
