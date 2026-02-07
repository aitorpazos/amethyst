#!/usr/bin/env python3
"""
Add XMR badge to Amethyst launcher icons.
Creates modified icons with an orange "XMR" badge in the corner.
"""

from PIL import Image, ImageDraw, ImageFont
import os

# Icon sizes for different densities
ICON_SIZES = {
    'mipmap-mdpi': 48,
    'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96,
    'mipmap-xxhdpi': 144,
    'mipmap-xxxhdpi': 192,
}

# Monero orange color
MONERO_ORANGE = (255, 102, 0)
WHITE = (255, 255, 255)

def add_badge(input_path, output_path, size):
    """Add XMR badge to an icon."""
    img = Image.open(input_path).convert('RGBA')
    
    # Create overlay
    overlay = Image.new('RGBA', img.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    
    # Badge dimensions (bottom-right corner)
    badge_width = int(size * 0.45)
    badge_height = int(size * 0.22)
    badge_x = size - badge_width - int(size * 0.05)
    badge_y = size - badge_height - int(size * 0.05)
    
    # Draw rounded rectangle badge background
    draw.rounded_rectangle(
        [badge_x, badge_y, badge_x + badge_width, badge_y + badge_height],
        radius=int(size * 0.04),
        fill=MONERO_ORANGE + (230,)  # Slightly transparent
    )
    
    # Add "XMR" text
    font_size = max(8, int(badge_height * 0.6))
    font = None
    
    # Try various font paths
    font_paths = [
        "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/freefont/FreeSansBold.ttf",
    ]
    
    for font_path in font_paths:
        try:
            if os.path.exists(font_path):
                font = ImageFont.truetype(font_path, font_size)
                break
        except:
            continue
    
    if font is None:
        # Use default font as fallback
        font = ImageFont.load_default()
        font_size = 10
    
    # Center text in badge
    text = "XMR"
    try:
        bbox = font.getbbox(text)
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]
    except:
        # Estimate for default font
        text_width = len(text) * 6
        text_height = 10
    
    text_x = badge_x + (badge_width - text_width) // 2
    text_y = badge_y + (badge_height - text_height) // 2 - 1
    
    draw.text((text_x, text_y), text, fill=WHITE + (255,), font=font)
    
    # Composite overlay onto original
    result = Image.alpha_composite(img, overlay)
    result.save(output_path, 'PNG')
    print(f"  Created: {output_path}")

def main():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    res_dir = os.path.join(base_dir, 'amethyst', 'src', 'main', 'res')
    
    print("Adding XMR badge to launcher icons...")
    
    for mipmap_dir, size in ICON_SIZES.items():
        mipmap_path = os.path.join(res_dir, mipmap_dir)
        if not os.path.exists(mipmap_path):
            print(f"  Skipping {mipmap_dir} (not found)")
            continue
        
        print(f"\nProcessing {mipmap_dir} ({size}x{size}):")
        
        for icon_name in ['ic_launcher.png', 'ic_launcher_round.png', 'ic_launcher_foreground.png']:
            icon_path = os.path.join(mipmap_path, icon_name)
            if os.path.exists(icon_path):
                add_badge(icon_path, icon_path, size)

if __name__ == '__main__':
    main()
