"""Sample the approved artwork's actual material grain into repeatable luminance tiles."""
from pathlib import Path
from PIL import Image, ImageOps, ImageEnhance, ImageFilter
root = Path(__file__).resolve().parents[1]
source = Image.open(root/'docs/st-logo-transparent.png').convert('RGB')
# Clean patches: no lettering, seams, or cut edges.
patches = {'leather': (490, 245, 710, 345), 'felt': (100, 100, 230, 230), 'suede': (800, 755, 870, 825)}
for name, box in patches.items():
    patch = source.crop(box).convert('L')
    import numpy as np
    from scipy.ndimage import gaussian_filter
    values = np.asarray(patch, dtype=float)
    # Remove baked-in lighting before tinting: retain grain, not mirrored motifs.
    detail = values - gaussian_filter(values, sigma=5)
    h,w=detail.shape
    y,x=np.mgrid[:h,:w]
    fade=np.minimum.reduce([x/8, (w-1-x)/8, y/8, (h-1-y)/8, np.ones_like(x)]).clip(0,1)
    detail=detail*fade
    tile=Image.fromarray(np.uint8(np.clip(128+detail*1.4,0,255)))
    tile=tile.resize((384,384),Image.Resampling.LANCZOS)
    for folder in [root/'app/src/main/res/drawable-nodpi',root/'preview/textures']:
        folder.mkdir(parents=True,exist_ok=True)
        tile.save(folder/f'texture_{name}.png')
