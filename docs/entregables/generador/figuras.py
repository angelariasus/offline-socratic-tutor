"""Genera las figuras del documento de avance: prototipo (3.7) y mapa de navegación (3.8)."""
import sys
from PIL import Image, ImageDraw, ImageFont

FIGMA_PNG, OUT_DIR = sys.argv[1], sys.argv[2]
F = "C:/Windows/Fonts/"
def font(size, bold=False):
    return ImageFont.truetype(F + ("arialbd.ttf" if bold else "arial.ttf"), size)

# ---------- Figura 3.7: prototipo del estudiante ----------
src = Image.open(FIGMA_PNG).convert("RGB")
screens = [
    ("Inicio", (0, 0)), ("Escaneo", (960, 996)), ("Biblioteca", (480, 0)),
    ("Chats", (0, 996)), ("Chat socrático", (480, 995)), ("Ajustes", (960, 0)),
]
W, H, GAP, PAD, LBL = 440, 956, 48, 24, 56
cols, rows = 3, 2
canvas = Image.new("RGB", (PAD * 2 + cols * W + (cols - 1) * GAP, PAD * 2 + rows * (H + LBL) + (rows - 1) * GAP), "white")
d = ImageDraw.Draw(canvas)
for i, (name, (x, y)) in enumerate(screens):
    c, r = i % cols, i // cols
    ox = PAD + c * (W + GAP)
    oy = PAD + r * (H + LBL + GAP)
    canvas.paste(src.crop((x, y, x + W, y + H)), (ox, oy))
    d.rectangle((ox - 1, oy - 1, ox + W, oy + H), outline="#C8C8C8", width=2)
    f = font(30, True)
    tw = d.textlength(name, font=f)
    d.text((ox + (W - tw) / 2, oy + H + 12), name, fill="#1F2937", font=f)
canvas.save(OUT_DIR + "/fig3_7_prototipo_estudiante.png", optimize=True)

# ---------- Figura 3.8: mapa de navegación ----------
BLUE, FILL, NAVY, GREY = "#1E63B8", "#E3F0FB", "#0D3B7A", "#475569"
img = Image.new("RGB", (1500, 760), "white")
d = ImageDraw.Draw(img)
BW, BH = 230, 84

def box(cx, cy, title, sub=None, fill=FILL):
    x0, y0 = cx - BW / 2, cy - BH / 2
    d.rounded_rectangle((x0, y0, x0 + BW, y0 + BH), radius=16, fill=fill, outline=BLUE, width=3)
    ft = font(24, True)
    ty = cy - (22 if sub else 13)
    d.text((cx - d.textlength(title, font=ft) / 2, ty), title, fill=NAVY, font=ft)
    if sub:
        fs = font(17)
        d.text((cx - d.textlength(sub, font=fs) / 2, cy + 10), sub, fill=GREY, font=fs)
    return (cx, cy)

def arrow(p0, p1, label=None, loff=(0, 0)):
    import math
    d.line((p0, p1), fill=BLUE, width=3)
    ang = math.atan2(p1[1] - p0[1], p1[0] - p0[0])
    L, A = 18, 0.42
    pts = [p1, (p1[0] - L * math.cos(ang - A), p1[1] - L * math.sin(ang - A)),
           (p1[0] - L * math.cos(ang + A), p1[1] - L * math.sin(ang + A))]
    d.polygon(pts, fill=BLUE)
    if label:
        fl = font(18)
        mx, my = (p0[0] + p1[0]) / 2 + loff[0], (p0[1] + p1[1]) / 2 + loff[1]
        tw = d.textlength(label, font=fl)
        d.rectangle((mx - tw / 2 - 6, my - 13, mx + tw / 2 + 6, my + 13), fill="white")
        d.text((mx - tw / 2, my - 11), label, fill=NAVY, font=fl)

# Barra inferior (contenedor)
d.rounded_rectangle((300, 50, 1470, 250), radius=22, fill="#F5F9FE", outline=BLUE, width=2)
d.text((322, 62), "Barra de navegación inferior (pestañas)", fill=BLUE, font=font(20, True))
TY = 170
inicio = box(450, TY, "Inicio")
biblio = box(740, TY, "Biblioteca")
tutor = box(1030, TY, "Tutor", "lista de chats")
box(1320, TY, "Ajustes", "perfil, texto y voz")
primer = box(140, TY, "Primer uso", "perfil local", fill="white")
arrow((255, TY), (335, TY))

# Fila 2: detalle de libro y chat
R2 = 420
detalle = box(740, R2, "Detalle de libro", "páginas escaneadas")
chat = box(1030, R2, "Chat socrático", "texto, voz y quechua")
arrow((740, TY + BH / 2), (740, R2 - BH / 2))
arrow((1030, TY + BH / 2), (1030, R2 - BH / 2))
arrow((855, R2), (915, R2))
d.text((838, R2 + 52), "Preguntar sobre este libro", fill=NAVY, font=font(18))
arrow((470, TY + BH / 2), (960, R2 - BH / 2), "Nueva consulta", (-130, -52))

# Fila 3: ingesta documental
R3 = 640
esc = box(300, R3, "Escaneo", "cámara y guías")
rev = box(600, R3, "Revisión del texto", "corregir OCR")
idx = box(900, R3, "Indexación", "progreso")
arrow((430, TY + BH / 2), (310, R3 - BH / 2), "Escanear página", (-30, 0))
arrow((415, R3), (485, R3))
arrow((715, R3), (785, R3))
arrow((900, R3 - BH / 2), (760, R2 + BH / 2))
img.save(OUT_DIR + "/fig3_8_navegacion.png", optimize=True)

# ---------- Figura 3.9: capturas del chat (Roborazzi) ----------
import os
SHOTS = os.path.normpath(os.path.join(OUT_DIR, "..", "..", "..", "app", "screenshots"))
def mosaico(capturas, salida):
    """Une capturas lado a lado con su etiqueta debajo. Un nombre sin ruta es una captura de Roborazzi."""
    ims = [Image.open(n if "/" in n else f"{SHOTS}/{n}.png").convert("RGB") for _, n in capturas]
    GAP, LBL = 48, 56
    lienzo = Image.new("RGB", (sum(i.width for i in ims) + GAP * (len(ims) - 1) + 2, max(i.height for i in ims) + LBL), "white")
    d = ImageDraw.Draw(lienzo)
    x = 1
    for (etiqueta, _), im in zip(capturas, ims):
        lienzo.paste(im, (x, 1))
        d.rectangle((x - 1, 0, x + im.width, im.height + 1), outline="#C8C8C8", width=2)
        f = font(24, True)
        d.text((x + (im.width - d.textlength(etiqueta, font=f)) / 2, im.height + 16), etiqueta, fill="#1F2937", font=f)
        x += im.width + GAP
    lienzo.save(f"{OUT_DIR}/{salida}", optimize=True)


mosaico([("Conversación con términos en quechua", "chat_conversacion"), ("Dictado por voz", "chat_grabando")], "fig3_9_chat.png")

# ---------- Figura 3.10: Inicio y Biblioteca ----------
mosaico([("Inicio", "inicio_con_datos"), ("Biblioteca filtrada por área", "biblioteca_filtrada"), ("Biblioteca vacía", "biblioteca_vacia")],
        "fig3_10_inicio_biblioteca.png")
# ---------- Figura 3.11: escaneo y revisión ----------
mosaico([("Escaneo con marco guía", "escaneo_camara"), ("Revisión del texto", "revision_libro_nuevo"), ("Página guardada", "revision_listo")],
        "fig3_11_escaneo.png")
# ---------- Figura 3.12: respuesta con contexto (RAG) ----------
mosaico([("Respuesta con la fuente citada", "flujo_rag")], "fig3_12_rag.png")
# ---------- Figura 3.13: Ajustes y bienvenida ----------
mosaico([("Ajustes", "ajustes"), ("Texto muy grande", "ajustes_texto_muy_grande"), ("Bienvenida", "bienvenida_completa")],
        "fig3_13_ajustes.png")
# ---------- Figura 3.14: identidad visual (antes y después) ----------
mosaico([("Antes (Sprint 2)", f"{OUT_DIR}/antes_inicio.png"), ("Después", "inicio_con_datos"), ("Logo en la bienvenida", "bienvenida_completa")],
        "fig3_14_identidad.png")
# ---------- Figuras 3.15 y 3.16: evaluación heurística ----------
mosaico([("Detalle de libro", "libro_detalle"), ("Aviso de reemplazo", "revision_reemplaza"), ("Conversaciones", "tutor_lista")],
        "fig3_15_heuristica.png")
mosaico([("Biblioteca en cuadrícula", "biblioteca_horizontal"), ("Revisión en dos columnas", "revision_horizontal")],
        "fig3_16_horizontal.png")
print("ok")
