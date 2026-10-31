# SmkzOverlay (Forge 1.8.9)

Overlay de stats para o MushMC: HUD + menu, score de suspeita de snipe, blacklist,
lista de interesse, alerta de winstreak e suporte a vários modos.

## Como compilar
1. Baixe o MDK do Forge 1.8.9 (1.8.9-11.15.1.2318) e configure como de costume (Java 8).
2. Copie a pasta `src/` deste projeto por cima da `src/` do MDK (apague o exemplo do MDK).
3. `gradlew build` -> o .jar fica em `build/libs/`. Coloque na pasta `mods` (Forge 1.8.9).

## Primeiro uso (IMPORTANTE)
O endpoint da API e os nomes dos campos do JSON (final_kills, winstreak, beds_destroyed...)
são PALPITES. Entre no MushMC e rode `/ov debug <nick>`: ele mostra as chaves reais da resposta.
Ajuste `config/smkzoverlay/config.json` (apiUrl, apiKey de cada modo, key/per das colunas).

## Uso
- Tecla `'` abre o menu (clique no jogador -> detalhes, botões de blacklist/interesse). Configurável em Controles.
- `/ov bl add|remove|list <nick> [motivo]`  - blacklist
- `/ov watch add|remove|list <nick>`        - lista de interesse
- `/ov ws <modo> <valor>`                   - limite do alerta de winstreak (0 desliga)
- `/ov mode <auto|id>`                      - fixa/automatiza o modo
- `/ov hud`, `/ov sound`, `/ov clear`, `/ov debug <nick>`

Arquivos em `config/smkzoverlay/`: config.json, lists.json, history.json.
Modos inclusos: BW Solo/Dupla/Trio/Quarteto, Bed Wars (geral), Bed Fight, Sky Wars e The Bridge.
Variações do BW usam o mesmo objeto `bedwars` da API, com `sub` apontando para o submodo (solo/doubles/trios/quads são palpites).
Modos inclusos: BW Solo/Dupla/Trio/Quarteto, Bed Wars (geral), Bed Fight, Sky Wars e The Bridge.
Variações do BW usam o objeto `bedwars` da API com `sub` apontando para o submodo (solo/doubles/trios/quads são palpites).
Para adicionar um modo novo, copie um bloco em `modes` do config.json. Modos novos de versões futuras são acrescentados sozinhos ao config existente. Modos novos de versões futuras são acrescentados sozinhos ao config existente.
