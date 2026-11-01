# Alcance de pantallas: entregables vs. Figma

Revisión del 31/10/2026. Compara lo comprometido en el 1.er y 2.º entregable con los bocetos de Figma (`cM6TosPwN83O3feuSkQNFS`) para decidir qué se implementa.

## 1. Línea base (lo que dicen los entregables)

| Fuente | Contenido comprometido |
|---|---|
| Requisitos funcionales (Tabla 3.2) | RF01 OCR offline · RF02 fragmentación + embeddings en Room · RF03 tutoría socrática · RF04 voz (STT Vosk en castellano + TTS) · RF05 traducción de conceptos al quechua collao/chanka |
| No funcionales (Tabla 3.3) | RNF01 100 % offline · RNF02 RAM del SLM · RNF03 tokens/s · RNF04 búsqueda ≤ 150 ms · RNF05 Android 9, ARM64, 2 GB |
| Casos de uso (Fig. 3.2) | **Un solo actor: Estudiante.** CU: Capturar página (incluye OCR y Fragmentar/vectorizar), Consultar al tutor (incluye Traducir al quechua; extendido por Formular consulta por voz y Escuchar respuesta) |
| Vistas (Fig. 3.3) | Visor de escaneo (guías para alinear) · Chat socrático (términos en quechua) · Controles multimodales (micrófono y lectura en voz alta) |
| Modelo de datos (Fig. 3.5) | Libro, Fragmento, Conversación, Mensaje, Contexto recuperado. Sin usuarios, cuentas, cursos, notas ni alertas |

## 2. Pantalla por pantalla

Veredicto: **Mantener**, **Ajustar** (entra con cambios) o **Fuera** (no está en los entregables).

### Flujo estudiante

| Pantalla Figma | Veredicto | Requisito | Cambios |
|---|---|---|---|
| Home – Estudiantes (1:2) | Ajustar | RF01, RF03 | Recientes deben ser conversaciones/libros reales (no «Titulo de curso»). Reducir la imagen de cabecera. El botón de cámara abre el escaneo |
| Biblioteca (1:5) | Ajustar | RF02 | Lista los libros escaneados (entidad LIBRO). Filtros por área curricular del MINEDU (Comunicación, Matemática, Ciencia y Tecnología…), no por «Religión» |
| Chats (5:294) | Ajustar | RF03 | Quitar selector «Gemini 3.5 Flash» (es nube, contradice RNF01). Añadir micrófono (RF04). Revisar atajos (ver §4) |
| Chat «La fotosíntesis» (7:832) | Ajustar | RF03, RF04, RF05 | Es la pantalla clave. Añadir: términos en quechua resaltados, botón «escuchar» por mensaje, micrófono, fuente del libro (página citada). Quitar «Gemini» |
| Scam / Escaneo (9:6) | Ajustar | RF01 | Usar página de un libro escolar, no un examen universitario. Añadir guías de encuadre (Fig. 3.3) y linterna |
| Exam «Resolver examen…» (15:16) | **Fuera** | — | Contradice el enfoque socrático (resolver exámenes). Se reemplaza por la pantalla de revisión del texto escaneado (§3) |
| Ajustes (4:85) | Ajustar | RF04, accesibilidad | Quitar «Cuenta» (correo/teléfono: no hay servidor). Dejar perfil local (nombre, grado), idioma de la interfaz, tamaño de texto, velocidad de voz y nivel de ayuda |

### Flujo docente

| Pantalla Figma | Veredicto | Motivo |
|---|---|---|
| Home – Profesores (10:2, 18:119) | **Fuera** | No hay actor Docente en los casos de uso ni RF que lo cubra |
| Biblioteca docente + filtros por grado (10:72, 18:2) | **Fuera** | Ídem |
| Estudiantes, Estudiante, Estudiante – Curso, Seguimiento (18:272, 18:382, 24:641, 30:8) | **Fuera** | Requieren datos de varios alumnos en varios dispositivos (sincronización), notas de exámenes y asignación de actividades. Choca con RNF01 y con el modelo de datos de un solo usuario |

Si el equipo quisiera incluir el modo docente habría que añadir al documento: actor Docente y sus casos de uso, nuevos RF (alertas, seguimiento, asignar actividades), tablas nuevas (estudiante, curso, alerta, evaluación) y un mecanismo de sincronización offline (por ejemplo, exportar/importar por Wi-Fi Direct o USB). Recomendación: dejarlo como **trabajo futuro** en las limitaciones.

## 3. Pantallas que faltan en Figma

| Pantalla | Requisito | Por qué hace falta |
|---|---|---|
| Revisar texto escaneado | RF01, RF02 | Tras el OCR, el estudiante corrige errores, elige libro y página y guarda. Sin esto no se crea el LIBRO ni los fragmentos |
| Indexando (progreso) | RF02 | La vectorización tarda; hay que mostrar progreso y no bloquear la interfaz |
| Preparando al tutor | RNF02, RNF03 | Cargar el modelo de ~1 GB tarda varios segundos; necesita estado de carga |
| Grabando voz | RF04 | Estado del micrófono mientras Vosk transcribe |
| Detalle de libro | RF02 | Ver páginas escaneadas y «Preguntar sobre este libro» (crea conversación ligada al LIBRO) |
| Estados vacíos y de error | — | Biblioteca vacía («Escanea tu primera página»), sin espacio, OCR sin texto |
| Primer uso | — | Nombre y grado del estudiante (perfil local) |

## 4. Atajos del chat

| Atajo en Figma | Propuesta |
|---|---|
| Explicar Simple | Renombrar a «Explícame con preguntas» (modo socrático por defecto) |
| Quiz Rápido | Mantener: preguntas de comprobación, compatible con RF03 |
| Resumir, Ideas Centrales, Extraer Citas | Quitar: entregan la respuesta hecha y no aparecen en los RF |
| (nuevo) Palabras difíciles | Añadir: lista los términos del pasaje con su equivalente en quechua (RF05) |

## 5. Navegación propuesta

Barra inferior de 4 pestañas: **Inicio · Biblioteca · Tutor · Ajustes**. «Chats» y «Tutor» de Figma se fusionan en «Tutor» (lista de conversaciones + nueva consulta). El escaneo se abre desde Inicio y Biblioteca.

```
Inicio ──► Escanear ──► Revisar texto ──► Indexando ──► Detalle de libro
  │                                                        │
  └──► Nueva consulta ──────────────► Chat socrático ◄─────┘
Biblioteca ──► Detalle de libro
Tutor ──► Chat socrático
Ajustes
```

## 6. Cambios a los documentos

- **Figura 3.4**: usar las pantallas finales del flujo estudiante (cuando estén pulidas en Compose).
- Añadir una tabla «Pantallas vs. requisitos» (secciones 2 y 3 de este archivo).
- Limitaciones (Tabla 3.5): mencionar el modo docente como trabajo futuro.
- Modelo entidad-relación: las fechas pasan de TEXT a INTEGER (epoch en milisegundos).
