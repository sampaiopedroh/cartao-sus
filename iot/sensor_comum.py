"""Funções comuns aos sensores simulados (SDD 9.1).

Os sensores só enviam dados à API. Não há regra de negócio aqui.
Configuração vem de variáveis de ambiente; se faltarem, são lidas do .env da pasta cartao-sus/.
"""
import os
import pathlib
import sys
import time

import requests

RAIZ = pathlib.Path(__file__).resolve().parent.parent
TIMEOUT_SEGUNDOS = 10


def carregar_env() -> None:
    """Carrega o .env da pasta cartao-sus/ para o ambiente, sem sobrescrever o que já existe."""
    arquivo = RAIZ / ".env"
    if not arquivo.exists():
        return
    for linha in arquivo.read_text(encoding="utf-8").splitlines():
        if "=" in linha and not linha.lstrip().startswith("#"):
            chave, valor = linha.split("=", 1)
            os.environ.setdefault(chave.strip(), valor.strip())


def exigir(nome: str) -> str:
    valor = os.environ.get(nome, "").strip()
    if not valor:
        sys.exit(f"Variável {nome} não definida. Rode python3 iot/gerar_chaves.py e confira o .env.")
    return valor


def api_url(caminho: str) -> str:
    base = os.environ.get("API_URL", "http://localhost:8080").rstrip("/")
    return base + caminho


def enviar(metodo: str, caminho: str, cabecalhos: dict, corpo: dict) -> tuple[int, dict | str]:
    """Envia a requisição e devolve (status, corpo). Erro de conexão sai com código 2."""
    try:
        resp = requests.request(metodo, api_url(caminho), headers=cabecalhos, json=corpo, timeout=TIMEOUT_SEGUNDOS)
    except requests.RequestException as erro:
        print(f"[{horario()}] ERRO de conexão com a API ({os.environ.get('API_URL', 'http://localhost:8080')}): {erro.__class__.__name__}")
        sys.exit(2)
    try:
        return resp.status_code, resp.json()
    except ValueError:
        return resp.status_code, resp.text


def horario() -> str:
    return time.strftime("%H:%M:%S")


def imprimir(rotulo: str, status: int, corpo) -> None:
    resumo = corpo if isinstance(corpo, str) else {k: v for k, v in corpo.items() if k != "campos"}
    print(f"[{horario()}] {rotulo} -> HTTP {status}: {resumo}")
