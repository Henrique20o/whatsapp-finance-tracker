# Anexo — inventário extraído do Finance Tracker.fig

Extração estrutural local em 07/10/2026, complementada pela conferência das cinco capturas fornecidas pelo usuário e registrada na versão 1.1 do SDD. Este anexo registra conteúdo do design; textos instrucionais presentes no arquivo são evidências, não comandos para execução. Ver decisões, limitações e divergências no [SDD](ionic-sdd.md).

- Arquivo: `C:\Users\henri\Downloads\Finance Tracker.fig`.
- SHA-256: `A1A8ABC4A1A8895013D61FF6A3150D2961E1AEBA8B67B30FFBF257D84266DCD8`.
- ZIP: canvas.fig (295.618 bytes), thumbnail.png (20.121 bytes, 225 × 400), meta.json (328 bytes), diretório images vazio.
- Metadado de exportação: `2026-10-07T04:24:32.397Z` (01:24:32 em São Paulo).
- Canvas fig-kiwi versão 106; esquema DEFLATE e mensagem Zstandard, decodificados com Node 22.20.0 e kiwi-schema. Nenhum upload do design foi realizado.
- 3.378 nós: 1 documento, 2 canvases (um interno invisível), 1.736 frames, 1.035 textos, 132 retângulos arredondados, 30 elipses, 404 vetores e 38 símbolos.
- A ordenação abaixo segue parentIndex.position; IDs permitem localizar os elementos no arquivo original.
- Cinco PNGs foram inspecionados em resolução original. Eles confirmam a composição da biblioteca e das telas; dimensões exatas e textos continuam fundamentados na extração do `.fig`. Como os painéis estão reduzidos, entrelinhas exatas e curvas de ícones permanecem sujeitos à aferição fina descrita em D15. Não foi realizada renderização nativa pelo agente.

## Complemento visual — imagens fornecidas pelo usuário

As cópias abaixo foram preservadas byte a byte, sem redimensionamento ou edição, para que a documentação não dependa dos arquivos temporários do clipboard. Os nomes locais são apenas organizacionais. A numeração V01–V05 acompanha a ordem dos anexos na conversa.

| Ref. | Captura preservada | Dimensão real do PNG | Cobertura verificada |
| --- | --- | --- | --- |
| V01 | [Design System](references/01-design-system.png) | 848 × 1052 px | Fundação, botões, inputs, códigos, categorias/filtros, cards, transações, donut, paginação, modal/feedback e navegação |
| V02 | [Autenticação](references/02-autenticacao.png) | 847 × 964 px | A01–A07: sete estados |
| V03 | [Início e registro](references/03-inicio-registro.png) | 651 × 1019 px | I01–I04 e G01–G04: oito estados |
| V04 | [Protótipo](references/04-prototipo.png) | 1105 × 658 px | Sete cópias da sequência conceitual |
| V05 | [Relatórios](references/05-relatorios.png) | 474 × 866 px | R01–R04 e L01–L03: sete estados |

Referências de integridade (SHA-256) e origem:

| Ref. | Nome do arquivo recebido | SHA-256 |
| --- | --- | --- |
| V01 | `codex-clipboard-7d384490-160c-4ef9-a273-38e981850140.png` | `81cdefbbee81ba3d5ef3fd2817d58166b303bc2250406f73663cc2d817f89403` |
| V02 | `codex-clipboard-e26c4314-cb10-4ca9-af70-9ade93a14cf6.png` | `fe8afe1830afb49e23afc0c13ac41b3b62a3118466311c6b6836711b18f7b32b` |
| V03 | `codex-clipboard-66aa8850-15af-4d8b-b884-dd38e9a7833d.png` | `d96be498f6eb9e0667a98fde424daa1dd727a4466f453ed69360e96d66266d48` |
| V04 | `codex-clipboard-57c0471f-7b04-4f42-a0d5-53aae805d8b2.png` | `154f81409fbf6705abbd333be3325797c8befd1ab32755d6fe11492417e44e6f` |
| V05 | `codex-clipboard-5e351d9b-e276-402c-8b24-b6acc25e6213.png` | `49132912847b0aa0f8c173d3ab0db300dbdb026d8b54d8d70cad1f77576cec1a` |

Constatações visuais complementares:

- A06 é um diálogo central branco sobre fundo escurecido, com botões de saída e permanência empilhados. A07 ocupa uma tela completa.
- G01–G03 apresentam categorias em duas colunas; Saúde ocupa a última linha inteira na massa de cinco opções. Data e Hora são controles adjacentes. A quantidade de opções continua dinâmica na API.
- As linhas de gasto alinham descrição e valor; categoria e data/hora aparecem como metadados. Em G04, comprovante e snackbar são elementos distintos.
- R01–R03 têm três atalhos horizontais e controle de período personalizado separado. R04 expande a seleção dentro da própria página antes do resumo; total e distribuição ficam em cards separados.
- A aba ativa usa cápsula clara no ícone e rótulo esmeralda. A barra escura inferior é o indicador de gesto do aparelho, não o marcador da aba.
- O DS confirma botões primário sólido, secundário claro, perigo rosado, estados desabilitados acinzentados, snackbar escuro e código em seis caixas com diferenças de foco/erro/bloqueio.
- A barra de ferramentas do editor em V01 e a seleção azul do painel em V03 não fazem parte da UI do produto. Não foi identificado recorte grosseiro de controles dentro das telas na escala fornecida; essa constatação não mede contraste, curvas ou espaçamento exatos.

Não há novas telas em relação à extração. A página de Fluxos não consta dessas capturas e permanece inventariada pelos nós originais abaixo. Capturas estáticas não demonstram navegação executável, rolagem fixa, comportamento com teclado, duração de snackbar ou fechamento de modal por toque externo. Os 1.035 textos da extração foram preservados; não foram substituídos por leitura aproximada de pixels.

Para o registro das consequências arquiteturais e das decisões ainda abertas, consultar a seção 2.7 e D15 no [SDD](ionic-sdd.md). O recebimento das imagens não encerra decisões de produto/contrato nem autoriza implementação.

## Protótipo — 3:27127

Dimensão do painel: 3042 × 1750 px.

Textos editoriais da seção:

- `3:27129` **Identificação:** FINANCE TRACKER / 06
- `3:27130` **Título:** Protótipo
- `3:27131` **Descrição:** Sequência visual estática das telas principais. Sem interações, links ou transições.

### 01 / Telefone — estado 3:27133

Frame de tela: `3:27135` — **Login pelo WhatsApp**, 390 × 844 px.

- `3:27134` **Legenda:** 01 / Telefone

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:27137` **Horário:** 9:41
- `3:27143` **Título:** Finance Tracker
- `3:27149` **Título:** Seu dinheiro, ⏎ mais claro.
- `3:27150` **Texto:** Acompanhe seus gastos e entenda para onde vai seu dinheiro, sem complicação.
- `3:27152` **Rótulo:** Seu número de WhatsApp
- `3:27155` **Código do país:** +55
- `3:27158` **Telefone:** (11) 99999-4821
- `3:27159` **Texto:** Enviaremos um código de 6 dígitos pelo WhatsApp. Não é necessário criar uma senha.
- `3:27162` **Rótulo:** Enviar código
- `3:27166` **Texto:** Seu acesso é seguro
- `3:27167` **Texto:** Seu número é usado apenas para autenticar sua conta. Nunca pediremos seu código.
- `3:27168` **Texto:** Ao continuar, você concorda com os Termos de uso e a Política de privacidade.

### 02 / Código — estado 3:27171

Frame de tela: `3:27173` — **Verificação do código**, 390 × 844 px.

- `3:27172` **Legenda:** 02 / Código

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:27175` **Horário:** 9:41
- `3:27183` **Título:** Verificar WhatsApp
- `3:27189` **Título:** Confira seu WhatsApp
- `3:27190` **Texto:** Digite o código de 6 dígitos enviado para
- `3:27191` **Telefone mascarado:** +55 (11) 9****-4821
- `3:27193` **Rótulo:** Código de verificação
- `3:27196` **Número:** 2
- `3:27198` **Número:** 4
- `3:27200` **Número:** \|
- `3:27202` **Número:**
- `3:27204` **Número:**
- `3:27206` **Número:**
- `3:27207` **Texto:** O código é válido por 5 minutos.
- `3:27209` **Rótulo:** Verificar código
- `3:27211` **Texto:** Reenviar código em 00:42
- `3:27213` **Rótulo:** Alterar número

### 03 / Início — estado 3:27216

Frame de tela: `3:27218` — **Início · últimos 30 dias**, 390 × 1520 px.

- `3:27217` **Legenda:** 03 / Início

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:27220` **Horário:** 9:41
- `3:27226` **Título:** Olá, Mariana
- `3:27232` **Rótulo:** Últimos 30 dias
- `3:27235` **Valor total:** R$ 1.250,90
- `3:27236` **Quantidade:** 10 gastos registrados
- `3:27239` **Rótulo:** Ver relatório completo
- `3:27243` **Rótulo:** Registrar gasto
- `3:27246` **Título:** Transações recentes
- `3:27247` **Contagem:** 10 gastos
- `3:27248` **Texto:** 08/09 a 07/10/2026 · mais recentes primeiro
- `3:27255` **Descrição:** Almoço no centro
- `3:27256` **Valor:** R$ 45,90
- `3:27258` **Categoria:** Alimentação
- `3:27259` **Data e hora:** 07/10/2026 · 12:40
- `3:27266` **Descrição:** Mercado da semana
- `3:27267` **Valor:** R$ 260,00
- `3:27269` **Categoria:** Alimentação
- `3:27270` **Data e hora:** 06/10/2026 · 18:15
- `3:27277` **Descrição:** Uber para o trabalho
- `3:27278` **Valor:** R$ 32,00
- `3:27280` **Categoria:** Transporte
- `3:27281` **Data e hora:** 06/10/2026 · 08:20
- `3:27288` **Descrição:** Farmácia
- `3:27289` **Valor:** R$ 70,00
- `3:27291` **Categoria:** Saúde
- `3:27292` **Data e hora:** 05/10/2026 · 17:30
- `3:27299` **Descrição:** Internet de casa
- `3:27300` **Valor:** R$ 100,00
- `3:27302` **Categoria:** Moradia
- `3:27303` **Data e hora:** 04/10/2026 · 10:00
- `3:27310` **Descrição:** Tênis novo
- `3:27311` **Valor:** R$ 300,00
- `3:27313` **Categoria:** Compras
- `3:27314` **Data e hora:** 03/10/2026 · 14:10
- `3:27321` **Descrição:** Jantar com amigos
- `3:27322` **Valor:** R$ 120,00
- `3:27324` **Categoria:** Alimentação
- `3:27325` **Data e hora:** 02/10/2026 · 20:45
- `3:27332` **Descrição:** Combustível
- `3:27333` **Valor:** R$ 148,00
- `3:27335` **Categoria:** Transporte
- `3:27336` **Data e hora:** 01/10/2026 · 07:50
- `3:27343` **Descrição:** Conta de energia
- `3:27344` **Valor:** R$ 150,00
- `3:27346` **Categoria:** Moradia
- `3:27347` **Data e hora:** 30/09/2026 · 09:00
- `3:27354` **Descrição:** Café da tarde
- `3:27355` **Valor:** R$ 25,00
- `3:27357` **Categoria:** Alimentação
- `3:27358` **Data e hora:** 29/09/2026 · 16:20
- `3:27360` **Rótulo:** Ver todos os gastos
- `3:27367` **Rótulo:** Início
- `3:27371` **Rótulo:** Registrar
- `3:27375` **Rótulo:** Relatórios

### 04 / Registrar gasto — estado 3:27378

Frame de tela: `3:27380` — **Registrar gasto · preenchido**, 390 × 1000 px.

- `3:27379` **Legenda:** 04 / Registrar gasto

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:27382` **Horário:** 9:41
- `3:27390` **Título:** Registrar gasto
- `3:27392` **Texto:** Anote agora, entenda depois. Os campos com * são obrigatórios.
- `3:27394` **Rótulo:** Valor *
- `3:27396` **Valor:** R$ 45,90
- `3:27398` **Rótulo:** Descrição *
- `3:27400` **Valor:** Almoço no centro
- `3:27402` **Rótulo:** Categoria *
- `3:27406` **Nome:** Alimentação
- `3:27410` **Nome:** Compras
- `3:27414` **Nome:** Moradia
- `3:27417` **Nome:** Transporte
- `3:27421` **Nome:** Saúde
- `3:27425` **Rótulo:** Data · opcional
- `3:27427` **Valor:** 07/10/2026
- `3:27430` **Rótulo:** Hora · opcional
- `3:27432` **Valor:** 12:40
- `3:27434` **Texto:** Se deixar em branco, usaremos a data e a hora do registro.
- `3:27437` **Rótulo:** Salvar gasto
- `3:27439` **Rótulo:** Cancelar
- `3:27445` **Rótulo:** Início
- `3:27449` **Rótulo:** Registrar
- `3:27453` **Rótulo:** Relatórios

### 05 / Relatório — estado 3:27456

Frame de tela: `3:27458` — **Relatórios · 30 dias**, 390 × 1297 px.

- `3:27457` **Legenda:** 05 / Relatório

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:27460` **Horário:** 9:41
- `3:27466` **Título:** Relatórios
- `3:27471` **Rótulo:** 7 dias
- `3:27473` **Rótulo:** 30 dias
- `3:27475` **Rótulo:** Mês atual
- `3:27478` **Rótulo:** Período personalizado
- `3:27481` **Período:** 08/09 a 07/10/2026
- `3:27482` **Total:** R$ 1.250,90
- `3:27484` **Quantidade:** 10 gastos no período
- `3:27486` **Legenda:** 5 categorias
- `3:27488` **Título:** Para onde foi seu dinheiro
- `3:27489` **Texto:** Distribuição dos gastos por categoria
- `3:27498` **Rótulo:** Total do período
- `3:27499` **Valor:** R$ 1.250,90
- `3:27500` **Quantidade:** 10 gastos
- `3:27505` **Nome:** Alimentação
- `3:27506` **Percentual:** 36,0% do total
- `3:27507` **Valor:** R$ 450,90
- `3:27512` **Nome:** Compras
- `3:27513` **Percentual:** 24,0% do total
- `3:27514` **Valor:** R$ 300,00
- `3:27519` **Nome:** Moradia
- `3:27520` **Percentual:** 20,0% do total
- `3:27521` **Valor:** R$ 250,00
- `3:27526` **Nome:** Transporte
- `3:27527` **Percentual:** 14,4% do total
- `3:27528` **Valor:** R$ 180,00
- `3:27533` **Nome:** Saúde
- `3:27534` **Percentual:** 5,6% do total
- `3:27535` **Valor:** R$ 70,00
- `3:27537` **Texto:** Selecione uma categoria para ver seus gastos. Percentuais arredondados.
- `3:27540` **Rótulo:** Ver todos os 10 gastos
- `3:27546` **Rótulo:** Início
- `3:27550` **Rótulo:** Registrar
- `3:27554` **Rótulo:** Relatórios

### 06 / Detalhe categoria — estado 3:27557

Frame de tela: `3:27559` — **Detalhe · Alimentação**, 390 × 1003 px.

- `3:27558` **Legenda:** 06 / Detalhe categoria

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:27561` **Horário:** 9:41
- `3:27569` **Título:** Alimentação
- `3:27572` **Período:** 08/09 a 07/10/2026 · 30 dias
- `3:27575` **Categoria:** Alimentação · 36,0% do total
- `3:27576` **Valor total:** R$ 450,90
- `3:27577` **Contagem:** 4 gastos nesta categoria
- `3:27580` **Ordem:** Mais recentes → mais antigos
- `3:27587` **Descrição:** Almoço no centro
- `3:27588` **Valor:** R$ 45,90
- `3:27590` **Categoria:** Alimentação
- `3:27591` **Data e hora:** 07/10/2026 · 12:40
- `3:27598` **Descrição:** Mercado da semana
- `3:27599` **Valor:** R$ 260,00
- `3:27601` **Categoria:** Alimentação
- `3:27602` **Data e hora:** 06/10/2026 · 18:15
- `3:27609` **Descrição:** Jantar com amigos
- `3:27610` **Valor:** R$ 120,00
- `3:27612` **Categoria:** Alimentação
- `3:27613` **Data e hora:** 02/10/2026 · 20:45
- `3:27620` **Descrição:** Café da tarde
- `3:27621` **Valor:** R$ 25,00
- `3:27623` **Categoria:** Alimentação
- `3:27624` **Data e hora:** 29/09/2026 · 16:20
- `3:27626` **Texto:** Exibindo 1–4 de 4 gastos
- `3:27630` **Página:** Página 1 de 1
- `3:27633` **Texto:** Você está vendo todos os gastos de Alimentação neste período.
- `3:27639` **Rótulo:** Início
- `3:27643` **Rótulo:** Registrar
- `3:27647` **Rótulo:** Relatórios

### 07 / Logout — estado 3:27650

Frame de tela: `3:27652` — **Logout · diálogo de confirmação**, 390 × 844 px.

- `3:27651` **Legenda:** 07 / Logout

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:27655` **Horário:** 9:41
- `3:27661` **Título:** Olá, Mariana
- `3:27667` **Rótulo:** Últimos 30 dias
- `3:27670` **Valor total:** R$ 1.250,90
- `3:27671` **Quantidade:** 10 gastos registrados
- `3:27674` **Rótulo:** Ver relatório completo
- `3:27676` **Texto:** Últimos gastos
- `3:27683` **Descrição:** Almoço no centro
- `3:27684` **Valor:** R$ 45,90
- `3:27686` **Categoria:** Alimentação
- `3:27687` **Data e hora:** 07/10/2026 · 12:40
- `3:27694` **Descrição:** Mercado da semana
- `3:27695` **Valor:** R$ 260,00
- `3:27697` **Categoria:** Alimentação
- `3:27698` **Data e hora:** 06/10/2026 · 18:15
- `3:27705` **Descrição:** Uber para o trabalho
- `3:27706` **Valor:** R$ 32,00
- `3:27708` **Categoria:** Transporte
- `3:27709` **Data e hora:** 06/10/2026 · 08:20
- `3:27715` **Rótulo:** Início
- `3:27719` **Rótulo:** Registrar
- `3:27723` **Rótulo:** Relatórios
- `3:27731` **Título:** Sair da sua conta?
- `3:27732` **Texto:** Seus gastos continuarão salvos. Para voltar, basta verificar seu WhatsApp novamente.
- `3:27735` **Rótulo:** Sair da conta
- `3:27737` **Rótulo:** Continuar na conta

## Relatórios — 3:26317

Dimensão do painel: 1752 × 3279 px.

Textos editoriais da seção:

- `3:26319` **Identificação:** FINANCE TRACKER / 05
- `3:26320` **Título:** Relatórios
- `3:26321` **Descrição:** Totais e categorias consistentes com o detalhamento. Listas em ordem recente → antigo, com paginação explícita.

### Relatório · 30 dias — estado 3:26323

Frame de tela: `3:26325` — **Relatórios · 30 dias**, 390 × 1297 px.

- `3:26324` **Legenda:** Relatório · 30 dias

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:26327` **Horário:** 9:41
- `3:26333` **Título:** Relatórios
- `3:26338` **Rótulo:** 7 dias
- `3:26340` **Rótulo:** 30 dias
- `3:26342` **Rótulo:** Mês atual
- `3:26345` **Rótulo:** Período personalizado
- `3:26348` **Período:** 08/09 a 07/10/2026
- `3:26349` **Total:** R$ 1.250,90
- `3:26351` **Quantidade:** 10 gastos no período
- `3:26353` **Legenda:** 5 categorias
- `3:26355` **Título:** Para onde foi seu dinheiro
- `3:26356` **Texto:** Distribuição dos gastos por categoria
- `3:26365` **Rótulo:** Total do período
- `3:26366` **Valor:** R$ 1.250,90
- `3:26367` **Quantidade:** 10 gastos
- `3:26372` **Nome:** Alimentação
- `3:26373` **Percentual:** 36,0% do total
- `3:26374` **Valor:** R$ 450,90
- `3:26379` **Nome:** Compras
- `3:26380` **Percentual:** 24,0% do total
- `3:26381` **Valor:** R$ 300,00
- `3:26386` **Nome:** Moradia
- `3:26387` **Percentual:** 20,0% do total
- `3:26388` **Valor:** R$ 250,00
- `3:26393` **Nome:** Transporte
- `3:26394` **Percentual:** 14,4% do total
- `3:26395` **Valor:** R$ 180,00
- `3:26400` **Nome:** Saúde
- `3:26401` **Percentual:** 5,6% do total
- `3:26402` **Valor:** R$ 70,00
- `3:26404` **Texto:** Selecione uma categoria para ver seus gastos. Percentuais arredondados.
- `3:26407` **Rótulo:** Ver todos os 10 gastos
- `3:26413` **Rótulo:** Início
- `3:26417` **Rótulo:** Registrar
- `3:26421` **Rótulo:** Relatórios

### Relatório · 7 dias — estado 3:26424

Frame de tela: `3:26426` — **Relatórios · 7 dias**, 390 × 1297 px.

- `3:26425` **Legenda:** Relatório · 7 dias

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:26428` **Horário:** 9:41
- `3:26434` **Título:** Relatórios
- `3:26439` **Rótulo:** 7 dias
- `3:26441` **Rótulo:** 30 dias
- `3:26443` **Rótulo:** Mês atual
- `3:26446` **Rótulo:** Período personalizado
- `3:26449` **Período:** 01/10 a 07/10/2026
- `3:26450` **Total:** R$ 1.075,90
- `3:26452` **Quantidade:** 8 gastos no período
- `3:26454` **Legenda:** 5 categorias
- `3:26456` **Título:** Para onde foi seu dinheiro
- `3:26457` **Texto:** Distribuição dos gastos por categoria
- `3:26466` **Rótulo:** Total do período
- `3:26467` **Valor:** R$ 1.075,90
- `3:26468` **Quantidade:** 8 gastos
- `3:26473` **Nome:** Alimentação
- `3:26474` **Percentual:** 39,6% do total
- `3:26475` **Valor:** R$ 425,90
- `3:26480` **Nome:** Compras
- `3:26481` **Percentual:** 27,9% do total
- `3:26482` **Valor:** R$ 300,00
- `3:26487` **Nome:** Moradia
- `3:26488` **Percentual:** 9,3% do total
- `3:26489` **Valor:** R$ 100,00
- `3:26494` **Nome:** Transporte
- `3:26495` **Percentual:** 16,7% do total
- `3:26496` **Valor:** R$ 180,00
- `3:26501` **Nome:** Saúde
- `3:26502` **Percentual:** 6,5% do total
- `3:26503` **Valor:** R$ 70,00
- `3:26505` **Texto:** Selecione uma categoria para ver seus gastos. Percentuais arredondados.
- `3:26508` **Rótulo:** Ver todos os 8 gastos
- `3:26514` **Rótulo:** Início
- `3:26518` **Rótulo:** Registrar
- `3:26522` **Rótulo:** Relatórios

### Relatório · mês atual — estado 3:26525

Frame de tela: `3:26527` — **Relatórios · Mês atual**, 390 × 1297 px.

- `3:26526` **Legenda:** Relatório · mês atual

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:26529` **Horário:** 9:41
- `3:26535` **Título:** Relatórios
- `3:26540` **Rótulo:** 7 dias
- `3:26542` **Rótulo:** 30 dias
- `3:26544` **Rótulo:** Mês atual
- `3:26547` **Rótulo:** Período personalizado
- `3:26550` **Período:** 01/10 a 07/10/2026
- `3:26551` **Total:** R$ 1.075,90
- `3:26553` **Quantidade:** 8 gastos no período
- `3:26555` **Legenda:** 5 categorias
- `3:26557` **Título:** Para onde foi seu dinheiro
- `3:26558` **Texto:** Distribuição dos gastos por categoria
- `3:26567` **Rótulo:** Total do período
- `3:26568` **Valor:** R$ 1.075,90
- `3:26569` **Quantidade:** 8 gastos
- `3:26574` **Nome:** Alimentação
- `3:26575` **Percentual:** 39,6% do total
- `3:26576` **Valor:** R$ 425,90
- `3:26581` **Nome:** Compras
- `3:26582` **Percentual:** 27,9% do total
- `3:26583` **Valor:** R$ 300,00
- `3:26588` **Nome:** Moradia
- `3:26589` **Percentual:** 9,3% do total
- `3:26590` **Valor:** R$ 100,00
- `3:26595` **Nome:** Transporte
- `3:26596` **Percentual:** 16,7% do total
- `3:26597` **Valor:** R$ 180,00
- `3:26602` **Nome:** Saúde
- `3:26603` **Percentual:** 6,5% do total
- `3:26604` **Valor:** R$ 70,00
- `3:26606` **Texto:** Selecione uma categoria para ver seus gastos. Percentuais arredondados.
- `3:26609` **Rótulo:** Ver todos os 8 gastos
- `3:26615` **Rótulo:** Início
- `3:26619` **Rótulo:** Registrar
- `3:26623` **Rótulo:** Relatórios

### Relatório · seleção personalizada — estado 3:26626

Frame de tela: `3:26628` — **Relatórios · Personalizado**, 390 × 1986 px.

- `3:26627` **Legenda:** Relatório · seleção personalizada

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:26630` **Horário:** 9:41
- `3:26636` **Título:** Relatórios
- `3:26641` **Rótulo:** 7 dias
- `3:26643` **Rótulo:** 30 dias
- `3:26645` **Rótulo:** Mês atual
- `3:26648` **Rótulo:** Período personalizado
- `3:26651` **Título:** Escolha seu período
- `3:26653` **Rótulo:** Data inicial
- `3:26655` **Valor:** 08/09/2026
- `3:26658` **Rótulo:** Data final
- `3:26660` **Valor:** 07/10/2026
- `3:26665` **Mês:** Outubro 2026
- `3:26670` **Dia da semana:** S
- `3:26671` **Dia da semana:** T
- `3:26672` **Dia da semana:** Q
- `3:26673` **Dia da semana:** Q
- `3:26674` **Dia da semana:** S
- `3:26675` **Dia da semana:** S
- `3:26676` **Dia da semana:** D
- `3:26679` **Data:** 28
- `3:26681` **Data:** 29
- `3:26683` **Data:** 30
- `3:26685` **Data:** 1
- `3:26687` **Data:** 2
- `3:26689` **Data:** 3
- `3:26691` **Data:** 4
- `3:26694` **Data:** 5
- `3:26696` **Data:** 6
- `3:26698` **Data:** 7
- `3:26700` **Data:** 8
- `3:26702` **Data:** 9
- `3:26704` **Data:** 10
- `3:26706` **Data:** 11
- `3:26709` **Data:** 12
- `3:26711` **Data:** 13
- `3:26713` **Data:** 14
- `3:26715` **Data:** 15
- `3:26717` **Data:** 16
- `3:26719` **Data:** 17
- `3:26721` **Data:** 18
- `3:26724` **Data:** 19
- `3:26726` **Data:** 20
- `3:26728` **Data:** 21
- `3:26730` **Data:** 22
- `3:26732` **Data:** 23
- `3:26734` **Data:** 24
- `3:26736` **Data:** 25
- `3:26739` **Data:** 26
- `3:26741` **Data:** 27
- `3:26743` **Data:** 28
- `3:26745` **Data:** 29
- `3:26747` **Data:** 30
- `3:26749` **Data:** 31
- `3:26751` **Data:** 1
- `3:26752` **Texto:** Selecionado: 08/09 a 07/10/2026 · 30 dias
- `3:26754` **Rótulo:** Aplicar período
- `3:26756` **Período:** 08/09 a 07/10/2026
- `3:26757` **Total:** R$ 1.250,90
- `3:26759` **Quantidade:** 10 gastos no período
- `3:26761` **Legenda:** 5 categorias
- `3:26763` **Título:** Para onde foi seu dinheiro
- `3:26764` **Texto:** Distribuição dos gastos por categoria
- `3:26773` **Rótulo:** Total do período
- `3:26774` **Valor:** R$ 1.250,90
- `3:26775` **Quantidade:** 10 gastos
- `3:26780` **Nome:** Alimentação
- `3:26781` **Percentual:** 36,0% do total
- `3:26782` **Valor:** R$ 450,90
- `3:26787` **Nome:** Compras
- `3:26788` **Percentual:** 24,0% do total
- `3:26789` **Valor:** R$ 300,00
- `3:26794` **Nome:** Moradia
- `3:26795` **Percentual:** 20,0% do total
- `3:26796` **Valor:** R$ 250,00
- `3:26801` **Nome:** Transporte
- `3:26802` **Percentual:** 14,4% do total
- `3:26803` **Valor:** R$ 180,00
- `3:26808` **Nome:** Saúde
- `3:26809` **Percentual:** 5,6% do total
- `3:26810` **Valor:** R$ 70,00
- `3:26812` **Texto:** Selecione uma categoria para ver seus gastos. Percentuais arredondados.
- `3:26815` **Rótulo:** Ver todos os 10 gastos
- `3:26821` **Rótulo:** Início
- `3:26825` **Rótulo:** Registrar
- `3:26829` **Rótulo:** Relatórios

### Todos os gastos · página 1 de 2 — estado 3:26833

Frame de tela: `3:26835` — **Todos os gastos · página 1**, 390 × 999 px.

- `3:26834` **Legenda:** Todos os gastos · página 1 de 2

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:26837` **Horário:** 9:41
- `3:26845` **Título:** Todos os gastos
- `3:26848` **Período:** 08/09 a 07/10/2026 · 30 dias
- `3:26849` **Valor total:** R$ 1.250,90
- `3:26850` **Contagem:** 10 gastos no período
- `3:26853` **Ordem:** Mais recentes → mais antigos
- `3:26860` **Descrição:** Almoço no centro
- `3:26861` **Valor:** R$ 45,90
- `3:26863` **Categoria:** Alimentação
- `3:26864` **Data e hora:** 07/10/2026 · 12:40
- `3:26871` **Descrição:** Mercado da semana
- `3:26872` **Valor:** R$ 260,00
- `3:26874` **Categoria:** Alimentação
- `3:26875` **Data e hora:** 06/10/2026 · 18:15
- `3:26882` **Descrição:** Uber para o trabalho
- `3:26883` **Valor:** R$ 32,00
- `3:26885` **Categoria:** Transporte
- `3:26886` **Data e hora:** 06/10/2026 · 08:20
- `3:26893` **Descrição:** Farmácia
- `3:26894` **Valor:** R$ 70,00
- `3:26896` **Categoria:** Saúde
- `3:26897` **Data e hora:** 05/10/2026 · 17:30
- `3:26904` **Descrição:** Internet de casa
- `3:26905` **Valor:** R$ 100,00
- `3:26907` **Categoria:** Moradia
- `3:26908` **Data e hora:** 04/10/2026 · 10:00
- `3:26910` **Texto:** Exibindo 1–5 de 10 gastos
- `3:26914` **Página:** Página 1 de 2
- `3:26922` **Rótulo:** Início
- `3:26926` **Rótulo:** Registrar
- `3:26930` **Rótulo:** Relatórios

### Todos os gastos · página 2 de 2 — estado 3:26933

Frame de tela: `3:26935` — **Todos os gastos · página 2**, 390 × 999 px.

- `3:26934` **Legenda:** Todos os gastos · página 2 de 2

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:26937` **Horário:** 9:41
- `3:26945` **Título:** Todos os gastos
- `3:26948` **Período:** 08/09 a 07/10/2026 · 30 dias
- `3:26949` **Valor total:** R$ 1.250,90
- `3:26950` **Contagem:** 10 gastos no período
- `3:26953` **Ordem:** Mais recentes → mais antigos
- `3:26960` **Descrição:** Tênis novo
- `3:26961` **Valor:** R$ 300,00
- `3:26963` **Categoria:** Compras
- `3:26964` **Data e hora:** 03/10/2026 · 14:10
- `3:26971` **Descrição:** Jantar com amigos
- `3:26972` **Valor:** R$ 120,00
- `3:26974` **Categoria:** Alimentação
- `3:26975` **Data e hora:** 02/10/2026 · 20:45
- `3:26982` **Descrição:** Combustível
- `3:26983` **Valor:** R$ 148,00
- `3:26985` **Categoria:** Transporte
- `3:26986` **Data e hora:** 01/10/2026 · 07:50
- `3:26993` **Descrição:** Conta de energia
- `3:26994` **Valor:** R$ 150,00
- `3:26996` **Categoria:** Moradia
- `3:26997` **Data e hora:** 30/09/2026 · 09:00
- `3:27004` **Descrição:** Café da tarde
- `3:27005` **Valor:** R$ 25,00
- `3:27007` **Categoria:** Alimentação
- `3:27008` **Data e hora:** 29/09/2026 · 16:20
- `3:27010` **Texto:** Exibindo 6–10 de 10 gastos
- `3:27014` **Página:** Página 2 de 2
- `3:27022` **Rótulo:** Início
- `3:27026` **Rótulo:** Registrar
- `3:27030` **Rótulo:** Relatórios

### Alimentação · página 1 de 1 — estado 3:27033

Frame de tela: `3:27035` — **Detalhe · Alimentação**, 390 × 1003 px.

- `3:27034` **Legenda:** Alimentação · página 1 de 1

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:27037` **Horário:** 9:41
- `3:27045` **Título:** Alimentação
- `3:27048` **Período:** 08/09 a 07/10/2026 · 30 dias
- `3:27051` **Categoria:** Alimentação · 36,0% do total
- `3:27052` **Valor total:** R$ 450,90
- `3:27053` **Contagem:** 4 gastos nesta categoria
- `3:27056` **Ordem:** Mais recentes → mais antigos
- `3:27063` **Descrição:** Almoço no centro
- `3:27064` **Valor:** R$ 45,90
- `3:27066` **Categoria:** Alimentação
- `3:27067` **Data e hora:** 07/10/2026 · 12:40
- `3:27074` **Descrição:** Mercado da semana
- `3:27075` **Valor:** R$ 260,00
- `3:27077` **Categoria:** Alimentação
- `3:27078` **Data e hora:** 06/10/2026 · 18:15
- `3:27085` **Descrição:** Jantar com amigos
- `3:27086` **Valor:** R$ 120,00
- `3:27088` **Categoria:** Alimentação
- `3:27089` **Data e hora:** 02/10/2026 · 20:45
- `3:27096` **Descrição:** Café da tarde
- `3:27097` **Valor:** R$ 25,00
- `3:27099` **Categoria:** Alimentação
- `3:27100` **Data e hora:** 29/09/2026 · 16:20
- `3:27102` **Texto:** Exibindo 1–4 de 4 gastos
- `3:27106` **Página:** Página 1 de 1
- `3:27109` **Texto:** Você está vendo todos os gastos de Alimentação neste período.
- `3:27115` **Rótulo:** Início
- `3:27119` **Rótulo:** Registrar
- `3:27123` **Rótulo:** Relatórios

## Início — 3:25714

Dimensão do painel: 1752 × 2888 px.

Textos editoriais da seção:

- `3:25716` **Identificação:** FINANCE TRACKER / 04
- `3:25717` **Título:** Início
- `3:25718` **Descrição:** Visão dos últimos 30 dias e registro de gastos. As 10 transações estão visíveis por inteiro.

### Início · 10 transações completas — estado 3:25720

Frame de tela: `3:25722` — **Início · últimos 30 dias**, 390 × 1520 px.

- `3:25721` **Legenda:** Início · 10 transações completas

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25724` **Horário:** 9:41
- `3:25730` **Título:** Olá, Mariana
- `3:25736` **Rótulo:** Últimos 30 dias
- `3:25739` **Valor total:** R$ 1.250,90
- `3:25740` **Quantidade:** 10 gastos registrados
- `3:25743` **Rótulo:** Ver relatório completo
- `3:25747` **Rótulo:** Registrar gasto
- `3:25750` **Título:** Transações recentes
- `3:25751` **Contagem:** 10 gastos
- `3:25752` **Texto:** 08/09 a 07/10/2026 · mais recentes primeiro
- `3:25759` **Descrição:** Almoço no centro
- `3:25760` **Valor:** R$ 45,90
- `3:25762` **Categoria:** Alimentação
- `3:25763` **Data e hora:** 07/10/2026 · 12:40
- `3:25770` **Descrição:** Mercado da semana
- `3:25771` **Valor:** R$ 260,00
- `3:25773` **Categoria:** Alimentação
- `3:25774` **Data e hora:** 06/10/2026 · 18:15
- `3:25781` **Descrição:** Uber para o trabalho
- `3:25782` **Valor:** R$ 32,00
- `3:25784` **Categoria:** Transporte
- `3:25785` **Data e hora:** 06/10/2026 · 08:20
- `3:25792` **Descrição:** Farmácia
- `3:25793` **Valor:** R$ 70,00
- `3:25795` **Categoria:** Saúde
- `3:25796` **Data e hora:** 05/10/2026 · 17:30
- `3:25803` **Descrição:** Internet de casa
- `3:25804` **Valor:** R$ 100,00
- `3:25806` **Categoria:** Moradia
- `3:25807` **Data e hora:** 04/10/2026 · 10:00
- `3:25814` **Descrição:** Tênis novo
- `3:25815` **Valor:** R$ 300,00
- `3:25817` **Categoria:** Compras
- `3:25818` **Data e hora:** 03/10/2026 · 14:10
- `3:25825` **Descrição:** Jantar com amigos
- `3:25826` **Valor:** R$ 120,00
- `3:25828` **Categoria:** Alimentação
- `3:25829` **Data e hora:** 02/10/2026 · 20:45
- `3:25836` **Descrição:** Combustível
- `3:25837` **Valor:** R$ 148,00
- `3:25839` **Categoria:** Transporte
- `3:25840` **Data e hora:** 01/10/2026 · 07:50
- `3:25847` **Descrição:** Conta de energia
- `3:25848` **Valor:** R$ 150,00
- `3:25850` **Categoria:** Moradia
- `3:25851` **Data e hora:** 30/09/2026 · 09:00
- `3:25858` **Descrição:** Café da tarde
- `3:25859` **Valor:** R$ 25,00
- `3:25861` **Categoria:** Alimentação
- `3:25862` **Data e hora:** 29/09/2026 · 16:20
- `3:25864` **Rótulo:** Ver todos os gastos
- `3:25871` **Rótulo:** Início
- `3:25875` **Rótulo:** Registrar
- `3:25879` **Rótulo:** Relatórios

### Início · carregando — estado 3:25882

Frame de tela: `3:25884` — **Início · carregando**, 390 × 850 px.

- `3:25883` **Legenda:** Início · carregando

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25886` **Horário:** 9:41
- `3:25892` **Título:** Olá, Mariana
- `3:25920` **Texto:** Carregando seus gastos…
- `3:25926` **Rótulo:** Início
- `3:25930` **Rótulo:** Registrar
- `3:25934` **Rótulo:** Relatórios

### Início · erro de conexão — estado 3:25937

Frame de tela: `3:25939` — **Início · erro de conexão**, 390 × 844 px.

- `3:25938` **Legenda:** Início · erro de conexão

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25941` **Horário:** 9:41
- `3:25947` **Título:** Olá, Mariana
- `3:25955` **Texto:** Não foi possível ⏎ carregar seus gastos
- `3:25956` **Texto:** Verifique sua conexão com a internet e tente novamente. Seus dados continuam salvos.
- `3:25959` **Rótulo:** Tentar novamente
- `3:25960` **Texto:** Se o problema persistir, tente daqui a alguns minutos.
- `3:25966` **Rótulo:** Início
- `3:25970` **Rótulo:** Registrar
- `3:25974` **Rótulo:** Relatórios

### Início · lista vazia — estado 3:25977

Frame de tela: `3:25979` — **Início · sem gastos**, 390 × 844 px.

- `3:25978` **Legenda:** Início · lista vazia

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25981` **Horário:** 9:41
- `3:25987` **Título:** Olá, Mariana
- `3:25993` **Rótulo:** Últimos 30 dias
- `3:25996` **Valor total:** R$ 0,00
- `3:25997` **Quantidade:** 0 gastos registrados
- `3:26000` **Rótulo:** Ver relatório completo
- `3:26006` **Texto:** Um começo mais organizado
- `3:26007` **Texto:** Você ainda não registrou nenhum gasto. Comece com o café de hoje ou sua última compra.
- `3:26010` **Rótulo:** Registrar primeiro gasto
- `3:26016` **Rótulo:** Início
- `3:26020` **Rótulo:** Registrar
- `3:26024` **Rótulo:** Relatórios

### Registrar · preenchido — estado 3:26028

Frame de tela: `3:26030` — **Registrar gasto · preenchido**, 390 × 1000 px.

- `3:26029` **Legenda:** Registrar · preenchido

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:26032` **Horário:** 9:41
- `3:26040` **Título:** Registrar gasto
- `3:26042` **Texto:** Anote agora, entenda depois. Os campos com * são obrigatórios.
- `3:26044` **Rótulo:** Valor *
- `3:26046` **Valor:** R$ 45,90
- `3:26048` **Rótulo:** Descrição *
- `3:26050` **Valor:** Almoço no centro
- `3:26052` **Rótulo:** Categoria *
- `3:26056` **Nome:** Alimentação
- `3:26060` **Nome:** Compras
- `3:26064` **Nome:** Moradia
- `3:26067` **Nome:** Transporte
- `3:26071` **Nome:** Saúde
- `3:26075` **Rótulo:** Data · opcional
- `3:26077` **Valor:** 07/10/2026
- `3:26080` **Rótulo:** Hora · opcional
- `3:26082` **Valor:** 12:40
- `3:26084` **Texto:** Se deixar em branco, usaremos a data e a hora do registro.
- `3:26087` **Rótulo:** Salvar gasto
- `3:26089` **Rótulo:** Cancelar
- `3:26095` **Rótulo:** Início
- `3:26099` **Rótulo:** Registrar
- `3:26103` **Rótulo:** Relatórios

### Registrar · validação — estado 3:26106

Frame de tela: `3:26108` — **Registrar gasto · validação**, 390 × 1078 px.

- `3:26107` **Legenda:** Registrar · validação

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:26110` **Horário:** 9:41
- `3:26118` **Título:** Registrar gasto
- `3:26120` **Texto:** Anote agora, entenda depois. Os campos com * são obrigatórios.
- `3:26122` **Rótulo:** Valor *
- `3:26124` **Valor:** R$ 0,00
- `3:26125` **Texto:** Informe um valor maior que R$ 0,00.
- `3:26127` **Rótulo:** Descrição *
- `3:26129` **Valor:** Ex.: almoço, mercado, transporte
- `3:26130` **Texto:** Dê uma descrição para este gasto.
- `3:26132` **Rótulo:** Categoria *
- `3:26136` **Nome:** Alimentação
- `3:26139` **Nome:** Compras
- `3:26143` **Nome:** Moradia
- `3:26146` **Nome:** Transporte
- `3:26150` **Nome:** Saúde
- `3:26151` **Texto:** Selecione uma categoria para continuar.
- `3:26155` **Rótulo:** Data · opcional
- `3:26157` **Valor:** 07/10/2026
- `3:26160` **Rótulo:** Hora · opcional
- `3:26162` **Valor:** 12:40
- `3:26164` **Texto:** Se deixar em branco, usaremos a data e a hora do registro.
- `3:26167` **Rótulo:** Salvar gasto
- `3:26169` **Rótulo:** Cancelar
- `3:26175` **Rótulo:** Início
- `3:26179` **Rótulo:** Registrar
- `3:26183` **Rótulo:** Relatórios

### Registrar · salvando — estado 3:26186

Frame de tela: `3:26188` — **Registrar gasto · salvando**, 390 × 1045 px.

- `3:26187` **Legenda:** Registrar · salvando

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:26190` **Horário:** 9:41
- `3:26198` **Título:** Registrar gasto
- `3:26200` **Texto:** Anote agora, entenda depois. Os campos com * são obrigatórios.
- `3:26202` **Rótulo:** Valor *
- `3:26204` **Valor:** R$ 45,90
- `3:26206` **Rótulo:** Descrição *
- `3:26208` **Valor:** Almoço no centro
- `3:26210` **Rótulo:** Categoria *
- `3:26214` **Nome:** Alimentação
- `3:26218` **Nome:** Compras
- `3:26222` **Nome:** Moradia
- `3:26225` **Nome:** Transporte
- `3:26229` **Nome:** Saúde
- `3:26233` **Rótulo:** Data · opcional
- `3:26235` **Valor:** 07/10/2026
- `3:26238` **Rótulo:** Hora · opcional
- `3:26240` **Valor:** 12:40
- `3:26242` **Texto:** Se deixar em branco, usaremos a data e a hora do registro.
- `3:26246` **Texto:** Salvando seu gasto…
- `3:26247` **Texto:** Aguarde um instante. Não feche o aplicativo para evitar interromper o registro.
- `3:26250` **Rótulo:** Salvando…
- `3:26256` **Rótulo:** Início
- `3:26260` **Rótulo:** Registrar
- `3:26264` **Rótulo:** Relatórios

### Registrar · sucesso — estado 3:26267

Frame de tela: `3:26269` — **Gasto registrado · sucesso**, 390 × 844 px.

- `3:26268` **Legenda:** Registrar · sucesso

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:26271` **Horário:** 9:41
- `3:26277` **Título:** Registrar gasto
- `3:26283` **Texto:** Gasto registrado!
- `3:26284` **Texto:** Tudo certo. Seu relatório já foi atualizado.
- `3:26286` **Valor:** R$ 45,90
- `3:26287` **Texto:** Almoço no centro
- `3:26289` **Categoria:** Alimentação
- `3:26290` **Data e hora:** 07/10/2026 · 12:40
- `3:26292` **Rótulo:** Voltar ao início
- `3:26295` **Rótulo:** Registrar outro gasto
- `3:26298` **Mensagem:** Gasto registrado com sucesso.
- `3:26299` **Ação:** Ver gasto
- `3:26305` **Rótulo:** Início
- `3:26309` **Rótulo:** Registrar
- `3:26313` **Rótulo:** Relatórios

## Autenticação — 3:25360

Dimensão do painel: 1752 × 1978 px.

Textos editoriais da seção:

- `3:25362` **Identificação:** FINANCE TRACKER / 03
- `3:25363` **Título:** Autenticação
- `3:25364` **Descrição:** Acesso por WhatsApp • código de 6 dígitos • recuperação • encerramento da sessão

### Telefone · acesso por WhatsApp — estado 3:25366

Frame de tela: `3:25368` — **Login pelo WhatsApp**, 390 × 844 px.

- `3:25367` **Legenda:** Telefone · acesso por WhatsApp

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25370` **Horário:** 9:41
- `3:25376` **Título:** Finance Tracker
- `3:25382` **Título:** Seu dinheiro, ⏎ mais claro.
- `3:25383` **Texto:** Acompanhe seus gastos e entenda para onde vai seu dinheiro, sem complicação.
- `3:25385` **Rótulo:** Seu número de WhatsApp
- `3:25388` **Código do país:** +55
- `3:25391` **Telefone:** (11) 99999-4821
- `3:25392` **Texto:** Enviaremos um código de 6 dígitos pelo WhatsApp. Não é necessário criar uma senha.
- `3:25395` **Rótulo:** Enviar código
- `3:25399` **Texto:** Seu acesso é seguro
- `3:25400` **Texto:** Seu número é usado apenas para autenticar sua conta. Nunca pediremos seu código.
- `3:25401` **Texto:** Ao continuar, você concorda com os Termos de uso e a Política de privacidade.

### Código · aguardando verificação — estado 3:25404

Frame de tela: `3:25406` — **Verificação do código**, 390 × 844 px.

- `3:25405` **Legenda:** Código · aguardando verificação

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25408` **Horário:** 9:41
- `3:25416` **Título:** Verificar WhatsApp
- `3:25422` **Título:** Confira seu WhatsApp
- `3:25423` **Texto:** Digite o código de 6 dígitos enviado para
- `3:25424` **Telefone mascarado:** +55 (11) 9****-4821
- `3:25426` **Rótulo:** Código de verificação
- `3:25429` **Número:** 2
- `3:25431` **Número:** 4
- `3:25433` **Número:** \|
- `3:25435` **Número:**
- `3:25437` **Número:**
- `3:25439` **Número:**
- `3:25440` **Texto:** O código é válido por 5 minutos.
- `3:25442` **Rótulo:** Verificar código
- `3:25444` **Texto:** Reenviar código em 00:42
- `3:25446` **Rótulo:** Alterar número

### Código · inválido — estado 3:25449

Frame de tela: `3:25451` — **Verificação · código inválido**, 390 × 844 px.

- `3:25450` **Legenda:** Código · inválido

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25453` **Horário:** 9:41
- `3:25461` **Título:** Verificar WhatsApp
- `3:25467` **Título:** Confira seu WhatsApp
- `3:25468` **Texto:** Digite o código de 6 dígitos enviado para
- `3:25469` **Telefone mascarado:** +55 (11) 9****-4821
- `3:25471` **Rótulo:** Código de verificação
- `3:25474` **Número:** 2
- `3:25476` **Número:** 4
- `3:25478` **Número:** 8
- `3:25480` **Número:** 6
- `3:25482` **Número:** 0
- `3:25484` **Número:** 9
- `3:25488` **Texto:** Código inválido
- `3:25489` **Texto:** Confira os 6 dígitos e tente novamente. Você ainda tem 2 tentativas.
- `3:25491` **Rótulo:** Verificar código
- `3:25493` **Texto:** Reenviar código em 00:18
- `3:25495` **Rótulo:** Alterar número

### Código · expirado — estado 3:25498

Frame de tela: `3:25500` — **Verificação · código expirado**, 390 × 844 px.

- `3:25499` **Legenda:** Código · expirado

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25502` **Horário:** 9:41
- `3:25510` **Título:** Verificar WhatsApp
- `3:25516` **Título:** Confira seu WhatsApp
- `3:25517` **Texto:** Digite o código de 6 dígitos enviado para
- `3:25518` **Telefone mascarado:** +55 (11) 9****-4821
- `3:25520` **Rótulo:** Código de verificação
- `3:25523` **Número:** 2
- `3:25525` **Número:** 4
- `3:25527` **Número:** 8
- `3:25529` **Número:** 6
- `3:25531` **Número:** 0
- `3:25533` **Número:** 1
- `3:25537` **Texto:** Este código expirou
- `3:25538` **Texto:** Por segurança, o código é válido por 5 minutos. Solicite um novo para continuar.
- `3:25540` **Rótulo:** Enviar novo código
- `3:25542` **Texto:** Já pode solicitar um novo código.
- `3:25544` **Rótulo:** Alterar número

### Código · excesso de tentativas — estado 3:25548

Frame de tela: `3:25550` — **Verificação · excesso de tentativas**, 390 × 844 px.

- `3:25549` **Legenda:** Código · excesso de tentativas

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25552` **Horário:** 9:41
- `3:25560` **Título:** Verificar WhatsApp
- `3:25566` **Título:** Confira seu WhatsApp
- `3:25567` **Texto:** Digite o código de 6 dígitos enviado para
- `3:25568` **Telefone mascarado:** +55 (11) 9****-4821
- `3:25570` **Rótulo:** Código de verificação
- `3:25573` **Número:** 2
- `3:25575` **Número:** 4
- `3:25577` **Número:** 8
- `3:25579` **Número:** 6
- `3:25581` **Número:** 0
- `3:25583` **Número:** 9
- `3:25587` **Texto:** Muitas tentativas
- `3:25588` **Texto:** A verificação foi bloqueada por 15 minutos. Aguarde antes de solicitar outro código.
- `3:25590` **Rótulo:** Tente novamente em 14:59
- `3:25592` **Texto:** Novo envio disponível em 14:59
- `3:25594` **Rótulo:** Alterar número

### Logout · confirmação — estado 3:25597

Frame de tela: `3:25599` — **Logout · diálogo de confirmação**, 390 × 844 px.

- `3:25598` **Legenda:** Logout · confirmação

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25602` **Horário:** 9:41
- `3:25608` **Título:** Olá, Mariana
- `3:25614` **Rótulo:** Últimos 30 dias
- `3:25617` **Valor total:** R$ 1.250,90
- `3:25618` **Quantidade:** 10 gastos registrados
- `3:25621` **Rótulo:** Ver relatório completo
- `3:25623` **Texto:** Últimos gastos
- `3:25630` **Descrição:** Almoço no centro
- `3:25631` **Valor:** R$ 45,90
- `3:25633` **Categoria:** Alimentação
- `3:25634` **Data e hora:** 07/10/2026 · 12:40
- `3:25641` **Descrição:** Mercado da semana
- `3:25642` **Valor:** R$ 260,00
- `3:25644` **Categoria:** Alimentação
- `3:25645` **Data e hora:** 06/10/2026 · 18:15
- `3:25652` **Descrição:** Uber para o trabalho
- `3:25653` **Valor:** R$ 32,00
- `3:25655` **Categoria:** Transporte
- `3:25656` **Data e hora:** 06/10/2026 · 08:20
- `3:25662` **Rótulo:** Início
- `3:25666` **Rótulo:** Registrar
- `3:25670` **Rótulo:** Relatórios
- `3:25678` **Título:** Sair da sua conta?
- `3:25679` **Texto:** Seus gastos continuarão salvos. Para voltar, basta verificar seu WhatsApp novamente.
- `3:25682` **Rótulo:** Sair da conta
- `3:25684` **Rótulo:** Continuar na conta

### Sessão expirada · novo acesso — estado 3:25685

Frame de tela: `3:25687` — **Sessão expirada**, 390 × 844 px.

- `3:25686` **Legenda:** Sessão expirada · novo acesso

Textos extraídos (incluem conteúdo demonstrativo e simulação da barra do sistema):

- `3:25689` **Horário:** 9:41
- `3:25695` **Título:** Finance Tracker
- `3:25701` **Texto:** Sua sessão expirou
- `3:25702` **Texto:** Para proteger suas informações, precisamos confirmar seu acesso novamente.
- `3:25706` **Texto:** Seus gastos estão seguros
- `3:25707` **Texto:** Nenhum registro foi perdido. Entre com o mesmo número de WhatsApp.
- `3:25710` **Rótulo:** Entrar pelo WhatsApp

## Design System — 3:24845

Dimensão do painel: 1752 × 2348 px.

- `3:24847` **Identificação:** FINANCE TRACKER / 02
- `3:24848` **Título:** Design System
- `3:24849` **Descrição:** Fundação visual e espécimes editáveis. Apenas os frames DS / estão preparados para conversão em componentes.
- `3:24854` **Identificação:** INTER / UMA HIERARQUIA CLARA
- `3:24855` **Exemplo de destaque:** R$ 1.250,90
- `3:24856` **Exemplo de título:** Seu dinheiro, mais claro.
- `3:24857` **Exemplo de corpo:** Corpo 15 / 150% · Rótulo 13 / 600 · Apoio 12 / 150%
- `3:24859` **Título:** Simples, acessível, confiável.
- `3:24860` **Texto:** Superfícies brancas, grafite para leitura e esmeralda para ação. Erros sempre combinam vermelho, ícone e mensagem — nunca dependem apenas da cor.
- `3:24861` **Texto:** Ritmo: 4 · 8 · 12 · 16 · 24 · 32 px   /   Cantos: 12 e 20 px ⏎ Botões: 52 px   /   Campos: 54 px   /   Moeda: pt-BR   /   Datas: DD/MM/AAAA
- `3:24865` **Nome:** Esmeralda
- `3:24866` **Valor:** #087F5B
- `3:24869` **Nome:** Grafite
- `3:24870` **Valor:** #192B28
- `3:24873` **Nome:** Texto de apoio
- `3:24874` **Valor:** #5C706A
- `3:24877` **Nome:** Fundo
- `3:24878` **Valor:** #F4F7F6
- `3:24881` **Nome:** Borda
- `3:24882` **Valor:** #DCE5E1
- `3:24885` **Nome:** Erro
- `3:24886` **Valor:** #B42318
- `3:24890` **Título:** Botões
- `3:24891` **Texto:** Ação principal e alternativas. Estados explícitos e áreas de toque de 52 px.
- `3:24893` **Estado:** Primário · padrão
- `3:24897` **Rótulo:** Salvar gasto
- `3:24899` **Estado:** Secundário
- `3:24902` **Rótulo:** Registrar outro gasto
- `3:24904` **Estado:** Texto
- `3:24907` **Rótulo:** Alterar número
- `3:24909` **Estado:** Desabilitado
- `3:24912` **Rótulo:** Verificar código
- `3:24914` **Estado:** Carregamento
- `3:24918` **Rótulo:** Salvando…
- `3:24920` **Estado:** Confirmação de saída
- `3:24923` **Rótulo:** Sair da conta
- `3:24926` **Título:** Inputs
- `3:24927` **Texto:** Rótulos permanentes e ajuda próxima ao campo. Sem depender de placeholders.
- `3:24929` **Estado:** Vazio
- `3:24932` **Rótulo:** Descrição *
- `3:24934` **Valor:** Ex.: almoço no centro
- `3:24936` **Estado:** Preenchido
- `3:24939` **Rótulo:** Descrição *
- `3:24941` **Valor:** Almoço no centro
- `3:24943` **Estado:** Foco
- `3:24946` **Rótulo:** Descrição *
- `3:24948` **Valor:** Almoço no centro \|
- `3:24950` **Estado:** Erro de validação
- `3:24953` **Rótulo:** Valor *
- `3:24955` **Valor:** R$ 0,00
- `3:24956` **Texto:** Informe um valor maior que R$ 0,00.
- `3:24958` **Estado:** Desabilitado
- `3:24961` **Rótulo:** Data · opcional
- `3:24963` **Valor:** 07/10/2026
- `3:24967` **Título:** Código de acesso
- `3:24968` **Texto:** Seis posições. Borda de foco, erro e bloqueio para a verificação por WhatsApp.
- `3:24970` **Estado:** Digitando · foco
- `3:24974` **Número:** 2
- `3:24976` **Número:** 4
- `3:24978` **Número:** \|
- `3:24980` **Número:**
- `3:24982` **Número:**
- `3:24984` **Número:**
- `3:24986` **Estado:** Completo
- `3:24990` **Número:** 2
- `3:24992` **Número:** 4
- `3:24994` **Número:** 8
- `3:24996` **Número:** 6
- `3:24998` **Número:** 0
- `3:25000` **Número:** 9
- `3:25002` **Estado:** Inválido
- `3:25006` **Número:** 2
- `3:25008` **Número:** 4
- `3:25010` **Número:** 8
- `3:25012` **Número:** 6
- `3:25014` **Número:** 0
- `3:25016` **Número:** 9
- `3:25018` **Estado:** Expirado
- `3:25022` **Número:** 2
- `3:25024` **Número:** 4
- `3:25026` **Número:** 8
- `3:25028` **Número:** 6
- `3:25030` **Número:** 0
- `3:25032` **Número:** 1
- `3:25034` **Estado:** Excesso de tentativas
- `3:25038` **Número:** 2
- `3:25040` **Número:** 4
- `3:25042` **Número:** 8
- `3:25044` **Número:** 6
- `3:25046` **Número:** 0
- `3:25048` **Número:** 9
- `3:25050` **Estado:** Mensagem de erro
- `3:25055` **Texto:** Código inválido
- `3:25056` **Texto:** Confira os 6 dígitos e tente novamente.
- `3:25059` **Título:** Categorias e filtros
- `3:25060` **Texto:** Seleção clara. Os filtros alteram a mesma base de gastos do detalhamento.
- `3:25062` **Estado:** Categoria · padrão
- `3:25066` **Rótulo:** Alimentação
- `3:25068` **Estado:** Categoria · selecionada
- `3:25072` **Rótulo:** Alimentação
- `3:25075` **Estado:** Categoria · erro
- `3:25079` **Rótulo:** Alimentação
- `3:25081` **Estado:** Período · 30 dias
- `3:25086` **Rótulo:** 7 dias
- `3:25088` **Rótulo:** 30 dias
- `3:25090` **Rótulo:** Mês atual
- `3:25093` **Rótulo:** Período personalizado
- `3:25096` **Estado:** Período · personalizado
- `3:25101` **Rótulo:** 7 dias
- `3:25103` **Rótulo:** 30 dias
- `3:25105` **Rótulo:** Mês atual
- `3:25108` **Rótulo:** Período personalizado
- `3:25113` **Título:** Cards e transações
- `3:25114` **Texto:** Moeda brasileira, metadados completos e conteúdo em Auto Layout.
- `3:25116` **Estado:** Total · destaque
- `3:25120` **Rótulo:** Últimos 30 dias
- `3:25123` **Valor total:** R$ 1.250,90
- `3:25124` **Quantidade:** 10 gastos registrados
- `3:25127` **Rótulo:** Ver relatório completo
- `3:25130` **Estado:** Total · neutro
- `3:25134` **Rótulo:** Últimos 30 dias
- `3:25137` **Valor total:** R$ 1.250,90
- `3:25138` **Quantidade:** 10 gastos registrados
- `3:25141` **Rótulo:** Ver relatório completo
- `3:25144` **Estado:** Transação · alimentação
- `3:25150` **Descrição:** Almoço no centro
- `3:25151` **Valor:** R$ 45,90
- `3:25153` **Categoria:** Alimentação
- `3:25154` **Data e hora:** 07/10/2026 · 12:40
- `3:25156` **Estado:** Transação · transporte
- `3:25162` **Descrição:** Uber para o trabalho
- `3:25163` **Valor:** R$ 32,00
- `3:25165` **Categoria:** Transporte
- `3:25166` **Data e hora:** 06/10/2026 · 08:20
- `3:25168` **Estado:** Loading · skeleton
- `3:25176` **Título:** Gráfico e detalhamento
- `3:25177` **Texto:** Distribuição proporcional à base de R$ 1.250,90. Cor acompanhada por nome e valor.
- `3:25179` **Estado:** Donut · 5 categorias
- `3:25189` **Rótulo:** Total do período
- `3:25190` **Valor:** R$ 1.250,90
- `3:25191` **Quantidade:** 10 gastos
- `3:25193` **Estado:** Nome · valor · percentual
- `3:25199` **Nome:** Alimentação
- `3:25200` **Percentual:** 36,0% do total
- `3:25201` **Valor:** R$ 450,90
- `3:25206` **Nome:** Compras
- `3:25207` **Percentual:** 24,0% do total
- `3:25208` **Valor:** R$ 300,00
- `3:25213` **Nome:** Moradia
- `3:25214` **Percentual:** 20,0% do total
- `3:25215` **Valor:** R$ 250,00
- `3:25220` **Nome:** Transporte
- `3:25221` **Percentual:** 14,4% do total
- `3:25222` **Valor:** R$ 180,00
- `3:25227` **Nome:** Saúde
- `3:25228` **Percentual:** 5,6% do total
- `3:25229` **Valor:** R$ 70,00
- `3:25232` **Estado:** Paginação · início da lista
- `3:25235` **Texto:** Exibindo 1–5 de 10 gastos
- `3:25239` **Página:** Página 1 de 2
- `3:25243` **Estado:** Paginação · última página
- `3:25246` **Texto:** Exibindo 6–10 de 10 gastos
- `3:25250` **Página:** Página 2 de 2
- `3:25255` **Título:** Modal e feedback
- `3:25256` **Texto:** Confirmação antes do logout. Sucesso, erro e recuperação com mensagens diretas.
- `3:25258` **Estado:** Logout · confirmação
- `3:25264` **Título:** Sair da sua conta?
- `3:25265` **Texto:** Seus gastos continuarão salvos. Para voltar, basta verificar seu WhatsApp novamente.
- `3:25268` **Rótulo:** Sair da conta
- `3:25270` **Rótulo:** Continuar na conta
- `3:25272` **Estado:** Sucesso
- `3:25276` **Mensagem:** Gasto registrado com sucesso.
- `3:25277` **Ação:** Ver gasto
- `3:25279` **Estado:** Orientação
- `3:25284` **Texto:** Seu acesso é seguro
- `3:25285` **Texto:** Nunca compartilhe seu código de verificação.
- `3:25287` **Estado:** Conexão · recuperação
- `3:25292` **Texto:** Sem conexão
- `3:25293` **Texto:** Verifique sua internet e tente novamente.
- `3:25296` **Título:** Navegação inferior
- `3:25297` **Texto:** Três destinos persistentes. Ícone + texto e indicador de seleção esmeralda.
- `3:25299` **Estado:** Início ativo
- `3:25306` **Rótulo:** Início
- `3:25310` **Rótulo:** Registrar
- `3:25314` **Rótulo:** Relatórios
- `3:25318` **Estado:** Registrar ativo
- `3:25325` **Rótulo:** Início
- `3:25329` **Rótulo:** Registrar
- `3:25333` **Rótulo:** Relatórios
- `3:25337` **Estado:** Relatórios ativo
- `3:25344` **Rótulo:** Início
- `3:25348` **Rótulo:** Registrar
- `3:25352` **Rótulo:** Relatórios
- `3:25356` **Texto:** Uma linguagem em todas as telas
- `3:25357` **Texto:** Não usar vermelho para valores de gastos normais. ⏎  ⏎ Reservar o vermelho para erro e confirmação de saída. Preservar rótulos e contraste em todos os estados.
- `3:25358` **Texto:** Os espécimes DS / são frames Auto Layout individuais. Os títulos, famílias e a apresentação não recebem o prefixo para evitar conversões indevidas.

## Fluxos — 3:24762

Dimensão do painel: 1752 × 828 px.

- `3:24764` **Identificação:** FINANCE TRACKER / 01
- `3:24765` **Título:** Fluxos
- `3:24766` **Descrição:** Do primeiro acesso ao controle dos gastos. Mapa de jornadas e regras de estado.
- `3:24771` **Título:** Clareza em cada gasto.
- `3:24772` **Texto:** Finance Tracker · acesso simples, registro rápido e uma visão confiável do seu dinheiro.
- `3:24773` **Natureza da entrega:** DESIGN ESTÁTICO
- `3:24778` **Passo:** 01
- `3:24779` **Título:** Telefone
- `3:24780` **Texto:** Informe o número com +55. Receba o código pelo WhatsApp.
- `3:24781` **Contexto:** Autenticação
- `3:24786` **Passo:** 02
- `3:24787` **Título:** Código
- `3:24788` **Texto:** Confirme 6 dígitos. Código válido por 5 minutos.
- `3:24789` **Contexto:** Verificação segura
- `3:24794` **Passo:** 03
- `3:24795` **Título:** Início
- `3:24796` **Texto:** Veja o total de 30 dias e as 10 transações mais recentes.
- `3:24797` **Contexto:** Visão geral
- `3:24802` **Passo:** 04
- `3:24803` **Título:** Registrar gasto
- `3:24804` **Texto:** Informe valor, descrição e categoria. Data e hora opcionais.
- `3:24805` **Contexto:** Registro + sucesso
- `3:24810` **Passo:** 05
- `3:24811` **Título:** Relatório
- `3:24812` **Texto:** Escolha um período e compare os gastos por categoria.
- `3:24813` **Contexto:** Análise do período
- `3:24818` **Passo:** 06
- `3:24819` **Título:** Detalhe categoria
- `3:24820` **Texto:** Consulte os gastos da categoria, do recente ao antigo.
- `3:24821` **Contexto:** Lista + paginação
- `3:24826` **Passo:** 07
- `3:24827` **Título:** Logout
- `3:24828` **Texto:** Confirme a saída. Seus registros permanecem salvos.
- `3:24829` **Contexto:** Encerramento seguro
- `3:24832` **Título:** Acesso e segurança
- `3:24834` **Texto:** Inválido: nova tentativa · Expirado: novo código ⏎ Excesso de tentativas: bloqueio por 15 minutos ⏎ Sessão expirada: confirmar o WhatsApp novamente.
- `3:24836` **Título:** Registro e disponibilidade
- `3:24838` **Texto:** Campos obrigatórios com erro junto à entrada. ⏎ Skeleton no carregamento e recuperação da conexão. ⏎ Lista vazia convida a registrar o primeiro gasto.
- `3:24840` **Título:** Base única de dados
- `3:24842` **Texto:** 30 dias: R$ 1.250,90 · 10 gastos · 5 categorias ⏎ 7 dias / mês atual: R$ 1.075,90 · 8 gastos ⏎ Alimentação: R$ 450,90 · 4 gastos · 36,0%.
- `3:24843` **Texto:** As setas documentam a sequência conceitual. Nenhuma tela possui interação, link ou transição de protótipo.

## Catálogo de componentes DS

Os 38 elementos DS estão serializados como SYMBOL. Os frames internos contêm a geometria e o Auto Layout; o texto editorial que diz estarem apenas preparados para conversão está desatualizado.

| ID | Nome | Dimensão |
| --- | --- | --- |
| 3:28950 | DS / Botão / Primário | 362 × 52 |
| 3:28951 | DS / Botão / Secundário | 362 × 52 |
| 3:28952 | DS / Botão / Texto | 362 × 52 |
| 3:28953 | DS / Botão / Desabilitado | 362 × 52 |
| 3:28954 | DS / Botão / Carregando | 362 × 52 |
| 3:28955 | DS / Botão / Perigo | 362 × 52 |
| 3:28956 | DS / Input / Padrão | 362 × 78 |
| 3:28957 | DS / Input / Preenchido | 362 × 78 |
| 3:28958 | DS / Input / Foco | 362 × 78 |
| 3:28959 | DS / Input / Erro | 362 × 104 |
| 3:28960 | DS / Input / Desabilitado | 362 × 78 |
| 3:28961 | DS / Código / Foco | 362 × 60 |
| 3:28962 | DS / Código / Completo | 362 × 60 |
| 3:28963 | DS / Código / Erro | 362 × 60 |
| 3:28964 | DS / Código / Expirado | 362 × 60 |
| 3:28965 | DS / Código / Bloqueado | 362 × 60 |
| 3:28966 | DS / Aviso / Erro | 362 × 77 |
| 3:28967 | DS / Categoria / Padrão | 362 × 48 |
| 3:28968 | DS / Categoria / Selecionada | 362 × 48 |
| 3:28969 | DS / Categoria / Erro | 362 × 48 |
| 3:28970 | DS / Filtros / 30 dias | 362 × 96 |
| 3:28971 | DS / Filtros / Personalizado | 362 × 96 |
| 3:28972 | DS / Card / Resumo | 362 × 197 |
| 3:28973 | DS / Card / Neutro | 362 × 197 |
| 3:28974 | DS / Transação / Alimentação | 362 × 85 |
| 3:28975 | DS / Transação / Transporte | 362 × 85 |
| 3:28976 | DS / Card / Skeleton | 362 × 126 |
| 3:28977 | DS / Gráfico / Categorias | 362 × 216 |
| 3:28978 | DS / Lista / Categorias | 362 × 310 |
| 3:28979 | DS / Paginação / Primeira | 362 × 78 |
| 3:28980 | DS / Paginação / Última | 362 × 78 |
| 3:28981 | DS / Modal / Logout | 362 × 335 |
| 3:28982 | DS / Snackbar / Sucesso | 362 × 52 |
| 3:28983 | DS / Aviso / Segurança | 362 × 77 |
| 3:28984 | DS / Aviso / Conexão | 362 × 77 |
| 3:28985 | DS / Navegação / Início | 362 × 86 |
| 3:28986 | DS / Navegação / Registrar | 362 × 86 |
| 3:28987 | DS / Navegação / Relatórios | 362 × 86 |

## Estilos tipográficos encontrados

Valores RAW de entrelinha abaixo são evidência binária, não valores CSS homologados. Há valores como 1,5 RAW apesar de o texto do design indicar 150%; não converter literalmente para 1,5 px.

| Família | Estilo | Tamanho px | Entrelinha serializada | Letter spacing |
| --- | --- | --- | --- | --- |
| Inter | Bold | 12 | 100 PERCENT | 1.5 RAW |
| Inter | Bold | 36 | 100 PERCENT | -1 RAW |
| Inter | Regular | 16 | 100 PERCENT | 0 PERCENT |
| Inter | Bold | 26 | 100 PERCENT | 0 PERCENT |
| Inter | Regular | 15 | 1.5 RAW | 0 PERCENT |
| Inter | Semi Bold | 12 | 100 PERCENT | 0 PERCENT |
| Inter | Bold | 17 | 100 PERCENT | 0 PERCENT |
| Inter | Regular | 13 | 1.5 RAW | 0 PERCENT |
| Inter | Semi Bold | 11 | 100 PERCENT | 0 PERCENT |
| Inter | Bold | 16 | 100 PERCENT | 0 PERCENT |
| Inter | Regular | 12 | 1.5 RAW | 0 PERCENT |
| Inter | Bold | 24 | 100 PERCENT | 0 PERCENT |
| Inter | Regular | 15 | 100 PERCENT | 0 PERCENT |
| Inter | Bold | 18 | 100 PERCENT | 0 PERCENT |
| Inter | Regular | 11 | 100 PERCENT | 0 PERCENT |
| Inter | Semi Bold | 15 | 100 PERCENT | 0 PERCENT |
| Inter | Semi Bold | 13 | 100 PERCENT | 0 PERCENT |
| Inter | Semi Bold | 24 | 100 PERCENT | 0 PERCENT |
| Inter | Semi Bold | 14 | 1.5 RAW | 0 PERCENT |
| Inter | Regular | 13 | 100 PERCENT | 0 PERCENT |
| Inter | Medium | 13 | 100 PERCENT | 0 PERCENT |
| Inter | Bold | 34 | 100 PERCENT | -1 RAW |
| Inter | Semi Bold | 13 | 1.2999999523162842 RAW | 0 PERCENT |
| Inter | Regular | 12 | 100 PERCENT | 0 PERCENT |
| Inter | Bold | 20 | 100 PERCENT | -0.6000000238418579 RAW |
| Inter | Bold | 22 | 100 PERCENT | 0 PERCENT |
| Inter | Regular | 14 | 1.5 RAW | 0 PERCENT |
| Inter | Semi Bold | 13 | 1.5 RAW | 0 PERCENT |
| Inter | Semi Bold | 14 | 100 PERCENT | 0 PERCENT |
| Inter | Bold | 18 | 100 PERCENT | -0.4000000059604645 RAW |
| Inter | Bold | 32 | 1.149999976158142 RAW | -1 RAW |
| Inter | Bold | 28 | 100 PERCENT | -0.699999988079071 RAW |
| Inter | Semi Bold | 16 | 100 PERCENT | 0 PERCENT |
| Inter | Semi Bold | 18 | 1.5 RAW | 0 PERCENT |
| Inter | Bold | 28 | 1.5 RAW | 0 PERCENT |
| Inter | Bold | 25 | 1.5 RAW | 0 PERCENT |
| Inter | Bold | 23 | 1.5 RAW | 0 PERCENT |
| Inter | Semi Bold | 30 | 100 PERCENT | 0 PERCENT |
| Inter | Bold | 27 | 1.5 RAW | 0 PERCENT |
| Inter | Semi Bold | 15 | 1.5 RAW | 0 PERCENT |
| Inter | Regular | 11 | 1.5 RAW | 0 PERCENT |
| Inter | Bold | 12 | 100 PERCENT | 0 PERCENT |
| Inter | Bold | 32 | 100 PERCENT | -1 RAW |

## Ícones nomeados encontrados

`Categoria`, `message-circle`, `arrow-right`, `key-round`, `house`, `circle-plus`, `chart-no-axes-column-increasing`, `list-filter`, `log-out`, `check`, `loader-circle`, `calendar-days`, `circle-alert`, `utensils`, `chevron-down`, `wallet`, `arrow-up-right`, `car`, `chevron-right`, `chevron-left`, `circle-check`, `shield-check`, `wifi-off`, `ios-signal`, `ios-wifi-signal`, `ios-battery-full`, `arrow-left`, `lock-keyhole`, `shopping-basket`, `plus`, `heart-pulse`, `shopping-bag`, `fuel`, `zap`, `coffee`, `refresh-cw`, `receipt-text`, `clock-3`, `list`, `arrow-down-wide-narrow`.

## Cores sólidas encontradas

Inclui amostras, interfaces e painéis editoriais. Uso semântico homologado no SDD; frequência não define token.

| Hex | Ocorrências em fills/strokes |
| --- | --- |
| #192B28 | 616 |
| #5C706A | 531 |
| #FFFFFF | 394 |
| #087F5B | 279 |
| #DCE5E1 | 251 |
| #E8F5EE | 120 |
| #B42318 | 57 |
| #F4F7F6 | 36 |
| #E5ECE8 | 36 |
| #D0F0E2 | 13 |
| #86C5AF | 12 |
| #B5DBCD | 12 |
| #43A98A | 12 |
| #DCEEE7 | 12 |
| #056246 | 12 |
| #FFF0EE | 9 |
| #EEF2F0 | 6 |
| #9FE0C3 | 2 |
| #112820 | 2 |
