# 🖊️ MeetPen

**MeetPen** é um assistente pessoal de produtividade para Android, focado em gravação de voz com inteligência artificial. Ele transforma áudio bruto em notas estruturadas, resumos e listas de tarefas interativas.

> Inspirado no [AudioPen](https://play.google.com/store/apps/details?id=com.audiopen.android), com foco em interface limpa, processamento rápido de voz e exportação simplificada.

---

## ✨ Funcionalidades

- **🎙️ Gravação de Áudio** — formato `.3gp` (AMR_NB) para compatibilidade e economia de espaço.
- **🧠 Transcrição Inteligente** — integração com Google Gemini 1.5 Flash (`gemini-flash-latest`) e OpenAI Whisper, além de reconhecimento local com Vosk.
- **🤖 Ações de IA**
  - **Resumo** conciso em 3 pontos.
  - **Checklist interativo** — extração de tarefas em JSON com marcar/desmarcar.
- **▶️ Player de Áudio** — controle de velocidade (0.5x, 1.0x, 1.5x, 2.0x).
- **🗂️ Gerenciamento de Notas** — busca em tempo real, categorias, renomeação e exclusão.
- **🔑 Multi-Provedor** — suporte a Gemini, OpenAI e Claude com chaves independentes.
- **🔒 Autenticação biométrica** e **widgets** (Glance).

---

## 🛠️ Stack Técnica

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Banco de dados | Room |
| Rede | Retrofit + OkHttp |
| Serialização | kotlinx.serialization |
| IA / Voz | Google Generative AI (REST), OpenAI Whisper, Vosk, TensorFlow Lite |
| Outros | Glance (widgets), Coil, Biometric |

---

## 🎨 Design System

- **Cores:** Midnight Background (`#0F1115`), Amber Main (`#FF8A00`).
- **Estilo:** Minimalismo Premium, Glassmorphism, Dark Mode.

---

## 📋 Requisitos

- Android Studio (versão recente)
- **Min SDK:** 26 (Android 8.0)
- **Target / Compile SDK:** 35 (Android 15)
- JDK 11

---

## 🚀 Como rodar

1. Clone o repositório:
   ```bash
   git clone https://github.com/fragosowallace/MeetPen.git
   ```
2. Abra o projeto no Android Studio.
3. Configure o caminho do SDK em `local.properties` (gerado automaticamente).
4. Informe suas chaves de API (Gemini / OpenAI / Claude) nas **Configurações** do app.
5. Compile e execute:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📦 Informações do App

- **Application ID:** `br.com.meetpen`
- **Versão:** 1.0 (`versionCode 1`)

### Permissões principais

| Permissão | Uso |
|-----------|-----|
| `RECORD_AUDIO` | Gravação de áudio para transcrição |
| `INTERNET` | Comunicação com APIs de IA |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MICROPHONE` | Gravação em background |
| `POST_NOTIFICATIONS` | Notificações (API 33+) |

> O áudio processado pelo Vosk/TensorFlow Lite é tratado **localmente** no dispositivo.

---

## 📄 Licença

Projeto privado / em desenvolvimento. Todos os direitos reservados.
