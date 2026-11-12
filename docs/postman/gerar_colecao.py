#!/usr/bin/env python3
"""Gera docs/postman-collection.json: coleção executável de ponta a ponta (Postman ou Newman).

Uso (na pasta cartao-sus/):  python3 docs/postman/gerar_colecao.py
"""
import json
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[2]
SAIDA = RAIZ / "docs" / "postman-collection.json"
URL = "{{baseUrl}}"


def bearer(var):
    return {"type": "bearer", "bearer": [{"key": "token", "value": "{{" + var + "}}", "type": "string"}]}


def req(nome, metodo, caminho, status, auth=None, corpo=None, headers=None, pre=None, testes=None, query=None):
    cab = [{"key": "Content-Type", "value": "application/json"}] if corpo is not None else []
    cab += [{"key": k, "value": v} for k, v in (headers or {}).items()]
    url = {"raw": URL + caminho + (("?" + "&".join(f"{k}={v}" for k, v in query.items())) if query else ""),
           "host": ["{{baseUrl}}"], "path": [p for p in caminho.strip("/").split("/")]}
    if query:
        url["query"] = [{"key": k, "value": v} for k, v in query.items()]
    r = {"method": metodo, "header": cab, "url": url}
    if auth:
        r["auth"] = bearer(auth)
    if corpo is not None:
        r["body"] = {"mode": "raw", "raw": json.dumps(corpo, indent=2, ensure_ascii=False), "options": {"raw": {"language": "json"}}}
    scripts = [f"pm.test('status {status}', () => pm.response.to.have.status({status}));"] + (testes or [])
    eventos = [{"listen": "test", "script": {"type": "text/javascript", "exec": scripts}}]
    if pre:
        eventos.insert(0, {"listen": "prerequest", "script": {"type": "text/javascript", "exec": pre}})
    return {"name": nome, "event": eventos, "request": r}


def erro(codigo):
    return f"pm.test('erro {codigo}', () => pm.expect(pm.response.json().erro).to.eql('{codigo}'));"


PRE_CADASTRO = [
    "// CPF (com dígitos verificadores) e CNS novos a cada execução, para a coleção poder rodar várias vezes.",
    "const rnd = n => Array.from({length: n}, () => Math.floor(Math.random() * 10));",
    "const dv = a => { let s = 0; for (let i = 0; i < a.length; i++) s += a[i] * (a.length + 1 - i); const r = (s * 10) % 11; return r === 10 ? 0 : r; };",
    "const c = rnd(9); c.push(dv(c)); c.push(dv(c));",
    "pm.collectionVariables.set('cpfNovo', c.join(''));",
    "pm.collectionVariables.set('cnsNovo', '7' + rnd(14).join(''));",
    "pm.collectionVariables.set('loginNovo', 'novo.' + Date.now() + '@cartaosus.demo');",
]

PRE_TOTP = [
    "// Gera o código TOTP atual (RFC 6238, HMAC-SHA1, 6 dígitos, passo de 30 s) a partir do segredo da Maria.",
    "const segredo = pm.variables.get('segredoTotpMaria');",
    "if (!segredo || segredo.startsWith('<')) { throw new Error('Defina a variável segredoTotpMaria (segredo impresso no console da API).'); }",
    "const A = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567'; let bits = '';",
    "for (const ch of segredo.replace(/=+$/, '').toUpperCase()) bits += A.indexOf(ch).toString(2).padStart(5, '0');",
    "const bytes = []; for (let i = 0; i + 8 <= bits.length; i += 8) bytes.push(parseInt(bits.substr(i, 8), 2));",
    "const chave = CryptoJS.lib.WordArray.create(new Uint8Array(bytes));",
    "const passo = Math.floor(Date.now() / 30000);",
    "const msg = CryptoJS.lib.WordArray.create([0, passo], 8);",
    "const h = CryptoJS.HmacSHA1(msg, chave).words;",
    "const b = []; h.forEach(w => { b.push((w >>> 24) & 255, (w >>> 16) & 255, (w >>> 8) & 255, w & 255); });",
    "const o = b[19] & 15;",
    "const n = (((b[o] & 127) << 24) | (b[o + 1] << 16) | (b[o + 2] << 8) | b[o + 3]) % 1000000;",
    "pm.collectionVariables.set('codigoTotp', String(n).padStart(6, '0'));",
]


def colecao():
    pastas = []

    pastas.append(("1. Autenticação", [
        req("Login da Maria (cidadã)", "POST", "/api/v1/auth/login", 200,
            corpo={"login": "maria.souza@cartaosus.demo", "senha": "{{senhaDemo}}"},
            testes=["const j = pm.response.json();", "pm.collectionVariables.set('tokenMaria', j.token);",
                    "pm.collectionVariables.set('idMaria', j.idCidadao);",
                    "pm.test('perfil CIDADAO', () => pm.expect(j.perfil).to.eql('CIDADAO'));"]),
        req("Login do João (cidadão)", "POST", "/api/v1/auth/login", 200,
            corpo={"login": "joao.pereira@cartaosus.demo", "senha": "{{senhaDemo}}"},
            testes=["const j = pm.response.json();", "pm.collectionVariables.set('tokenJoao', j.token);",
                    "pm.collectionVariables.set('idJoao', j.idCidadao);"]),
        req("Login do administrador", "POST", "/api/v1/auth/login", 200,
            corpo={"login": "admin@cartaosus.demo", "senha": "{{senhaDemo}}"},
            testes=["const j = pm.response.json();", "pm.collectionVariables.set('tokenAdmin', j.token);",
                    "pm.test('perfil ADMIN', () => pm.expect(j.perfil).to.eql('ADMIN'));"]),
        req("Login com senha errada (401)", "POST", "/api/v1/auth/login", 401,
            corpo={"login": "maria.souza@cartaosus.demo", "senha": "senha-errada-123"}, testes=[erro("NAO_AUTORIZADO")]),
        req("Login sem o campo senha (400)", "POST", "/api/v1/auth/login", 400,
            corpo={"login": "maria.souza@cartaosus.demo"}, testes=[erro("VALIDACAO")]),
        req("Quem sou eu (me)", "GET", "/api/v1/auth/me", 200, auth="tokenMaria",
            testes=["pm.test('login da Maria', () => pm.expect(pm.response.json().login).to.eql('maria.souza@cartaosus.demo'));"]),
        req("Cadastro de novo cidadão", "POST", "/api/v1/auth/cadastro", 201, pre=PRE_CADASTRO,
            corpo={"login": "{{loginNovo}}", "senha": "SenhaForte123", "cpf": "{{cpfNovo}}", "numeroCns": "{{cnsNovo}}",
                   "nomeCompleto": "Cidadao Postman", "dataNascimento": "1990-05-20"},
            testes=["pm.test('segredo TOTP devolvido uma única vez', () => pm.expect(pm.response.json().segredoTotp).to.be.a('string'));"]),
        req("Cadastro repetido (409)", "POST", "/api/v1/auth/cadastro", 409,
            corpo={"login": "{{loginNovo}}", "senha": "SenhaForte123", "cpf": "{{cpfNovo}}", "numeroCns": "{{cnsNovo}}",
                   "nomeCompleto": "Cidadao Postman", "dataNascimento": "1990-05-20"}, testes=[erro("CONFLITO")]),
    ]))

    pastas.append(("2. Cidadão (token da Maria)", [
        req("Prontuário", "GET", "/api/v1/cidadao/{{idMaria}}/prontuario", 200, auth="tokenMaria"),
        req("Prontuário de outro cidadão (403)", "GET", "/api/v1/cidadao/{{idJoao}}/prontuario", 403, auth="tokenMaria",
            testes=[erro("SEM_PERMISSAO")]),
        req("Sinais vitais", "GET", "/api/v1/cidadao/{{idMaria}}/sinais-vitais", 200, auth="tokenMaria"),
        req("Atualizar histórico clínico", "PUT", "/api/v1/cidadao/{{idMaria}}/historico/1", 200, auth="tokenMaria",
            corpo={"descricao": "Penicilina (confirmado 2026)", "grauSeveridade": "CRITICO"}),
        req("Alertas da Maria", "GET", "/api/v1/cidadao/{{idMaria}}/alertas", 200, auth="tokenMaria"),
        req("Histórico de acessos", "GET", "/api/v1/cidadao/{{idMaria}}/acessos", 200, auth="tokenMaria"),
    ]))

    pastas.append(("3. Documentos (Maria)", [
        req("Registrar documento", "POST", "/api/v1/documentos", 201, auth="tokenMaria",
            corpo={"tipoDocumento": "LAUDO", "nomeArquivo": "laudo.pdf", "hashIntegridade": "a" * 64},
            testes=["const j = pm.response.json();", "pm.collectionVariables.set('idDoc', j.idDocumento);"]),
        req("Excluir documento", "DELETE", "/api/v1/documentos/{{idDoc}}", 204, auth="tokenMaria"),
    ]))

    pastas.append(("4. Dados do SUS e unidades", [
        req("Unidades de saúde", "GET", "/api/v1/unidades", 200, auth="tokenMaria"),
        req("Catálogo de medicamentos", "GET", "/api/v1/sus/medicamentos/catalogo", 200, auth="tokenMaria",
            testes=["const lista = pm.response.json();", "pm.test('catálogo com itens', () => pm.expect(lista.length).to.be.above(0));",
                    "pm.collectionVariables.set('catmat', String(lista[0].catmat || lista[0].codigoCatmat));"]),
        req("Estoque do medicamento em São Paulo", "GET", "/api/v1/sus/medicamentos", 200, auth="tokenMaria",
            query={"municipio": "355030", "catmat": "{{catmat}}"}),
        req("Equipamentos da unidade (CNES)", "GET", "/api/v1/sus/equipamentos/9186492", 200, auth="tokenMaria"),
    ]))

    pastas.append(("5. Dispositivos IoT (X-API-KEY) e alertas", [
        req("Tensiômetro: pressão crítica", "POST", "/api/v1/telemetria/sinais-vitais", 201,
            headers={"X-API-KEY": "{{chaveTensiometro}}", "X-Device-Type": "TENSIOMETRO"},
            corpo={"tipoMedicao": "PRESSAO_SISTOLICA", "valor": 185.0},
            testes=["pm.test('alerta disparado', () => pm.expect(pm.response.json().alertaDisparado).to.eql(true));"]),
        req("Alertas pendentes do João", "GET", "/api/v1/cidadao/{{idJoao}}/alertas", 200, auth="tokenJoao",
            query={"status": "PENDENTE"},
            testes=["const c = pm.response.json().content;", "pm.test('há alerta pendente', () => pm.expect(c.length).to.be.above(0));",
                    "pm.collectionVariables.set('idAlerta', c[0].idAlerta);"]),
        req("Confirmar entrega do alerta", "PUT", "/api/v1/cidadao/{{idJoao}}/alertas/{{idAlerta}}", 204, auth="tokenJoao", corpo={}),
        req("Terminal NFC: código TOTP válido", "POST", "/api/v1/acolhimento/validar-nfc", 200, pre=PRE_TOTP,
            headers={"X-API-KEY": "{{chaveTerminalUbs}}", "X-Device-Type": "TERMINAL_NFC"},
            corpo={"numeroCns": "898000000000000", "codigoCnesUnidade": "9186492", "tokenRotativo": "{{codigoTotp}}"},
            testes=["pm.test('AUTORIZADO', () => pm.expect(pm.response.json().status).to.eql('AUTORIZADO'));"]),
    ]))

    pastas.append(("6. Administração", [
        req("Auditoria como administrador", "GET", "/api/v1/admin/auditoria", 200, auth="tokenAdmin"),
        req("Auditoria como cidadã (403)", "GET", "/api/v1/admin/auditoria", 403, auth="tokenMaria", testes=[erro("SEM_PERMISSAO")]),
    ]))

    return {
        "info": {"name": "Cartão SUS Inteligente API", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
                 "description": "Coleção executável em ordem (Postman ou Newman). Defina as variáveis senhaDemo, segredoTotpMaria, "
                                "chaveTerminalUbs e chaveTensiometro (valores do seu .env e do console da API). Gerada por docs/postman/gerar_colecao.py."},
        "item": [{"name": n, "item": itens} for n, itens in pastas],
        "variable": [
            {"key": "baseUrl", "value": "http://localhost:8080"},
            {"key": "senhaDemo", "value": "<SENHA_DEMO do .env>"},
            {"key": "segredoTotpMaria", "value": "<segredo TOTP da Maria impresso no console da API>"},
            {"key": "chaveTerminalUbs", "value": "<IOT_KEY_TERMINAL_UBS do .env>"},
            {"key": "chaveTensiometro", "value": "<IOT_KEY_TENSIOMETRO do .env>"},
        ],
    }


if __name__ == "__main__":
    SAIDA.write_text(json.dumps(colecao(), indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("Coleção gerada:", SAIDA)
