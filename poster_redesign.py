from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from reportlab.lib.pagesizes import inch
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen import canvas


W, H = 2592, 7776
M = 86

BG = "#F7F2E8"
GREEN = "#1D5138"
GREEN_2 = "#2E6B4B"
GREEN_3 = "#D9F0D9"
GREEN_4 = "#9AD7B4"
TEXT = "#26352F"
MUTED = "#5E6A62"
GOLD = "#D6B441"
SALMON = "#D9846A"
TEAL = "#4E9A74"
BROWN = "#9A6B42"
NAVY = "#15386B"
OUTLINE = "#C98870"
GRID = "#D8D4C9"

ROOT = Path(r"C:\Users\Owner\AndroidStudioProjects\PlantDiseaseIdentifier2")
OUT_PNG = ROOT / "agriverse_right_12x36_redesign.png"
OUT_PDF = ROOT / "agriverse_right_12x36_redesign.pdf"


FONT_DIR = Path(r"C:\Windows\Fonts")
FONT_BOLD = str(FONT_DIR / "arialbd.ttf")
FONT_REG = str(FONT_DIR / "arial.ttf")
FONT_ITALIC = str(FONT_DIR / "ariali.ttf")


def font(size: int, *, bold: bool = False, italic: bool = False) -> ImageFont.FreeTypeFont:
    path = FONT_REG
    if bold:
        path = FONT_BOLD
    if italic:
        path = FONT_ITALIC
    return ImageFont.truetype(path, size=size)


H1 = font(54, bold=True)
SECTION = font(34, bold=True)
SUB = font(23)
BODY = font(22)
BODY_BOLD = font(22, bold=True)
SMALL = font(18)
SMALL_BOLD = font(18, bold=True)
ITALIC = font(20, italic=True)
IMPACT_BIG = font(54, bold=True)
IMPACT_MID = font(20, bold=True)
IMPACT_BODY = font(18)
CONCLUSION = font(22)
REFERENCE = font(20)
FOOTER = font(15, bold=True)


img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)


def rr(x1, y1, x2, y2, *, fill, outline=None, width=2, radius=26):
    d.rounded_rectangle([x1, y1, x2, y2], radius=radius, fill=fill, outline=outline, width=width)


def write(x, y, s, fnt, *, fill=TEXT, anchor="la"):
    d.text((x, y), s, font=fnt, fill=fill, anchor=anchor)


def wrap_text(x, y, width, text, fnt, *, fill=TEXT, line_gap=8):
    words = text.split()
    lines = []
    current = ""
    for word in words:
        candidate = word if not current else f"{current} {word}"
        if d.textlength(candidate, font=fnt) <= width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    yy = y
    line_h = fnt.size + line_gap
    for line in lines:
        d.text((x, yy), line, font=fnt, fill=fill)
        yy += line_h
    return yy


def section_header(y, label):
    rr(M, y, W - M, y + 110, fill=GREEN, radius=28)
    write(M + 36, y + 54, label, SECTION, fill="#F4F0E7", anchor="lm")


def draw_header():
    rr(0, 0, W, 165, fill=GREEN, radius=0)
    for i in range(12):
        cx = 105 + i * 185
        cy = 78
        d.ellipse([cx - 18, cy - 8, cx + 18, cy + 8], fill="#3D7F5A")
        d.line([cx - 5, cy + 8, cx + 9, cy - 7], fill="#2C6A49", width=3)
    write(W // 2, 84, "DISCUSSION & IMPACT", H1, fill="#F4F0E7", anchor="ma")


def draw_bar_chart(x, y, w, h):
    rr(x, y, x + w, y + h, fill="#FFFDFC", radius=18)
    left = x + 96
    right = x + w - 38
    top = y + 30
    bottom = y + h - 170

    for tick in range(0, 101, 20):
        yy = bottom - (tick / 100) * (bottom - top)
        d.line([left, yy, right, yy], fill=GRID, width=2)
        label = str(tick)
        tw = d.textlength(label, font=SMALL)
        write(left - tw - 18, yy - 10, label, SMALL, fill=MUTED)

    d.line([left, top, left, bottom], fill=MUTED, width=3)
    d.line([left, bottom, right, bottom], fill=MUTED, width=3)

    try:
        axis_label = Image.new("RGBA", (80, 380), (0, 0, 0, 0))
        axis_draw = ImageDraw.Draw(axis_label)
        axis_draw.text((18, 160), "Test accuracy (%)", font=BODY, fill=MUTED)
        axis_label = axis_label.rotate(90, expand=True)
        img.paste(axis_label, (x - 10, y + 150), axis_label)
    except Exception:
        pass

    values = [46, 72, 89, 93.8, 95.51]
    labels = [
        "Naive CNN\n(no transfer)",
        "+ Dropout\n(0.5 / 0.3)",
        "+ Transfer\n(MobileNetV2)",
        "+ Augment\n+ tuning",
        "Final\n(TFLite)",
    ]
    fills = ["#E96C4A", BROWN, "#499A70", "#397959", GREEN]
    outlines = [None, None, None, None, GOLD]
    bar_w = 270
    gap = (right - left - len(values) * bar_w) // (len(values) + 1)
    bx = left + gap
    for idx, value in enumerate(values):
        bh = (value / 100) * (bottom - top)
        by = bottom - bh
        d.rectangle([bx, by, bx + bar_w, bottom], fill=fills[idx], outline=outlines[idx] or "#355744", width=5 if outlines[idx] else 2)
        pct = f"{value:.2f}%" if idx == 4 else (f"{value:.1f}%" if value % 1 else f"{int(value)}%")
        write(bx + bar_w // 2, by - 24, pct, BODY_BOLD, fill=GREEN, anchor="ma")
        label_y = bottom + 18
        for line in labels[idx].split("\n"):
            write(bx + bar_w // 2, label_y, line, BODY, fill=TEXT, anchor="ma")
            label_y += 30
        bx += bar_w + gap


draw_header()

section_header(205, "ABLATION STUDY")
wrap_text(
    M + 22,
    350,
    W - 2 * M - 44,
    "We iteratively added techniques to the plant model. Each step was evaluated on the same PlantVillage test split to show how disciplined model improvements translated into deployable accuracy gains.",
    SUB,
    line_gap=10,
)
draw_bar_chart(M + 10, 500, W - 2 * M - 20, 1180)
write(W // 2, 1740, "+ 49.5 percentage points from v1 to final through systematic iteration.", ITALIC, fill=MUTED, anchor="ma")
d.line([M + 20, 1845, W - M - 20, 1845], fill="#D5CCB7", width=2)

section_header(1915, "CHALLENGES OVERCOME")
challenge_titles = [
    "#1   Overfitting on PlantVillage",
    "#2   No real insect dataset",
    "#3   Motorola TFLite incompatibility",
    "#4   Arthropod visual ambiguity",
]
challenge_probs = [
    "Train accuracy greatly exceeded validation accuracy.",
    "Public pest datasets were small and inconsistent.",
    "Deployment device would not load the .tflite model.",
    "Insect classes were visually hard to distinguish.",
]
challenge_solutions = [
    "Added dropout (0.5 / 0.3) between dense layers.",
    "Built a synthetic generator with 1000 images across 15 classes.",
    "Re-ran export with TFLITE_BUILTINS and fixed Gradle dependencies.",
    "Used highlighted active regions plus augmentation; 83.33% was reached.",
]
y = 2060
for i in range(4):
    box_h = 208
    rr(M, y, W - M, y + box_h, fill="#FFFDFC", outline=OUTLINE, width=3, radius=24)
    write(M + 38, y + 38, challenge_titles[i], font(23, bold=True), fill=GREEN)
    write(M + 38, y + 92, "Problem:", SMALL_BOLD, fill=SALMON)
    wrap_text(M + 285, y + 88, W - M - 340, challenge_probs[i], BODY, line_gap=6)
    write(M + 38, y + 146, "Solution:", SMALL_BOLD, fill=TEAL)
    wrap_text(M + 285, y + 142, W - M - 340, challenge_solutions[i], BODY, line_gap=6)
    y += box_h + 20

section_header(y + 10, "APP FEATURE INVENTORY")
features = [
    ("1", "Plant Encyclopedia", "Offline JSON + SQLite, image galleries, search, linked from diagnosis.", "#E1F4DE", GREEN),
    ("2", "Regional Guides", "Crop calendars and pest windows keyed by user-selected region.", "#A4DBBA", GREEN_2),
    ("3", "Satellite / NDVI", "Map tiles plus vegetation vigor views: (NIR-Red) / (NIR+Red).", "#E1F4DE", GREEN),
    ("4", "Soil Moisture", "SMAP / Sentinel-derived tiles, color-coded with confidence.", "#A4DBBA", BROWN),
    ("5", "OpenWeather", "24-72 hour forecast, frost risk, spray windows, and precipitation.", "#E1F4DE", GREEN_2),
    ("6", "Space Weather (Kp)", "NOAA SWPC feed; GPS precision alerts when geomagnetic activity is elevated.", "#A4DBBA", GOLD),
    ("7", "Telemetry + Active Learning", "Opt-in confirmed images feed retraining and remain GDPR/CCPA aware.", "#E1F4DE", GREEN),
    ("8", "Notifications & Reports", "Push alerts for outbreaks plus weekly or monthly PDF field reports.", "#A4DBBA", GREEN_2),
]
y = y + 150
card_w = (W - 2 * M - 40) // 2
card_h = 210
for idx, feat in enumerate(features):
    col = idx % 2
    row = idx // 2
    x = M + col * (card_w + 40)
    yy = y + row * (card_h + 22)
    num, title, desc, fillc, bubble = feat
    rr(x, yy, x + card_w, yy + card_h, fill=fillc, outline=bubble, width=3, radius=24)
    d.ellipse([x + 24, yy + 24, x + 94, yy + 94], fill=bubble)
    write(x + 59, yy + 58, num, font(22, bold=True), fill="#FFFFFF", anchor="mm")
    write(x + 120, yy + 56, title, font(21, bold=True), fill=TEXT, anchor="lm")
    wrap_text(x + 32, yy + 118, card_w - 64, desc, BODY, fill=TEXT, line_gap=8)

rai_y = y + 4 * (card_h + 22) + 16
section_header(rai_y, "RESPONSIBLE AI DESIGN")
rai_items = [
    ("Confidence Gating", "Predictions below 50% softmax are suppressed and the user is prompted to retake the image."),
    ("No Chemical Dosages", "The chatbot refuses pesticide-dosage requests and defers those decisions to agronomists."),
    ("Privacy by Default", "Photos stay on-device and telemetry remains opt-in with a clear retention policy."),
    ("Expert Handoff", "Every advisory response includes a disclaimer and local extension links."),
    ("No Secret Keys", "The GPT API key is held only on the Render backend and is never embedded in the APK."),
]
row_y = rai_y + 132
for title_s, body_s in rai_items:
    rr(M, row_y, W - M, row_y + 94, fill="#FFFDFC", outline="#6AA88D", width=2, radius=20)
    d.ellipse([M + 26, row_y + 34, M + 56, row_y + 60], fill=GREEN_2)
    d.line([M + 33, row_y + 52, M + 48, row_y + 40], fill="#C6E7D0", width=2)
    write(M + 88, row_y + 31, title_s, font(19, bold=True), fill=GREEN)
    wrap_text(M + 88, row_y + 54, W - 2 * M - 120, body_s, SMALL, fill=TEXT, line_gap=4)
    row_y += 108

impact_y = row_y + 12
section_header(impact_y, "PROJECTED IMPACT")
card_y = impact_y + 132
impact_w = (W - 2 * M - 48) // 3
impact_h = 220
impact_cards = [
    ("10x", "faster", "diagnosis versus lab testing"),
    ("0", "marginal cost", "per additional farmer reached"),
    ("~475 M", "smallholder farms", "worldwide (FAO); 84% under 2 ha"),
]
impact_colors = [GREEN_3, GREEN_4, "#E5F7DA"]
impact_inks = [GREEN, GREEN, NAVY]
for i, (big, mid, body) in enumerate(impact_cards):
    x = M + i * (impact_w + 24)
    rr(x, card_y, x + impact_w, card_y + impact_h, fill=impact_colors[i], outline=GREEN_2, width=3, radius=28)
    write(x + impact_w // 2, card_y + 68, big, IMPACT_BIG, fill=impact_inks[i], anchor="ma")
    write(x + impact_w // 2, card_y + 122, mid, IMPACT_MID, fill=TEXT, anchor="ma")
    wrap_text(x + 46, card_y + 146, impact_w - 92, body, IMPACT_BODY, fill=TEXT, line_gap=4)

conc_y = card_y + impact_h + 26
section_header(conc_y, "CONCLUSIONS")
cy = conc_y + 126
conclusions = [
    "H1 supported: MobileNetV2 plus transfer learning plus dropout reached 95.51%, competitive with cloud CNNs while still deployable as a 3 MB TFLite model.",
    "H2 partially supported: the synthetic-data insect classifier reached 83.33%; curated real pest imagery would likely close the remaining gap.",
    "A unified offline-first architecture combining inference, advisory logic, and agronomic context is achievable on commodity Android hardware.",
    "Multi-modal integration of vision, NDVI, weather, and LLM guidance turns a class label into a more actionable farmer workflow.",
]
for item in conclusions:
    d.ellipse([M + 18, cy + 8, M + 46, cy + 36], fill=GREEN_2)
    d.line([M + 26, cy + 28, M + 40, cy + 14], fill="#D7F0DE", width=2)
    cy = wrap_text(M + 70, cy, W - 2 * M - 80, item, CONCLUSION, fill=TEXT, line_gap=8) + 10

fw_y = cy + 8
section_header(fw_y, "FUTURE WORK")
fy = fw_y + 130
future = [
    "Replace the synthetic insect set with curated real pest imagery.",
    "Use quantization-aware training to further reduce model size.",
    "Partner with USDA extension offices and NGOs for field validation.",
    "Expand toward crop-yield forecasting using satellite and weather layers.",
]
for item in future:
    d.ellipse([M + 18, fy + 10, M + 46, fy + 36], fill=GOLD)
    d.line([M + 24, fy + 30, M + 40, fy + 14], fill="#8A6B12", width=2)
    fy = wrap_text(M + 70, fy, W - 2 * M - 80, item, CONCLUSION, fill=TEXT, line_gap=8) + 12

ref_y = fy + 6
section_header(ref_y, "REFERENCES (MLA)")
ry = ref_y + 126
refs = [
    'Mohanty, Hughes, and Salathe. "Using Deep Learning for Image-Based Plant Disease Detection." Frontiers in Plant Science, vol. 7, 2016.',
    'Sandler et al. "MobileNetV2: Inverted Residuals and Linear Bottlenecks." CVPR, 2018.',
    'Hughes and Salathe. "An Open Access Repository of Images on Plant Health to Enable the Development of Mobile Disease Diagnostics." PlantVillage / Frontiers in Plant Science, 2016.',
    "FAO. Impact of Pests and Diseases on Global Food Security. FAO, 2021. USDA Plant Disease Program.",
]
for ref in refs:
    d.ellipse([M + 28, ry + 12, M + 42, ry + 26], fill=GREEN_2)
    ry = wrap_text(M + 70, ry, W - 2 * M - 100, ref, REFERENCE, fill=TEXT, line_gap=10) + 18

rr(0, H - 88, W, H, fill=GREEN, radius=0)
write(
    W // 2,
    H - 46,
    "AgriVerse · Tattala · Lee · Valisetty · Parthasarathy · github.com/vtattala-maker/AgriVerse",
    FOOTER,
    fill="#F4F0E7",
    anchor="ma",
)

img.save(OUT_PNG, quality=95)
c = canvas.Canvas(str(OUT_PDF), pagesize=(12 * inch, 36 * inch))
c.drawImage(ImageReader(str(OUT_PNG)), 0, 0, width=12 * inch, height=36 * inch, preserveAspectRatio=False, mask="auto")
c.showPage()
c.save()

print(OUT_PNG)
print(OUT_PDF)
