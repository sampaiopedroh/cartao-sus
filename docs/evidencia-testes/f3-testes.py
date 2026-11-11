"""Testes de ponta a ponta da F3 (curl-equivalentes). Uso: LOG=<log do dotnet run> python3 f3-testes.py

O log do dotnet run contém os segredos TOTP demo impressos pelo seeder. Não versionar o log."""
import base64, hashlib, hmac, json, struct, sys, time, urllib.request, urllib.error, re, os
B = os.environ.get("API", "http://localhost:8080")
env = dict(l.split("=", 1) for l in open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", ".env")).read().splitlines() if "=" in l and not l.startswith("#"))
log = open(os.environ["LOG"]).read()  # saída do dotnet run (contém segredos TOTP demo; não versionar)
SEG_MARIA = re.search(r"Maria Souza Lima .*?: (\w+)", log).group(1)
SEG_JOAO = re.search(r"João Pereira Santos .*?: (\w+)", log).group(1)
SENHA = env["SENHA_DEMO"]
K_UBS, K_UPA, K_TENS = env["IOT_KEY_TERMINAL_UBS"], env["IOT_KEY_TERMINAL_UPA"], env["IOT_KEY_TENSIOMETRO"]
out = []

def totp(seg, delta=0):
    key = base64.b32decode(seg + "=" * (-len(seg) % 8))
    c = int(time.time()) // 30 + delta
    h = hmac.new(key, struct.pack(">Q", c), hashlib.sha1).digest()
    o = h[-1] & 0x0F
    return str((struct.unpack(">I", h[o:o+4])[0] & 0x7FFFFFFF) % 1000000).zfill(6)

def req(metodo, caminho, corpo=None, token=None, chave=None):
    h = {"Content-Type": "application/json"}
    if token: h["Authorization"] = "Bearer " + token
    if chave: h["X-API-KEY"] = chave
    data = json.dumps(corpo).encode() if corpo is not None else None
    r = urllib.request.Request(B + caminho, data=data, method=metodo, headers=h)
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            return resp.status, resp.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()

def login(usuario):
    s, b = req("POST", "/api/v1/auth/login", {"login": usuario, "senha": SENHA})
    assert s == 200, (usuario, s, b)
    return json.loads(b)["token"]

def caso(titulo, esperado, resultado, corpo=""):
    s, b = resultado
    marca = "OK " if s == esperado else "FALHOU"
    b = re.sub(r'"token":"[^"]*"', '"token":"<omitido>"', b)
    out.append(f"{marca} {titulo}\n    esperado={esperado} obtido={s}\n    {b[:300]}")
    return s, b

def tok(s, b, campo):
    return json.loads(b)[campo]

tJ = login("joao.pereira@cartaosus.demo")
tM = login("maria.souza@cartaosus.demo")
tC = login("carla.lima@cartaosus.demo")
tA = login("admin@cartaosus.demo")

out.append("## E3 — acolhimento NFC + TOTP")
caso("E3 sem X-API-KEY (esperado 401 NAO_AUTORIZADO)", 401, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "898000000000000", "codigoCnesUnidade": "9186492", "tokenRotativo": "123456"}))
caso("E3 chave inválida (esperado 401)", 401, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "898000000000000", "codigoCnesUnidade": "9186492", "tokenRotativo": "123456"}, chave="chave-falsa"))
caso("E3 terminal de outra unidade (esperado 403 SEM_PERMISSAO)", 403, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "182345678901234", "codigoCnesUnidade": "8530378", "tokenRotativo": "123456"}, chave=K_UBS))
caso("E3 CNS inexistente (esperado 404)", 404, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "999999999999999", "codigoCnesUnidade": "9186492", "tokenRotativo": "123456"}, chave=K_UBS))
caso("E3 token com letras (esperado 400)", 400, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "898000000000000", "codigoCnesUnidade": "9186492", "tokenRotativo": "ABCDEF"}, chave=K_UBS))
s, b = caso("E3 Maria com TOTP válido (esperado 200 AUTORIZADO)", 200, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "898000000000000", "codigoCnesUnidade": "9186492", "tokenRotativo": totp(SEG_MARIA)}, chave=K_UBS))
caso("E3 mesmo código reutilizado (esperado 403 TOKEN_INVALIDO)", 403, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "898000000000000", "codigoCnesUnidade": "9186492", "tokenRotativo": totp(SEG_MARIA)}, chave=K_UBS))
caso("E3 código errado 2 (esperado 403 TOKEN_INVALIDO)", 403, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "898000000000000", "codigoCnesUnidade": "9186492", "tokenRotativo": "000001" if totp(SEG_MARIA) != "000001" else "000002"}, chave=K_UBS))
caso("E3 código errado 3 (esperado 403 TOKEN_INVALIDO; gera alerta)", 403, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "898000000000000", "codigoCnesUnidade": "9186492", "tokenRotativo": "000003" if totp(SEG_MARIA) != "000003" else "000004"}, chave=K_UBS))
caso("E3 após 3 falhas, mesmo com código certo (esperado 403 BLOQUEADO)", 403, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "898000000000000", "codigoCnesUnidade": "9186492", "tokenRotativo": totp(SEG_MARIA)}, chave=K_UBS))

out.append("\n## E4/E5 — acesso do profissional com consentimento")
caso("E4 profissional sem acolhimento recente (esperado 200: Maria foi autorizada agora)", 200, req("GET", "/api/v1/cidadao/1/prontuario", token=tC))
caso("E4 cidadã lê o próprio prontuário (esperado 200)", 200, req("GET", "/api/v1/cidadao/1/prontuario", token=tM))
caso("E4 João tenta prontuário da Maria (esperado 403 SEM_PERMISSAO)", 403, req("GET", "/api/v1/cidadao/1/prontuario", token=tJ))
caso("E4 profissional em cidadão sem consentimento recente (esperado 403)", 403, req("GET", "/api/v1/cidadao/2/prontuario", token=tC))
caso("E4 sem token (esperado 401)", 401, req("GET", "/api/v1/cidadao/1/prontuario"))
caso("E4 id inexistente para o próprio admin (esperado 403)", 403, req("GET", "/api/v1/cidadao/1/prontuario", token=tA))
caso("E5 sinais de glicemia da Maria (esperado 200 com resumo)", 200, req("GET", "/api/v1/cidadao/1/sinais-vitais?tipo=GLICEMIA", token=tM))
caso("E5 sinais com data (esperado 200)", 200, req("GET", "/api/v1/cidadao/1/sinais-vitais?de=2026-11-08&ate=2026-11-09", token=tM))

out.append("\n## E6 — telemetria (tensiômetro, cidadão e terminal)")
caso("E6 tensiômetro pressão crítica 185 (esperado 201 alertaDisparado=true)", 201, req("POST", "/api/v1/telemetria/sinais-vitais", {"tipoMedicao": "PRESSAO_SISTOLICA", "valor": 185.0}, chave=K_TENS))
caso("E6 tensiômetro pressão normal 120 (esperado 201 sem alerta)", 201, req("POST", "/api/v1/telemetria/sinais-vitais", {"tipoMedicao": "PRESSAO_SISTOLICA", "valor": 120.0}, chave=K_TENS))
caso("E6 tensiômetro com CNS de outra pessoa (esperado 403)", 403, req("POST", "/api/v1/telemetria/sinais-vitais", {"tipoMedicao": "PRESSAO_SISTOLICA", "valor": 120.0, "numeroCns": "898000000000000"}, chave=K_TENS))
caso("E6 glicemia crítica 310 pelo app (esperado 201 GLICEMIA_CRITICA)", 201, req("POST", "/api/v1/telemetria/sinais-vitais", {"tipoMedicao": "GLICEMIA", "valor": 310.0}, token=tJ))
caso("E6 valor zero (esperado 400)", 400, req("POST", "/api/v1/telemetria/sinais-vitais", {"tipoMedicao": "PESO", "valor": 0}, token=tJ))
caso("E6 tipo inválido (esperado 400)", 400, req("POST", "/api/v1/telemetria/sinais-vitais", {"tipoMedicao": "XPTO", "valor": 10}, token=tJ))
caso("E6 terminal NFC não envia telemetria (esperado 403)", 403, req("POST", "/api/v1/telemetria/sinais-vitais", {"tipoMedicao": "PESO", "valor": 70}, chave=K_UBS))

out.append("\n## E10 — alertas e E7 — histórico")
caso("E10 alertas do João (esperado 200 paginado)", 200, req("GET", "/api/v1/cidadao/2/alertas?page=0&size=2", token=tJ))
s, b = req("GET", "/api/v1/cidadao/2/alertas?size=1", token=tJ)
idal = json.loads(b)["content"][0]["idAlerta"]
caso("E10 marcar alerta como ENTREGUE (esperado 204)", 204, req("PUT", f"/api/v1/cidadao/2/alertas/{idal}", token=tJ))
caso("E7 Maria atualiza o próprio histórico (esperado 200)", 200, req("PUT", "/api/v1/cidadao/1/historico/1", {"descricao": "Penicilina (confirmado 2026)", "grauSeveridade": "CRITICO"}, token=tM))
caso("E7 grau inválido (esperado 400)", 400, req("PUT", "/api/v1/cidadao/1/historico/1", {"descricao": "x", "grauSeveridade": "GRAVE"}, token=tM))
caso("E7 histórico de outro cidadão (esperado 404)", 404, req("PUT", "/api/v1/cidadao/1/historico/4", {"descricao": "x", "grauSeveridade": "ALTO"}, token=tM))
caso("Extra acessos da Maria (esperado 200)", 200, req("GET", "/api/v1/cidadao/1/acessos", token=tM))

out.append("\n## E8 e E9 — carteira de documentos")
caso("E8 receita sem validade (esperado 400 campos.dataValidade)", 400, req("POST", "/api/v1/documentos", {"tipoDocumento": "RECEITA_MEDICA", "nomeArquivo": "receita.pdf", "hashIntegridade": "a" * 64}, token=tJ))
caso("E8 hash inválido (esperado 400)", 400, req("POST", "/api/v1/documentos", {"tipoDocumento": "RG", "nomeArquivo": "rg.pdf", "hashIntegridade": "xyz"}, token=tJ))
s, b = caso("E8 RG válido (esperado 201)", 201, req("POST", "/api/v1/documentos", {"tipoDocumento": "LAUDO", "nomeArquivo": "laudo.pdf", "hashIntegridade": "b" * 64}, token=tJ))
idd = json.loads(b)["idDocumento"]
caso("E9 excluir o próprio documento (esperado 204)", 204, req("DELETE", f"/api/v1/documentos/{idd}", token=tJ))
caso("E9 excluir documento da Maria (esperado 403)", 403, req("DELETE", "/api/v1/documentos/1", token=tJ))
caso("E9 documento inexistente (esperado 404)", 404, req("DELETE", "/api/v1/documentos/999999", token=tJ))

out.append("\n## E11, E12 e catálogo — APIs do SUS")
caso("E11 equipamentos de um CNES (esperado 200)", 200, req("GET", "/api/v1/sus/equipamentos/2078015", token=tM))
caso("E11 CNES com formato errado (esperado 400)", 400, req("GET", "/api/v1/sus/equipamentos/20780%20OR", token=tM))
caso("E12 insulina asparte em SP (esperado 200)", 200, req("GET", "/api/v1/sus/medicamentos?municipio=355030&catmat=BR0276234-2&size=5", token=tM))
caso("E12 sem município (esperado 400)", 400, req("GET", "/api/v1/sus/medicamentos?catmat=BR0276234-2", token=tM))
caso("E12 paginação inválida (esperado 400)", 400, req("GET", "/api/v1/sus/medicamentos?municipio=355030&catmat=BR0276234-2&size=500", token=tM))
caso("Catálogo busca 'insulina' sem acento (esperado 200)", 200, req("GET", "/api/v1/sus/medicamentos/catalogo?nome=INSULINA", token=tM))

out.append("\n## E13 e E14 — unidades e auditoria")
caso("E13 unidades paginadas (esperado 200)", 200, req("GET", "/api/v1/unidades?page=0&size=5", token=tM))
caso("E14 auditoria como ADMIN (esperado 200)", 200, req("GET", "/api/v1/admin/auditoria?status=REJEITADO", token=tA))
caso("E14 auditoria como cidadão (esperado 403)", 403, req("GET", "/api/v1/admin/auditoria", token=tM))
caso("E14 status inválido (esperado 400)", 400, req("GET", "/api/v1/admin/auditoria?status=XPTO", token=tA))

out.append("\n## E3 — unidade que não é a do terminal, para registro")
caso("E3 João com terminal da UPA (esperado 403: unidade não é a do terminal)", 403, req("POST", "/api/v1/acolhimento/validar-nfc", {"numeroCns": "182345678901234", "codigoCnesUnidade": "8530378", "tokenRotativo": "123456"}, chave=K_UBS))

print("\n".join(out))
