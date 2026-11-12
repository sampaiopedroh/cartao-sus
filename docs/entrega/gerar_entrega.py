#!/usr/bin/env python3
"""Gera ENTREGA-PDF.md, ENTREGA-PDF.html e o PDF da entrega a partir dos documentos do repositório.

Uso (na pasta cartao-sus/):  python3 docs/entrega/gerar_entrega.py
Requisitos: pacote Python `markdown` e o Google Chrome (para imprimir o PDF).
"""
import html, json, re, subprocess, sys
from pathlib import Path

import markdown

RAIZ = Path(__file__).resolve().parents[2]
SAIDA = RAIZ / "docs" / "entrega"
EV = "../evidencia-testes/"  # caminho relativo das imagens a partir de docs/entrega/
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"


def ler(rel):
    return (RAIZ / rel).read_text(encoding="utf-8")


def secao(texto, titulo_inicio, nivel=None):
    """Trecho do SDD que começa no título dado e vai até o próximo título do mesmo nível ou acima."""
    linhas = texto.splitlines()
    ini = next(i for i, l in enumerate(linhas) if l.startswith(titulo_inicio))
    nivel = nivel or len(re.match(r"#+", linhas[ini]).group(0))
    fim = len(linhas)
    for j in range(ini + 1, len(linhas)):
        m = re.match(r"(#+) ", linhas[j])
        if m and len(m.group(1)) <= nivel:
            fim = j
            break
    return "\n".join(linhas[ini:fim]).rstrip().rstrip("-").rstrip()


def rebaixar(md, n=1):
    """Desce o nível dos títulos (## vira ###) sem mexer em blocos de código."""
    saida, dentro = [], False
    for l in md.splitlines():
        if l.startswith("```"):
            dentro = not dentro
        if not dentro and re.match(r"#{1,5} ", l):
            l = "#" * n + l
        saida.append(l)
    return "\n".join(saida)


def tabela_endpoints():
    d = json.loads(ler("docs/swagger.json"))
    linhas = ["| Método | Caminho | Grupo | Respostas |", "|---|---|---|---|"]
    for caminho, ops in d["paths"].items():
        for metodo, op in ops.items():
            if metodo not in ("get", "post", "put", "delete", "patch"):
                continue
            tag = ", ".join(op.get("tags", []))
            codigos = ", ".join(sorted(op.get("responses", {}).keys()))
            linhas.append(f"| {metodo.upper()} | `{caminho}` | {tag} | {codigos} |")
    return "\n".join(linhas), sum(1 for ops in d["paths"].values() for m in ops if m in ("get", "post", "put", "delete", "patch"))


def integrantes():
    txt = ler("ENTREGA-TXT.txt")
    return [l[2:].strip() for l in txt.splitlines() if l.startswith("- ")]


def figura(arquivo, legenda):
    return f"![{legenda}]({EV}{arquivo})\n\n*{legenda}*\n"


def montar():
    sdd = ler("docs/sdd/sdd-v3.md")
    endpoints, total = tabela_endpoints()
    partes = []

    membros = "\n".join(f"- {m}" for m in integrantes())
    partes.append(f"""<div class="capa" markdown="1">

# Cartão SUS Inteligente — Saúde Conectada

**Global Solution 2026/2 — 3ESOA — FIAP — CitizenTech**

**Grupo: GateMinds**

{membros}

Repositório: <https://github.com/sampaiopedroh/cartao-sus>

Vídeo pitch: **[PENDENTE: link público]**

Documento de entrega com as seis partes do projeto (banco de dados, API, testes, aplicativo móvel, segurança e simulação de IoT) e anexos.

</div>
""")

    partes.append("# Visão geral\n\n" + rebaixar(secao(sdd, "### 1.1 O problema"), 0).replace("### ", "## ", 1)
                  + "\n\n" + secao(sdd, "### 1.2 A solução").replace("### ", "## ", 1)
                  + "\n\n" + secao(sdd, "### 1.4 ODS").replace("### ", "## ", 1))

    # Parte 1
    der = ler("database/DER.md")
    der = der.split("\n", 1)[1]  # tira o título H1 do arquivo
    m = re.search(r"```mermaid\n(.*?)```", der, flags=re.S)
    completo = m.group(1)
    compacto = re.sub(r"^\s+\w+ \{.*?^\s+\}\n", "", completo, flags=re.S | re.M)
    resto = der.replace(m.group(0), "@@DER@@")
    bloco = ("```mermaid\n" + compacto + "```\n\n"
             '<div class="der" markdown="1">\n\n### Diagrama entidade-relacionamento completo (com atributos)\n\n```mermaid\n' + completo + "```\n\n</div>\n")
    der = resto.replace("@@DER@@", bloco)
    partes.append("# Parte 1 — Banco de dados\n\n## Diagrama entidade-relacionamento\n" + rebaixar(der, 1)
                  + "\n\n" + rebaixar(secao(sdd, "### 4.2 Dicionário"), 0).replace("### ", "## ", 1)
                  + "\n\n## As sete consultas (`database/consultas.sql`)\n\n```sql\n" + ler("database/consultas.sql").rstrip() + "\n```\n\n"
                  "## Resultado da execução das consultas\n\n```text\n" + ler("docs/evidencia-testes/f1-consultas.txt").rstrip() + "\n```\n")

    # Parte 2
    partes.append(f"# Parte 2 — API RESTful\n\nA API é ASP.NET Core (.NET 10) em três camadas (Controller, Service, Repository), com EF Core, JWT e chave de dispositivo (`X-API-KEY`) para os sensores. O Swagger fica em `/swagger`, e a exportação em `docs/swagger.json`.\n\n"
                  f"## Endpoints ({total} operações)\n\n{endpoints}\n\n"
                  + secao(sdd, "### 5.1 Convenções").replace("### ", "## ", 1) + "\n\n"
                  + secao(sdd, "### 5.4 Camadas").replace("### ", "## ", 1) + "\n\n"
                  + secao(sdd, "### 5.7 Exemplos").replace("### ", "## ", 1))

    # Parte 3
    rel = ler("docs/evidencia-testes/RELATORIO.md").split("\n", 1)[1]
    partes.append("# Parte 3 — Plano e resultados de testes\n\n"
                  + secao(sdd, "### 6.1 Estratégia").replace("### ", "## ", 1) + "\n\n"
                  + "## Relatório consolidado\n" + rebaixar(rel, 1))

    # Parte 4
    partes.append("""# Parte 4 — Aplicativo móvel (Android, Kotlin)

Aplicativo nativo em Kotlin com Jetpack Compose e Material 3, arquitetura MVVM com `StateFlow`, Retrofit com Gson para consumir a API, Room para a fila offline e para os caminhos dos documentos, e Navigation Compose.

## Telas e funcionalidades

| Tela | O que faz |
|---|---|
| Login e cadastro | Entrar com e-mail e senha; criar conta com CPF, CNS e data (com máscara), recebendo o código secreto uma única vez |
| Início | Cartão digital com o número do CNS, atalhos e avisos de saúde |
| Atendimento | Código de 6 dígitos (TOTP) com contagem regressiva; histórico de atendimentos; compartilhar documentos depois que o atendente valida o código |
| Consultar | Remédios com estoque em São Paulo e unidades de saúde por distância, com "Como chegar" e equipamentos |
| Saúde | Registro de pressão, glicemia e peso, gráfico, alerta crítico e fila offline |
| Meus documentos | Anexar PDF ou foto pelo seletor do Android; só o caminho fica no celular |

Cada tela trata os três estados (carregando, vazio e erro com "Tentar de novo") e dá retorno a cada ação. A interface usa fonte grande, alto contraste, botões de 56 dp e textos em português simples, pensando em pessoas de todas as idades.

""" + figura("ux-login.png", "Tela de login") + figura("ux-inicio.png", "Início: cartão digital e atalhos")
                  + figura("ux-atendimento.png", "Atendimento: histórico e código secreto")
                  + figura("ux-consultar.png", "Consultar remédios e unidades")
                  + figura("ux-saude.png", "Sinais de saúde")
                  + figura("ux-documentos.png", "Meus documentos")
                  + figura("ux-compartilhar-liberado.png", "Compartilhar documentos liberado depois da validação do código")
                  + figura("ux-compartilhar-android.png", "Compartilhamento do Android com o documento escolhido")
                  + figura("f8_07_sinais_alerta.png", "Alerta crítico de pressão")
                  + figura("f8_08_offline_pendente.png", "Medição guardada sem conexão")
                  + """
## Navegação e usabilidade

A barra inferior tem quatro abas com ícone e texto (Início, Atendimento, Consultar e Saúde). O botão Início volta à tela inicial a partir de qualquer aba, e o botão Voltar do sistema também. Sair da conta pede confirmação.

## Compartilhamento de documentos e NFC

A ideia do produto é compartilhar as informações por NFC. Por limitação da simulação (sem leitor físico e sem emulação de cartão no emulador), o aplicativo libera o compartilhamento pelo seletor do Android depois que o atendente valida o código. A decisão completa está no anexo.
""")

    # Parte 5
    partes.append("# Parte 5 — Segurança\n\n"
                  + "\n\n".join(secao(sdd, t).replace("### ", "## ", 1) for t in
                                ["### 8.1 Modelo de ameaças", "### 8.2 Login seguro", "### 8.3 Controle de acesso",
                                 "### 8.4 TOTP no servidor", "### 8.5 Segredos", "### 8.6 Boas práticas", "### 8.7 LGPD"]))

    # Parte 6
    iot = ler("iot/README.md").split("\n", 1)[1]
    partes.append("# Parte 6 — Simulação de IoT\n" + rebaixar(iot, 1) + "\n\n" + secao(sdd, "### 9.5 Explicação").replace("### ", "## ", 1))

    # Anexos
    readme = ler("README.md")
    ia = secao(readme, "## Declaração de uso de IA").replace("## ", "## ", 1)
    adr = ler("docs/adr/ADR-nfc-compartilhamento.md").split("\n", 1)[1]
    lim = secao(ler("docs/evidencia-testes/RELATORIO.md"), "## Limitações conhecidas")
    partes.append("# Anexos\n\n## Repositório e vídeo\n\n- Repositório: <https://github.com/sampaiopedroh/cartao-sus>\n- Vídeo pitch: **[PENDENTE: link público]**\n\n"
                  + ia + "\n\n" + lim.replace("## ", "## ", 1) + "\n\n## Decisão: compartilhamento sem NFC real\n" + rebaixar(adr, 1))

    return "\n\n".join(partes) + "\n"


CSS = """
@page { size: A4; margin: 18mm 15mm; }
body { font-family: -apple-system, 'Helvetica Neue', Arial, sans-serif; font-size: 10.5pt; line-height: 1.45; color: #1b1b1f; }
h1 { font-size: 20pt; color: #0b4fa8; border-bottom: 2px solid #0b4fa8; padding-bottom: 4px; page-break-before: always; }
.capa h1 { page-break-before: avoid; border: none; font-size: 28pt; margin-top: 60mm; }
.capa { page-break-after: always; }
h2 { font-size: 14pt; color: #0b4fa8; margin-top: 18px; }
h3 { font-size: 12pt; }
table { border-collapse: collapse; width: 100%; font-size: 9pt; margin: 8px 0; page-break-inside: auto; }
th, td { border: 1px solid #c7c7d0; padding: 4px 6px; text-align: left; vertical-align: top; }
th { background: #e8eefb; }
tr { page-break-inside: avoid; }
code { font-family: Menlo, monospace; font-size: 8.5pt; background: #f1f1f5; padding: 0 2px; }
pre { background: #f6f6f9; border: 1px solid #dcdce4; padding: 8px; font-size: 8pt; white-space: pre-wrap; word-break: break-word; page-break-inside: avoid; }
pre code { background: none; padding: 0; }
img { max-width: 62mm; display: block; margin: 8px auto 2px; border: 1px solid #ccc; }
em { color: #555; }
p:has(> img) { page-break-inside: avoid; }
@page land { size: A4 landscape; margin: 12mm; }
.der { page: land; page-break-before: always; page-break-after: always; }
.der h1 { page-break-before: avoid; }
.der svg { width: 100% !important; max-width: none !important; height: auto; }
pre.mermaid { background: #fff; border: none; text-align: center; white-space: pre; font-size: 10px; }
blockquote { border-left: 3px solid #0b4fa8; margin-left: 0; padding-left: 10px; color: #444; }
"""


def para_html(md):
    # Mermaid: troca os blocos por <pre class="mermaid">
    def troca(m):
        return '<pre class="mermaid">' + html.escape(m.group(1)) + "</pre>"
    md_html = re.sub(r"```mermaid\n(.*?)```", troca, md, flags=re.S)
    corpo = markdown.markdown(md_html, extensions=["tables", "fenced_code", "sane_lists", "md_in_html"])
    return f"""<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>Cartão SUS Inteligente - Entrega GateMinds</title>
<style>{CSS}</style>
<script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script>
<script>window.addEventListener('load', function(){{ if (window.mermaid) {{ mermaid.initialize({{startOnLoad:false, theme:'default', er:{{useMaxWidth:true}}}}); mermaid.run({{querySelector:'pre.mermaid'}}); }} }});</script>
</head><body>{corpo}</body></html>"""


def main():
    md = montar()
    (SAIDA / "ENTREGA-PDF.md").write_text(md, encoding="utf-8")
    h = SAIDA / "ENTREGA-PDF.html"
    h.write_text(para_html(md), encoding="utf-8")
    pdf = SAIDA / "GateMinds-Cartao-SUS-Entrega.pdf"
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                    "--virtual-time-budget=20000", f"--print-to-pdf={pdf}", h.as_uri()],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print("PDF:", pdf, pdf.stat().st_size // 1024, "KB")


if __name__ == "__main__":
    sys.exit(main())
