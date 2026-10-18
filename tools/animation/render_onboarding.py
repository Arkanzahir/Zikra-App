"""Original articulated cartoon character. Run with Blender 4.3+, not a runtime 3D engine.
Example: blender -b -t 2 --python tools/animation/render_onboarding.py -- study /tmp/zikra-art poster
Outputs original RGBA frames; pack_frames.sh encodes a bounded native Canvas sprite sheet.
"""
import bpy, math, sys, os
from mathutils import Vector
args=sys.argv[sys.argv.index('--')+1:]
SCENE=args[0] if args else 'study'
OUT=args[1] if len(args)>1 else '/tmp/zikra-onboarding'
MODE=args[2] if len(args)>2 else 'poster'
os.makedirs(OUT,exist_ok=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
S=bpy.context.scene
S.render.engine='CYCLES';S.cycles.samples=128 if MODE=='poster' else 48
S.cycles.use_denoising=False;S.render.threads_mode='FIXED';S.render.threads=2
S.render.use_persistent_data=True
S.render.resolution_x=640 if MODE=='poster' else 320
S.render.resolution_y=480 if MODE=='poster' else 240
S.render.resolution_percentage=100;S.render.film_transparent=True
S.render.image_settings.file_format='PNG';S.render.image_settings.color_mode='RGBA'
S.render.fps=24;S.frame_start=1;S.frame_end=48
S.view_settings.view_transform='Standard';S.view_settings.look='None'
S.world.color=(.65,.65,.65)
def linear(x):return x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4
def color(h):return tuple(linear(int(h[i:i+2],16)/255) for i in (0,2,4))
def mat(name,h,rough=.65,metal=0):
 m=bpy.data.materials.new(name);m.diffuse_color=(*color(h),1);m.use_nodes=True
 bs=m.node_tree.nodes.get('Principled BSDF');bs.inputs['Base Color'].default_value=(*color(h),1);bs.inputs['Roughness'].default_value=rough;bs.inputs['Metallic'].default_value=metal
 return m
skin=mat('Warm brown skin','CA9475');pine=mat('Pine overshirt','386A59');cream=mat('Ivory cotton','F2E8D2');camel=mat('Sand trousers','CBAA79');hair=mat('Dark soft hair','283435');white=mat('Eyes and soles','F8F2E3');ink=mat('Pupils','293831');wood=mat('Walnut','A7764B');woodLight=mat('Oak','CCA77B');paper=mat('Warm paper','FBF1D8');gold=mat('Quiet brass','C49A57',.42,.2);bookPine=mat('Book cloth','2E6354');rug=mat('Woven sage','8BAE9A');shoe=mat('Pine shoes','405B50');lineMat=mat('Notebook graphite','9E9B8C')
def smooth(o):
 if o.type=='MESH':
  for f in o.data.polygons:f.use_smooth=True
 return o
def uv(name,loc,scale,material,parent=None):
 bpy.ops.mesh.primitive_uv_sphere_add(segments=24,ring_count=16,location=loc)
 o=bpy.context.object;o.name=name;o.scale=scale;o.data.materials.append(material);smooth(o)
 if parent:o.parent=parent;o.location=loc
 return o
def cube(name,loc,scale,material,bevel=.04,parent=None):
 bpy.ops.mesh.primitive_cube_add(size=1,location=loc);o=bpy.context.object;o.name=name;o.scale=scale
 bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
 if bevel:
  mod=o.modifiers.new('Soft manufactured edges','BEVEL');mod.width=bevel;mod.segments=3
  o.modifiers.new('Weighted normals','WEIGHTED_NORMAL')
 o.data.materials.append(material)
 if parent:o.parent=parent;o.location=loc
 return o
def empty(name,loc=(0,0,0),parent=None):
 o=bpy.data.objects.new(name,None);bpy.context.collection.objects.link(o);o.location=loc
 if parent:o.parent=parent
 return o
def curve(name,points,material,r=.009,parent=None):
 cu=bpy.data.curves.new(name,'CURVE');cu.dimensions='3D';cu.bevel_depth=r;cu.bevel_resolution=3
 sp=cu.splines.new('BEZIER');sp.bezier_points.add(len(points)-1)
 for b,p in zip(sp.bezier_points,points):b.co=p;b.handle_left_type='AUTO';b.handle_right_type='AUTO'
 o=bpy.data.objects.new(name,cu);bpy.context.collection.objects.link(o);o.data.materials.append(material)
 if parent:o.parent=parent
 return o
def beam(name,a,b,r,material):
 d=Vector(b)-Vector(a);bpy.ops.mesh.primitive_cylinder_add(vertices=20,radius=r,depth=1,location=(Vector(a)+Vector(b))/2)
 o=bpy.context.object;o.name=name;o.scale.z=d.length;o.rotation_euler=d.to_track_quat('Z','Y').to_euler();o.data.materials.append(material);smooth(o);return o
def set_beam(o,a,b,frame):
 d=Vector(b)-Vector(a);o.location=(Vector(a)+Vector(b))/2;o.rotation_euler=d.to_track_quat('Z','Y').to_euler();o.scale.z=d.length
 for key in ('location','rotation_euler','scale'):o.keyframe_insert(data_path=key,frame=frame)
def lamp(name,loc,energy,size):
 data=bpy.data.lights.new(name,'AREA');data.energy=energy;data.shape='DISK';data.size=size
 o=bpy.data.objects.new(name,data);bpy.context.collection.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,1))-o.location).to_track_quat('-Z','Y').to_euler()
lamp('Large soft window',(-3,-4,5),430,4);lamp('Cream fill',(4,-1,3),210,3);lamp('Soft rim',(1,4,4),360,3)
# Camera and coherent 3:2 composition. All geometry is original; no user logo is regenerated.
cam=bpy.data.cameras.new('Camera');camera=bpy.data.objects.new('Camera',cam);bpy.context.collection.objects.link(camera)
camera.location=(3.6,-7,3.3);target=Vector((0,-.02,.68 if SCENE=='quran' else .92));camera.rotation_euler=(target-camera.location).to_track_quat('-Z','Y').to_euler();cam.type='ORTHO';cam.ortho_scale=2.5 if SCENE=='quran' else 2.85;S.camera=camera
# Body proportions deliberately read as a young adult, rather than a baby/chibi mascot.
seated=SCENE in ('study','quran')
pelvis=.58 if SCENE=='study' else .31 if SCENE=='quran' else .9
torsoBottom=pelvis+.04;headZ=torsoBottom+.68
uv('Trouser seat',(0,.10,pelvis),(.23,.18,.17),camel)
uv('Overshirt torso',(0,.08,torsoBottom+.25),(.255,.17,.34),pine)
cube('Cream tee inset',(0,-.087,torsoBottom+.30),(.22,.025,.36),cream,.065)
for side in (-1,1):
 lapel=cube('Soft shirt placket',(side*.133,-.103,torsoBottom+.29),(.055,.03,.43),pine,.017);lapel.rotation_euler.y=side*-.07
 collar=cube('Overshirt collar',(side*.10,-.079,torsoBottom+.51),(.105,.04,.075),pine,.025);collar.rotation_euler.y=side*.28
for z in (.13,.28,.43):uv('Small shirt button',(.13,-.127,torsoBottom+z),(.011,.009,.011),woodLight)
uv('Neck',(0,.08,headZ-.20),(.079,.075,.16),skin)
head=empty('Head nod rig',(0,.065,headZ))
uv('Face',(0,0,0),(.205,.175,.245),skin,head)
for side in (-1,1):
 uv('Ear',(side*.202,.005,-.015),(.039,.036,.064),skin,head)
 uv('Eye white',(side*.074,-.161,.028),(.037,.021,.046),white,head)
 pupil=uv('Pupil',(side*.074,-.180,.023),(.018,.012,.025),ink,head)
 uv('Catchlight',(side*.074-.006,-.191,.033),(.005,.004,.007),white,head)
 curve('Eyebrow',[(side*.108,-.165,.097),(side*.074,-.179,.110),(side*.040,-.17,.103)],hair,.010,head)
uv('Nose',(0,-.183,-.028),(.030,.039,.040),skin,head)
curve('Small relaxed smile',[(-.049,-.157,-.112),(0,-.171,-.124),(.049,-.157,-.112)],mat('Smile','895E4B'),.006,head)
uv('Hair cap',(0,.025,.155),(.211,.169,.132),hair,head)
for x,z,rz in [(-.13,.18,.4),(-.05,.209,.25),(.035,.219,0),(.12,.21,-.3),(.17,.16,-.45)]:
 tuft=uv('Swept hair tuft',(x,-.079,z),(.067,.115,.067),hair,head);tuft.rotation_euler.y=rz
for side in (-1,1):uv('Sideburn',(side*.177,-.015,.075),(.035,.085,.10),hair,head)
# Legs have distinct seated/cross-legged/standing poses, with rounded articulated joints.
if SCENE=='study':
 legs=[((-.13,.06,pelvis),(-.16,-.30,.51),(-.16,-.35,.16)),((.13,.06,pelvis),(.18,-.29,.51),(.19,-.36,.16))]
elif SCENE=='quran':
 legs=[((-.12,.08,pelvis),(-.37,-.16,.19),(.17,-.39,.12)),((.12,.08,pelvis),(.37,-.16,.19),(-.17,-.38,.115))]
else:legs=[((-.12,.08,pelvis),(-.13,.06,.49),(-.14,-.01,.11)),((.12,.08,pelvis),(.13,.06,.49),(.14,-.01,.11))]
for a,b,c in legs:
 beam('Trouser upper leg',a,b,.118,camel);uv('Rounded trouser knee',b,(.12,.12,.12),camel)
 beam('Trouser lower leg',b,c,.083,camel);uv('Shoe' if SCENE=='study' else 'Clean ivory sock',(c[0],c[1]-.07,.082),(.10,.18,.08),shoe if SCENE=='study' else cream)
 uv('Ivory sole',(c[0],c[1]-.07,.035),(.103,.182,.025),white)
# Arms animate from inverse-kinematic wrist targets. Hands are real geometry with five digits.
shoulders=[Vector((-.245,.07,torsoBottom+.43)),Vector((.245,.07,torsoBottom+.43))]
arms=[]
for side,shoulder in zip((-1,1),shoulders):
 upper=beam('Sleeve upper arm',shoulder,shoulder+Vector((side*.05,0,-.2)),.087,pine)
 elbow=uv('Elbow',shoulder+Vector((side*.05,0,-.2)),(.083,.083,.083),pine)
 fore=beam('Sleeve forearm',shoulder,shoulder+Vector((0,-.1,-.15)),.070,pine)
 hand=empty('Hand wrist rig')
 uv('Palm',(0,-.020,0),(.063,.075,.033),skin,hand)
 for i in range(4):
  xx=-.042+i*.027
  finger=uv('Finger',(xx,-.091,.001),(.013,.047-(.008 if i in (0,3) else 0),.014),skin,hand)
 uv('Thumb',(side*.069,-.027,-.001),(.022,.045,.019),skin,hand).rotation_euler.z=side*.6
 arms.append((side,shoulder,upper,elbow,fore,hand))
def arm_pose(index,wrist,frame,angle=0):
 side,shoulder,upper,elbow,fore,hand=arms[index];wrist=Vector(wrist);d=wrist-shoulder
 L=.265;M=.26;distance=min(L+M-.001,max(.06,d.length));axis=d.normalized()
 base=(L*L-M*M+distance*distance)/(2*distance)
 normal=Vector((side*.8,.45,-.25));normal=(normal-axis*normal.dot(axis)).normalized()
 joint=shoulder+axis*base+normal*math.sqrt(max(.001,L*L-base*base))
 set_beam(upper,shoulder,joint,frame);set_beam(fore,joint,wrist,frame)
 elbow.location=joint;elbow.keyframe_insert(data_path='location',frame=frame)
 hand.location=wrist;hand.rotation_euler=(.12,angle,side*-.08)
 hand.keyframe_insert(data_path='location',frame=frame);hand.keyframe_insert(data_path='rotation_euler',frame=frame)
# Props and physical book hinge.
bookHinge=None
if SCENE=='study':
 cube('Study desk',(0,-.32,.81),(1.12,.64,.065),woodLight,.045)
 for x in (-.46,.46):
  for y in (-.56,-.08):beam('Tapered desk leg',(x,y,.035),(x*.97,y,.78),.036,wood)
 cube('Chair seat',(0,.13,.48),(.49,.44,.07),woodLight,.045)
 cube('Chair back',(0,.34,.84),(.47,.06,.52),woodLight,.045)
 for x in (-.19,.19):
  for y in (-.01,.30):beam('Chair leg',(x,y,.03),(x,y,.46),.026,wood)
 for z,ma in [(.85,bookPine),(.90,cream)]:cube('Stacked textbook',(-.34,-.28,z),(.22,.31,.045),ma,.012)
 beam('Pencil',(.28,-.49,.857),(.47,-.32,.857),.009,gold)
 # Small desk lamp reads as studying, without random floating decorations.
 uv('Lamp foot',(.39,-.05,.855),(.10,.075,.017),bookPine)
 beam('Lamp stem',(.39,-.05,.87),(.39,-.05,1.12),.012,gold)
 lampShade=uv('Lamp shade',(.36,-.07,1.16),(.108,.08,.05),bookPine)
 origin=(-.075,-.44,.854);bw=.28;bh=.27
 cube('Book back',(origin[0]+bw/2,origin[1],origin[2]),(bw,bh,.017),bookPine,.008)
 cube('Right page block',(origin[0]+bw/2,origin[1],origin[2]+.020),(bw-.013,bh-.018,.027),paper,.006)
 bookHinge=empty('Book cover hinge',origin)
 cube('Opening front cover',(bw/2,0,.041),(bw,bh,.017),bookPine,.009,bookHinge)
 # Three paper leaves follow the cover to create a physical opening, not a translated image.
 leaves=[]
 for j in range(3):
  leaf=empty('Paper leaf hinge',origin)
  cube('Opening paper leaf',(bw/2,0,.034-j*.004),(bw-.018,bh-.023,.002),paper,.001,leaf);leaves.append(leaf)
 # Faint ruled notebook strokes are academic marks, not invented Quran lettering.
 for y in (-.51,-.47,-.43,-.39):beam('Notebook rule',(.0,y,.889),(.17,y,.889),.0017,lineMat)
 for f,a in [(1,0),(9,0),(18,.35),(32,2.78),(42,3.04),(48,3.04)]:
  bookHinge.rotation_euler.y=-a;bookHinge.keyframe_insert(data_path='rotation_euler',frame=f)
  for j,leaf in enumerate(leaves):leaf.rotation_euler.y=-max(0,a-.18-j*.09);leaf.keyframe_insert(data_path='rotation_euler',frame=f)
  left=(-.27,-.25,.91)
  right=(origin[0]+bw*math.cos(a),origin[1]-.04,origin[2]+bw*math.sin(a)+.065)
  if f<=9:right=(.32,-.16,.86)
  if f>=42:right=(.28,-.35,.92)
  arm_pose(0,left,f);arm_pose(1,right,f,angle=-a*.45)
elif SCENE=='prayer':
 cube('Prayer rug',(0,-.15,.015),(.85,1.25,.025),rug,.065)
 # Woven border and arch, geometry rather than religious text.
 curve('Rug border',[(-.35,-.70,.031),(-.35,.39,.031),(.35,.39,.031),(.35,-.70,.031),(-.35,-.70,.031)],cream,.008)
 curve('Mihrab motif',[(-.22,-.35,.034),(-.22,-.52,.034),(0,-.64,.034),(.22,-.52,.034),(.22,-.35,.034)],cream,.010)
 for x in [i*.035-.35 for i in range(21)]:curve('Rug fringe',[(x,-.77,.015),(x,-.81,.012)],cream,.005)
 for f in (1,10,20,30,39,48):
  if f==1:wrists=[(-.29,.02,.96),(.29,.02,.96)]
  elif f in (10,20):wrists=[(-.31,-.12,1.46),(.31,-.12,1.46)]
  else:wrists=[(.06,-.16,1.17),(-.045,-.22,1.19)]
  for i,w in enumerate(wrists):
   arm_pose(i,w,f,angle=(.15 if f in (10,20) else .1))
   if f in (10,20):
    arms[i][-1].rotation_euler.x=-1.4
    arms[i][-1].keyframe_insert(data_path='rotation_euler',frame=f)
else:
 uv('Sitting cushion',(0,.09,.10),(.38,.30,.10),rug)
 # Sloping crossed walnut rehal supports the open book.
 for side in (-1,1):
  plank=cube('Rehal crossed support',(side*.05,-.47,.25),(.12,.38,.48),wood,.022);plank.rotation_euler.y=side*.53
 origin=(0,-.47,.45);bookHinge=empty('Quran open spine',origin)
 for side in (-1,1):
  cover=cube('Quran cloth cover',(side*.145,0,-.012),(.29,.35,.025),bookPine,.012,bookHinge);cover.rotation_euler.y=side*.20
  pages=cube('Quran ivory pages',(side*.145,0,.012),(.272,.329,.035),paper,.010,bookHinge);pages.rotation_euler.y=side*.20
  # Gold edging only: no fabricated Arabic verses.
  for yy in (-.145,.145):
   curve('Subtle gold page border',[(side*.025,yy,.049),(side*.26,yy,.006)],gold,.002,bookHinge)
 leaf=empty('Turning page',origin)
 cube('Single turning Quran page',(.136,0,.045),(.265,.322,.002),paper,.001,leaf)
 for f,a in [(1,0),(10,0),(21,.8),(32,2.5),(40,3.12),(48,3.12)]:
  leaf.rotation_euler.y=-a;leaf.keyframe_insert(data_path='rotation_euler',frame=f)
  arm_pose(0,(-.30,-.15,.45),f)
  w=(.27*math.cos(a),-.51,.50+.27*math.sin(a)) if f in (10,21,32) else (.30,-.16,.44)
  arm_pose(1,w,f,angle=-a*.25)
# Gentle anticipation and reading nod. Real poses stop at frame 48.
for f,a in [(1,-.04),(10,.03),(28,.17),(40,.13),(48,.13)]:
 head.rotation_euler.x=a;head.keyframe_insert(data_path='rotation_euler',frame=f)
# Curves interpolate smoothly between a small number of poses.
for obj in bpy.data.objects:
 if obj.animation_data and obj.animation_data.action:
  for fc in obj.animation_data.action.fcurves:
   for k in fc.keyframe_points:k.interpolation='BEZIER'
bpy.ops.wm.save_as_mainfile(filepath=os.path.join(OUT,SCENE+'.blend'))
if MODE=='poster':
 for f,n in [(1,'start'),(48,'end')]:
  S.frame_set(f);S.render.filepath=os.path.join(OUT,SCENE+'-'+n+'.png');bpy.ops.render.render(write_still=True)
else:
 S.render.filepath=os.path.join(OUT,SCENE+'-');bpy.ops.render.render(animation=True)
print('ZIKRA_RENDER_COMPLETE',SCENE,MODE)
