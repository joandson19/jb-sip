# JB SIP

Aplicativo Android de VoIP (softphone SIP) para centrais privadas Asterisk/FreePBX.
Permite chamadas de áudio e vídeo e troca de mensagens de texto via SIP MESSAGE.

* Pacote: `br.com.jb.voip`
* Versão atual: 1.0.10 (`versionName` em `app/build.gradle.kts`)
* Android mínimo: 9.0 (API 28)
* Licença: GNU General Public License v3.0 (GPLv3)
* APK assinado de cada versão: https://github.com/joandson19/jb-sip/releases

Cada versão é publicada com o APK assinado e o código-fonte correspondente na
mesma tag (`v1.0.x`), como exige a GPLv3.

> **Aviso de obra derivada:** este é um projeto modificado (obra derivada) do
> aplicativo de código aberto `linphone-android`, Copyright © Belledonne
> Communications SARL, licenciado sob a GPLv3. As modificações de 2026 estão
> descritas em [NOTICE](NOTICE) e resumidas abaixo. Os avisos de copyright
> originais e as cabeças de licença GPLv3 foram mantidos em todos os arquivos,
> como exige a licença.
>
> JB SIP **não é afiliado, patrocinado ou endossado** pela Belledonne
> Communications SARL. "Linphone" e o logotipo Linphone são marcas de seus
> respectivos titulares e não identificam este aplicativo.

---

## O que foi modificado em relação ao projeto original

* Rebranding completo: nome do aplicativo, ícone, splash screen, cores e ícone
  de notificação.
* Configuração padrão apontando para um servidor SIP privado; remoção dos
  serviços públicos do projeto original (provedor SIP, STUN, upload de logs,
  verificação de atualização).
* Fluxo de criação/login de conta do serviço original desativado.
* Conversas em grupo, reuniões e conferências desativados — apenas conversa
  1 a 1 por SIP MESSAGE (texto).
* Removida qualquer forma de envio ou recebimento de anexos (foto, arquivo,
  áudio, vídeo) no chat.
* Telas de Ajuda/Diagnóstico limpas de itens específicos do projeto original.
* Persistência em segundo plano reforçada: serviço `START_STICKY`, reinício no
  boot e após atualização, além de orientação e atalhos para isenção de
  otimização de bateria e autostart (Xiaomi, Huawei, Oppo, Vivo, Samsung,
  OnePlus, Asus, Letv, Nokia).
* Remoção dos esquemas de URI e filtros de compartilhamento específicos do
  projeto original.

## Como compilar

Requisitos:

* JDK 21
* Android SDK com a plataforma 37 (Android 17)
* Gradle Wrapper incluído no repositório (não é necessário instalar Gradle)

O motor SIP (`org.linphone:linphone-sdk-android`, GPLv3) é baixado pronto do
repositório Maven, portanto **não** é necessário compilar o SDK localmente.

```bash
# APK de depuração
./gradlew :app:assembleDebug

# APK de release (assinado, se houver keystore configurado)
./gradlew :app:assembleRelease
```

No Windows use `gradlew.bat`.

O APK gerado fica em `app/build/outputs/apk/release/`.

### Assinatura

O build de release procura o arquivo `keystore.properties` na raiz do
repositório, com o seguinte formato:

```properties
storeFile=caminho/para/sua-keystore.jks
storePassword=********
keyAlias=********
keyPassword=********
```

Esse arquivo **nunca** deve ser publicado (ele já está no `.gitignore`). Sem
ele, o build de release continua funcionando, porém gera um APK não assinado.

## Licença

Este projeto é distribuído sob a GNU General Public License v3.0. O texto
completo está em [LICENSE.txt](LICENSE.txt).

O código original é Copyright © Belledonne Communications SARL e
colaboradores; as modificações deste repositório são de 2026. Qualquer
redistribuição deste aplicativo, com ou sem modificações, deve manter a licença
GPLv3 e disponibilizar o código-fonte correspondente.
