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
B.append(para('Se conserva el estilo monocromo del prototipo, con tres ajustes de accesibilidad. Primero, el gris de los textos secundarios se oscurece a #5F5F5F, que sobre fondo blanco alcanza una relación de contraste de 6,4:1, por encima del mínimo de 4,5:1 que exige el nivel AA de las WCAG 2.1 (World Wide Web Consortium [W3C], 2018). Segundo, el texto principal pasa a 18 sp, pensando en estudiantes con dificultades de fluidez lectora. Tercero, se reserva un color de acento (#8A4B08, contraste 6,8:1) exclusivamente para resaltar los términos en quechua. La tipografía es la del sistema, porque las fuentes descargables requieren conexión a internet. Al inicio del Sprint 3, este estilo se reemplazó por una identidad visual propia, que se describe en el apartado «Identidad visual».'))

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
    ['CameraX', '1.6.2', 'Vista previa y captura de la cámara (RF01)'],
    ['ML Kit Text Recognition v2 (modelo incluido)', '16.0.1', 'OCR sin conexión (RF01)'],
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
    ['Eliminar del manifiesto final los permisos de red que agrega ML Kit', 'RNF01', 'ML Kit los declara para enviar estadísticas de uso; sin ellos el OCR funciona igual y la app sigue sin acceso a la red.'],
]))
B.append(note('Elaboración propia. API = nivel de la interfaz de programación de Android; APK = paquete de instalación de Android.'))

B.append(heading('Estructura del proyecto'))
B.append(para('El código sigue las capas de la Figura 3.3 dentro del paquete *pe.edu.unmsm.yachaytext*. El subpaquete *data* contiene la base de datos de Room (*data.local*), los repositorios de conversaciones, libros y perfil local, y la síntesis de voz (*data.voz*); *domain* contiene el glosario castellano–quechua, el contrato del tutor socrático y el caso de uso que inicia una consulta, y *ui* contiene el tema visual, la navegación y las pantallas de Compose con sus ViewModel. El OCR, los embeddings, el reconocimiento de voz y el modelo de lenguaje se incorporarán en los Sprints 2 y 3.'))

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
B.append(para('Mientras se integra el modelo de lenguaje (Sprint 3), el tutor se implementa con una clase de demostración que, sin libros escaneados, reproduce el diálogo del prototipo y, con libros, responde a partir del contexto recuperado (ver «Recuperación de contexto en el chat»). Ambas implementaciones comparten la misma interfaz, por lo que el reemplazo no afectará a la pantalla ni a la persistencia de los mensajes en Room.'))

B.append(heading('Pantallas de Inicio y Biblioteca'))
B.append(para('La pantalla de Inicio reemplaza los textos de relleno del prototipo por datos reales de Room: las tres conversaciones y los tres libros más recientes, con su antigüedad expresada en términos relativos («Hace 2 horas», «Ayer»). El campo «¿Tienes alguna pregunta?» abre una consulta nueva y el botón de cámara inicia el escaneo. La cabecera se redujo a la mitad de la altura del prototipo para dar espacio al contenido, y saluda al estudiante por su nombre cuando el perfil local lo registra.'))
B.append(para('La Biblioteca lista los libros escaneados con un buscador que ignora tildes y mayúsculas, y con filtros por área curricular que solo muestran las áreas que tienen libros, para no ofrecer filtros vacíos. Ambas pantallas incluyen estados vacíos que orientan al estudiante hacia la acción siguiente (Figura 3.10).'))
B.append(caption('Figura 3.10', 'Pantallas de Inicio y Biblioteca implementadas'))
B.append(figure(f'{IMG_DIR}/fig3_10_inicio_biblioteca.png', 6.2, 104))
B.append(note('Elaboración propia. Capturas generadas con Roborazzi con datos de ejemplo.'))
B.append(para('En el Sprint 2, mientras no existía la pantalla Detalle de libro (Tabla 3.8), tocar un libro abría directamente una consulta ligada a él; en el Sprint 3 se implementó el detalle, como se describe en el apartado «Evaluación heurística».'))

B.append(heading('Escaneo e ingesta documental'))
B.append(para('El flujo de ingesta documental de la Figura 3.6 se implementó en dos pantallas (Figura 3.11). La pantalla de escaneo muestra la vista previa de CameraX con un marco guía de proporción 3:4 que oscurece el exterior, un botón de linterna y un disparador; si el estudiante no concedió el permiso de cámara, explica para qué se usa antes de pedirlo. Al tomar la foto, ML Kit extrae el texto con el modelo incluido en el APK, sin descargar nada en el primer uso. Si no detecta texto, se pide acercar la página o buscar más luz.'))
B.append(para('La pantalla de revisión permite corregir el texto reconocido, elegir el libro (por defecto, el último usado) o crear uno nuevo con su área curricular, e indicar el número de página. Al guardar, el texto se fragmenta, se vectoriza y se almacena en Room, mostrando el avance; al terminar, se ofrece preguntar al tutor sobre el libro, escanear otra página o ir a la biblioteca. Escanear de nuevo una página reemplaza sus fragmentos anteriores. La Tabla 3.14 resume los parámetros de la ingesta.'))
B.append(caption('Figura 3.11', 'Pantallas de escaneo y revisión del texto'))
B.append(figure(f'{IMG_DIR}/fig3_11_escaneo.png', 6.2, 105))
B.append(note('Elaboración propia. Capturas generadas con Roborazzi; en la primera, un fondo uniforme reemplaza la imagen de la cámara.'))
B.append(caption('Tabla 3.14', 'Parámetros de la ingesta documental'))
B.append(table([3200, 2300, 3860], ['Parámetro', 'Valor', 'Justificación'], [
    ['Tamaño máximo del fragmento', '500 caracteres', 'Unas 3 a 5 oraciones: suficiente contexto sin saturar la ventana del modelo de lenguaje.'],
    ['Solapamiento', '1 oración', 'Evita perder el sentido en los cortes entre fragmentos.'],
    ['Limpieza del OCR', 'Une líneas y palabras cortadas con guion', 'El OCR devuelve cada renglón por separado y corta palabras al final de línea.'],
    ['Dimensión del vector', '512', 'Vectorizador léxico provisional; el definitivo dependerá del modelo de embeddings.'],
    ['Tiempo máximo de captura', '12 segundos', 'Si la cámara no entrega la foto, se avisa en lugar de dejar la pantalla en espera.'],
]))
B.append(note('Elaboración propia.'))
B.append(para('La vectorización usa por ahora un vectorizador léxico: convierte cada palabra (sin tildes, en minúsculas y sin plurales) y cada par de palabras consecutivas en posiciones de un vector de 512 dimensiones mediante una función hash, y normaliza el resultado. No requiere modelo, ocupa memoria despreciable y recupera los fragmentos que comparten vocabulario con la pregunta. Su limitación es que no reconoce sinónimos; por ello se reemplazará por el Text Embedder de MediaPipe con un modelo multilingüe, ya que los modelos que MediaPipe ofrece por defecto están entrenados en inglés. El reemplazo solo exige otra implementación de la misma interfaz.'))

B.append(heading('Recuperación de contexto en el chat (RAG)'))
B.append(para('Con los libros ya indexados, el chat completa el flujo socrático RAG de la Figura 3.6. Antes de responder, el buscador vectoriza la pregunta y la compara por similitud de coseno con los fragmentos almacenados: si la conversación se abrió desde un libro, solo con los de ese libro; si no, con toda la biblioteca. Se conservan los tres fragmentos más parecidos cuyo coseno supera 0,12, umbral por debajo del cual se considera que el fragmento no se relaciona con la pregunta. Los vectores se mantienen en memoria y solo se recargan cuando cambian los fragmentos, de modo que la búsqueda en 3000 fragmentos (unas 150 páginas) tomó 3 ms en promedio en el equipo de desarrollo, muy por debajo del umbral de 150 ms del RNF04; el valor en las tabletas se medirá en el Sprint 4.'))
B.append(para('Los fragmentos usados se registran en la tabla CONTEXTO_RECUPERADO con su puntaje, lo que permite mostrar bajo cada respuesta el libro y la página de origen (Figura 3.12) y, más adelante, evaluar la calidad de la recuperación. Mientras se integra el modelo de lenguaje, el tutor de demostración usa ese contexto sin entregar la respuesta: cita la oración de la página que comparte más palabras con la pregunta y pide al estudiante que la interprete. Las consultas abiertas desde un libro saludan con su título, y las consultas libres toman como título la primera pregunta del estudiante.'))
B.append(caption('Figura 3.12', 'Respuesta del tutor con contexto recuperado del libro escaneado'))
B.append(figure(f'{IMG_DIR}/fig3_12_rag.png', 3.6, 106))
B.append(note('Elaboración propia. Captura generada por la prueba de extremo a extremo: se guarda una página, se abre la consulta desde la biblioteca y se pregunta «¿Qué absorben las raíces?».'))

B.append(heading('Ajustes y primer uso'))
B.append(para('La primera vez que se abre la aplicación, una pantalla de bienvenida presenta a Yachay y pide el nombre y el grado del estudiante; puede omitirse y completarse después. Estos datos forman un perfil local, sin cuentas ni servidor, y reemplazan la sección «Cuenta» (correo y teléfono) del prototipo. La pantalla de Ajustes agrupa las preferencias de la Tabla 3.15; cada una tiene un efecto inmediato en la aplicación (Figura 3.13). Las opciones «Apariencia» y «Color» del prototipo se postergan, porque el tema oscuro no forma parte de los requisitos.'))
B.append(caption('Tabla 3.15', 'Preferencias del estudiante y su efecto en la aplicación'))
B.append(table([2500, 2700, 3060, 1100], ['Preferencia', 'Opciones', 'Efecto', 'Requisito'], [
    ['Nombre y grado', 'Texto libre; 1.º a 6.º de primaria y 1.º a 5.º de secundaria', 'Saludo personalizado en Inicio.', '—'],
    ['Variante del quechua', 'Collao o chanka', 'Traducción mostrada para los términos del glosario (por ejemplo, agua → unu o yaku).', 'RF05'],
    ['Tamaño de texto', 'Normal, grande (115 %) o muy grande (130 %)', 'Escala todo el texto de la aplicación.', 'RF04'],
    ['Velocidad de voz', 'Lenta, normal o rápida', 'Velocidad del Text-to-Speech al escuchar las respuestas; incluye un botón para probarla.', 'RF04'],
    ['Nivel de ayuda', 'Más pistas, normal o menos pistas', 'Cuánto revela el tutor: la cita con la palabra clave, la cita con una pregunta, o solo la página donde buscar.', 'RF03'],
]))
B.append(note('Elaboración propia. Las preferencias se guardan únicamente en el dispositivo.'))
B.append(caption('Figura 3.13', 'Pantallas de Ajustes y de bienvenida'))
B.append(figure(f'{IMG_DIR}/fig3_13_ajustes.png', 6.2, 107))
B.append(note('Elaboración propia. Capturas generadas con Roborazzi. La segunda muestra los Ajustes con el tamaño de texto «muy grande».'))

B.append(heading('Identidad visual'))
B.append(para('Las pantallas del Sprint 2 conservaban el estilo monocromo del prototipo, que funcionaba como boceto pero resultaba frío para estudiantes de primaria y no tenía elementos reconocibles. Al inicio del Sprint 3 se definió una identidad visual con tres decisiones. Primero, una paleta inspirada en el paisaje andino: el verde de los andenes para las acciones principales y la selección, el amarillo del sol solo como decoración, un fondo de tono papel y un tono tierra reservado para los términos en quechua, de modo que no se confundan con las acciones (Tabla 3.16). Todos los pares de texto y fondo superan la relación de contraste de 4,5:1 del nivel AA de las WCAG 2.1 (W3C, 2018).'))
B.append(para('Segundo, la tipografía Lexend, diseñada con el propósito de facilitar la lectura (Lexend, s. f.) y distribuida bajo la licencia SIL Open Font License. Se empaqueta dentro del APK en tres pesos (unos 230 KB en total), porque las fuentes descargables requieren conexión a internet (RNF01). Tercero, un logo que representa un libro abierto con el sol saliendo detrás, en referencia a «yachay» (saber, aprender); el mismo dibujo se usa como ícono adaptativo de la aplicación, con una versión monocroma para los íconos temáticos de Android, y en la pantalla de bienvenida. La portada de Inicio muestra un paisaje con sol, cerros y andenes que se dibuja con código, por lo que no añade imágenes al APK y se adapta a cualquier ancho de pantalla (Figura 3.14).'))
B.append(caption('Tabla 3.16', 'Paleta de colores de la aplicación'))
B.append(table([2000, 1300, 3860, 2200], ['Color', 'Código', 'Uso', 'Contraste'], [
    ['Verde andén', '#1E6B52', 'Botones principales, chips y pestaña seleccionados, logo.', '6,4:1 con texto blanco'],
    ['Verde suave', '#E1F0E8', 'Fondo de íconos de libros y estados vacíos.', '5,4:1 con verde andén'],
    ['Sol', '#F2B544', 'Solo decorativo (sol del logo y de la portada); nunca lleva texto.', '—'],
    ['Papel', '#FAF7F2', 'Fondo de las pantallas; las tarjetas son blancas.', '15,7:1 con el texto principal'],
    ['Tinta', '#1F1D1A', 'Texto principal e íconos.', '15,7:1 sobre papel'],
    ['Gris cálido', '#5C574F', 'Texto secundario (fechas, descripciones).', '6,7:1 sobre papel'],
    ['Tierra', '#8A4B08', 'Términos en quechua, sobre fondo #FFF1E0.', '6,1:1'],
]))
B.append(note('Elaboración propia. Contrastes calculados con la fórmula de luminancia relativa de las WCAG 2.1.'))
B.append(caption('Figura 3.14', 'Pantalla de Inicio antes y después de la identidad visual'))
B.append(figure(f'{IMG_DIR}/fig3_14_identidad.png', 6.2, 108))
B.append(note('Elaboración propia. Capturas generadas con Roborazzi. Las demás figuras de este documento ya muestran la identidad visual.'))

B.append(heading('Evaluación heurística'))
B.append(para('Con el flujo del estudiante completo, se realizó una evaluación heurística experta de la interfaz con las diez heurísticas de usabilidad de Nielsen (1994a), clasificando cada problema con su escala de severidad de 0 (no es un problema) a 4 (catástrofe de usabilidad) (Nielsen, 1994b). Se revisaron las capturas de todas las pantallas y estados, el código de cada pantalla y los 38 íconos usados, y se contrastaron con las pautas de Material Design 3 (Google, s. f.). No se hallaron problemas de severidad 4. La Tabla 3.17 resume los problemas y su corrección; las heurísticas 7 (flexibilidad y eficiencia) y 9 (recuperación de errores) se cumplían, gracias a los atajos del chat, el dictado y los mensajes de error que explican la causa y la solución.'))
B.append(caption('Tabla 3.17', 'Problemas de la evaluación heurística y su corrección'))
B.append(table([2300, 3000, 700, 3360], ['Heurística', 'Problema', 'Sev.', 'Corrección'], [
    ['3. Control y libertad', 'No se podían borrar ni renombrar libros, páginas ni conversaciones.', '3', 'Menú «Más opciones» con «Cambiar nombre» y «Borrar»; las conversaciones borradas se pueden recuperar con «Deshacer».'],
    ['3. Control y libertad', 'Volver desde la revisión descartaba el texto corregido sin preguntar.', '3', 'Diálogo «¿Descartar esta página?» con la opción «Seguir revisando».'],
    ['5. Prevención de errores', 'Volver a escanear una página la reemplazaba sin avisar.', '3', 'Aviso «La página 42 ya está guardada…» y botón «Reemplazar página 42».'],
    ['1. Visibilidad del estado', 'El aviso «sin internet» usaba el ícono de wifi tachado, que se lee como un error.', '2', 'Ícono de disponible sin conexión y texto «Funciona sin internet».'],
    ['2. Relación con el mundo real', 'Jerga interna: el texto se dividía en «partes» (fragmentos del RAG).', '2', 'Mensajes sin tecnicismos: «Preparando la página para Yachay…».'],
    ['4. Consistencia y estándares', 'Tocar un libro abría un chat en lugar de mostrar el libro.', '2', 'Nueva pantalla Detalle de libro (Figura 3.15).'],
    ['6. Reconocer antes que recordar', 'El botón de escanear en Inicio solo tenía un ícono.', '2', 'Botón «Escanear» con texto; la otra acción pasa a «Pregúntale a Yachay».'],
    ['6. Reconocer antes que recordar', 'La cita «pág. 42» no permitía ver la página usada.', '2', 'Tocar la cita muestra el texto completo de la página.'],
    ['6. Reconocer antes que recordar', 'Al iniciar una pregunta había que tocar otra vez la caja de texto.', '2', 'En una conversación nueva, el teclado se abre solo.'],
    ['8. Estética y minimalismo', 'El botón «Escuchar» con borde se repetía en cada mensaje.', '2', 'Acciones del mensaje sin borde, en el color de marca, junto a la cita.'],
    ['10. Ayuda y documentación', 'Nada explicaba el flujo de uso más allá de una frase.', '2', 'Guía de tres pasos en la bienvenida y en Ajustes («Cómo usar Yachay»).'],
    ['2. Relación con el mundo real', '«Quiz rápido» y «Nueva consulta» no son lenguaje de un niño.', '1', '«Ponme a prueba» y «Nueva pregunta».'],
    ['4. Consistencia y estándares', 'Las conversaciones se veían distinto en Inicio y en Tutor.', '1', 'Una sola tarjeta con el ícono del área, el libro y la antigüedad.'],
    ['8. Estética y minimalismo', 'Los 11 grados ocupaban media pantalla.', '1', 'Selección en dos pasos: nivel (2 opciones) y grado (5 o 6).'],
]))
B.append(note('Elaboración propia. Sev. = severidad según Nielsen (1994b): 1, problema estético; 2, problema menor; 3, problema mayor.'))

B.append(para('La iconografía se revisó para que cada ícono represente una sola metáfora (Tabla 3.18). El cambio principal es que el concepto «libro» tenía tres íconos distintos; ahora la pila de libros representa la biblioteca y cada libro muestra el ícono de su área curricular, lo que permite reconocerlo de un vistazo.'))
B.append(caption('Tabla 3.18', 'Revisión de la iconografía'))
B.append(table([2300, 2200, 2200, 2660], ['Concepto', 'Antes', 'Después', 'Motivo'], [
    ['Pestaña Biblioteca', 'Persona leyendo', 'Pila de libros', 'A 24 dp se confundía con un ícono de usuario.'],
    ['Libro', 'El mismo libro abierto en todos', 'Ícono del área curricular (por ejemplo, calculadora para Matemática y matraz para Ciencia y Tecnología)', 'El ícono aporta información y diferencia los libros.'],
    ['Antigüedad («Hace 2 horas»)', 'Calendario', 'Reloj', 'El calendario representa una fecha, no un tiempo transcurrido.'],
    ['«Explícame con preguntas»', 'Signo de interrogación', 'Globos de pregunta y respuesta', 'El «?» es el estándar de ayuda; el diálogo representa el método socrático.'],
    ['«Palabras difíciles»', 'Traducir (文A)', 'Letras «ABC»', 'El ícono de traducir prometía una traducción.'],
    ['Funciona sin internet', 'Wifi tachado', 'Disponible sin conexión', 'Comunica una ventaja, no un fallo.'],
]))
B.append(note('Elaboración propia. Íconos de Material Symbols (Google, s. f.).'))

B.append(para('En la distribución del contenido se reforzó la jerarquía visual: los títulos de sección pasan a 22 sp en seminegrita, por encima de los títulos de las tarjetas (18 sp), y se marcan como encabezados para que TalkBack permita navegar por secciones; el texto secundario sube de 13 a 14 sp. La pantalla de revisión mantiene el botón «Guardar página» fijo en la parte inferior, porque en la tableta quedaba fuera de la vista. Además, las pantallas se adaptan al ancho disponible según las clases de tamaño de ventana de Material Design (Google, s. f.): la Biblioteca y el detalle de libro usan una cuadrícula que pasa de una columna en el teléfono a dos en la tableta vertical y tres en horizontal, y la revisión se divide en dos columnas (texto y datos de la página) a partir de 840 dp de ancho (Figura 3.16). El chat y los formularios conservan un ancho máximo de lectura de 720 dp.'))
B.append(para('La nueva pantalla Detalle de libro muestra el área, el número de páginas y la fecha, las acciones «Preguntar sobre este libro» y «Escanear página» (que propone ese libro al guardar) y la lista de páginas escaneadas; al tocar una, se lee su texto completo y se puede borrar (Figura 3.15). Como el texto se guarda dividido en fragmentos que repiten una oración, la página se reconstruye uniendo los fragmentos sin duplicar la oración compartida.'))
B.append(caption('Figura 3.15', 'Detalle de libro, aviso de reemplazo y lista de conversaciones'))
B.append(figure(f'{IMG_DIR}/fig3_15_heuristica.png', 6.2, 109))
B.append(note('Elaboración propia. Capturas generadas con Roborazzi.'))
B.append(caption('Figura 3.16', 'Biblioteca y revisión en una tableta en horizontal'))
B.append(figure(f'{IMG_DIR}/fig3_16_horizontal.png', 6.2, 110))
B.append(note('Elaboración propia. Capturas generadas con Roborazzi con un ancho de 960 dp.'))
B.append(para('Una evaluación experta no reemplaza la observación de usuarios reales. Por ello, en el Sprint 4 se realizará una prueba de usabilidad con cinco estudiantes, número con el que suele detectarse la mayor parte de los problemas de usabilidad (Nielsen & Landauer, 1993), con las tareas de la Tabla 3.19.'))
B.append(caption('Tabla 3.19', 'Tareas de la prueba de usabilidad planificada'))
B.append(table([800, 5000, 3560], ['N.º', 'Tarea', 'Criterio de éxito'], [
    ['1', 'Escanear una página del libro de Ciencia y Tecnología y guardarla.', 'La página aparece en el detalle del libro sin ayuda.'],
    ['2', 'Preguntar a Yachay qué necesitan las plantas para fabricar su alimento.', 'Envía la pregunta y lee o escucha la respuesta.'],
    ['3', 'Encontrar en qué página del libro está la respuesta.', 'Toca la cita y lee la página.'],
    ['4', 'Ver cómo se dice «agua» en quechua chanka.', 'Cambia la variante en Ajustes y la reconoce en el chat.'],
    ['5', 'Borrar una conversación y recuperarla.', 'Usa «Borrar» y luego «Deshacer».'],
]))
B.append(note('Elaboración propia. Se medirán la tasa de éxito, el tiempo por tarea y los comentarios de los estudiantes.'))

B.append(heading('Verificación en emulador'))
B.append(para('Además de las pruebas en la JVM, el flujo se verificó en un emulador con el perfil del Samsung Galaxy A05 (Android 13, 2 GB de RAM), con el Wi-Fi y los datos móviles desactivados. Una prueba instrumentada dibuja una página con texto en castellano y confirma que ML Kit la lee sin conexión. La prueba manual de la cámara reveló dos problemas que se corrigieron. Primero, la vista previa podía quedar en negro porque PreviewView usa por defecto un SurfaceView, que se dibuja detrás de la ventana y queda tapado por el fondo de la pantalla; se cambió al modo compatible (TextureView). Segundo, la cámara virtual del emulador entrega fotogramas con marcas de tiempo inválidas y la captura nunca termina; aunque es un defecto del emulador, evidenció que la pantalla no daba respuesta al tocar el disparador, por lo que se añadió el mensaje «Tomando la foto…» y el tiempo máximo de la Tabla 3.14. Con la cámara emulada estándar el flujo completo funcionó: captura, OCR y aviso de texto no encontrado. Queda pendiente la prueba con una página real en un dispositivo físico.'))

B.append(heading('Pruebas'))
B.append(para('Las pruebas automatizadas se ejecutan en la máquina virtual de Java mediante Robolectric (Robolectric, s. f.), sin necesidad de emulador. Además, Roborazzi (Menju, s. f.) genera capturas de las pantallas en formato PNG con el perfil de una tableta de 8 pulgadas, lo que permite revisar la interfaz en cada iteración. La Tabla 3.13 resume las 80 pruebas implementadas (79 en la JVM y una instrumentada en el emulador), todas con resultado satisfactorio.'))
B.append(caption('Tabla 3.13', 'Pruebas automatizadas implementadas'))
B.append(table([3600, 4160, 1600], ['Prueba', 'Qué verifica', 'Requisito'], [
    ['Persistencia del embedding', 'El vector guardado en Room se recupera sin pérdida.', 'RF02'],
    ['Eliminación de un libro', 'La integridad referencial entre libros, fragmentos y conversaciones.', 'RF02'],
    ['Orden de los mensajes', 'El orden cronológico del historial y el registro de las consultas por voz.', 'RF03, RF04'],
    ['Similitud de coseno', 'El cálculo de la similitud entre vectores normalizados.', 'RNF04'],
    ['Glosario (3 pruebas)', 'La detección de términos y su posición, que no se confundan con partes de otras palabras («soleado») y la variante elegida.', 'RF05'],
    ['Flujo del chat', 'De extremo a extremo: crear una consulta, enviar una pregunta y recibir la respuesta del tutor guardada en Room.', 'RF03'],
    ['Búsqueda y filtros de la biblioteca (3 pruebas)', 'La búsqueda sin tildes ni mayúsculas, la combinación de búsqueda y área, y que solo se ofrezcan áreas con libros.', 'RF02'],
    ['Tiempo relativo', 'Los textos «Hace un momento», «Hace 5 min», «Ayer», «Hace 4 días» y la fecha para antigüedades mayores.', '—'],
    ['Fragmentador (5 pruebas)', 'La unión de líneas y guiones del OCR, el tamaño máximo, el solapamiento sin pérdida de oraciones y el corte de oraciones muy largas.', 'RF02'],
    ['Vectorizador léxico (3 pruebas)', 'La norma unitaria, la indiferencia a tildes, mayúsculas y plurales, y que la pregunta recupere el fragmento que la responde.', 'RF02, RNF04'],
    ['Ingesta de una página (2 pruebas)', 'La creación del libro con sus fragmentos y el conteo de páginas, y el reemplazo al volver a escanear una página.', 'RF02'],
    ['Pantalla de escaneo (3 pruebas)', 'El aviso tras el tiempo máximo de captura, el aviso de foto sin texto y el paso a la revisión cuando hay texto.', 'RF01'],
    ['Buscador de fragmentos (4 pruebas)', 'Que el primer resultado responda la pregunta, el filtro por libro, el descarte de lo no relacionado, la actualización al reescanear y una búsqueda en 3000 fragmentos en ≤ 150 ms.', 'RF02, RNF04'],
    ['Tutor de demostración (5 pruebas)', 'La cita de la oración más relacionada con la página, la respuesta con pregunta, lo que revela según el nivel de ayuda, el guion sin contexto y el saludo según el tema.', 'RF03'],
    ['Preferencias (3 pruebas)', 'Los valores por defecto, que todas las preferencias se guarden y se lean, y que un nombre vacío no se guarde.', '—'],
    ['Flujo de bienvenida', 'De extremo a extremo: el nombre del primer uso aparece en Inicio y en Ajustes, y el cambio de variante se guarda.', 'RF05'],
    ['Formato del chat (2 pruebas)', 'El texto de la fuente («libro · pág. N» o «págs. N y M») y el título tomado de la primera pregunta.', 'RF03'],
    ['Flujo RAG', 'De extremo a extremo: con una página guardada, se abre el libro desde la biblioteca, se pregunta, la respuesta cita la página y la cita abre su texto completo.', 'RF02, RF03'],
    ['Reconstrucción de páginas (2 pruebas)', 'Que al unir los fragmentos se recupere el texto original sin repetir la oración compartida.', 'RF02'],
    ['Gestión de datos (3 pruebas)', 'Borrar una página actualiza el conteo; renombrar y borrar un libro conserva sus conversaciones; «Deshacer» restaura una conversación con sus mensajes y fuentes.', 'RF02, RF03'],
    ['Aviso de reemplazo', 'Que el aviso aparezca solo si la página ya existe en el libro elegido.', 'RF02'],
    ['OCR sin conexión (instrumentada)', 'En el emulador sin red, ML Kit lee una página con texto en castellano.', 'RF01, RNF01'],
    ['Capturas de pantalla (32)', 'El renderizado de la navegación, Inicio, Biblioteca, la lista del tutor, los estados del chat (incluida la variante chanka), el escaneo, la revisión (incluido el aviso de reemplazo), el detalle de libro, los Ajustes (normal y con texto muy grande), la bienvenida y tres pantallas en tableta horizontal.', '—'],
]))
B.append(note('Elaboración propia.'))

# Referencias ----------------------------------------------------------
B.append(heading('Referencias'))
B.append(reference('Android Developers. (s. f.). *Save data in a local database using Room*. Recuperado el 31 de octubre de 2026, de https://developer.android.com/training/data-storage/room'))
B.append(reference('Google. (s. f.). *Material Design 3*. Recuperado el 12 de noviembre de 2026, de https://m3.material.io/'))
B.append(reference('Lexend. (s. f.). *Lexend: A variable font designed to improve reading proficiency*. Recuperado el 11 de noviembre de 2026, de https://www.lexend.com/'))
B.append(reference('Menju, T. (s. f.). *Roborazzi* [Software]. GitHub. Recuperado el 31 de octubre de 2026, de https://github.com/takahirom/roborazzi'))
B.append(reference('Nielsen, J. (1994a). Enhancing the explanatory power of usability heuristics. En *Proceedings of the SIGCHI Conference on Human Factors in Computing Systems* (pp. 152–158). ACM. https://doi.org/10.1145/191666.191729'))
B.append(reference('Nielsen, J. (1994b). *Severity ratings for usability problems*. Nielsen Norman Group. https://www.nngroup.com/articles/how-to-rate-the-severity-of-usability-problems/'))
B.append(reference('Nielsen, J., & Landauer, T. K. (1993). A mathematical model of the finding of usability problems. En *Proceedings of the INTERACT ’93 and CHI ’93 Conference on Human Factors in Computing Systems* (pp. 206–213). ACM. https://doi.org/10.1145/169059.169166'))
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
