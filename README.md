# Plantão+

![CI](https://github.com/EduardoMuniz-Dev/plantao-mais/actions/workflows/ci.yml/badge.svg)

API REST para gestão de escalas de plantão de uma unidade de saúde, desenvolvida com Java e Spring Boot.

> **Status:** em desenvolvimento. Projeto de estudo, construído por fases.

## Sobre o projeto

O Plantão+ nasceu da minha experiência como técnico em Enfermagem: coordenadores montam a escala da unidade e os profissionais consultam seus plantões. A ideia é cobrir, na prática, o que se espera de uma API de back-end profissional: REST, validação, segurança com papéis, banco relacional e containers, com autenticação OAuth2 prevista nas próximas fases.

## Tecnologias

- Java 21
- Spring Boot 4 (Spring Web, Spring Data JPA, Bean Validation, Spring Security)
- Maven (via `mvnw`)
- springdoc-openapi 3 (documentação OpenAPI e Swagger UI)
- PostgreSQL 17
- Docker e Docker Compose
- GitHub Actions (integração contínua)

## Como rodar

Pré-requisito: Docker.

```bash
git clone https://github.com/EduardoMuniz-Dev/plantao-mais.git
cd plantao-mais
docker compose up --build
```

A primeira execução demora alguns minutos, porque baixa as imagens e as dependências. A API sobe em `http://localhost:8080`, junto com o PostgreSQL. Os dados ficam em um volume do Docker e sobrevivem a reinícios. A senha padrão do banco (`plantao_dev`) é apenas para desenvolvimento local; as portas só aceitam conexões do próprio computador.

### Modo desenvolvimento

Para rodar a API pela IDE ou pelo Maven, suba só o banco (pré-requisitos: JDK 21 e Docker):

```bash
docker compose up -d db
./mvnw spring-boot:run
```

### Testes

Com o banco rodando (`docker compose up -d db`):

```bash
./mvnw verify
```

Os mesmos testes rodam automaticamente no GitHub Actions a cada push na `main`.

## Endpoints

Todas as rotas exigem autenticação (veja a seção [Segurança](#segurança)).

| Método | Rota             | Descrição                  | Resposta de sucesso |
|--------|------------------|----------------------------|---------------------|
| GET    | `/plantoes`      | Lista todos os plantões    | 200 OK              |
| GET    | `/plantoes/{id}` | Busca um plantão           | 200 OK (404 se não existe) |
| POST   | `/plantoes`      | Cadastra um plantão        | 201 Created + `Location` |
| PUT    | `/plantoes/{id}` | Atualiza um plantão        | 200 OK (404 se não existe) |
| DELETE | `/plantoes/{id}` | Remove um plantão          | 204 No Content (404 se não existe) |

### Exemplo

```bash
curl -i -u coordenador:coord-dev -X POST localhost:8080/plantoes \
  -H "Content-Type: application/json" \
  -d '{"setor":"UTI","profissional":"Eduardo","inicio":"2026-10-10T19:00:00","fim":"2026-10-11T07:00:00"}'
```

### Resposta

O `POST` acima devolve `201 Created` com o cabeçalho `Location: /plantoes/1` e o corpo:

```json
{
  "id": 1,
  "setor": "UTI",
  "profissional": "Eduardo",
  "inicio": "2026-10-10T19:00:00",
  "fim": "2026-10-11T07:00:00"
}
```

### Validação

Dados inválidos retornam `400 Bad Request` indicando cada campo com problema:

```json
{
  "setor": "não deve estar em branco",
  "profissional": "não deve estar em branco",
  "inicio": "não deve ser nulo",
  "fim": "não deve ser nulo"
}
```

## Documentação da API

Com a aplicação rodando, a documentação interativa (Swagger UI) fica em `http://localhost:8080/swagger-ui/index.html` e a especificação OpenAPI em `http://localhost:8080/v3/api-docs`. Use o botão **Authorize** com um dos usuários de desenvolvimento (veja a seção de segurança) para testar os endpoints direto pelo navegador.

## Segurança

Todas as rotas exigem autenticação (HTTP Basic) e a autorização é feita por papéis:

| Papel         | Listar e buscar plantões | Criar, alterar e remover |
|---------------|--------------------------|--------------------------|
| `COORDENADOR` | sim                      | sim                      |
| `TECNICO`     | sim                      | não (403)                |

Sem credenciais, a API responde `401`. Com credenciais sem permissão, responde `403`.

Usuários de **desenvolvimento local** (em memória; não usar em produção):
`coordenador` / `coord-dev` e `tecnico` / `tec-dev`. Serão substituídos por autenticação OAuth2 com Keycloak.

## Decisões técnicas

- **PostgreSQL em container Docker.** Os dados ficam em disco, em um volume, e persistem entre reinícios. A porta é publicada só em `127.0.0.1`, para o banco não ficar exposto na rede.
- **Imagem da API em duas etapas (multi-stage).** A primeira etapa compila o projeto com o JDK; a imagem final leva só o JRE e o jar, o que a deixa menor, e roda com um usuário sem privilégios em vez de `root`.
- **Configuração por variáveis de ambiente.** O `application.properties` serve para rodar na máquina (`localhost`). No Compose, variáveis como `SPRING_DATASOURCE_URL` e `SERVER_ADDRESS` sobrescrevem esses valores para o ambiente de container, sem duplicar arquivos de configuração. A API continua publicada apenas em `127.0.0.1` no computador.
- **Espera pelo banco.** O serviço da API só inicia depois que o healthcheck do PostgreSQL (`pg_isready`) confirma que ele aceita conexões.
- **Integração contínua com GitHub Actions.** A cada push na `main` e em pull requests, um workflow sobe um PostgreSQL e roda `./mvnw verify`, para que um commit que quebra os testes seja percebido logo.
- **`ddl-auto=update` (limitação atual).** O Hibernate cria e ajusta as tabelas sozinho. É prático em desenvolvimento, mas em produção o esquema deve ser versionado com uma ferramenta de migração, como o Flyway.
- **Validação na entrada com Bean Validation.** As regras (`@NotBlank`, `@NotNull`) ficam no DTO de entrada e o `@Valid` no controlador barra dados inválidos antes de chegar ao banco.
- **Tratamento de erros centralizado.** Uma classe com `@RestControllerAdvice` converte falhas de validação em `400` com um corpo que indica cada campo inválido, em vez de repetir esse código em cada endpoint.
- **Códigos HTTP deliberados.** `POST` devolve `201` com o cabeçalho `Location`. `DELETE` devolve `204` quando remove e `404` quando o id não existe. `PUT` e `DELETE` são idempotentes.
- **`PUT` substitui o recurso inteiro.** Alterações parciais com `PATCH` não estão implementadas.
- **DTOs de entrada e saída.** A API recebe `PlantaoRequest` e devolve `PlantaoResponse` (records), e não a entidade JPA. Assim o cliente não controla o `id` e o formato da API não fica amarrado à tabela. A validação mora no DTO de entrada.
- **API sem sessão e sem CSRF.** A API é stateless: as credenciais vão em cada pedido, então não há sessão de navegador para proteger com CSRF. Por isso o CSRF foi desligado de propósito.
- **Autorização por papéis (RBAC).** Leitura para `COORDENADOR` e `TECNICO`; escrita só para `COORDENADOR`. Tudo que não foi listado exige autenticação.
- **Senhas de desenvolvimento no código (limitação atual).** São apenas para uso local e ficam em memória com hash BCrypt. A fase do Keycloak remove esses usuários.
- **Documentação aberta em desenvolvimento (limitação atual).** As rotas `/swagger-ui` e `/v3/api-docs` são liberadas sem autenticação, mas os endpoints da API continuam protegidos. Em produção, a documentação deve ser restringida ou desligada (`springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false`).
- **Código organizado por funcionalidade.** Os pacotes `plantao`, `seguranca`, `erro` e `documentacao` agrupam o que muda junto.

## Roteiro

- [x] API REST com CRUD de plantões e códigos HTTP corretos
- [x] Validação de entrada com mensagens por campo
- [x] Spring Security com papéis (`COORDENADOR` e `TECNICO`)
- [x] DTOs de entrada e saída no lugar da entidade exposta
- [x] PostgreSQL com Docker Compose
- [x] Dockerfile da API, para subir tudo com um comando
- [x] Testes de integração e CI com GitHub Actions
- [x] Documentação das decisões técnicas
- [x] Documentação interativa da API com Swagger/OpenAPI
- [ ] Autenticação com OAuth2 e Keycloak (authorization code e client credentials)
- [ ] Front-end em TypeScript consumindo a API

## Autor

Eduardo Muniz, [GitHub](https://github.com/EduardoMuniz-Dev)
