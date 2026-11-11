"""Render the 15-second reference-style promo using local beta footage."""
from pathlib import Path
import argparse
import json
import math
import subprocess
from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[1]
W, H, FPS, DURATION = 720, 1280, 30, 15
PINE = '#173c34'
INK = '#18352e'


@lru_cache(maxsize=24)
def font(size, bold=False, serif=False):
    name = 'georgia.ttf' if serif else 'seguisb.ttf' if bold else 'segoeui.ttf'
    return ImageFont.truetype('C:/Windows/Fonts/' + name, size)


def ease(x):
    x = max(0., min(1., x))
    return 1 - (1-x)**3


def backdrop():
    img = Image.new('RGBA', (W, H), '#fafbf7')
    glow = Image.new('RGBA', (W, H))
    d = ImageDraw.Draw(glow)
    d.ellipse((-280, 440, 590, 1270), fill=(175, 210, 174, 100))
    d.ellipse((280, -230, 1050, 560), fill=(210, 231, 208, 115))
    return Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(100)))


def text_layer(lines, x, y, size, at, start, color=INK, bold=True, gap=1.12):
    layer = Image.new('RGBA', (W, H))
    d = ImageDraw.Draw(layer)
    for i, line in enumerate(lines):
        p = ease((at-start-i*.10)/.48)
        d.text((x, y+i*size*gap+18*(1-p)), line, font=font(size, bold),
               fill=color+(f'{round(255*p):02x}'))
    return layer


def device(screen, tilt):
    surface = Image.new('RGBA', (350, 770))
    d = ImageDraw.Draw(surface)
    d.rounded_rectangle((2, 2, 347, 767), 43, fill='#0d1615', outline='#8d9a94', width=2)
    d.rounded_rectangle((6, 5, 343, 763), 40, outline='#43514b', width=1)
    screen = ImageOps.fit(screen, (326, 744), Image.Resampling.LANCZOS)
    mask = Image.new('L', screen.size)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, 325, 743), 31, fill=255)
    surface.paste(screen, (12, 13), mask)
    d = ImageDraw.Draw(surface)
    d.rounded_rectangle((144, 20, 207, 35), 8, fill='#101919')
    d.ellipse((193, 24, 199, 30), fill='#34403e')
    # Project the flat screen onto a slight trapezoid, preserving the recording.
    skew = tilt*7
    dst = [(14+skew, 13), (335, 5+skew), (341-skew, 760), (9, 752-skew)]
    src = [(0, 0), (350, 0), (350, 770), (0, 770)]
    rows, vals = [], []
    for (x, y), (u, v) in zip(dst, src):
        rows += [[x,y,1,0,0,0,-u*x,-u*y], [0,0,0,x,y,1,-v*x,-v*y]]
        vals += [u,v]
    coeff = np.linalg.solve(np.array(rows), np.array(vals))
    projected = surface.transform(surface.size, Image.Transform.PERSPECTIVE,
                                  coeff, Image.Resampling.BICUBIC)
    return projected.rotate(tilt, Image.Resampling.BICUBIC, expand=True)


def stamp_logo(frame, symbol, x, y, size=39, word=True):
    icon = ImageOps.contain(symbol, (size, size), Image.Resampling.LANCZOS)
    frame.alpha_composite(icon, (x, y))
    if word:
        ImageDraw.Draw(frame).text((x+size+12, y-3), 'zikra', font=font(size, serif=True), fill=PINE)


def load_recording(ffmpeg, file, start, seconds):
    result = subprocess.run([str(ffmpeg), '-hide_banner', '-loglevel', 'error',
        '-ss', str(start), '-i', str(file), '-t', str(seconds), '-vf',
        'fps=30,scale=326:724', '-pix_fmt', 'rgb24', '-threads', '1',
        '-f', 'rawvideo', '-'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
    size = 326*724*3
    if len(result.stdout) < size:
        raise RuntimeError('Empty recording: '+str(file))
    return [Image.frombytes('RGB', (326,724), result.stdout[i:i+size])
            for i in range(0,len(result.stdout)-size+1,size)]


def render(footage, voice, work, output, ffmpeg):
    if output.exists():
        raise FileExistsError(output)
    work.mkdir(parents=True, exist_ok=True)
    home = load_recording(ffmpeg, footage/'home.mp4', 0, 7.7)
    # The first part of this take is a settings sheet; use the verified reader.
    quran = load_recording(ffmpeg, footage/'quran.mp4', 4.1, 3.6)
    master = Image.open(ROOT/'android/app/src/main/res/drawable-nodpi/zikra_symbol_white.png').convert('RGBA')
    symbol = Image.new('RGBA', master.size, PINE)
    symbol.putalpha(master.getchannel('A'))
    bg = backdrop()
    picture = work/'picture.mp4'
    with (work/'render.log').open('wb') as log:
        encoder = subprocess.Popen([str(ffmpeg), '-hide_banner', '-loglevel', 'error', '-y',
            '-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(FPS),'-i','-',
            '-an','-c:v','libx264','-threads','2','-preset','fast','-crf','18',
            '-pix_fmt','yuv420p','-movflags','+faststart',str(picture)],
            stdin=subprocess.PIPE,stderr=log)
        try:
            for i in range(DURATION*FPS):
                t=i/FPS
                frame=bg.copy()
                d=ImageDraw.Draw(frame)
                stamp_logo(frame,symbol,48,47)
                d.text((670,64),'PREVIEW',font=font(13,True),fill='#718277',anchor='rm')
                # Intro keeps a strong product title before the first screen enters.
                if t < 1.65:
                    frame.alpha_composite(text_layer(['Kenalkan,'],48,228,54,t,0.05))
                    frame.alpha_composite(text_layer(['Zikra.'],44,290,108,t,0.35))
                    frame.alpha_composite(text_layer(['Ibadah dan belajar.'],50,440,27,t,.7,bold=False))
                elif t < 5.7:
                    frame.alpha_composite(text_layer(['Ibadah dan belajar,','dalam satu tempat.'],48,177,47,t,1.65))
                    frame.alpha_composite(text_layer(['Lebih dekat dengan keseharianmu.'],50,300,23,t,2.1,bold=False,color='#63776c'))
                elif t < 9.12:
                    frame.alpha_composite(text_layer(['Al-Quran,','teman setiap hari.'],48,168,48,t,5.7))
                    frame.alpha_composite(text_layer(['Baca.','Dengar.','Pahami.'],440,582,31,t,6.0,gap=1.65))
                    frame.alpha_composite(text_layer(['Terjemahan dan audio'],50,290,23,t,6.1,bold=False,color='#63776c'))
                else:
                    frame.alpha_composite(text_layer(['Jaga rutinitas','ibadahmu.'],48,168,50,t,9.12))
                    for j,(start,label) in enumerate([(9.25,'Hafalan'),(10.8,'Arah kiblat'),(12.0,'Pengingat salat')]):
                        p=ease((t-start)/.5)
                        layer=Image.new('RGBA',(W,H))
                        ld=ImageDraw.Draw(layer)
                        yy=548+j*83+round(15*(1-p))
                        ld.ellipse((49,yy+8,57,yy+16),fill=(50,105,73,round(p*255)))
                        ld.text((75,yy),label,font=font(25,True),fill=(24,53,46,round(p*255)))
                        frame.alpha_composite(layer)
                if t >= .85:
                    if t < 5.4:
                        screen=home[min(len(home)-1,max(0,int((t-1.1)*FPS)))]
                        x,y=185,401+round(390*(1-ease((t-.85)/.95)))
                        angle=-2.2+1.3*ease((t-1)/4)
                    elif t < 9.12:
                        screen=quran[min(len(quran)-1,max(0,int((t-5.7)*FPS)))]
                        slide=ease((t-5.4)/.55)
                        x,y=round(185-134*slide),398
                        angle=-2.0+math.sin(t)*.5
                    else:
                        screen=home[min(len(home)-1,int((t-9.12)*FPS))]
                        x,y=round(51+282*ease((t-9.12)/.7)),400
                        angle=2.5-math.sin(t)*.45
                    phone=device(screen,angle)
                    # A soft floor shadow and a tight body shadow anchor the device.
                    shadow=Image.new('RGBA',(W,H))
                    sd=ImageDraw.Draw(shadow)
                    sd.ellipse((x+4,y+phone.height-7,x+phone.width+40,y+phone.height+29),fill=(24,57,39,40))
                    frame=Image.alpha_composite(frame,shadow.filter(ImageFilter.GaussianBlur(20)))
                    body=Image.new('RGBA',(W,H))
                    shade=Image.new('RGBA',phone.size,(20,48,33,0))
                    shade.putalpha(phone.getchannel('A').point(lambda v:round(v*.17)))
                    body.alpha_composite(shade,(x+11,y+18))
                    frame=Image.alpha_composite(frame,body.filter(ImageFilter.GaussianBlur(13)))
                    frame.alpha_composite(phone,(x,y+round(3*math.sin(t*1.1))))
                d=ImageDraw.Draw(frame)
                d.text((48,1230),'CUPLIKAN KONSEP  /  REKAMAN APK BETA',font=font(12),fill='#74877a')
                d.rounded_rectangle((48,1199,672,1202),2,fill='#dfe8dc')
                d.rounded_rectangle((48,1199,48+max(2,624*t/DURATION),1202),2,fill='#407557')
                if i in [15,105,225,390]:
                    frame.convert('RGB').save(work/f'frame-{i:03}.jpg',quality=92)
                encoder.stdin.write(frame.convert('RGB').tobytes())
                if i%90==0:print(json.dumps({'renderedSeconds':t}),flush=True)
        finally:
            encoder.stdin.close()
            encoder.wait(timeout=90)
        if encoder.returncode:raise RuntimeError('Encoder failed')
    subprocess.run([str(ffmpeg),'-hide_banner','-loglevel','error','-i',str(picture),
        '-i',str(voice),'-map','0:v:0','-map','1:a:0','-c:v','copy',
        '-af','loudnorm=I=-16:TP=-1.5:LRA=9,apad','-c:a','aac','-b:a','160k',
        '-ar','48000','-t',str(DURATION),'-movflags','+faststart',str(output)],check=True)
    print(json.dumps({'output':str(output),'bytes':output.stat().st_size,'seconds':DURATION}))


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['footage','voice','work','output','ffmpeg']:
        p.add_argument('--'+name,type=Path,required=True)
    args=p.parse_args()
    render(**vars(args))
