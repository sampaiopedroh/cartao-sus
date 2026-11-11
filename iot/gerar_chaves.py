"""Gera as chaves X-API-KEY dos dispositivos IoT (SDD 8.5 e 9.1).

- Chaves aleatórias gravadas no .env (ignorado pelo git). Chaves já existentes não são trocadas.
- Hash SHA-256 gravado em database/seed_dispositivos.sql (ignorado pelo git). O banco guarda só o hash.

Uso (na pasta cartao-sus/):
    python3 iot/gerar_chaves.py
    docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" cartao_sus' < database/seed_dispositivos.sql
"""
import hashlib
import pathlib
import secrets
import sys

RAIZ = pathlib.Path(__file__).resolve().parent.parent
ENV = RAIZ / ".env"
SAIDA_SQL = RAIZ / "database" / "seed_dispositivos.sql"

# nome da variável -> (tipo, ID_UNIDADE, ID_CIDADAO)
DISPOSITIVOS = {
    "IOT_KEY_TERMINAL_UBS": ("TERMINAL_NFC", 1, None),      # UBS Jardim Colombo (CNES 9186492)
    "IOT_KEY_TERMINAL_UPA": ("TERMINAL_NFC", 2, None),      # UPA Vila Prudente (CNES 8530378)
    "IOT_KEY_TENSIOMETRO": ("TENSIOMETRO", None, 2),        # tensiômetro do João (cidadão 2)
}


def ler_env() -> dict:
    if not ENV.exists():
        sys.exit("Crie o .env antes: cp .env.example .env")
    valores = {}
    for linha in ENV.read_text(encoding="utf-8").splitlines():
        if "=" in linha and not linha.lstrip().startswith("#"):
            chave, valor = linha.split("=", 1)
            valores[chave.strip()] = valor.strip()
    return valores


def gravar_env(valores: dict) -> None:
    linhas = []
    vistos = set()
    for linha in ENV.read_text(encoding="utf-8").splitlines():
        if "=" in linha and not linha.lstrip().startswith("#"):
            chave = linha.split("=", 1)[0].strip()
            if chave in valores:
                linhas.append(f"{chave}={valores[chave]}")
                vistos.add(chave)
                continue
        linhas.append(linha)
    ENV.write_text("\n".join(linhas) + "\n", encoding="utf-8")


def main() -> None:
    valores = ler_env()
    novas = {}
    for nome in DISPOSITIVOS:
        if not valores.get(nome):
            novas[nome] = secrets.token_hex(16)  # 32 caracteres hexadecimais
            valores[nome] = novas[nome]

    if novas:
        gravar_env({**novas})
        print(f"Chaves geradas e gravadas no .env: {', '.join(novas)}")
    else:
        print("Chaves já existem no .env; nenhuma foi alterada.")

    linhas = [
        "-- Gerado por iot/gerar_chaves.py. NÃO versionar (ignorado pelo git).",
        "USE cartao_sus;",
    ]
    for nome, (tipo, unidade, cidadao) in DISPOSITIVOS.items():
        h = hashlib.sha256(valores[nome].encode("utf-8")).hexdigest()
        un = "NULL" if unidade is None else str(unidade)
        ci = "NULL" if cidadao is None else str(cidadao)
        linhas.append(
            f"INSERT INTO TB_DISPOSITIVO_IOT (ID_UNIDADE, ID_CIDADAO, TIPO, CHAVE_API_HASH, ATIVO) "
            f"VALUES ({un}, {ci}, '{tipo}', '{h}', 1);"
        )
    SAIDA_SQL.write_text("\n".join(linhas) + "\n", encoding="utf-8")
    print(f"Hashes gravados em {SAIDA_SQL.relative_to(RAIZ)}")


if __name__ == "__main__":
    main()
