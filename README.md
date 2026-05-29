
# Setup do projeto

### Rodar os terminais tcp
1.
Compile once:
javac -d out $(find src -name '*.java')
2.
Terminal 1 — start the monitor:
java -cp out com.fabrica.tcp.MonitorMain
3.
Terminal 2 — start the factory app:
java -cp out com.fabrica.app.Main

Este documento detalha o contexto, escopo e requisitos técnicos do projeto **TraceCore**, um sistema desenvolvido em Java para controle de fluxo de produção, focado na aplicação prática de Programação Orientada a Objetos e comunicação em rede.

## 1. Ideia do Projeto

O **TraceCore** é uma solução de rastreabilidade industrial que simula o ciclo de vida completo de produtos dentro de um ambiente de fábrica. Ele resolve o problema de falta de visibilidade sobre o estoque e a produção, permitindo que gestores saibam exatamente onde cada lote de matéria-prima está e quando um produto acabado foi gerado.

### O que é o projeto?
É um sistema distribuído composto por dois módulos principais:
1.  **Núcleo de Fábrica (Server):** Onde o usuário interage via CLI para cadastrar matérias-primas, transformar insumos em produtos e gerenciar o estoque.
2.  **Painel de Monitoramento (Monitor):** Um terminal independente que recebe alertas em tempo real via TCP/IP quando o estoque atinge níveis críticos.

### Como será usado?
1.  O usuário inicia o **Painel de Monitoramento** (que fica aguardando conexões).
2.  O usuário inicia o **Núcleo de Fábrica**.
3.  Pelo menu CLI, o usuário cadastra matérias-primas (ex: Aço, Plástico). Os dados são persistidos imediatamente em arquivos locais.
4.  O sistema inicia uma **Thread** em segundo plano que verifica as quantidades.
5.  Quando o usuário "produz" um item, o sistema abate a matéria-prima do estoque.
6.  Se o estoque cair abaixo do limite, o terminal de monitoramento exibe instantaneamente um alerta enviado via Socket.

---

## 2. Mapeamento de Conceitos (Aulas 03 a 15)

O projeto foi estruturado para utilizar cada conceito do cronograma de estudos:

* **Aula 03 (Variáveis Primitivas):** Uso de `int` para quantidades, `double` para pesos e `boolean` para status de ativação.
* **Aula 04 (Introdução à POO):** Representação de entidades do mundo real como `Lote` e `Fornecedor`.
* **Aula 05 (Arrays):** Manipulação inicial de buffers de dados e estruturas fixas de categorias.
* **Aula 06 (Pacotes):** Organização por conceito em `com.fabrica.classes`, `abstractclasses`, `interfaces`, `exceptions`, `repositories`, `threads` e `tcp`.
* **Aula 07 (Modificadores de Acesso):** Uso rigoroso de `private` para atributos, protegendo a integridade dos dados.
* **Aula 08 (Construtores e Static):** Construtores personalizados para garantir objetos válidos; `static` para contadores globais de IDs.
* **Aula 09 (Herança e Polimorfismo):** `MateriaPrima` e `ProdutoAcabado` herdam de `Item`. O sistema trata diferentes tipos de itens de forma polimórfica.
* **Aula 10 (Classes Abstratas):** A classe base `Item` é abstrata, impedindo a criação de itens genéricos sem tipo definido.
* **Aula 11 (Interface):** Interface `Rastreavel` que obriga a implementação de métodos de log de movimentação.
* **Aula 12 (Collections API):** Uso de `ArrayList` para o inventário e `Map` para busca rápida de itens por código único.
* **Aula 13 (Exceções):** Criação de `EstoqueInsuficienteException` e tratamento de erros de input com `try-catch`.
* **Aula 14 (Java NIO):** Persistência de dados em arquivos `.txt` ou `.csv` usando `Paths`, `Files` e `StandardOpenOption` para salvar o estado da fábrica.
* **Aula 15 (Threads):** Implementação de uma thread de monitoramento que roda independentemente do menu principal.

---

## 3. Escopo Técnico

### Camada de Persistência (Java NIO)
O sistema não perderá dados ao ser fechado. Cada movimentação (entrada ou saída) será registrada em arquivos de log e o estado atual do estoque será salvo em um arquivo de "Snapshot".

### Comunicação (TCP/IP)
Utilização de `ServerSocket` no monitor e `Socket` na fábrica. A comunicação será baseada em strings formatadas que trafegam pela rede local na porta definida nas constantes do sistema.

### Interface de Usuário (CLI)
Um menu amigável e validado:
1. Cadastrar Matéria-Prima
2. Iniciar Produção
3. Consultar Estoque Atual
4. Ver Histórico de Lote
5. Sair

---
## 4. Estrutura Base de Classes

- **`Item` (Abstract):** `String codigo`, `String nome`, `int quantidade`.
- **`MateriaPrima` (Extends Item):** `String fornecedor`.
- **`ProdutoAcabado` (Extends Item):** `String dataFabricacao`.
- **`Rastreavel` (Interface):** `void registrarMovimentacao(String msg)`.

### Organização por conceitos

- **`com.fabrica.app`**: pontos de entrada da aplicação.
- **`com.fabrica.abstractclasses`**: classe base abstrata.
- **`com.fabrica.classes`**: classes concretas do domínio e DTOs.
- **`com.fabrica.interfaces`**: contratos do projeto.
- **`com.fabrica.exceptions`**: exceções customizadas.
- **`com.fabrica.repositories`**: persistência em arquivos.
- **`com.fabrica.services`**: regras de negócio.
- **`com.fabrica.threads`**: concorrência.
- **`com.fabrica.tcp`**: comunicação entre terminais.

---

# Utilização de IAs
Modelos utilizados:
- Gemini PRO
- GitHub copilot pro (escolha de modelo = auto)

## Prompts:

### 01 Gemini Pro
**Atue como um Arquiteto de Software Java Senior.**  
**Tarefa:** Gere um arquivo de documentação `CONTEXTO.md` para um projeto acadêmico de Java que consiste em uma **CLI (Interface de Linha de Comando) para gerenciamento de fluxo de produção de uma empresa desde a matéria prima até o despache.

**Requisitos do Escopo:**

1. **Arquitetura Cliente-Servidor:** O sistema deve operar via sockets TCP.

2. **Concorrência:** O servidor deve utilizar Threads para gerenciar múltiplos clientes simultâneos.

3. **Persistência:** O armazenamento de dados deve ser feito em arquivos locais (txt ou binários) utilizando as bibliotecas de I/O do Java.


**Estrutura do Documento .md:**

- **Título do Projeto:** Nome criativo para a ferramenta.

- **Visão Geral:** Descrição detalhada da ideia, o problema que resolve e o público-alvo (usuário técnico via CLI).

- **Fluxo de Uso:** Passo a passo de como o usuário interage com o cliente e como o servidor processa as requisições.

- **Especificações Técnicas (Mapeamento de Conceitos):** Descreva como cada um dos seguintes conceitos de aula será aplicado no código:  
  • _Variáveis Primitivas, Arrays e Collections API:_ (Ex: Gerenciamento de memória e listas de objetos).  
  • _POO, Herança, Polimorfismo, Classes Abstratas e Interfaces:_ (Ex: Definição da estrutura de dados e contratos de serviço).  
  • _Modificadores de Acesso, Construtores e Static:_ (Ex: Encapsulamento e padrões de criação).  
  • _Exceções:_ (Ex: Tratamento de erros de conexão e leitura de arquivos).  
  • _Java NIO:_ (Ex: Manipulação eficiente de arquivos e caminhos).  
  • _Threads & TCP:_ (Ex: Implementação do servidor multithreaded).

#### Resultado:
A LLM respondeu com o arquivo que foi utilizado acima no readme com todas as especificações do projeto que foi utilizado com contexto para todos os prompts subsequentes

---
### 02 GitHub copilot pro (auto)
/plan Create a plan to implement the tcp tread described in the readme file

#### Resultado:
Plano criado:
Ele cobre a implementação em 8 etapas, partindo do estado atual do projeto — hoje só há o menu em Main.java — até o fluxo completo do README:

1. estruturar domínio e serviços de estoque,
2. adicionar persistência com Java NIO,
3. criar a thread de monitoramento de estoque,
4. implementar o envio de alertas via TCP (Socket/ServerSocket),
5. criar o terminal separado de monitoramento,
6. integrar tudo ao Main,
7. validar o cenário ponta a ponta com alerta em tempo real.
Também registrei os todos e dependências no rastreamento interno para execução posterior.

---

### 3 GitHub copilot pro (auto)
Create a .md file in the folder i gave you explaining how does the tcp connection works in detail and why it was used to tread two terminals at the same time

#### Resultado:
Criou um arquivo md explicando em detalhes por o protoclo tcp com tread foi usado para rodar dois terminais em paralelo

---

### 4 GitHub copilot pro (GPT-5.4)
Reestructure the pages to instead of use the industry standard use a concept based folder strucute ex: classes, abstract classes, superclass, exeptions

#### Resultado:
O projeto foi reorganizado para uma estrutura baseada em conceitos de POO. Os pacotes antigos orientados por camada foram substituídos por pastas como `app`, `abstractclasses`, `classes`, `interfaces`, `exceptions`, `repositories`, `services`, `threads` e `tcp`, com atualização dos imports, packages e comandos documentados no README.

---
