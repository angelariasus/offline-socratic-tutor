"""Construye el documento de avance reutilizando la portada y el formato del 2.º entregable.

Uso: python build_avance.py <plantilla.docx> <img_dir> <salida.docx>
"""
import re
import sys
import zipfile
from xml.sax.saxutils import escape

from PIL import Image

TEMPLATE, IMG_DIR, OUT = sys.argv[1], sys.argv[2], sys.argv[3]

FONT = '<w:rFonts w:ascii="Times New Roman" w:cs="Times New Roman" w:eastAsia="Times New Roman" w:hAnsi="Times New Roman"/>'


def run(text, bold=False, italic=False, size=None):
    rpr = FONT
    if bold:
        rpr += '<w:b w:val="1"/><w:bCs w:val="1"/>'
    if italic:
        rpr += '<w:i w:val="1"/><w:iCs w:val="1"/>'
    if size:
        rpr += f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>'
    return f'<w:r><w:rPr>{rpr}<w:rtl w:val="0"/></w:rPr><w:t xml:space="preserve">{escape(text)}</w:t></w:r>'


def rich(text, size=None, base_italic=False):
    """Admite **negrita** y *cursiva* en línea."""
    out = []
    for part in re.split(r'(\*\*[^*]+\*\*|\*[^*]+\*)', text):
        if not part:
            continue
        if part.startswith('**'):
            out.append(run(part[2:-2], bold=True, size=size, italic=base_italic))
        elif part.startswith('*'):
            out.append(run(part[1:-1], italic=True, size=size))
        else:
            out.append(run(part, size=size, italic=base_italic))
    return ''.join(out)


BODY_PPR = '<w:pPr><w:spacing w:line="360" w:lineRule="auto"/><w:jc w:val="both"/><w:rPr>' + FONT + '</w:rPr></w:pPr>'


HEADING_PPR = BODY_PPR.replace('<w:pPr>', '<w:pPr><w:keepNext w:val="1"/>')


def heading(text):
    return f'<w:p>{HEADING_PPR}{run(text, bold=True)}</w:p>'


def para(text):
    return f'<w:p>{BODY_PPR}{rich(text)}</w:p>'


def caption(label, title):
    p1 = ('<w:p><w:pPr><w:keepNext w:val="1"/><w:spacing w:after="0" w:before="120" w:line="240" w:lineRule="auto"/>'
          f'<w:rPr>{FONT}<w:b w:val="1"/><w:bCs w:val="1"/></w:rPr></w:pPr>{run(label, bold=True)}</w:p>')
    p2 = ('<w:p><w:pPr><w:keepNext w:val="1"/><w:spacing w:after="120" w:line="240" w:lineRule="auto"/>'
          f'<w:rPr>{FONT}<w:i w:val="1"/><w:iCs w:val="1"/></w:rPr></w:pPr>{run(title, italic=True)}</w:p>')
    return p1 + p2


def note(text):
    return ('<w:p><w:pPr><w:spacing w:after="240" w:before="80" w:line="240" w:lineRule="auto"/><w:jc w:val="both"/>'
            f'<w:rPr>{FONT}<w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr></w:pPr>'
            f'{run("Nota. ", italic=True, size=20)}{rich(text, size=20)}</w:p>')


def cell(text, width, header=False):
    tcpr = f'<w:tcW w:w="{width}" w:type="dxa"/>'
    if header:
        tcpr += '<w:tcBorders><w:bottom w:color="000000" w:space="0" w:sz="6" w:val="single"/></w:tcBorders><w:vAlign w:val="center"/>'
    jc = '<w:jc w:val="center"/>' if header else ''
    paras = []
    for line in text.split('\n'):
        content = run(line, bold=True, size=20) if header else rich(line, size=20)
        paras.append(f'<w:p><w:pPr><w:spacing w:after="0" w:line="240" w:lineRule="auto"/>{jc}'
                     f'<w:rPr>{FONT}<w:sz w:val="20"/><w:szCs w:val="20"/></w:rPr></w:pPr>{content}</w:p>')
    return f'<w:tc><w:tcPr>{tcpr}</w:tcPr>{"".join(paras)}</w:tc>'


def table(widths, header, rows):
    assert sum(widths) == 9360, sum(widths)
    grid = ''.join(f'<w:gridCol w:w="{w}"/>' for w in widths)
    tbl = ('<w:tbl><w:tblPr><w:tblW w:w="9360" w:type="dxa"/><w:jc w:val="left"/><w:tblBorders>'
           '<w:top w:color="000000" w:space="0" w:sz="8" w:val="single"/><w:left w:color="000000" w:space="0" w:sz="0" w:val="nil"/>'
           '<w:bottom w:color="000000" w:space="0" w:sz="8" w:val="single"/><w:right w:color="000000" w:space="0" w:sz="0" w:val="nil"/>'
           '<w:insideH w:color="000000" w:space="0" w:sz="0" w:val="nil"/><w:insideV w:color="000000" w:space="0" w:sz="0" w:val="nil"/>'
           '</w:tblBorders><w:tblLayout w:type="fixed"/>'
           '<w:tblCellMar><w:top w:w="60" w:type="dxa"/><w:left w:w="100" w:type="dxa"/><w:bottom w:w="60" w:type="dxa"/><w:right w:w="100" w:type="dxa"/></w:tblCellMar>'
           f'<w:tblLook w:val="0000"/></w:tblPr><w:tblGrid>{grid}</w:tblGrid>')
    tbl += '<w:tr><w:trPr><w:cantSplit w:val="1"/><w:tblHeader w:val="1"/></w:trPr>' + ''.join(cell(h, w, True) for h, w in zip(header, widths)) + '</w:tr>'
    for r in rows:
        tbl += '<w:tr><w:trPr><w:cantSplit w:val="1"/></w:trPr>' + ''.join(cell(c, w) for c, w in zip(r, widths)) + '</w:tr>'
    return tbl + '</w:tbl>'


IMAGES = []  # (rId, nombre, bytes)


def figure(path, width_in, docpr_id):
    rid = f'rId{100 + len(IMAGES)}'
    name = f'avance{len(IMAGES) + 1}.png'
    IMAGES.append((rid, name, open(path, 'rb').read()))
    w, h = Image.open(path).size
    cx = int(width_in * 914400)
    cy = int(cx * h / w)
    return ('<w:p><w:pPr><w:keepNext w:val="1"/><w:spacing w:after="0" w:line="240" w:lineRule="auto"/><w:jc w:val="center"/>'
            f'<w:rPr>{FONT}</w:rPr></w:pPr><w:r><w:rPr>{FONT}</w:rPr><w:drawing><wp:inline distB="0" distT="0" distL="0" distR="0">'
            f'<wp:extent cx="{cx}" cy="{cy}"/><wp:effectExtent b="0" l="0" r="0" t="0"/><wp:docPr id="{docpr_id}" name="{name}"/>'
            '<a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic>'
            f'<pic:nvPicPr><pic:cNvPr id="0" name="{name}"/><pic:cNvPicPr preferRelativeResize="0"/></pic:nvPicPr>'
            f'<pic:blipFill><a:blip r:embed="{rid}"/><a:srcRect b="0" l="0" r="0" t="0"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
            f'<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm><a:prstGeom prst="rect"/><a:ln/></pic:spPr>'
            '</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>')


def reference(text):
    return ('<w:p><w:pPr><w:spacing w:line="360" w:lineRule="auto"/><w:ind w:left="720" w:hanging="720"/>'
            f'<w:rPr>{FONT}</w:rPr></w:pPr>{rich(text)}</w:p>')


# ------------------------------------------------------------------ contenido
B = []
B.append(heading('CAPÍTULO III: DESARROLLO TÉCNICO (continuación)'))

# 3.1.3 ---------------------------------------------------------------
B.append(heading('3.1.3 Diseño de la interfaz de usuario'))
B.append(para('En el Sprint 1 el equipo elaboró un prototipo de alta fidelidad en Figma con 17 pantallas, organizadas en un flujo para el estudiante y otro para el docente; la Figura 3.7 muestra las del estudiante. Antes de implementarlo, cada pantalla se contrastó con los requisitos funcionales (Tabla 3.2), el diagrama de casos de uso (Figura 3.2) y la arquitectura (Figura 3.3), para que el aplicativo no incorpore funciones que el análisis de requisitos no respalda ni omita las que sí exige.'))
B.append(caption('Figura 3.7', 'Prototipo inicial en Figma: flujo del estudiante'))
B.append(figure(f'{IMG_DIR}/fig3_7_prototipo_estudiante.png', 3.9, 101))
B.append(note('Elaboración propia. Corresponde al prototipo previo a la revisión descrita en las Tablas 3.6 a 3.9.'))

B.append(heading('Revisión del prototipo frente a los requisitos'))
B.append(para('La Tabla 3.6 resume la revisión de las pantallas del estudiante. Todas se conservan, pero requieren ajustes para respetar la operatividad sin conexión (RNF01) y para cubrir la interfaz de voz (RF04) y la traducción al quechua (RF05), que no figuraban en el prototipo.'))
B.append(caption('Tabla 3.6', 'Pantallas del flujo del estudiante y ajustes requeridos'))
B.append(table([1900, 1500, 5960], ['Pantalla', 'Requisitos', 'Ajustes al prototipo'], [
    ['Inicio', 'RF01, RF03', 'Mostrar conversaciones y libros reales en lugar de textos de relleno; reducir la imagen de cabecera; el botón de cámara abre el escaneo.'],
    ['Escaneo', 'RF01', 'Reemplazar la imagen de ejemplo (un examen universitario) por la página de un libro escolar; añadir guías de encuadre (Figura 3.3) y linterna.'],
    ['Biblioteca', 'RF02', 'Listar los libros escaneados (entidad LIBRO) y filtrar por las áreas curriculares de la EBR.'],
    ['Tutor (lista de chats)', 'RF03, RF04', 'Retirar el selector «Gemini 3.5 Flash», que corresponde a un modelo en la nube (RNF01); añadir el micrófono; revisar los atajos (Tabla 3.9).'],
    ['Chat socrático', 'RF03, RF04, RF05', 'Resaltar los términos en quechua, añadir el botón «escuchar» en cada respuesta y el micrófono, e indicar la página del libro usada como contexto.'],
    ['Ajustes', 'RF04, RNF05', 'Retirar la sección «Cuenta» (correo y teléfono), pues no existe un servidor; conservar el perfil local, el tamaño de texto, la velocidad de voz y el nivel de ayuda.'],
]))
B.append(note('Elaboración propia. EBR = Educación Básica Regular. Las pantallas «Chats» y «Tutor» del prototipo se fusionan en una sola (ver Figura 3.8).'))

B.append(heading('Pantallas fuera del alcance'))
B.append(para('El prototipo incluye además un flujo para el docente y una pantalla para resolver exámenes que no se derivan del análisis de requisitos. La Tabla 3.7 detalla los motivos de su exclusión. El flujo docente se registra como trabajo futuro: requeriría un nuevo actor en el diagrama de casos de uso, requisitos adicionales y un mecanismo de sincronización entre dispositivos compatible con la operación sin conexión.'))
B.append(caption('Tabla 3.7', 'Pantallas del prototipo excluidas del alcance'))
B.append(table([3000, 6360], ['Pantalla', 'Motivo de la exclusión'], [
    ['Resolver examen', 'Contradice el enfoque socrático (RF03): el tutor guía con preguntas y no resuelve evaluaciones.'],
    ['Inicio del docente y alertas', 'El diagrama de casos de uso (Figura 3.2) define un único actor, el estudiante; ningún requisito funcional describe funciones para el docente.'],
    ['Biblioteca del docente y filtros por grado', 'Igual que la anterior; además, agrupa los libros por grados y secciones que el modelo de datos (Figura 3.5) no contempla.'],
    ['Estudiantes, ficha del estudiante, desempeño por curso y seguimiento', 'Requieren datos de varios estudiantes y dispositivos (sincronización), calificaciones y asignación de actividades, lo que es incompatible con la operación 100 % offline (RNF01) y con un modelo de datos de un solo usuario.'],
]))
B.append(note('Elaboración propia.'))

B.append(heading('Pantallas añadidas'))
B.append(para('A la inversa, algunos requisitos no tenían pantalla en el prototipo. La Tabla 3.8 lista las pantallas y los estados que se añaden para cubrirlos.'))
B.append(caption('Tabla 3.8', 'Pantallas y estados añadidos al prototipo'))
B.append(table([2700, 1500, 5160], ['Pantalla o estado', 'Requisitos', 'Propósito'], [
    ['Revisión del texto escaneado', 'RF01, RF02', 'Corregir errores del OCR, asignar el libro y la página, y confirmar el guardado.'],
    ['Progreso de indexación', 'RF02', 'Informar el avance de la fragmentación y la vectorización sin bloquear la interfaz.'],
    ['Detalle de libro', 'RF02, RF03', 'Consultar las páginas escaneadas e iniciar una conversación sobre ese libro.'],
    ['Preparación del tutor', 'RNF02, RNF03', 'Indicar la carga del modelo de lenguaje, que demora varios segundos.'],
    ['Grabación de voz', 'RF04', 'Mostrar que el micrófono está activo mientras Vosk transcribe la consulta.'],
    ['Primer uso, estados vacíos y de error', '—', 'Registrar el perfil local (nombre y grado) y orientar al estudiante cuando no haya libros o el OCR no detecte texto.'],
]))
B.append(note('Elaboración propia.'))

B.append(heading('Atajos del chat'))
B.append(para('El prototipo ofrece atajos que generan contenido terminado. Para mantener el enfoque socrático solo se conservan los que guían al estudiante, y se añade uno para el apoyo bilingüe (Tabla 3.9).'))
B.append(caption('Tabla 3.9', 'Atajos del chat socrático'))
B.append(table([2700, 2400, 4260], ['Atajo del prototipo', 'Decisión', 'Justificación'], [
    ['Explicar Simple', 'Se renombra «Explícame con preguntas»', 'Es el modo socrático por defecto (RF03).'],
    ['Quiz Rápido', 'Se conserva', 'Formula preguntas de comprobación sin entregar las respuestas.'],
    ['Resumir, Ideas Centrales y Extraer Citas', 'Se retiran', 'Entregan el producto terminado y no figuran en los requisitos.'],
    ['Palabras difíciles (nuevo)', 'Se añade', 'Lista los términos del pasaje con su equivalente en quechua (RF05).'],
]))
B.append(note('Elaboración propia.'))

B.append(heading('Navegación'))
B.append(para('La navegación principal se reduce a cuatro pestañas: Inicio, Biblioteca, Tutor y Ajustes. Las pestañas «Chats» y «Tutor» del prototipo se fusionan porque cumplían la misma función. El escaneo se inicia desde Inicio y conduce al flujo de ingesta documental (Figura 3.6). La Figura 3.8 muestra el mapa de navegación resultante.'))
B.append(caption('Figura 3.8', 'Mapa de navegación del aplicativo'))
B.append(figure(f'{IMG_DIR}/fig3_8_navegacion.png', 6.2, 102))
B.append(note('Elaboración propia. Las flechas indican las transiciones entre pantallas; las cuatro pestañas de la barra inferior son accesibles desde cualquier pantalla principal.'))

B.append(heading('Lineamientos visuales'))
B.append(para('Se conserva el estilo monocromo del prototipo, con tres ajustes de accesibilidad. Primero, el gris de los textos secundarios se oscurece a #5F5F5F, que sobre fondo blanco alcanza una relación de contraste de 6,4:1, por encima del mínimo de 4,5:1 que exige el nivel AA de las WCAG 2.1 (World Wide Web Consortium [W3C], 2018). Segundo, el texto principal pasa a 18 sp, pensando en estudiantes con dificultades de fluidez lectora. Tercero, se reserva un color de acento (#8A4B08, contraste 6,8:1) exclusivamente para resaltar los términos en quechua. La tipografía es la del sistema, porque las fuentes descargables requieren conexión a internet.'))

# 3.1.4 ---------------------------------------------------------------
B.append(heading('3.1.4 Implementación'))
B.append(heading('Entorno de desarrollo'))
B.append(para('El esqueleto del aplicativo se construyó en Android Studio con las versiones estables vigentes en octubre de 2026, que se detallan en la Tabla 3.10.'))
B.append(caption('Tabla 3.10', 'Herramientas y versiones del entorno de desarrollo'))
B.append(table([3300, 2100, 3960], ['Componente', 'Versión', 'Uso'], [
    ['Android Studio', '2026.1.3', 'Entorno de desarrollo integrado'],
    ['JDK (Eclipse Temurin)', '21', 'Compilación'],
    ['Gradle / Android Gradle Plugin', '9.5.0 / 9.3.3', 'Sistema de construcción'],
    ['Kotlin / KSP', '2.3.21 / 2.3.13', 'Lenguaje y procesamiento de anotaciones de Room'],
    ['Jetpack Compose (BOM) y Material 3', '2026.09.00', 'Interfaz de usuario'],
    ['Navigation Compose', '2.10.2', 'Navegación entre pantallas'],
    ['Lifecycle (ViewModel)', '2.11.0', 'Capa de presentación (MVVM)'],
    ['Room', '2.8.5', 'Base de datos local (Figura 3.5)'],
    ['Robolectric / Roborazzi', '4.17 / 1.77.0', 'Pruebas en la JVM y capturas de pantalla'],
]))
B.append(note('Elaboración propia. BOM = lista de materiales que fija versiones compatibles de las bibliotecas de Compose; KSP = Kotlin Symbol Processing.'))

B.append(heading('Configuración y requisitos no funcionales'))
B.append(para('Varias decisiones de configuración del proyecto responden directamente a los requisitos no funcionales, como se resume en la Tabla 3.11.'))
B.append(caption('Tabla 3.11', 'Decisiones de configuración y su relación con los requisitos'))
B.append(table([3500, 1300, 4560], ['Decisión', 'Requisito', 'Justificación'], [
    ['Versión mínima Android 9 (API 28)', 'RNF05', 'Compatibilidad con las tabletas escolares del MINEDU.'],
    ['Compilación contra la API 37', '—', 'Las bibliotecas de Jetpack vigentes lo exigen; no altera la versión mínima soportada.'],
    ['APK de release solo para arm64-v8a; la de depuración incluye x86_64', 'RNF05', 'Reduce el tamaño del instalador; x86_64 permite probar en el emulador del computador.'],
    ['Sin permiso INTERNET en el manifiesto', 'RNF01', 'Garantiza por diseño que ningún módulo acceda a la red.'],
    ['Inyección de dependencias manual, sin Hilt', 'RNF02', 'Evita bibliotecas y código generado que aumentan la memoria y el tiempo de arranque.'],
    ['Tipografía del sistema', 'RNF01', 'Las fuentes descargables requieren conexión.'],
]))
B.append(note('Elaboración propia. API = nivel de la interfaz de programación de Android; APK = paquete de instalación de Android.'))

B.append(heading('Estructura del proyecto'))
B.append(para('El código sigue las capas de la Figura 3.3 dentro del paquete *pe.edu.unmsm.yachaytext*. El subpaquete *data* contiene la base de datos de Room (*data.local*), el repositorio de conversaciones y la síntesis de voz (*data.voz*); *domain* contiene el glosario castellano–quechua y el contrato del tutor socrático, y *ui* contiene el tema visual, la navegación y las pantallas de Compose con sus ViewModel. El OCR, los embeddings, el reconocimiento de voz y el modelo de lenguaje se incorporarán en los Sprints 2 y 3.'))

B.append(heading('Base de datos local'))
B.append(para('Las cinco entidades del modelo entidad-relación (Figura 3.5) se implementaron en Room con sus claves foráneas (Android Developers, s. f.). Al eliminar un libro se eliminan en cascada sus fragmentos, mientras que las conversaciones asociadas conservan su historial y su referencia al libro queda en nulo. Respecto del diseño se introdujeron dos precisiones: las fechas se almacenan como enteros (milisegundos transcurridos desde 1970) en lugar de texto, lo que simplifica el ordenamiento y el filtrado, y los embeddings se guardan normalizados como arreglos de números de punto flotante de 32 bits, de modo que la similitud de coseno se reduce a un producto punto.'))

B.append(heading('Pantalla de chat socrático'))
B.append(para('La primera pantalla implementada es el chat socrático (Figura 3.9), por concentrar los requisitos RF03, RF04 y RF05. Incorpora los ajustes de la Tabla 3.6: las respuestas del tutor se muestran sin burbuja y las del estudiante en burbuja gris, cada respuesta tiene un botón «Escuchar» que usa el Text-to-Speech nativo de Android, el compositor incluye el micrófono y un indicador de funcionamiento sin internet en lugar del selector de modelo en la nube, y sobre él se ubican los atajos de la Tabla 3.9. Cuando el tutor recupera contexto de un libro, se indica el título y la página de origen.'))
B.append(caption('Figura 3.9', 'Pantalla de chat socrático implementada en Jetpack Compose'))
B.append(figure(f'{IMG_DIR}/fig3_9_chat.png', 5.6, 103))
B.append(note('Elaboración propia. Capturas generadas con Roborazzi en el perfil de una tableta de 8 pulgadas. El diálogo reproduce el ejemplo del prototipo (Figura 3.7).'))
B.append(para('Los términos en quechua se identifican con un glosario curado y no con el modelo de lenguaje, porque un modelo de 1000 millones de parámetros no traduce el quechua con fiabilidad (Tabla 3.5). El glosario reconoce las formas de cada término en el texto, las resalta con el color de acento y muestra su equivalente en la variante elegida (Tabla 3.12). La versión inicial cubre el vocabulario del ejemplo de fotosíntesis y debe ser validada y ampliada con un docente bilingüe y con los vocabularios pedagógicos del MINEDU.'))
B.append(caption('Tabla 3.12', 'Glosario inicial castellano–quechua'))
B.append(table([2400, 2600, 2180, 2180], ['Castellano', 'Formas reconocidas', 'Quechua collao', 'Quechua chanka'], [
    ['sol', 'sol', 'inti', 'inti'],
    ['agua', 'agua, aguas', 'unu', 'yaku'],
    ['tierra', 'tierra', 'allpa', 'allpa'],
    ['raíz', 'raíz, raíces', 'saphi', 'sapi'],
]))
B.append(note('Elaboración propia. Entradas pendientes de validación por un docente de Educación Intercultural Bilingüe. La variante chanka no emplea consonantes aspiradas, de ahí la diferencia entre *saphi* y *sapi*.'))
B.append(para('Mientras se integra el modelo de lenguaje (Sprint 3), el tutor se implementa con una clase de demostración que reproduce el diálogo del prototipo y responde con preguntas genéricas fuera de él. Ambas implementaciones comparten la misma interfaz, por lo que el reemplazo no afectará a la pantalla ni a la persistencia de los mensajes en Room.'))

B.append(heading('Pruebas'))
B.append(para('Las pruebas automatizadas se ejecutan en la máquina virtual de Java mediante Robolectric (Robolectric, s. f.), sin necesidad de emulador. Además, Roborazzi (Menju, s. f.) genera capturas de las pantallas en formato PNG con el perfil de una tableta de 8 pulgadas, lo que permite revisar la interfaz en cada iteración. La Tabla 3.13 resume las 15 pruebas implementadas, todas con resultado satisfactorio.'))
B.append(caption('Tabla 3.13', 'Pruebas automatizadas implementadas'))
B.append(table([3600, 4160, 1600], ['Prueba', 'Qué verifica', 'Requisito'], [
    ['Persistencia del embedding', 'El vector guardado en Room se recupera sin pérdida.', 'RF02'],
    ['Eliminación de un libro', 'La integridad referencial entre libros, fragmentos y conversaciones.', 'RF02'],
    ['Orden de los mensajes', 'El orden cronológico del historial y el registro de las consultas por voz.', 'RF03, RF04'],
    ['Similitud de coseno', 'El cálculo de la similitud entre vectores normalizados.', 'RNF04'],
    ['Glosario (3 pruebas)', 'La detección de términos y su posición, que no se confundan con partes de otras palabras («soleado») y la variante elegida.', 'RF05'],
    ['Flujo del chat', 'De extremo a extremo: crear una consulta, enviar una pregunta y recibir la respuesta del tutor guardada en Room.', 'RF03'],
    ['Capturas de pantalla (7)', 'El renderizado de la navegación, la lista del tutor y los estados del chat (conversación, pensando, dictado, texto escrito y teléfono).', '—'],
]))
B.append(note('Elaboración propia.'))

# Referencias ----------------------------------------------------------
B.append(heading('Referencias'))
B.append(reference('Android Developers. (s. f.). *Save data in a local database using Room*. Recuperado el 31 de octubre de 2026, de https://developer.android.com/training/data-storage/room'))
B.append(reference('Menju, T. (s. f.). *Roborazzi* [Software]. GitHub. Recuperado el 31 de octubre de 2026, de https://github.com/takahirom/roborazzi'))
B.append(reference('Robolectric. (s. f.). *Robolectric*. Recuperado el 31 de octubre de 2026, de https://robolectric.org/'))
B.append(reference('World Wide Web Consortium. (2018). *Web Content Accessibility Guidelines (WCAG) 2.1*. https://www.w3.org/TR/WCAG21/'))

# ------------------------------------------------------------------ ensamblado
zin = zipfile.ZipFile(TEMPLATE)
doc = zin.read('word/document.xml').decode('utf8')
body_start = doc.index('<w:body>') + len('<w:body>')
blocks = re.findall(r'<w:p[ >].*?</w:p>|<w:tbl>.*?</w:tbl>', doc[body_start:], re.S)
cover_end_marker = blocks[24]  # párrafo «2026» con el salto de página
cover_end = doc.index(cover_end_marker, body_start) + len(cover_end_marker)
sect = re.search(r'<w:sectPr>.*?</w:sectPr>', doc[cover_end:], re.S).group(0)
new_doc = doc[:cover_end] + ''.join(B) + sect + '</w:body></w:document>'


def clean(xml):
    # Google Docs exporta medidas con decimales que el esquema OOXML no admite.
    xml = re.sub(r'(w:[A-Za-z]+)="(-?\d+)\.\d+"', r'\1="\2"', xml)
    return re.sub(r'<w:pgMar (?![^>]*w:gutter)', '<w:pgMar w:gutter="0" ', xml)


new_doc = clean(new_doc)

rels = zin.read('word/_rels/document.xml.rels').decode('utf8')
# Conserva solo la imagen de la portada (rId6); quita las figuras del 2.º entregable.
rels = re.sub(r'<Relationship Id="rId(7|8|9|10|11)"[^>]*/>', '', rels)
rels = rels.replace('</Relationships>', ''.join(
    f'<Relationship Id="{rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/{name}"/>'
    for rid, name, _ in IMAGES) + '</Relationships>')

with zipfile.ZipFile(OUT, 'w', zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        if item.filename.startswith('word/media/') and item.filename != 'word/media/image1.png':
            continue
        if item.filename == 'word/document.xml':
            zout.writestr(item, new_doc)
        elif item.filename == 'word/_rels/document.xml.rels':
            zout.writestr(item, rels)
        elif item.filename == 'word/styles.xml':
            zout.writestr(item, clean(zin.read(item.filename).decode('utf8')))
        else:
            zout.writestr(item, zin.read(item.filename))
    for _, name, data in IMAGES:
        zout.writestr(f'word/media/{name}', data)
print('ok', OUT)
