"""Compose a local beta promo from actual emulator recordings and synthetic narration."""
from pathlib import Path
import argparse
import json
import math
import subprocess
import textwrap
from functools import lru_cache

from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parents[1]
WIDTH, HEIGHT, FPS = 720, 1280, 30
PINE, IVORY, GOLD = '#15332f', '#f8f8f0', '#c8ac6b'


@lru_cache(maxsize=32)
def font(size, bold=False, serif=False):
    name = 'georgia.ttf' if serif else 'seguisb.ttf' if bold else 'segoeui.ttf'
    return ImageFont.truetype('C:/Windows/Fonts/' + name, size)


def centered(draw, y, text, face, fill):
    draw.text((WIDTH / 2, y), text, font=face, fill=fill, anchor='mt')


def base_card(title, subtitle, number):
    frame = Image.new('RGB', (WIDTH, HEIGHT), IVORY)
    draw = ImageDraw.Draw(frame)
    draw.ellipse((430, -180, 1000, 420), fill='#e8eee5')
    draw.text((48, 46), f'{number}  /  ZIKRA', font=font(17, True), fill=PINE)
    draw.multiline_text((48, 90), title, font=font(47, True), fill=PINE, spacing=0)
    draw.text((48, 226), subtitle, font=font(20), fill='#5c7068')
    centered(draw, 1240, 'DRAF PROMO  ·  APK BETA  ·  DATA CONTOH', font(12), '#61756c')
    return frame


def brand_card(logo, outro=False):
    frame = Image.new('RGB', (WIDTH, HEIGHT), PINE)
    draw = ImageDraw.Draw(frame)
    for radius in [270, 380, 510]:
        draw.ellipse((360-radius, 520-radius, 360+radius, 520+radius), outline='#24463f', width=1)
    symbol = ImageOps.contain(logo, (220, 220), Image.Resampling.LANCZOS)
    frame.paste(symbol, ((WIDTH-symbol.width)//2, 285), symbol)
    centered(draw, 505, 'zikra', font(100, serif=True), IVORY)
    if outro:
        centered(draw, 685, 'Satu langkah baik,', font(36, True), IVORY)
        centered(draw, 738, 'setiap hari.', font(36, True), IVORY)
        draw.rounded_rectangle((218, 876, 502, 934), radius=29, fill=GOLD)
        centered(draw, 892, 'KENALI ZIKRA', font(18, True), PINE)
    else:
        centered(draw, 685, 'Ibadah dan belajar,', font(36, True), IVORY)
        centered(draw, 738, 'dalam satu ruang.', font(36, True), IVORY)
    centered(draw, 1180, 'Video konsep beta · belum dirilis publik', font(16), '#c2d1c4')
    return frame


def subtitle_at(cues, at):
    for cue in cues:
        if cue['start'] <= at < cue['end']:
            return cue['text']
    return None


def srt_time(seconds):
    millis = round(seconds * 1000)
    return f'{millis//3600000:02}:{millis//60000%60:02}:{millis//1000%60:02},{millis%1000:03}'


def render(work, output, ffmpeg):
    if output.exists():
        raise FileExistsError('Refusing to overwrite an existing video')
    for name in ['home', 'quran', 'focus', 'protector']:
        if not (work / (name + '.mp4')).is_file():
            raise FileNotFoundError(name + '.mp4')
    cues = []
    for line in (work / 'narration-metadata.jsonl').read_text(encoding='utf-8').splitlines():
        value = json.loads(line)
        if value['type'] == 'SentenceBoundary':
            start = value['offset'] / 10000000 / .8 + .7
            end = (value['offset'] + value['duration']) / 10000000 / .8 + .7
            cues.append({'start': start, 'end': end, 'text': value['text']})
    srt = '\n\n'.join(f"{i+1}\n{srt_time(c['start'])} --> {srt_time(c['end'])}\n{c['text']}"
                      for i, c in enumerate(cues)) + '\n'
    output.with_suffix('.srt').write_text(srt, encoding='utf-8')
    logo = Image.open(ROOT / 'android/app/src/main/res/drawable-nodpi/zikra_symbol_white.png').convert('RGBA')
    scenes = [
        (None, 2, brand_card(logo)),
        ('home', 5.5, base_card('Ruang untuk\nhari yang baik.', 'Quran, salat, dan rutinitas harian.', '01')),
        ('quran', 6, base_card('Baca. Pahami.\nDekatkan hati.', 'Al-Quran dalam genggamanmu.', '02')),
        ('focus', 5.5, base_card('Satu tujuan.\nLebih fokus.', 'Temani sesi belajar dan jedamu.', '03')),
        ('protector', 5, base_card('Kenali cuaca.\nRencanakan harimu.', 'Prakiraan lokasi dan informasi sekitar.', '04')),
        (None, 6, brand_card(logo, True)),
    ]
    silent = work / 'promo-picture.mp4'
    log = (work / 'render-log.txt').open('wb')
    encoder = subprocess.Popen([str(ffmpeg), '-hide_banner', '-loglevel', 'error', '-y',
        '-f', 'rawvideo', '-vcodec', 'rawvideo', '-pix_fmt', 'rgb24', '-s', f'{WIDTH}x{HEIGHT}',
        '-r', str(FPS), '-i', '-', '-an', '-c:v', 'libx264', '-threads', '2', '-preset', 'fast',
        '-crf', '19', '-pix_fmt', 'yuv420p', '-movflags', '+faststart', str(silent)],
        stdin=subprocess.PIPE, stderr=log)
    offset, last = 0.0, None
    try:
        for name, duration, background in scenes:
            decoder = None
            if name:
                decoder = subprocess.Popen([str(ffmpeg), '-hide_banner', '-loglevel', 'error',
                    '-i', str(work / (name + '.mp4')), '-vf',
                    'fps=30,scale=412:916:force_original_aspect_ratio=decrease,pad=412:916:(ow-iw)/2:(oh-ih)/2:color=0xf8f8f0',
                    '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-threads', '1', '-'],
                    stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
            previous_scene = last
            try:
                for i in range(round(duration * FPS)):
                    frame = background.copy()
                    if decoder:
                        raw = decoder.stdout.read(412 * 916 * 3)
                        if len(raw) != 412 * 916 * 3:
                            raise RuntimeError('Recording ended before scene: ' + name)
                        screen = Image.frombytes('RGB', (412, 916), raw)
                        x, y = 154, 294 + round(3 * math.sin(i / FPS))
                        shade = Image.new('RGBA', frame.size)
                        ImageDraw.Draw(shade).rounded_rectangle((x-11, y-7, x+425, y+931), radius=39, fill=(0, 0, 0, 55))
                        frame = Image.alpha_composite(frame.convert('RGBA'), shade.filter(ImageFilter.GaussianBlur(13))).convert('RGB')
                        draw = ImageDraw.Draw(frame)
                        draw.rounded_rectangle((x-9, y-9, x+421, y+925), radius=34, fill='#092621')
                        mask = Image.new('L', screen.size)
                        ImageDraw.Draw(mask).rounded_rectangle((0, 0, 411, 915), radius=26, fill=255)
                        frame.paste(screen, (x, y), mask)
                    if previous_scene is not None and i < 7:
                        frame = Image.blend(previous_scene, frame, (i + 1) / 7)
                    text = subtitle_at(cues, offset + i / FPS)
                    if text:
                        lines = textwrap.wrap(text, width=44)
                        draw = ImageDraw.Draw(frame)
                        h = len(lines) * 29 + 22
                        draw.rounded_rectangle((40, 1215-h, 680, 1215), radius=16, fill=PINE)
                        for j, line in enumerate(lines):
                            centered(draw, 1226-h+j*29, line, font(22), '#ffffff')
                    encoder.stdin.write(frame.tobytes())
                    last = frame
                offset += duration
                print(json.dumps({'scene': name or 'brand', 'duration': duration, 'rendered': True}), flush=True)
            finally:
                if decoder:
                    decoder.stdout.close()
                    decoder.terminate()
                    decoder.wait(timeout=10)
    finally:
        encoder.stdin.close()
        encoder.wait(timeout=90)
        log.close()
    if encoder.returncode:
        raise RuntimeError('Video encoder failed')
    subprocess.run([str(ffmpeg), '-hide_banner', '-loglevel', 'error', '-i', str(silent),
        '-i', str(work / 'narration.mp3'), '-filter_complex',
        '[1:a]atempo=0.8,highpass=f=70,loudnorm=I=-16:TP=-1.5:LRA=9,adelay=700|700,apad[a]',
        '-map', '0:v:0', '-map', '[a]', '-c:v', 'copy', '-c:a', 'aac', '-b:a', '128k', '-ar', '48000',
        '-t', '30', '-movflags', '+faststart', str(output)], check=True)
    print(json.dumps({'output': str(output.resolve()), 'seconds': offset, 'bytes': output.stat().st_size}), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--ffmpeg', type=Path, required=True)
    args = parser.parse_args()
    render(args.work, args.output, args.ffmpeg)
