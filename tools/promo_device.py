"""Projected rounded phone with distinct front, rear and metal side faces."""
import math
from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw, ImageOps

from render_zikra_promo_landscape import phone_texture, warp
from render_zikra_promo_teaser import ROOT


def rotation(yaw, pitch, roll):
    y, p, r = map(math.radians, (yaw, pitch, roll))
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0],
                   [-math.sin(y), 0, math.cos(y)]])
    rx = np.array([[1, 0, 0], [0, math.cos(p), -math.sin(p)],
                   [0, math.sin(p), math.cos(p)]])
    rz = np.array([[math.cos(r), -math.sin(r), 0],
                   [math.sin(r), math.cos(r), 0], [0, 0, 1]])
    return rz @ rx @ ry


def project(vertices, transform, scale):
    rotated = np.asarray(vertices) @ transform.T
    return rotated[:, :2] * (1100 / (1100 + rotated[:, 2, None])) * scale


@lru_cache(maxsize=1)
def rear_texture():
    texture = Image.new("RGBA", (350, 750))
    draw = ImageDraw.Draw(texture)
    draw.rounded_rectangle((0, 0, 349, 749), 43, fill="#26352f", outline="#8b9991", width=2)
    draw.rounded_rectangle((14, 17, 115, 165), 26, fill="#34473e", outline="#718478", width=2)
    for y in (55, 119):
        draw.ellipse((30, y-22, 74, y+22), fill="#050c09", outline="#7a8d80", width=3)
        draw.ellipse((43, y-9, 61, y+9), fill="#112c27", outline="#3c6659", width=2)
        draw.ellipse((44, y-8, 49, y-3), fill="#82978a")
    draw.ellipse((84, 64, 99, 79), fill="#c2cdb2")
    with Image.open(ROOT / "android/app/src/main/res/drawable-nodpi/zikra_symbol_white.png") as source:
        mark = ImageOps.contain(source.convert("RGBA"), (72, 72), Image.Resampling.LANCZOS)
    texture.alpha_composite(mark, ((350-mark.width)//2, 327))
    return ImageOps.mirror(texture)


@lru_cache(maxsize=1)
def perimeter():
    points = []
    for x, y, start in [(132, -332, -90), (132, 332, 0),
                        (-132, 332, 90), (-132, -332, 180)]:
        for angle in np.linspace(start, start+90, 13):
            a = math.radians(angle)
            points.append((x+43*math.cos(a), y+43*math.sin(a)))
    return np.asarray(points)


def device(screen, state):
    """Return an RGBA device layer and its top-left position on the canvas.

    State contains center x/y, scale, yaw, pitch and roll in degrees. Screen is
    only visible from the front. Edge-on views render the rim without solving
    a singular perspective transform.
    """
    x, y, scale, yaw, pitch, roll = state
    transform = rotation(yaw, pitch, roll)
    ring = perimeter()
    front_ring = np.column_stack((ring, np.full(len(ring), -9)))
    rear_ring = np.column_stack((ring, np.full(len(ring), 9)))
    front = project(front_ring, transform, scale)
    rear = project(rear_ring, transform, scale)
    low = np.floor(np.concatenate((front, rear)).min(axis=0))-5
    high = np.ceil(np.concatenate((front, rear)).max(axis=0))+5
    layer = Image.new("RGBA", tuple((high-low).astype(int)))
    draw = ImageDraw.Draw(layer)

    depths = ((front_ring + rear_ring) / 2) @ transform.T
    order = sorted(range(len(ring)), key=lambda i: depths[i, 2], reverse=True)
    for i in order:
        j = (i+1) % len(ring)
        shade = round(67+29*math.cos(i/len(ring)*math.tau+math.radians(yaw)))
        draw.polygon([tuple(front[i]-low), tuple(front[j]-low),
                      tuple(rear[j]-low), tuple(rear[i]-low)],
                     fill=(shade, shade+10, shade+5, 255))

    for z, normal, texture in [(-9, -1, None), (9, 1, rear_texture())]:
        center = transform @ np.array([0, 0, z])
        outward = transform @ np.array([0, 0, normal])
        visible = np.dot(outward, np.array([0, 0, -1100])-center) > 0
        if not visible:
            continue
        corners = project([(-175, -375, z), (175, -375, z),
                           (175, 375, z), (-175, 375, z)], transform, scale)-low
        a, b = corners[1]-corners[0], corners[3]-corners[0]
        area = abs(a[0]*b[1]-a[1]*b[0])
        if area > 1:
            texture = phone_texture(screen) if texture is None else texture
            layer.alpha_composite(warp(texture, corners, layer.size))

    # Side buttons stay attached to the metal rim through the turn.
    for top, bottom in [(-190, -135), (-100, -28)]:
        button = project([(175, top, -3), (175, bottom, -3)], transform, scale)-low
        if (transform @ np.array([1, 0, 0]))[2] < -.05:
            ImageDraw.Draw(layer).line([tuple(point) for point in button],
                                      fill="#afbbb2", width=max(1, round(3*scale)))
    return layer, (round(x+low[0]), round(y+low[1]))
