"""Render a Zikra product demo from a reproducible edit decision list."""
import argparse
import json
import math
import subprocess
from functools import lru_cache
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageOps

from promo_device import device
from render_zikra_promo_landscape import background, smooth
from render_zikra_promo_teaser import ROOT, font

W, H, FPS = 1280, 720, 30
PINE, MUTED = "#173c34", "#66776c"


def opacity(image, amount):
    result = image.copy()
    result.putalpha(result.getchannel("A").point(lambda a: round(a*max(0, min(1, amount)))))
    return result


def text(lines, x, y, size, elapsed, *, centered=False, color=PINE):
    layer = Image.new("RGBA", (W, H))
    draw = ImageDraw.Draw(layer)
    for i, line in enumerate(lines):
        appear = smooth((elapsed-i*.12)/.42)
        draw.text((x, y+i*size*1.22+10*(1-appear)), line, font=font(size, True),
                  fill=color+f"{round(255*appear):02x}", anchor="mt" if centered else "lt")
    return layer


@lru_cache(maxsize=1)
def logo():
    source = Image.open(ROOT/"android/app/src/main/res/drawable-nodpi/zikra_symbol_white.png").convert("RGBA")
    mark = Image.new("RGBA", source.size, PINE)
    mark.putalpha(source.getchannel("A"))
    return ImageOps.contain(mark, (92, 92), Image.Resampling.LANCZOS)


def mark_frame(base, ending, elapsed):
    frame = base.copy()
    lockup = Image.new("RGBA", (W, H))
    lockup.alpha_composite(logo(), (475, 263))
    draw = ImageDraw.Draw(lockup)
    draw.text((588, 252), "zikra", font=font(94, serif=True), fill=PINE)
    frame.alpha_composite(opacity(lockup, smooth(elapsed/.5)))
    if ending:
        frame.alpha_composite(text(["Ibadah, belajar, dan aktivitas harian."], 640, 391, 30, elapsed-.1, centered=True))
        frame.alpha_composite(text(["Yuk, coba versi betanya."], 640, 473, 23, elapsed-2.4, centered=True))
    return frame


def media_frames(ffmpeg, source, start, duration):
    """Decode one shot at 15 fps, bounding memory while preserving its full view."""
    command = [str(ffmpeg), "-hide_banner", "-loglevel", "error", "-ss", str(start),
               "-i", str(source), "-t", str(duration), "-vf", "fps=15:round=up:eof_action=pass,scale=326:724",
               "-pix_fmt", "rgb24", "-threads", "1", "-f", "rawvideo", "-"]
    raw = subprocess.run(command, capture_output=True, check=True).stdout
    stride = 326*724*3
    if not raw or len(raw) % stride:
        raise RuntimeError(f"Incomplete source recording: {source.name}")
    return [Image.frombytes("RGB", (326, 724), raw[i:i+stride]) for i in range(0, len(raw), stride)]


def screen_at(frames, elapsed, rate):
    return frames[min(len(frames)-1, max(0, int(elapsed*15*rate)))]


@lru_cache(maxsize=16)
def character_texture(path, width, height):
    with Image.open(ROOT/path) as source:
        return ImageOps.contain(source.convert("RGBA"), (width, height), Image.Resampling.LANCZOS)


def composite_character(frame, shot, elapsed):
    item = shot.get("character")
    if item is None:
        return
    x, y, width, height = item["box"]
    image = character_texture(item["source"], width, height)
    appear = smooth((elapsed-item.get("delay", 0))/.4)
    position = (round(x+(width-image.width)/2),
                round(y+height-image.height+12*(1-appear)))
    frame.alpha_composite(opacity(image, appear), position)


def camera(shot, previous, elapsed):
    target = shot["pose"]
    if previous is None:
        return target
    u = min(1, elapsed/shot.get("transition", .85))
    ease = smooth(u)
    state = [a+(b-a)*ease for a, b in zip(previous, target)]
    if shot.get("spin"):
        state[3] += 360*ease
        state[5] += 5*math.sin(math.pi*u)
    return state


def composite_phone(frame, screen, pose):
    phone, (x, y) = device(screen, pose)
    shadow = Image.new("L", (W, H))
    shadow.paste(phone.getchannel("A").point(lambda a: round(a*.13)), (x+12, y+13))
    frame.paste("#28533d", (0, 0, W, H), shadow.filter(ImageFilter.GaussianBlur(13)))
    frame.alpha_composite(phone, (x, y))


def callout(frame, item, elapsed):
    appear = smooth((elapsed-item.get("delay", 0))/.3)
    if appear <= 0:
        return
    label, tip, box = item["label"], item["tip"], item["box"]
    layer = Image.new("RGBA", (W, H))
    draw = ImageDraw.Draw(layer)
    face = font(21, True)
    width = round(draw.textlength(label, font=face))+38
    x, y = box
    target_x, target_y = tip
    edge = x if x > target_x else x+width
    draw.line((target_x, target_y, edge, y+24), fill="#5c8e6c", width=2)
    draw.ellipse((target_x-5, target_y-5, target_x+5, target_y+5), fill="#477e5a")
    draw.rounded_rectangle((x, y, x+width, y+48), 16, fill="#ffffff", outline="#d5e3d5", width=1)
    draw.text((x+19, y+11), label, font=face, fill=PINE)
    frame.alpha_composite(opacity(layer, appear))


def assemble_voice(ffmpeg, raw_voice, work, audio):
    # Split only at measured pauses, adding room for actions without changing voice speed.
    boundaries = [0, 1.59, 5.31, 8.20, 12.45, 15.50, 20.10, 23.17, 27.18, 30.35, 34.70, 41.80]
    gaps = [.2, .5, .6, .4, .5, .7, 1., .3, .2, .2, .2]
    filters, pieces = ["anullsrc=r=48000:cl=mono:d=0.4[lead]"], ["[lead]"]
    for i, (a, b, gap) in enumerate(zip(boundaries, boundaries[1:], gaps)):
        filters.append(f"[0:a]atrim=start={a}:end={b},asetpts=PTS-STARTPTS,aresample=48000[a{i}]")
        filters.append(f"anullsrc=r=48000:cl=mono:d={gap}[s{i}]")
        pieces.extend((f"[a{i}]", f"[s{i}]"))
    filters.append("".join(pieces)+f"concat=n={len(pieces)}:v=0:a=1,loudnorm=I=-16:TP=-1.5:LRA=9,apad,atrim=duration=47[out]")
    subprocess.run([str(ffmpeg), "-hide_banner", "-loglevel", "error", "-n", "-i", str(raw_voice),
                    "-filter_complex", ";".join(filters), "-map", "[out]", "-c:a", "pcm_s16le", str(audio)], check=True)


def render(edit, ffmpeg, output, work, voice, prepared_audio=None):
    if output.exists():
        raise FileExistsError(output)
    work.mkdir(parents=True, exist_ok=True)
    shots = json.loads(edit.read_text(encoding="utf-8-sig"))
    duration = shots[-1]["end"]
    assert shots[0]["start"] == 0 and math.isfinite(duration) and duration > 0
    assert all(a["end"] == b["start"] for a, b in zip(shots, shots[1:]))
    assert all(s["end"] > s["start"] for s in shots)
    if prepared_audio is None and duration != 47:
        raise ValueError("Supply prepared audio for a new narration timeline")
    audio = prepared_audio or work/"narration-timed.wav"
    if not audio.exists():
        if prepared_audio is not None:
            raise FileNotFoundError(prepared_audio)
        assemble_voice(ffmpeg, voice, work, audio)
    base = background()
    picture = work/"picture-full.mp4"
    command = [str(ffmpeg), "-hide_banner", "-loglevel", "error", "-y", "-f", "rawvideo",
               "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-", "-an",
               "-c:v", "libx264", "-threads", "2", "-preset", "fast", "-crf", "18",
               "-pix_fmt", "yuv420p", "-movflags", "+faststart", str(picture)]
    with (work/"render-full.log").open("wb") as log, subprocess.Popen(command, stdin=subprocess.PIPE, stderr=log) as encoder:
        previous = None
        last_frame = base.convert("RGB")
        for shot in shots:
            frames = None
            if shot.get("source"):
                path = ROOT/shot["source"]
                frames = media_frames(ffmpeg, path, shot.get("in", 0), shot.get("length", 8))
            first, end = round(shot["start"]*FPS), round(shot["end"]*FPS)
            for i in range(first, end):
                t = i/FPS
                elapsed = t-shot["start"]
                if shot["type"] in ("intro", "outro"):
                    frame = mark_frame(base, shot["type"] == "outro", elapsed)
                elif shot["type"] == "title":
                    frame = base.copy()
                    composite_character(frame, shot, elapsed)
                    frame.alpha_composite(text(shot["copy"], shot.get("copyX", 640),
                                               shot.get("copyY", 290), shot.get("font", 46),
                                               elapsed, centered=shot.get("centered", True)))
                else:
                    frame = base.copy()
                    pose = camera(shot, previous, elapsed)
                    if shot["type"] == "hero":
                        halo = Image.new("RGBA", (W, H))
                        cx, cy = pose[:2]
                        ImageDraw.Draw(halo).ellipse((cx-190, cy-175, cx+190, cy+195), fill="#dcebd4")
                        frame.alpha_composite(halo)
                    composite_character(frame, shot, elapsed)
                    screen = screen_at(frames, elapsed, shot.get("rate", 1))
                    composite_phone(frame, screen, pose)
                    if shot.get("copy"):
                        text_x = 120 if shot["pose"][0] > 700 else 615
                        delay = shot.get("copyDelay", .5 if previous is not None else .1)
                        frame.alpha_composite(text(shot["copy"], text_x, shot.get("copyY", 270), shot.get("font", 43), elapsed-delay))
                    for item in shot.get("callouts", []):
                        callout(frame, item, elapsed)
                    if shot.get("note"):
                        ImageDraw.Draw(frame).text((640, 698), shot["note"], font=font(13), fill=MUTED, anchor="mb")
                # Short dissolve only at cuts. Full spins are kept visible at hero transitions.
                if shot.get("dissolve") and elapsed < .18:
                    frame = Image.blend(last_frame, frame.convert("RGB"), smooth(elapsed/.18))
                frame = frame.convert("RGB")
                if i in [first+min(45, end-first-1), end-1]:
                    frame.save(work/f"review-{i:04}.jpg", quality=94)
                encoder.stdin.write(frame.tobytes())
            last_frame = frame
            previous = shot.get("pose")
            print(json.dumps({"shot": shot["id"], "seconds": shot["end"]}), flush=True)
        encoder.stdin.close()
        if encoder.wait(timeout=90):
            raise RuntimeError("Video encoding failed")
    subprocess.run([str(ffmpeg), "-hide_banner", "-loglevel", "error", "-n", "-i", str(picture),
                    "-i", str(audio), "-map", "0:v", "-map", "1:a", "-c:v", "copy",
                    "-c:a", "aac", "-b:a", "160k", "-ar", "48000", "-t", str(duration),
                    "-movflags", "+faststart", str(output)], check=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("edit", "ffmpeg", "output", "work", "voice"):
        parser.add_argument("--"+name, type=Path, required=True)
    parser.add_argument("--prepared-audio", type=Path)
    render(**vars(parser.parse_args()))
