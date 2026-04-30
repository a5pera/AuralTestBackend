import html
import re
import zipfile
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


DOCS = [
    {
        "src": ROOT / "技术方案.md",
        "out": ROOT / "AI情境听力教学系统设计与开发-技术方案.docx",
        "title": "《AI 情境听力教学系统设计与开发》技术方案",
        "doc_type": "软件工程技术方案",
        "doc_no": "Aural-SE-TS-001",
    },
    {
        "src": ROOT / "VR虚拟场景开发手册.md",
        "out": ROOT / "AI情境听力教学系统设计与开发-VR虚拟场景开发手册.docx",
        "title": "《AI 情境听力教学系统设计与开发》VR 虚拟场景开发手册",
        "doc_type": "软件工程开发手册",
        "doc_no": "Aural-SE-VR-001",
    },
    {
        "src": ROOT / "NLP动态任务设计指南.md",
        "out": ROOT / "AI情境听力教学系统设计与开发-NLP动态任务设计指南.docx",
        "title": "《AI 情境听力教学系统设计与开发》NLP 动态任务设计指南",
        "doc_type": "软件工程设计指南",
        "doc_no": "Aural-SE-NLP-001",
    },
]


NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"


def esc(s):
    return html.escape(str(s), quote=True)


def clean_inline(s):
    return re.sub(r"`([^`]+)`", r"\1", s).strip()


def r(text, bold=False, color=None, size=None, font="宋体"):
    props = []
    if bold:
        props.append("<w:b/>")
    if color:
        props.append(f'<w:color w:val="{color}"/>')
    if size:
        props.append(f'<w:sz w:val="{size}"/><w:szCs w:val="{size}"/>')
    props.append(f'<w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="{font}" w:cs="{font}"/>')
    rpr = f"<w:rPr>{''.join(props)}</w:rPr>"
    return f"<w:r>{rpr}<w:t xml:space=\"preserve\">{esc(text)}</w:t></w:r>"


def p(text="", style=None, align=None, bold=False, color=None, size=None, spacing_after=120, indent_left=None, keep_next=False):
    ppr = []
    if style:
        ppr.append(f'<w:pStyle w:val="{style}"/>')
    if align:
        ppr.append(f'<w:jc w:val="{align}"/>')
    if spacing_after is not None:
        ppr.append(f'<w:spacing w:after="{spacing_after}" w:line="360" w:lineRule="auto"/>')
    if indent_left:
        ppr.append(f'<w:ind w:left="{indent_left}"/>')
    if keep_next:
        ppr.append("<w:keepNext/>")
    ppr_xml = f"<w:pPr>{''.join(ppr)}</w:pPr>" if ppr else ""
    return f"<w:p>{ppr_xml}{r(text, bold=bold, color=color, size=size)}</w:p>"


def page_break():
    return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'


def table(rows, widths=None, header=True):
    if not rows:
        return ""
    cols = len(rows[0])
    if widths is None:
        widths = [int(9000 / cols)] * cols
    grid = "".join(f'<w:gridCol w:w="{w}"/>' for w in widths)
    xml = [
        "<w:tbl>",
        "<w:tblPr>",
        '<w:tblStyle w:val="TableGrid"/>',
        '<w:tblW w:w="0" w:type="auto"/>',
        '<w:tblCellMar><w:top w:w="120" w:type="dxa"/><w:left w:w="120" w:type="dxa"/><w:bottom w:w="120" w:type="dxa"/><w:right w:w="120" w:type="dxa"/></w:tblCellMar>',
        '<w:tblLook w:firstRow="1" w:lastRow="0" w:firstColumn="0" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/>',
        "</w:tblPr>",
        f"<w:tblGrid>{grid}</w:tblGrid>",
    ]
    for i, row in enumerate(rows):
        xml.append("<w:tr>")
        if i == 0 and header:
            xml.append("<w:trPr><w:tblHeader/></w:trPr>")
        for j, cell in enumerate(row):
            shade = '<w:shd w:fill="D9EAF7"/>' if i == 0 and header else ""
            xml.append(
                f'<w:tc><w:tcPr><w:tcW w:w="{widths[min(j, len(widths)-1)]}" w:type="dxa"/>{shade}<w:vAlign w:val="center"/></w:tcPr>'
            )
            text = clean_inline(str(cell))
            xml.append(p(text, bold=(i == 0 and header), spacing_after=40))
            xml.append("</w:tc>")
        xml.append("</w:tr>")
    xml.append("</w:tbl>")
    xml.append(p("", spacing_after=160))
    return "".join(xml)


def note_box(title, body):
    return table([[title], [body]], widths=[9000], header=True)


def parse_markdown(md_text):
    lines = md_text.splitlines()
    blocks = []
    table_buf = []
    code_buf = []
    in_code = False
    code_lang = ""

    def flush_table():
        nonlocal table_buf
        if table_buf:
            rows = []
            for line in table_buf:
                if re.match(r"^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$", line):
                    continue
                cells = [c.strip() for c in line.strip().strip("|").split("|")]
                rows.append(cells)
            if rows:
                blocks.append(("table", rows))
            table_buf = []

    for line in lines:
        if line.startswith("```"):
            if not in_code:
                flush_table()
                in_code = True
                code_lang = line.strip()[3:].strip()
                code_buf = []
            else:
                blocks.append(("code", code_lang, "\n".join(code_buf)))
                in_code = False
                code_lang = ""
                code_buf = []
            continue
        if in_code:
            code_buf.append(line)
            continue
        if line.strip().startswith("|") and "|" in line.strip()[1:]:
            table_buf.append(line)
            continue
        flush_table()
        if not line.strip():
            blocks.append(("blank",))
            continue
        m = re.match(r"^(#{1,4})\s+(.*)$", line)
        if m:
            blocks.append(("heading", len(m.group(1)), clean_inline(m.group(2))))
            continue
        if re.match(r"^\s*[-*]\s+", line):
            blocks.append(("bullet", clean_inline(re.sub(r"^\s*[-*]\s+", "", line))))
            continue
        if re.match(r"^\s*\d+\.\s+", line):
            blocks.append(("number", clean_inline(re.sub(r"^\s*\d+\.\s+", "", line))))
            continue
        blocks.append(("para", clean_inline(line)))
    flush_table()
    return blocks


def collect_toc(blocks):
    toc = []
    for b in blocks:
        if b[0] == "heading" and b[1] <= 3:
            toc.append((b[1], b[2]))
    return toc


def cover(doc):
    date = datetime.now().strftime("%Y年%m月%d日")
    rows = [
        ["文档编号", doc["doc_no"], "版本", "V1.0"],
        ["文档状态", "正式稿", "密级", "内部资料"],
        ["编制日期", date, "适用阶段", "设计与开发阶段"],
        ["编制单位", "项目组", "适用项目", "AI 情境听力教学系统设计与开发"],
    ]
    xml = []
    xml.append(p("软件工程文档", align="center", bold=True, color="1F4E79", size=32, spacing_after=800))
    xml.append(p(doc["title"], align="center", bold=True, color="1F4E79", size=44, spacing_after=320))
    xml.append(p(doc["doc_type"], align="center", color="666666", size=28, spacing_after=900))
    xml.append(table(rows, widths=[1800, 3000, 1500, 2700], header=False))
    xml.append(p("说明：本文档依据项目现有后端实现、材料资源管理流程和软件工程交付文档格式整理。图表位置已预留，可在后续排版中替换为正式流程图或架构图。", spacing_after=240))
    xml.append(page_break())
    return "".join(xml)


def doc_control(doc):
    rev = [
        ["版本", "日期", "修订说明", "修订人"],
        ["V1.0", datetime.now().strftime("%Y-%m-%d"), "根据项目 Markdown 文档整理为软件工程 DOCX 交付文档。", "项目组"],
    ]
    appr = [
        ["角色", "姓名", "职责", "签字/日期"],
        ["编制", "项目组", "文档编写与整理", ""],
        ["审核", "", "内容审核", ""],
        ["批准", "", "发布批准", ""],
    ]
    xml = []
    xml.append(p("文档控制信息", style="Heading1"))
    xml.append(table([
        ["项目名称", "AI 情境听力教学系统设计与开发"],
        ["文档名称", doc["title"]],
        ["文档类型", doc["doc_type"]],
        ["文档编号", doc["doc_no"]],
        ["版本号", "V1.0"],
        ["密级", "内部资料"],
    ], widths=[2200, 6800], header=False))
    xml.append(p("修订记录", style="Heading2"))
    xml.append(table(rev, widths=[1200, 1800, 4500, 1500]))
    xml.append(p("审核与批准", style="Heading2"))
    xml.append(table(appr, widths=[1500, 1700, 3800, 2000]))
    return "".join(xml)


def toc_xml(toc):
    xml = [p("目录", style="Heading1")]
    for level, title in toc:
        indent = (level - 1) * 420
        xml.append(p(title, indent_left=indent, spacing_after=80))
    xml.append(page_break())
    return "".join(xml)


def body_xml(blocks):
    xml = []
    fig_no = 1
    tbl_no = 1
    skip_first_title = True
    for b in blocks:
        kind = b[0]
        if kind == "heading":
            level, text = b[1], b[2]
            if skip_first_title and level == 1:
                skip_first_title = False
                continue
            skip_first_title = False
            style = f"Heading{min(level, 3)}"
            xml.append(p(text, style=style, keep_next=True))
        elif kind == "para":
            xml.append(p(b[1]))
        elif kind == "bullet":
            xml.append(p("· " + b[1], indent_left=360))
        elif kind == "number":
            xml.append(p(b[1], indent_left=360))
        elif kind == "table":
            xml.append(p(f"表 {tbl_no}  数据表", align="center", bold=True, color="1F4E79", spacing_after=80))
            rows = b[1]
            widths = None
            if rows and len(rows[0]) == 2:
                widths = [2600, 6400]
            elif rows and len(rows[0]) == 3:
                widths = [2200, 3200, 3600]
            elif rows and len(rows[0]) == 4:
                widths = [1400, 2200, 3400, 2000]
            xml.append(table(rows, widths=widths))
            tbl_no += 1
        elif kind == "code":
            lang, code = b[1], b[2]
            if lang.lower() == "mermaid":
                xml.append(note_box(
                    f"图 {fig_no} 占位：流程图/架构图",
                    "此处预留图形位置。后续可根据原 Markdown 中的 Mermaid 描述插入正式流程图、架构图或 ER 图。"
                ))
                xml.append(p(f"图 {fig_no}  流程图/架构图占位", align="center", color="666666", spacing_after=160))
                fig_no += 1
            else:
                xml.append(note_box("代码/数据示例", code))
        elif kind == "blank":
            pass
    return "".join(xml)


def styles_xml():
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="{NS}">
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:qFormat/>
    <w:pPr><w:spacing w:after="120" w:line="360" w:lineRule="auto"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="宋体"/><w:sz w:val="22"/><w:szCs w:val="22"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>
    <w:pPr><w:keepNext/><w:spacing w:before="360" w:after="180"/><w:outlineLvl w:val="0"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="黑体"/><w:b/><w:color w:val="1F4E79"/><w:sz w:val="32"/><w:szCs w:val="32"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading2">
    <w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>
    <w:pPr><w:keepNext/><w:spacing w:before="260" w:after="140"/><w:outlineLvl w:val="1"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="黑体"/><w:b/><w:color w:val="2F75B5"/><w:sz w:val="28"/><w:szCs w:val="28"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading3">
    <w:name w:val="heading 3"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>
    <w:pPr><w:keepNext/><w:spacing w:before="180" w:after="100"/><w:outlineLvl w:val="2"/></w:pPr>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="黑体"/><w:b/><w:color w:val="5B9BD5"/><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr>
  </w:style>
  <w:style w:type="table" w:default="1" w:styleId="TableGrid">
    <w:name w:val="Table Grid"/>
    <w:tblPr><w:tblBorders><w:top w:val="single" w:sz="4" w:color="A6A6A6"/><w:left w:val="single" w:sz="4" w:color="A6A6A6"/><w:bottom w:val="single" w:sz="4" w:color="A6A6A6"/><w:right w:val="single" w:sz="4" w:color="A6A6A6"/><w:insideH w:val="single" w:sz="4" w:color="D9D9D9"/><w:insideV w:val="single" w:sz="4" w:color="D9D9D9"/></w:tblBorders></w:tblPr>
  </w:style>
</w:styles>'''


def document_xml(doc, blocks):
    toc = collect_toc(blocks)
    content = []
    content.append(cover(doc))
    content.append(doc_control(doc))
    content.append(toc_xml(toc))
    content.append(body_xml(blocks))
    sect = '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1260" w:bottom="1440" w:left="1260" w:header="720" w:footer="720" w:gutter="0"/></w:sectPr>'
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="{NS}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <w:body>{''.join(content)}{sect}</w:body>
</w:document>'''


def write_docx(doc):
    text = doc["src"].read_text(encoding="utf-8")
    blocks = parse_markdown(text)
    doc_xml = document_xml(doc, blocks)
    core = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>{esc(doc["title"])}</dc:title><dc:creator>项目组</dc:creator><cp:lastModifiedBy>项目组</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">{datetime.utcnow().isoformat()}Z</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">{datetime.utcnow().isoformat()}Z</dcterms:modified>
</cp:coreProperties>'''
    app = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Microsoft Office Word</Application></Properties>'''
    content_types = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/><Override PartName="/word/settings.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/></Types>'''
    rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>'''
    doc_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>'''
    settings = f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:settings xmlns:w="{NS}"><w:zoom w:percent="100"/><w:defaultTabStop w:val="420"/></w:settings>'''

    with zipfile.ZipFile(doc["out"], "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("[Content_Types].xml", content_types)
        z.writestr("_rels/.rels", rels)
        z.writestr("docProps/core.xml", core)
        z.writestr("docProps/app.xml", app)
        z.writestr("word/document.xml", doc_xml)
        z.writestr("word/styles.xml", styles_xml())
        z.writestr("word/settings.xml", settings)
        z.writestr("word/_rels/document.xml.rels", doc_rels)


def main():
    for doc in DOCS:
        write_docx(doc)
        print(doc["out"])


if __name__ == "__main__":
    main()
