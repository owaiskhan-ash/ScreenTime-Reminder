"""Export transparent selected ST artwork and Android density assets.
Run with Pillow, numpy and scipy installed. Only edge-connected near-white is removed.
"""
from pathlib import Path
from PIL import Image
import numpy as np
from scipy import ndimage

root = Path(__file__).resolve().parents[1]
im = Image.open(root / 'docs/st-logo-concept.png').convert('RGBA')
a = np.array(im)
rgb = a[:, :, :3].astype(float)
# Flood exterior only: the ivory ST lettering remains fully opaque.
near_white = (rgb.min(axis=2) > 210) & (rgb.max(axis=2) - rgb.min(axis=2) < 27)
seed = np.zeros(near_white.shape, dtype=bool)
seed[0, :] = near_white[0, :]; seed[-1, :] = near_white[-1, :]
seed[:, 0] = near_white[:, 0]; seed[:, -1] = near_white[:, -1]
outside = ndimage.binary_propagation(seed, mask=near_white)
a[outside, 3] = 0
im = Image.fromarray(a).resize((1024, 1024), Image.Resampling.LANCZOS)
im.save(root / 'docs/st-logo-transparent.png')
im.save(root / 'docs/icon.png')
im.resize((256, 256), Image.Resampling.LANCZOS).save(root / 'preview/icon.png')
res = root / 'app/src/main/res'
(res / 'drawable-nodpi').mkdir(exist_ok=True)
im.resize((512, 512), Image.Resampling.LANCZOS).save(res / 'drawable-nodpi/st_logo.png')
for density, scale in [('mdpi', 1), ('hdpi', 1.5), ('xhdpi', 2), ('xxhdpi', 3), ('xxxhdpi', 4)]:
    folder = res / f'mipmap-{density}'
    legacy = im.resize((int(48*scale), int(48*scale)), Image.Resampling.LANCZOS)
    legacy.save(folder / 'ic_launcher.png'); legacy.save(folder / 'ic_launcher_round.png')
    # Keep complete artwork inset inside the adaptive-icon mask safe area.
    side = int(108*scale); art = int(60*scale)
    foreground = Image.new('RGBA', (side, side))
    foreground.alpha_composite(im.resize((art, art), Image.Resampling.LANCZOS), ((side-art)//2, (side-art)//2))
    foreground.save(folder / 'ic_launcher_foreground.png')
alpha = np.array(im)[:, :, 3]
assert alpha.min() == 0 and alpha.max() == 255
assert alpha[512, 512] == 255
print('Transparent RGBA logo and all density assets exported; transparent pixels:', (alpha == 0).sum())
