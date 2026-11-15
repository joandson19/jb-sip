# Histórico de versões — JB SIP

Somente as versões do JB SIP são listadas aqui. O código é uma obra derivada do
projeto `linphone-android` (GPLv3); os avisos de copyright originais estão
mantidos em cada arquivo e o resumo das modificações está em [NOTICE](NOTICE).

## [1.0.12] — 2026

### Corrigido
- **Número reescrito na pré-visualização**: ao digitar um número com 10 ou 11
  dígitos, a linha de sugestão mostrava `sip:+5575983256064@servidor` (formato
  internacional) e tocar nela ligava para esse número alterado. Agora qualquer
  número digitado é montado literalmente (`sip:75983256064@servidor`), tanto na
  pré-visualização quanto na chamada. Quem normaliza prefixos continua sendo o
  dialplan da central.

## [1.0.11] — 2026

### Corrigido
- **Traduções em português que faltavam** (91 textos): as abas do histórico
  apareciam como "All calls" / "Missed" e os botões da chamada recebida como
  "Answer" / "Decline". Todos os textos visíveis do app agora estão em
  português, incluindo configurações avançadas, contatos, conversas e
  notificações.

## [1.0.10] — 2026

### Corrigido
- Chamadas para números de telefone externos falhavam com **"Usuário não
  encontrado"**: o app convertia o número digitado para o formato internacional
  (`75983256064` virava `+5575983256064`) e o Asterisk não achava esse padrão. O
  número agora vai para o tronco SIP exatamente como foi discado
  (`sip:75983256064@servidor`); quem normaliza é o dialplan da central.

### Removido
- Interruptores **"Formatar números de telefone usando prefixo internacional"** e
  **"Substituir + por 00 ao formatar números de telefone"** das Configurações da
  conta — a função deixou de existir, não é mais só uma opção desligada.

## [1.0.9] — 2026

### Corrigido
- O botão de anexo (clipe) voltava a aparecer na conversa: o
  `ConstraintLayout.Group` `standard_messages` aplicava `VISIBLE` em
  `attach_file`, anulando o `visibility="gone"` do layout.

### Removido
- **Função de anexos eliminada do código** (não apenas oculta). Saíram: botão de
  clipe, bandeja câmera/galeria/arquivo, fila de anexos, gravação de mensagem de
  voz, recebimento de arquivos por compartilhamento e colagem de conteúdo rico no
  campo de texto. O chat agora só envia e recebe texto (`SIP MESSAGE`).

## [1.0.8] — 2026

### Alterado
- Versão `versionCode 9` / `versionName 1.0.8`.
- Endereço do código-fonte publicado no aplicativo (Ajuda > Código-fonte) e na
  descrição da loja.
- Build de release deixa de exigir `keystore.properties`: um clone novo compila
  (APK não assinado) em vez de falhar na configuração.

### Removido
- Itens **Mídia** e **Documentos** do menu da conversa.

## [1.0.7] — 2026

### Adicionado
- Tela de Ajuda com entrada "Código-fonte" (GPLv3).
- `NOTICE` com o aviso de obra derivada e não afiliação.

### Alterado
- Reforço de persistência em segundo plano: serviço `START_STICKY`, reinício no
  boot e após atualização.
- Aviso recorrente de isenção de otimização de bateria ("Continue recebendo
  chamadas") a cada 12 horas.
- Configurações > Avançado: status da otimização de bateria e atalho para
  autostart do fabricante.

### Removido
- Conteúdo residual do projeto original nas telas de Ajuda e Diagnóstico
  (guia do usuário, privacidade, tradução, verificação de atualização).
- Esquemas de URI e filtros de compartilhamento do projeto original.

## [1.0.6] e anteriores — 2026

- Rebranding inicial: nome, ícone, splash, cores, notificação.
- Chat restrito a texto (SIP MESSAGE), sem anexos; grupos e reuniões
  desativados.
- Configuração padrão apontando para servidor SIP privado.
