"""Sample the approved artwork's actual material grain into repeatable luminance tiles."""
from pathlib import Path
from PIL import Image, ImageOps, ImageEnhance, ImageFilter
root = Path(__file__).resolve().parents[1]
source = Image.open(root/'docs/st-logo-transparent.png').convert('RGB')
# Clean patches: no lettering, seams, or cut edges.
patches = {'leather': (240, 235, 430, 355), 'felt': (100, 100, 230, 230), 'suede': (800, 755, 870, 825)}
for name, box in patches.items():
    patch = source.crop(box).convert('L')
    patch = ImageOps.autocontrast(patch, cutoff=2)
    patch = ImageEnhance.Contrast(patch).enhance(.68)
    # Mirrored edges make the repeat continuous without blurred leather grain.
    w,h=patch.size
    tile=Image.new('L',(w*2,h*2))
    tile.paste(patch,(0,0));tile.paste(ImageOps.mirror(patch),(w,0))
    tile.paste(ImageOps.flip(tile.crop((0,0,w*2,h))),(0,h))
    tile=tile.resize((384,384),Image.Resampling.LANCZOS)
    for folder in [root/'app/src/main/res/drawable-nodpi',root/'preview/textures']:
        folder.mkdir(parents=True,exist_ok=True)
        tile.save(folder/f'texture_{name}.png')
