"""Landscape revision with continuous camera motion and projected device depth."""
import argparse
import json
import math
import subprocess
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageOps

from render_zikra_promo_teaser import ROOT, font, load_recording

W, H, FPS, SECONDS = 1280, 720, 30, 15
PINE = '#173c34'

# Time, horizontal/vertical position, scale, yaw, pitch and roll.
FLUID_KEYS = np.array([
    [0, -220, 430, .52, 58, -17, -24],
    [1.05, 345, 350, .73, -24, 7, 7],
    [2.2, 315, 360, .73, 8, -4, -2],
    [4.7, 330, 355, .75, -8, 4, -4],
    [5.25, 500, 307, .68, -42, 9, -13],
    [5.85, 950, 358, .74, 24, -6, 6],
    [6.65, 930, 360, .75, -6, 2, -3],
    [8.65, 940, 350, .74, 9, 6, 4],
    [9.2, 775, 400, .67, 46, -8, 12],
    [9.85, 320, 352, .74, -25, 7, -6],
    [10.8, 330, 360, .73, 7, -3, 2],
    [12.8, 315, 355, .75, -9, 5, -4],
    [15, 325, 360, .73, -19, -1, -7],
])
FLUID_TANGENTS = np.gradient(FLUID_KEYS[:,1:], FLUID_KEYS[:,0], axis=0)


def fluid_pose(t):
    """One C1-continuous spline supplies anticipation, banking and follow-through."""
    t = min(15., max(0., t))
    j = min(len(FLUID_KEYS)-2, max(0, int(np.searchsorted(FLUID_KEYS[:,0],t))-1))
    duration = FLUID_KEYS[j+1,0]-FLUID_KEYS[j,0]
    u = (t-FLUID_KEYS[j,0])/duration
    return ((2*u**3-3*u**2+1)*FLUID_KEYS[j,1:]
            +(u**3-2*u**2+u)*duration*FLUID_TANGENTS[j]
            +(-2*u**3+3*u**2)*FLUID_KEYS[j+1,1:]
            +(u**3-u**2)*duration*FLUID_TANGENTS[j+1])


def smooth(t):
    t = min(1., max(0., t))
    return t*t*t*(10+t*(-15+6*t))


def between(t, a, b):
    return smooth((t-a)/(b-a))


def pose(t):
    enter = between(t, 0, 1.5)
    right = between(t, 4.95, 6.12)
    left = between(t, 8.95, 10.12)
    x = -230+550*enter+625*(right-left)+10*math.sin(t*.85)
    y = 358+5*math.sin(t*.92)-18*math.sin(right*math.pi)+18*math.sin(left*math.pi)
    scale = .727+.014*math.sin(t*.75)-.035*math.sin(right*math.pi)-.035*math.sin(left*math.pi)
    yaw = -12+24*(right-left)+5*math.sin(t*.9)
    pitch = 3*math.sin(t*.7)
    roll = -3.2*math.cos(t*.86)+8*math.sin(right*math.pi)-8*math.sin(left*math.pi)
    return x, y, scale, yaw, pitch, roll


def project(points, scale, yaw, pitch, roll):
    y, p, r = map(math.radians, [yaw,pitch,roll])
    ry = np.array([[math.cos(y),0,math.sin(y)],[0,1,0],[-math.sin(y),0,math.cos(y)]])
    rx = np.array([[1,0,0],[0,math.cos(p),-math.sin(p)],[0,math.sin(p),math.cos(p)]])
    rz = np.array([[math.cos(r),-math.sin(r),0],[math.sin(r),math.cos(r),0],[0,0,1]])
    v = np.array(points) @ (rz @ rx @ ry).T
    return v[:,:2]*(1100/(1100+v[:,2,None]))*scale


def warp(image, corners, size):
    rows, vals = [], []
    for (x,y),(u,v) in zip(corners,[(0,0),(image.width,0),(image.width,image.height),(0,image.height)]):
        rows.extend([[x,y,1,0,0,0,-u*x,-u*y],[0,0,0,x,y,1,-v*x,-v*y]])
        vals.extend([u,v])
    coeff = np.linalg.solve(np.array(rows),np.array(vals))
    return image.transform(size,Image.Transform.PERSPECTIVE,coeff,Image.Resampling.BICUBIC)


def phone_texture(screen):
    texture = Image.new('RGBA',(350,750))
    d = ImageDraw.Draw(texture)
    d.rounded_rectangle((1,1,348,748),43,fill='#101b18',outline='#a0aaa4',width=2)
    d.rounded_rectangle((5,5,344,744),40,outline='#495c52',width=1)
    screen = screen.resize((326,724),Image.Resampling.LANCZOS)
    mask = Image.new('L',screen.size)
    ImageDraw.Draw(mask).rounded_rectangle((0,0,325,723),31,fill=255)
    texture.paste(screen,(12,13),mask)
    d = ImageDraw.Draw(texture)
    d.ellipse((169,20,181,32),fill='#132521',outline='#4d5c55')
    d.line((20,44,20,695),fill=(255,255,255,38),width=1)
    return texture


def device(screen, state, depth=False):
    x,y,scale,yaw,pitch,roll = state
    corners = [(-175,-375,0),(175,-375,0),(175,375,0),(-175,375,0)]
    front = project(corners,scale,yaw,pitch,roll)
    rear = project([(a,b,18 if depth else 9) for a,b,_ in corners],scale,yaw,pitch,roll)
    pts = np.concatenate([front,rear])
    low = np.floor(pts.min(axis=0))-4
    high = np.ceil(pts.max(axis=0))+4
    size = tuple((high-low).astype(int))
    front -= low
    rear -= low
    texture = phone_texture(screen)
    if depth:
        # A restrained moving glass highlight follows the viewing angle.
        highlight = Image.new('RGBA',texture.size)
        hd = ImageDraw.Draw(highlight)
        hx = 175+round(yaw*5)
        for offset in range(-40,41,4):
            hd.line((hx+offset-100,0,hx+offset+100,750),
                    fill=(255,255,255,round(5*(1-abs(offset)/44))),width=7)
        highlight.putalpha(Image.fromarray(np.minimum(
            np.asarray(highlight.getchannel('A')),
            np.asarray(texture.getchannel('A')))))
        texture.alpha_composite(highlight)
    back = Image.new('RGBA',texture.size,'#52665b')
    back.putalpha(texture.getchannel('A'))
    layer = warp(back,rear,size)
    if depth:
        # Rounded perimeter side faces give the device a real projected rim.
        ring=[]
        for cx,cy,a in [(132,-332,-90),(132,332,0),(-132,332,90),(-132,-332,180)]:
            for angle in np.linspace(a,a+90,9):
                angle=math.radians(angle)
                ring.append((cx+43*math.cos(angle),cy+43*math.sin(angle),0))
        near=project(ring,scale,yaw,pitch,roll)-low
        far=project([(a,b,18) for a,b,_ in ring],scale,yaw,pitch,roll)-low
        draw=ImageDraw.Draw(layer)
        for j in range(len(ring)):
            k=(j+1)%len(ring)
            shade=round(62+27*math.cos(j/len(ring)*math.tau+math.radians(yaw)))
            draw.polygon([tuple(near[j]),tuple(near[k]),tuple(far[k]),tuple(far[j])],
                         fill=(shade,shade+10,shade+6,255))
    layer.alpha_composite(warp(texture,front,size))
    return layer,(round(x+low[0]),round(y+low[1]))


def background():
    bg = Image.new('RGBA',(W,H),'#fbfcf9')
    glow = Image.new('RGBA',(W,H))
    d = ImageDraw.Draw(glow)
    d.ellipse((-130,-150,680,680),fill=(187,216,175,100))
    d.ellipse((870,280,1550,1060),fill=(196,225,195,80))
    return Image.alpha_composite(bg,glow.filter(ImageFilter.GaussianBlur(95)))


def caption(lines, x, y, size, t, start, end, gap=1.18, color=PINE, bold=True):
    layer = Image.new('RGBA',(W,H))
    d = ImageDraw.Draw(layer)
    leave = 1-between(t,end-.30,end)
    for i,line in enumerate(lines):
        appear = between(t,start+i*.09,start+i*.09+.45)
        alpha = round(255*appear*leave)
        d.text((x,y+i*size*gap+14*(1-appear)),line,font=font(size,bold),fill=color+f'{alpha:02x}')
    return layer


def render(footage,voice,work,output,ffmpeg,motion='gentle',fps=30):
    if output.exists():raise FileExistsError(output)
    work.mkdir(parents=True,exist_ok=True)
    home = load_recording(ffmpeg,footage/'home.mp4',0,7.7)
    quran = load_recording(ffmpeg,footage/'quran.mp4',4.1,3.6)
    white = Image.open(ROOT/'android/app/src/main/res/drawable-nodpi/zikra_symbol_white.png').convert('RGBA')
    logo = Image.new('RGBA',white.size,PINE)
    logo.putalpha(white.getchannel('A'))
    logo = ImageOps.contain(logo,(35,35),Image.Resampling.LANCZOS)
    bg = background()
    picture = work/'picture.mp4'
    with (work/'render.log').open('wb') as log:
        enc = subprocess.Popen([str(ffmpeg),'-hide_banner','-loglevel','error','-y',
            '-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(fps),'-i','-',
            '-an','-c:v','libx264','-threads','2','-preset','fast','-crf','18','-pix_fmt','yuv420p',
            '-movflags','+faststart',str(picture)],stdin=subprocess.PIPE,stderr=log)
        try:
            for i in range(SECONDS*fps):
                t=i/fps
                frame=bg.copy()
                state=fluid_pose(t) if motion=='fluid' else pose(t)
                # All six pose channels evolve continuously, including at scene swaps.
                if t<5.56:
                    screen=home[min(len(home)-1,int(t*FPS))]
                elif t<9.55:
                    screen=quran[min(len(quran)-1,max(0,int((t-5.56)*FPS)))]
                else:
                    screen=home[min(len(home)-1,int((t-9.55)*FPS))]
                phone,(px,py)=device(screen,state,depth=motion=='fluid')
                shadow=Image.new('RGBA',(W,H))
                sd=ImageDraw.Draw(shadow)
                sd.ellipse((state[0]-142,641,state[0]+157,666),fill=(25,62,38,35))
                frame=Image.alpha_composite(frame,shadow.filter(ImageFilter.GaussianBlur(15)))
                shadow=Image.new('RGBA',(W,H))
                dim=Image.new('RGBA',phone.size,(27,52,35,0))
                dim.putalpha(phone.getchannel('A').point(lambda v:round(v*.12)))
                shadow.alpha_composite(dim,(px+15,py+17))
                frame=Image.alpha_composite(frame,shadow.filter(ImageFilter.GaussianBlur(14)))
                if motion=='fluid':
                    # Sample a half-frame shutter without discrete double silhouettes.
                    previous=fluid_pose(max(0,t-.5/fps))
                    travel=math.hypot(state[0]-previous[0],state[1]-previous[1])
                    if travel>2.5:
                        exposure=None
                        for sample,offset in enumerate([-.25,-1/12,1/12,.25]):
                            trail,(tx,ty)=device(screen,fluid_pose(t+offset/fps),depth=True)
                            instant=frame.copy()
                            instant.alpha_composite(trail,(tx,ty))
                            exposure=instant if exposure is None else Image.blend(exposure,instant,1/(sample+1))
                        frame=exposure
                    else:
                        frame.alpha_composite(phone,(px,py))
                else:
                    frame.alpha_composite(phone,(px,py))
                layers=[
                    caption(['Kenalkan,','Zikra.'],620,240,68,t,.05,1.85,gap=1.05),
                    caption(['Ibadah dan belajar,','dalam satu tempat.'],620,285,43,t,1.8,4.95),
                    caption(['Buka satu aplikasi.','Temukan kebutuhan harianmu.'],622,415,23,t,2.25,4.9,color='#6b7f70',bold=False),
                    caption(['Al-Quran lengkap','dengan terjemahan','dan audio.'],100,260,45,t,6.1,8.9),
                    caption(['Baca. Dengar. Pahami.'],102,447,24,t,6.5,8.85,color='#6b7f70',bold=False),
                    caption(['Jaga rutinitas','ibadahmu.'],620,240,47,t,10.08,15.5),
                    caption(['Hafalan'],622,379,26,t,10.15,15.5),
                    caption(['Arah kiblat'],622,426,26,t,10.75,15.5),
                    caption(['Pengingat salat'],622,473,26,t,11.95,15.5),
                ]
                for layer in layers:frame.alpha_composite(layer)
                frame.alpha_composite(logo,(42,32))
                d=ImageDraw.Draw(frame)
                d.text((90,29),'zikra',font=font(32,serif=True),fill=PINE)
                d.text((1235,45),'PREVIEW  /  15 DETIK',font=font(12,True),fill='#798b7c',anchor='rm')
                d.text((42,685),'REKAMAN APK BETA  ·  DATA CONTOH',font=font(11),fill='#819180')
                if i in [round(s*fps) for s in [1,3.5,5.5,7.5,9.5,13]]:
                    frame.convert('RGB').save(work/f'frame-{i:03}.jpg',quality=93)
                enc.stdin.write(frame.convert('RGB').tobytes())
                if i%(3*fps)==0:print(json.dumps({'renderedSeconds':t}),flush=True)
        finally:
            enc.stdin.close();enc.wait(timeout=90)
        if enc.returncode:raise RuntimeError('Video encoder failed')
    subprocess.run([str(ffmpeg),'-hide_banner','-loglevel','error','-i',str(picture),'-i',str(voice),
        '-map','0:v:0','-map','1:a:0','-c:v','copy','-af','loudnorm=I=-16:TP=-1.5:LRA=9,apad',
        '-c:a','aac','-b:a','160k','-ar','48000','-t',str(SECONDS),'-movflags','+faststart',str(output)],check=True)
    print(json.dumps({'output':str(output),'bytes':output.stat().st_size,'seconds':SECONDS}),flush=True)


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for arg in ['footage','voice','work','output','ffmpeg']:p.add_argument('--'+arg,type=Path,required=True)
    p.add_argument('--motion',choices=['gentle','fluid'],default='gentle')
    p.add_argument('--fps',type=int,choices=[30,60],default=30)
    render(**vars(p.parse_args()))
