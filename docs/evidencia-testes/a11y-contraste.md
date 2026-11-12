# Tabela de contraste (WCAG 2.1)

Calculada em 12/11/2026 a partir das cores de `mobile/app/src/main/java/br/edu/cartaosus/ui/tema/Tema.kt`, com a fórmula de luminância da WCAG 2.1. O mesmo cálculo roda no teste automático `ContrasteTest`.

Mínimos: **4,5:1** para texto comum (nível AA) e **7:1** para nível AAA.

## Tema claro

| Uso | Texto | Fundo | Contraste | AA (4,5) | AAA (7) |
|---|---|---|---|---|---|
| Texto no fundo da tela | `#14181F` | `#F6F8FB` | 16,72:1 | Sim | Sim |
| Texto em cartões | `#14181F` | `#FFFFFF` | 17,79:1 | Sim | Sim |
| Texto secundário (dicas, datas) em cartões | `#394150` | `#FFFFFF` | 10,26:1 | Sim | Sim |
| Texto secundário no fundo | `#394150` | `#F6F8FB` | 9,65:1 | Sim | Sim |
| Links e botões de contorno no fundo | `#0B4FA8` | `#F6F8FB` | 7,32:1 | Sim | Sim |
| Links e botões de contorno em cartões | `#0B4FA8` | `#FFFFFF` | 7,78:1 | Sim | Sim |
| Texto de botão principal | `#FFFFFF` | `#0B4FA8` | 7,78:1 | Sim | Sim |
| Aviso informativo / cartão do código | `#001B3E` | `#D9E6FA` | 13,58:1 | Sim | Sim |
| Aviso de sucesso | `#00210E` | `#D8F3E1` | 14,57:1 | Sim | Sim |
| Aviso de atenção | `#2B1700` | `#FFEFD1` | 15,11:1 | Sim | Sim |
| Aviso de erro | `#410002` | `#FDE8E6` | 14,57:1 | Sim | Sim |
| Texto de erro em cartões | `#B3261E` | `#FFFFFF` | 6,54:1 | Sim | Não |
| Texto sobre botão de erro | `#FFFFFF` | `#B3261E` | 6,54:1 | Sim | Não |

## Tema escuro

| Uso | Texto | Fundo | Contraste | AA (4,5) | AAA (7) |
|---|---|---|---|---|---|
| Texto no fundo da tela | `#E3E7EF` | `#101319` | 15,0:1 | Sim | Sim |
| Texto em cartões | `#E3E7EF` | `#181C23` | 13,78:1 | Sim | Sim |
| Texto secundário (dicas, datas) em cartões | `#C3CAD6` | `#181C23` | 10,36:1 | Sim | Sim |
| Texto secundário no fundo | `#C3CAD6` | `#101319` | 11,28:1 | Sim | Sim |
| Links e botões de contorno no fundo | `#A9C7FF` | `#101319` | 10,89:1 | Sim | Sim |
| Links e botões de contorno em cartões | `#A9C7FF` | `#181C23` | 10,01:1 | Sim | Sim |
| Texto de botão principal | `#002F67` | `#A9C7FF` | 7,68:1 | Sim | Sim |
| Aviso informativo / cartão do código | `#D9E6FA` | `#0B4FA8` | 6,17:1 | Sim | Não |
| Aviso de sucesso | `#D8F3E1` | `#0F7B3F` | 4,54:1 | Sim | Não |
| Aviso de atenção | `#FFEFD1` | `#633B00` | 8,59:1 | Sim | Sim |
| Aviso de erro | `#FDE8E6` | `#8C1D18` | 7,75:1 | Sim | Sim |
| Texto de erro em cartões | `#FFB4AB` | `#181C23` | 10,06:1 | Sim | Sim |
| Texto sobre botão de erro | `#690005` | `#FFB4AB` | 7,72:1 | Sim | Sim |
