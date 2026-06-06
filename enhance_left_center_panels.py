from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from reportlab.lib.pagesizes import inch
from reportlab.lib.utils import ImageReader
from reportlab.pdfgen import canvas


ROOT = Path(r"C:\Users\Owner\AndroidStudioProjects\PlantDiseaseIdentifier2")
LEFT_IN = ROOT / "left_preview.jpg"
CENTER_IN = ROOT / "center_preview.jpg"
LEFT_OUT_PNG = ROOT / "agriverse_left_12x36_redesign.png"
LEFT_OUT_PDF = ROOT / "agriverse_left_12x36_redesign.pdf"
CENTER_OUT_PNG = ROOT / "agriverse_center_24x36_redesign.png"
CENTER_OUT_PDF = ROOT / "agriverse_center_24x36_redesign.pdf"

BG = "#F7F2E8"
GREEN = "#1D5138"
GREEN_2 = "#2E6B4B"
GREEN_3 = "#D9F0D9"
GREEN_4 = "#9AD7B4"
TEXT = "#26352F"
MUTED = "#55635C"
GOLD = "#D6B441"
PALE = "#FDFCF8"
TABLE = "#EAF7E2"
BROWN = "#9A6B42"
NAVY = "#15386B"

FONT_DIR = Path(r"C:\Windows\Fonts")
FONT_BOLD = str(FONT_DIR / "arialbd.ttf")
FONT_REG = str(FONT_DIR / "arial.ttf")
FONT_ITALIC = str(FONT_DIR / "ariali.ttf")


def font(size, bold=False, italic=False):
    path = FONT_REG
    if bold:
        path = FONT_BOLD
    if italic:
        path = FONT_ITALIC
    return ImageFont.truetype(path, size=size)


def fit_font(draw, text, width, max_size, min_size=16, bold=False):
    for size in range(max_size, min_size - 1, -1):
        f = font(size, bold=bold)
        if draw.textlength(text, font=f) <= width:
            return f
    return font(min_size, bold=bold)


def wrap(draw, text, fnt, width):
    words = text.split()
    lines = []
    current = ""
    for word in words:
        candidate = word if not current else f"{current} {word}"
        if draw.textlength(candidate, font=fnt) <= width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def write_block(draw, box, text, *, max_size, min_size=18, fill=TEXT, bold=False, align="left", vcenter=True, line_gap=8):
    x1, y1, x2, y2 = box
    width = x2 - x1
    height = y2 - y1
    best_font = font(min_size, bold=bold)
    best_lines = wrap(draw, text, best_font, width)
    for size in range(max_size, min_size - 1, -1):
        fnt = font(size, bold=bold)
        lines = wrap(draw, text, fnt, width)
        total_h = len(lines) * fnt.size + max(0, len(lines) - 1) * line_gap
        if total_h <= height:
            best_font = fnt
            best_lines = lines
            break
    total_h = len(best_lines) * best_font.size + max(0, len(best_lines) - 1) * line_gap
    yy = y1 + ((height - total_h) // 2 if vcenter else 0)
    for line in best_lines:
        if align == "center":
            xx = x1 + (width - draw.textlength(line, font=best_font)) / 2
        else:
            xx = x1
        draw.text((xx, yy), line, font=best_font, fill=fill)
        yy += best_font.size + line_gap


def pill_box(draw, box, fill, outline=GREEN_2, width=3, radius=24):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def enhance_left():
    img = Image.open(LEFT_IN).convert("RGB")
    d = ImageDraw.Draw(img)

    # Abstract body
    pill_box(d, (88, 325, 2500, 1088), BG, outline=GREEN_2, width=3, radius=28)
    abstract = (
        "Plant disease and insect pests destroy roughly 40% of global crop production each year and create more than "
        "$220 billion in losses, with the burden falling hardest on smallholder farmers. Most existing mobile tools still "
        "depend on cloud connectivity that rural fields often lack. AgriVerse addresses that gap with an Android application "
        "that runs two convolutional neural networks entirely on-device through TensorFlow Lite: a 38-class plant-disease "
        "classifier reaching 95.51% held-out accuracy and a 15-class insect classifier reaching 83.33% test accuracy. "
        "Both models use a MobileNetV2 backbone with transfer learning, and their predictions feed a context-aware advisory "
        "stack that includes a chatbot, offline encyclopedia content, and satellite, weather, and soil overlays."
    )
    write_block(d, (235, 455, 2360, 960), abstract, max_size=38, min_size=26, align="center", line_gap=10)

    # Problem cards
    for box in [(88, 1320, 872, 1775), (920, 1320, 1682, 1775), (1730, 1320, 2505, 1775)]:
        d.rectangle(box, fill=None)
    pill_box(d, (88, 1320, 872, 1775), GREEN_3, outline=GREEN_2, width=3, radius=0)
    pill_box(d, (920, 1320, 1682, 1775), GREEN_4, outline=GREEN_2, width=3, radius=0)
    pill_box(d, (1730, 1320, 2505, 1775), "#E3F7D8", outline=GREEN_2, width=3, radius=0)
    write_block(d, (225, 1540, 730, 1735), "of global crop production lost annually from pests and disease pressure", max_size=28, min_size=20, align="center", line_gap=4)
    write_block(d, (1040, 1540, 1565, 1735), "in worldwide annual losses from pests, crop disease, and delayed diagnosis", max_size=28, min_size=20, align="center", line_gap=4)
    write_block(d, (1880, 1498, 2355, 1705), "smallholder farms worldwide (FAO), with 84% operating on under 2 hectares", max_size=28, min_size=20, align="center", line_gap=5, fill=NAVY if 'NAVY' in globals() else "#15386B")

    # Problem paragraph
    pill_box(d, (88, 1800, 2500, 2440), BG, outline=GREEN_2, width=3, radius=24)
    problem = (
        "Smallholder farmers in rural and under-served regions often lack fast, affordable access to agronomists and laboratory "
        "diagnostics. By the time disease is identified visually, crop damage may already be severe and treatments may be "
        "applied too late or too broadly. Existing diagnosis apps also assume stable internet, which is unrealistic in many "
        "fields. AgriVerse is motivated by the need for a practical offline workflow that can diagnose, explain, and guide action "
        "without requiring cloud inference."
    )
    write_block(d, (210, 1930, 2380, 2310), problem, max_size=33, min_size=22, align="center", line_gap=8)

    # Clean lower half entirely and rebuild to avoid stacked text artifacts
    d.rectangle((0, 2440, 2592, 7688), fill=BG)
    pill_box(d, (88, 2445, 2500, 2595), GREEN, outline=GREEN, width=3, radius=24)
    d.text((150, 2490), "LITERATURE REVIEW", font=font(34), fill="#F4F0E7")
    d.text((88, 2650), "Prior Work · Plant Disease Classification", font=font(28), fill=GREEN)
    d.rectangle((88, 2720, 575, 2732), fill=GOLD)
    intro = "CNN-based classifiers dominate agricultural image recognition, but benchmark leaders typically assume cloud inference or heavier deployment footprints."
    write_block(d, (88, 2790, 2460, 2920), intro, max_size=28, min_size=20, align="left", vcenter=False)

    # Apps comparison rows
    d.rectangle((88, 2995, 2500, 3540), fill=PALE)
    d.text((150, 3050), "Architecture", font=font(24), fill="#F4F0E7")
    d.text((880, 3050), "Accuracy", font=font(24), fill="#F4F0E7")
    d.text((1210, 3050), "Year", font=font(24), fill="#F4F0E7")
    d.text((1510, 3050), "Reference", font=font(24), fill="#F4F0E7")
    d.rectangle((88, 2995, 2500, 3090), fill=GREEN)
    lit_rows = [
        ("AlexNet", "85.0%", "2012", "Krizhevsky et al."),
        ("GoogLeNet", "95.0%", "2015", "Mao et al."),
        ("VGG-16", "88.0%", "2017", "Prajapati et al."),
        ("ResNet-50", "94.0%", "2017", "Park et al."),
        ("Mohanty CNN", "99.35%", "2016", "Mohanty et al."),
        ("MobileNetV2", "95.51%", "2026", "AgriVerse (ours)"),
    ]
    y = 3090
    for idx, row in enumerate(lit_rows):
        d.rectangle((88, y, 2500, y + 70), fill=TABLE if idx % 2 == 0 else PALE)
        d.text((140, y + 18), row[0], font=font(20), fill=TEXT)
        d.text((885, y + 18), row[1], font=font(20), fill=TEXT)
        d.text((1215, y + 18), row[2], font=font(20), fill=TEXT)
        d.text((1515, y + 18), row[3], font=font(20), fill=TEXT)
        y += 70
    write_block(d, (120, 3515, 2460, 3575), "All prior entries require cloud inference. AgriVerse runs fully on-device.", max_size=24, min_size=18, align="center")

    d.text((88, 3660), "Existing Agricultural Apps", font=font(28), fill=GREEN)
    d.rectangle((88, 3730, 575, 3742), fill=GOLD)
    app_rows = [
        ("Plantix", "Large disease database, but cloud-only and subscription-constrained."),
        ("Agrio", "Rule-based and ML-assisted, though diagnosis still relies on internet access."),
        ("Xarvio Scouting", "Strong enterprise workflow but oriented toward larger commercial farms."),
        ("CropDiagnosis", "Image recognition support with limited offline capability and narrower context tools."),
    ]
    y = 3825
    for name, desc in app_rows:
        pill_box(d, (88, y, 2500, y + 108), PALE, outline=GREEN_2, width=2, radius=18)
        name_f = fit_font(d, name, 360, 28, 22, bold=False)
        d.text((140, y + 34), name, font=name_f, fill=TEXT)
        write_block(d, (560, y + 20, 2360, y + 88), desc, max_size=24, min_size=18, align="left", vcenter=True, line_gap=4)
        y += 126

    # Research gap
    pill_box(d, (88, 4370, 2500, 4695), GREEN, outline=GOLD, width=3, radius=24)
    gap = (
        "No existing tool combines on-device CNN inference, pest and disease coverage, and contextual agronomic guidance "
        "in a single offline-capable package."
    )
    d.text((1120, 4440), "RESEARCH GAP", font=font(34), fill=GOLD)
    write_block(d, (320, 4495, 2260, 4630), gap, max_size=28, min_size=22, align="center", fill="#F4F0E7", line_gap=6)

    # Hypothesis box
    pill_box(d, (88, 4895, 2500, 5610), BG, outline=GREEN_2, width=3, radius=24)
    pill_box(d, (88, 4735, 2500, 4870), GREEN, outline=GREEN, width=3, radius=24)
    d.text((150, 4778), "HYPOTHESIS", font=font(34), fill="#F4F0E7")
    h1 = (
        "H1. A lightweight MobileNetV2 trained with transfer learning on PlantVillage can exceed 90% test accuracy when deployed "
        "as a TensorFlow Lite model on commodity Android hardware without cloud inference."
    )
    h2 = (
        "H2. The same backbone can be adapted to a 15-class insect task, and transfer learning plus augmentation can compensate "
        "for a comparatively small synthetic training set."
    )
    d.text((160, 5035), "H·", font=font(44), fill=GOLD)
    write_block(d, (270, 4995, 2330, 5235), h1, max_size=34, min_size=22, line_gap=6)
    d.text((160, 5345), "H·", font=font(44), fill=GOLD)
    write_block(d, (270, 5310, 2330, 5545), h2, max_size=34, min_size=22, line_gap=6)

    # Engineering goals cards
    pill_box(d, (88, 5690, 2500, 5825), GREEN, outline=GREEN, width=3, radius=24)
    d.text((150, 5733), "ENGINEERING GOALS", font=font(34), fill="#F4F0E7")
    goals = [
        ("01", "Novel AI Models", "Train plant disease and pest classifiers with measurable accuracy and size targets suitable for real field use."),
        ("02", "Offline Deployment", "Convert both models to TensorFlow Lite so inference runs fully on-device with no cloud dependency."),
        ("03", "Full-Stack Mobile App", "Ship a unified Android experience that includes camera capture, classification, and advisory views."),
        ("04", "Advisory Integration", "Connect model outputs to a GPT-based chatbot, offline encyclopedia content, and environmental overlays."),
    ]
    y = 5885
    for num, title, body in goals:
        pill_box(d, (88, y, 2500, y + 255), "#E0F5DE", outline=GREEN_2, width=3, radius=26)
        d.ellipse((130, y + 38, 290, y + 198), fill=GREEN, outline=GOLD, width=3)
        nf = fit_font(d, num, 120, 34, 28, bold=False)
        d.text((210 - d.textlength(num, font=nf) / 2, y + 102), num, font=nf, fill="#F4F0E7")
        d.text((330, y + 45), title, font=font(30), fill=TEXT)
        write_block(d, (330, y + 98, 2340, y + 210), body, max_size=26, min_size=20, align="left", line_gap=5)
        y += 278

    # Research questions
    pill_box(d, (88, 7050, 2500, 7185), GREEN, outline=GREEN, width=3, radius=24)
    d.text((150, 7093), "RESEARCH QUESTIONS", font=font(34), fill="#F4F0E7")
    rq = [
        "Q1. Can a MobileNetV2 backbone with frozen ImageNet weights approach parity with cloud CNNs on PlantVillage while staying small enough for on-device deployment?",
        "Q2. Can transfer learning compensate for a small synthetic insect training set and still produce actionable pest accuracy?",
        "Q3. What model-size versus accuracy tradeoff remains acceptable for a mid-range Android phone used in rural conditions?",
        "Q4. Does linking classifier output to an LLM advisor improve diagnosis usability and decision support for farmers?",
    ]
    y = 7220
    for item in rq:
        d.ellipse((110, y + 16, 142, y + 48), fill=GREEN_2)
        write_block(d, (185, y, 2390, y + 120), item, max_size=28, min_size=20, align="left", vcenter=False, line_gap=6)
        y += 128

    img.save(LEFT_OUT_PNG, quality=95)
    c = canvas.Canvas(str(LEFT_OUT_PDF), pagesize=(12 * inch, 36 * inch))
    c.drawImage(ImageReader(str(LEFT_OUT_PNG)), 0, 0, width=12 * inch, height=36 * inch, preserveAspectRatio=False)
    c.showPage()
    c.save()


def enhance_center():
    img = Image.open(CENTER_IN).convert("RGB")
    d = ImageDraw.Draw(img)

    # Top stat cards subtitles
    stat_boxes = [
        (120, 1140, 835, 1745, "Held-out plant classifier accuracy on the PlantVillage split after transfer learning, dropout, and tuning."),
        (955, 1140, 1700, 1745, "Held-out insect classifier accuracy on the synthetic pest benchmark after augmentation and transfer learning."),
        (1805, 1140, 2555, 1745, "Combined classes covered by both production models: 38 plant diseases plus 30 insect categories."),
        (2640, 1140, 3390, 1745, "Approximate TensorFlow Lite footprint for each deployed model, sized for commodity Android devices."),
        (3475, 1140, 4225, 1745, "Total parameter count, with roughly 100k to 200k trainable layers after freezing the backbone."),
        (4310, 1140, 5050, 1745, "Internet requirement for diagnosis: zero. Inference executes completely on-device."),
    ]
    for x1, y1, x2, y2, body in stat_boxes:
        write_block(d, (x1 + 70, y1 + 410, x2 - 70, y2 - 45), body, max_size=30, min_size=20, align="center", line_gap=5)

    # Clean pipeline + methods area before redrawing
    d.rectangle((40, 1860, 5140, 5175), fill=BG)
    pill_box(d, (88, 1865, 5090, 2010), GREEN, outline=GREEN, width=3, radius=24)
    d.text((170, 1912), "SYSTEM ARCHITECTURE  ·  End-to-End Pipeline", font=font(34, bold=True), fill="#F4F0E7")

    # Pipeline stage text fill
    pipeline = [
        ((115, 2120, 845, 2965), "Image Capture", "Capture plant or insect photos directly with the camera or choose from the Android gallery. The workflow is designed for rapid field collection with minimal taps."),
        ((955, 2120, 1685, 2965), "Preprocess", "Resize every frame to 224×224, normalize pixels to [0,1], and assemble inference-ready tensors that match the training pipeline."),
        ((1795, 2120, 2525, 2965), "MobileNetV2", "Use a frozen ImageNet-pretrained MobileNetV2 backbone to extract compact visual features while keeping the model lightweight enough for mobile deployment."),
        ((2635, 2120, 3365, 2965), "Classifier Head", "Apply global average pooling, dense layers of 256 and 128 units, and tuned dropout rates of 0.5 and 0.3 before softmax classification."),
        ((3475, 2120, 4205, 2965), "TFLite Inference", "Run the quantized model locally on-device. The final package is about 3 MB, avoids cloud round-trips, and works offline in the field."),
        ((4315, 2120, 5045, 2965), "Advisory Layer", "Route results into the chatbot, encyclopedia, weather, NDVI, and space-weather context layer to turn labels into actionable guidance."),
    ]
    for box, title, body in pipeline:
        x1, y1, x2, y2 = box
        pill_box(d, box, PALE, outline=GREEN_2, width=3, radius=24)
        head_fill = GREEN if title == "Image Capture" else GREEN_2 if title in ("Preprocess", "MobileNetV2") else "#93D4B0" if title == "Classifier Head" else GOLD if title == "TFLite Inference" else BROWN
        d.rounded_rectangle((x1, y1, x2, y1 + 145), radius=24, fill=head_fill)
        d.rectangle((x1, y1 + 110, x2, y1 + 145), fill=head_fill)
        d.ellipse((x1 + 28, y1 + 72, x1 + 112, y1 + 156), fill=PALE, outline=head_fill, width=3)
        d.text((x1 + 66, y1 + 101), str(pipeline.index((box, title, body)) + 1), font=font(22, bold=True), fill=head_fill, anchor="mm")
        write_block(d, (x1 + 120, y1 + 30, x2 - 40, y1 + 100), title, max_size=28, min_size=22, bold=True, align="center", fill="#F4F0E7" if head_fill != GOLD else TEXT)
        write_block(d, (x1 + 90, y1 + 120, x2 - 90, y2 - 70), body, max_size=26, min_size=18, align="center", line_gap=5)

    # Methods left architecture panel: overwrite with denser text stack
    arch_box = (110, 3400, 2520, 4780)
    pill_box(d, arch_box, BG, outline=GREEN_2, width=3, radius=24)
    d.text((170, 3505), "Network Architecture (Keras / TensorFlow)", font=font(32, bold=True), fill=GREEN)
    d.rectangle((170, 3585, 900, 3598), fill=GOLD)
    layers = [
        ("Input", "224 × 224 × 3 RGB images, standardized to the same spatial footprint used during training."),
        ("Backbone", "Frozen MobileNetV2 pretrained on ImageNet for efficient transfer learning and mobile-friendly feature extraction."),
        ("Pooling", "GlobalAveragePooling2D compresses the final feature map without introducing a large parameter increase."),
        ("Dense 256", "ReLU-activated dense layer with dropout 0.5 to reduce overfitting on the plant model."),
        ("Dense 128", "Second compact dense layer with dropout 0.3 to stabilize the classifier head before softmax."),
        ("Output", "Softmax output configured for either 38 plant classes or 30 insect classes depending on the active model."),
    ]
    y = 3655
    colors = ["#A7DDBB", GREEN_2, "#4D9A73", BROWN, "#2E6B4B", GOLD]
    for idx, (name, body) in enumerate(layers):
        pill_box(d, (190, y, 2340, y + 130), colors[idx], outline=GREEN_2, width=2, radius=12)
        write_block(d, (240, y + 18, 800, y + 60), name, max_size=28, min_size=22, bold=True, fill="#F4F0E7" if idx in (1, 3, 4) else TEXT)
        write_block(d, (820, y + 18, 2280, y + 108), body, max_size=22, min_size=16, fill="#F4F0E7" if idx in (1, 3, 4) else TEXT, line_gap=4)
        y += 150
    code_box = (190, 4565, 2340, 4745)
    pill_box(d, code_box, "#1E2924", outline=GOLD, width=2, radius=12)
    code = "Sequential([MobileNetV2(frozen), GAP, Dense(256,'relu'), Dropout(0.5), Dense(128,'relu'), Dropout(0.3), Dense(classes,'softmax')])"
    write_block(d, (225, 4605, 2290, 4710), code, max_size=22, min_size=16, fill="#D7F0DE", align="left", line_gap=2)

    # Methods right protocol panel denser rows
    proto_box = (2660, 3400, 5070, 4780)
    pill_box(d, proto_box, BG, outline=GOLD, width=3, radius=24)
    d.text((2720, 3505), "Training Protocol & Hyperparameters", font=font(32, bold=True), fill=GREEN)
    d.rectangle((2720, 3585, 3480, 3598), fill=GOLD)
    rows = [
        ("Framework", "TensorFlow 2.x + Keras + TensorFlow Lite"),
        ("Environment", "Google Colab with GPU runtime"),
        ("Base Model", "MobileNetV2 (ImageNet, frozen)"),
        ("Total Params", "≈ 2.62 M"),
        ("Trainable Params", "≈ 100,000 to 200,000 in classifier head"),
        ("Input Size", "224 × 224 × 3 RGB"),
        ("Batch Size", "32"),
        ("Optimizer", "Adam, learning rate 0.001"),
        ("Loss Function", "Sparse categorical cross-entropy"),
        ("Metrics", "Accuracy and loss for train + validation"),
        ("Dropout", "0.5 after D-256, 0.3 after D-128"),
        ("Plant Dataset", "PlantVillage, split 70/15/15"),
        ("Plant Training", "10 epochs, 1,188 batches per epoch"),
        ("Insect Dataset", "Synthetic, 1,000 images × 30 classes"),
        ("Insect Training", "15 epochs, 32 batches per epoch"),
        ("Deployment", "Quantized TFLite on Android"),
    ]
    y = 3665
    for idx, (label, value) in enumerate(rows):
        fill = TABLE if idx % 2 == 0 else PALE
        d.rectangle((2700, y, 5005, y + 66), fill=fill)
        d.text((2740, y + 17), label, font=font(21, bold=True), fill=GREEN_2)
        write_block(d, (3480, y + 8, 4950, y + 58), value, max_size=20, min_size=16, align="left", line_gap=2)
        y += 72

    # Results caption strip
    d.rectangle((80, 4865, 5100, 5015), fill=BG)
    caption = "Results are shown from actual Colab training runs. Plant training converges smoothly to 95.51% test accuracy, while the insect model stabilizes near 83.33% despite a smaller synthetic dataset."
    write_block(d, (180, 4875, 4950, 4998), caption, max_size=28, min_size=20, align="center", line_gap=4)

    # Clean right user-flow panel before rewrite
    d.rectangle((2850, 5870, 5105, 6890), fill=BG)
    pill_box(d, (2860, 5878, 5090, 6815), BG, outline=GOLD, width=3, radius=24)
    flow_box = (2860, 5880, 5090, 6815)
    d.text((2920, 5980), "App User Flow — 3 Key Screens", font=font(30, bold=True), fill=GREEN)
    d.rectangle((2920, 6050, 3660, 6062), fill=GOLD)
    flow_text = (
        "1. Choose mode: select disease or insect diagnosis, then jump into the knowledge base.\n"
        "2. Get diagnosis: capture or upload an image, run local inference, and surface confidence.\n"
        "3. Ask chatbot: turn the predicted class into treatment, context, and follow-up actions."
    )
    write_block(d, (2900, 6640, 5050, 6780), flow_text, max_size=22, min_size=16, align="center", line_gap=4)

    # Clean validation section before rewrite
    d.rectangle((70, 7080, 5110, 8350), fill=BG)
    pill_box(d, (88, 6900, 2800, 7050), GREEN, outline=GREEN, width=3, radius=24)
    d.text((170, 6945), "VALIDATION & COMPARISON TO STATE OF THE ART", font=font(34, bold=True), fill="#F4F0E7")

    # Validation comparison table rewrite to ensure density
    table_box = (110, 7085, 2520, 7920)
    pill_box(d, table_box, BG, outline=GREEN_2, width=3, radius=24)
    d.text((170, 7200), "Validation Metrics — Both Models", font=font(32, bold=True), fill=GREEN)
    d.rectangle((170, 7270, 920, 7282), fill=GOLD)
    headers = [("Metric", 210), ("Plant Disease Model", 1200), ("Insect Pest Model", 1970)]
    d.rectangle((170, 7345, 2440, 7445), fill=GREEN)
    for label, x in headers:
        d.text((x, 7373), label, font=font(20, bold=True), fill="#F4F0E7")
    metrics = [
        ("Number of classes", "38", "30"),
        ("Training epochs", "10", "15"),
        ("Batches per epoch", "1,188", "32"),
        ("Final train accuracy", "92.5%", "83.0%"),
        ("Final validation accuracy", "94.7%", "86.0%"),
        ("Held-out test accuracy", "95.51%", "83.33%"),
        ("Held-out test loss", "0.1349", "0.3870"),
        ("Confidence threshold", "> 50% softmax", "> 50% softmax"),
        ("Model format", "TFLite (3 MB)", "TFLite (3 MB)"),
        ("Deployment", "Android on-device", "Android on-device"),
    ]
    y = 7445
    for idx, row in enumerate(metrics):
        d.rectangle((170, y, 2440, y + 62), fill=TABLE if idx % 2 == 0 else PALE)
        d.text((210, y + 16), row[0], font=font(18), fill=TEXT)
        d.text((1200, y + 16), row[1], font=font(18, bold=True), fill=GREEN_2)
        d.text((1970, y + 16), row[2], font=font(18, bold=True), fill=BROWN)
        y += 62

    # SOTA caption
    sota_box = (2660, 7085, 5070, 7920)
    pill_box(d, sota_box, BG, outline=GOLD, width=3, radius=24)
    d.text((2720, 7200), "Plant Model vs Published SOTA", font=font(32, bold=True), fill=GREEN)
    d.rectangle((2720, 7270, 3480, 7282), fill=GOLD)
    bars = [
        ("AlexNet ('12)", 85.0, "#687867", "Cloud"),
        ("VGG-16 ('17)", 88.0, BROWN, "Cloud"),
        ("ResNet-50 ('17)", 94.0, "#4C9970", "Cloud"),
        ("GoogLeNet ('15)", 95.0, GREEN_2, "Cloud"),
        ("Mohanty CNN ('16)", 99.35, "#1E2924", "Cloud"),
        ("AgriVerse (2026)", 95.51, GREEN, "On-device"),
    ]
    y = 7360
    for label, val, color, mode in bars:
        d.text((2720, y + 10), label, font=font(18), fill=MUTED)
        d.rectangle((3020, y, 4740, y + 44), outline="#D6D6D6", width=1)
        bw = int((val / 100.0) * 1500)
        d.rectangle((3020, y, 3020 + bw, y + 44), fill=color, outline=GOLD if mode == "On-device" else color, width=3 if mode == "On-device" else 1)
        d.text((3030 + bw, y + 10), f"{val:.2f}%" if val % 1 else f"{int(val)}%", font=font(18, bold=True), fill=TEXT)
        d.text((4850, y + 10), mode, font=font(18, bold=True) if mode == "On-device" else font(18), fill=GOLD if mode == "On-device" else MUTED)
        y += 72
    note = (
        "AgriVerse reaches 95.51% test accuracy while staying on-device, which makes it competitive with published cloud-centric baselines. "
        "The design goal is not only accuracy, but deployability: small model size, no network dependency, and integration with an advisory layer."
    )
    write_block(d, (2720, 7800, 4980, 7895), note, max_size=20, min_size=16, align="center", line_gap=4)

    img.save(CENTER_OUT_PNG, quality=95)
    c = canvas.Canvas(str(CENTER_OUT_PDF), pagesize=(24 * inch, 36 * inch))
    c.drawImage(ImageReader(str(CENTER_OUT_PNG)), 0, 0, width=24 * inch, height=36 * inch, preserveAspectRatio=False)
    c.showPage()
    c.save()


if __name__ == "__main__":
    enhance_left()
    enhance_center()
    print(LEFT_OUT_PNG)
    print(LEFT_OUT_PDF)
    print(CENTER_OUT_PNG)
    print(CENTER_OUT_PDF)
