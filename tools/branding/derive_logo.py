"""Deterministic monochrome variants from the user-owned master; no geometry redraw."""
from pathlib import Path
from PIL import Image
import numpy as np
root=Path(__file__).resolve().parents[2]
source=Image.open(root/'design/branding/zikra-logo-primary.jpg').convert('RGB')
pixels=np.asarray(source).astype(float)
r,g,b=pixels[:,:,0],pixels[:,:,1],pixels[:,:,2]
chroma=np.maximum(g-np.maximum(r,b),np.minimum(r-b,g-b))
alpha=np.clip((chroma-10)/30,0,1)
# Master symbol occupies the central area; external white/navy pixels have no chroma.
alpha=(alpha*255).round().astype('uint8')
assert alpha[410:460,550:620].max()>240, 'Master spark must remain present'
folder=root/'android/app/src/main/res/drawable-nodpi'
for name,color in [('pine',(21,51,47)),('white',(255,255,255))]:
    output=Image.new('RGBA',source.size,color+(0,));output.putalpha(Image.fromarray(alpha))
    output.save(folder/f'zikra_symbol_{name}.png',optimize=True)
print('Identical alpha geometry and full 1024x1024 proportions preserved; spark included.')
