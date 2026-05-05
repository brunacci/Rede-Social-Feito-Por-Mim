# Micro Rede Social

## Descrição Curta
Aplicativo Android de rede social baseada em localização, com feed de postagens e pesquisa por cidade.

## Demonstração
[Link para o Vídeo](assets/Demonstração.webm)

## Visão Geral e Funcionalidades
A aplicação foi desenvolvida para oferecer um fluxo de publicação e navegação social com foco em simplicidade, desempenho e contexto geográfico do usuário.

- **Autenticação e gestão de conta:** cadastro, login, logout e edição de perfil.
- **Criação de publicações:** envio de texto, imagem e localização atual (cidade) no momento da postagem.
- **Feed interativo:** navegação por lista rolável com paginação e atualização manual via Pull-to-Refresh.
- **Pesquisa por cidade:** filtro de postagens com base no nome da cidade informada.

## Tecnologias Utilizadas
- **Linguagem e plataforma:** Kotlin e Android SDK (API 33).
- **Interface:** Jetpack Compose para construção da UI.
- **Arquitetura:** Clean Architecture e MVVM.
- **Injeção de dependências:** Hilt.
- **Backend:** Firebase Authentication e Cloud Firestore.
- **Concorrência:** Coroutines para operações assíncronas.

## Como Executar o Projeto
### Pré-requisitos
- Android Studio instalado e atualizado.
- JDK compatível com a configuração do projeto.
- Projeto Firebase configurado com Authentication e Firestore.

### Passos
1. Clone o repositório:

```bash
git clone <URL_DO_REPOSITORIO>
cd Rede-Social-Feito-Por-Mim
```

2. Adicione o arquivo `google-services.json` na pasta `app/`.

3. Abra o projeto no Android Studio e aguarde a sincronização do Gradle.

4. Execute o aplicativo em um emulador ou dispositivo físico.