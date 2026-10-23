# Plantão+

![CI](https://github.com/EduardoMuniz-Dev/plantao-mais/actions/workflows/ci.yml/badge.svg)

API REST para gestão de escalas de plantão de uma unidade de saúde, desenvolvida com Java e Spring Boot.

> **Status:** em desenvolvimento. Projeto de estudo, construído por fases.

## Sobre o projeto

O Plantão+ nasceu da minha experiência como técnico em Enfermagem: coordenadores montam a escala da unidade e os profissionais consultam seus plantões. A ideia é cobrir, na prática, o que se espera de uma API de back-end profissional: REST, validação, segurança com papéis, autenticação OAuth2 com Keycloak, banco relacional e containers.

## Tecnologias

- Java 21
- Spring Boot 4 (Spring Web, Spring Data JPA, Bean Validation, Spring Security com OAuth2 Resource Server)
- Keycloak 26 (servidor de identidade; emite os tokens JWT)
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

Sobem três serviços: o PostgreSQL, o Keycloak (que importa o realm `plantao` de `keycloak/plantao-realm.json`) e a API.

| Serviço  | Endereço                | Observação                                  |
|----------|-------------------------|---------------------------------------------|
| API      | `http://localhost:8080` | Exige token (veja a seção abaixo)           |
| Keycloak | `http://localhost:8180` | Painel de administração: `admin` / `admin_dev` |
| Banco    | `localhost:5432`        | Usuário `plantao`, senha `plantao_dev`      |

A primeira execução demora alguns minutos, porque baixa as imagens e as dependências, e o Keycloak leva cerca de 1 minuto para ficar pronto. Os dados ficam em volumes do Docker e sobrevivem a reinícios. As senhas padrão (banco, admin do Keycloak e secret do client) são apenas para desenvolvimento local e podem ser trocadas pelas variáveis de ambiente `POSTGRES_PASSWORD`, `KEYCLOAK_ADMIN_PASSWORD` e `PLANTAO_M2M_SECRET`. As portas só aceitam conexões do próprio computador.

O Keycloak só importa o realm quando ele ainda não existe. Para recriá-lo a partir do arquivo, use `docker compose down -v` (atenção: isso apaga também os dados do banco).

### Modo desenvolvimento

Para rodar a API pela IDE ou pelo Maven, suba só o banco e o Keycloak (pré-requisitos: JDK 21 e Docker):

```bash
docker compose up -d db keycloak
./mvnw spring-boot:run
```

### Testes

Os testes usam um banco H2 em memória e um `JwtDecoder` de teste, que transforma tokens fictícios em tokens com os papéis `COORDENADOR` e `TECNICO`. Por isso não precisam de Docker nem do Keycloak:

```bash
./mvnw verify
```

Os mesmos testes rodam automaticamente no GitHub Actions a cada push na `main`.

## Como obter um token

A API não tem tela de login nem guarda senhas: ela valida tokens emitidos pelo Keycloak. Para chamar a API sem usuário (por exemplo, um script), use o client `plantao-m2m` com o fluxo **client credentials**. A conta de serviço dele tem o papel `COORDENADOR`:

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/plantao/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=plantao-m2m \
  -d client_secret=plantao_m2m_dev \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

Para testar o papel `TECNICO`, em desenvolvimento dá para pedir o token de um usuário de teste (o login por tela, com authorization code, virá com o front-end):

```bash
TOKEN_TECNICO=$(curl -s -X POST http://localhost:8180/realms/plantao/protocol/openid-connect/token \
  -d grant_type=password -d client_id=plantao-web \
  -d username=tecnico -d password=tec-dev \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

Os tokens duram 5 minutos.

## Endpoints

Todas as rotas exigem o cabeçalho `Authorization: Bearer <token>`.

| Método | Rota             | Descrição                  | Resposta de sucesso |
|--------|------------------|----------------------------|---------------------|
| GET    | `/plantoes`      | Lista todos os plantões    | 200 OK              |
| GET    | `/plantoes/{id}` | Busca um plantão           | 200 OK (404 se não existe) |
| POST   | `/plantoes`      | Cadastra um plantão        | 201 Created + `Location` |
| PUT    | `/plantoes/{id}` | Atualiza um plantão        | 200 OK (404 se não existe) |
| DELETE | `/plantoes/{id}` | Remove um plantão          | 204 No Content (404 se não existe) |

### Exemplo

```bash
curl -i -X POST localhost:8080/plantoes \
  -H "Authorization: Bearer $TOKEN" \
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

Com a aplicação rodando, a documentação interativa (Swagger UI) fica em `http://localhost:8080/swagger-ui/index.html` e a especificação OpenAPI em `http://localhost:8080/v3/api-docs`. Use o botão **Authorize** e cole um token (veja a seção anterior) para testar os endpoints direto pelo navegador.

## Segurança

A API é um *resource server* OAuth2: ela não emite tokens. A cada pedido, valida a assinatura, o emissor e a validade do JWT enviado e autoriza pelos papéis do realm, lidos do campo `realm_access.roles` do token.

| Papel         | Listar e buscar plantões | Criar, alterar e remover |
|---------------|--------------------------|--------------------------|
| `COORDENADOR` | sim                      | sim                      |
| `TECNICO`     | sim                      | não (403)                |

Sem token, ou com token inválido ou expirado, a API responde `401`. Com token sem permissão, responde `403`.

O realm `plantao` (papéis, client `plantao-m2m` e usuários de teste) está versionado em `keycloak/plantao-realm.json` e é importado automaticamente quando o Keycloak sobe. Usuários de **desenvolvimento local**: `coordenador` / `coord-dev` e `tecnico` / `tec-dev`. São apenas para uso local, não para produção.

## Decisões técnicas

- **PostgreSQL em container Docker.** Os dados ficam em disco, em um volume, e persistem entre reinícios. A porta é publicada só em `127.0.0.1`, para o banco não ficar exposto na rede.
- **Imagem da API em duas etapas (multi-stage).** A primeira etapa compila o projeto com o JDK; a imagem final leva só o JRE e o jar, o que a deixa menor, e roda com um usuário sem privilégios em vez de `root`.
- **Configuração por variáveis de ambiente.** O `application.properties` serve para rodar na máquina (`localhost`). No Compose, variáveis como `SPRING_DATASOURCE_URL` e `SERVER_ADDRESS` sobrescrevem esses valores para o ambiente de container, sem duplicar arquivos de configuração. A API continua publicada apenas em `127.0.0.1` no computador.
- **Espera pelo banco.** O serviço da API só inicia depois que o healthcheck do PostgreSQL (`pg_isready`) confirma que ele aceita conexões.
- **Integração contínua com GitHub Actions.** A cada push na `main` e em pull requests, um workflow roda `./mvnw verify`, para que um commit que quebra os testes seja percebido logo.
- **`ddl-auto=update` (limitação atual).** O Hibernate cria e ajusta as tabelas sozinho. É prático em desenvolvimento, mas em produção o esquema deve ser versionado com uma ferramenta de migração, como o Flyway.
- **Validação na entrada com Bean Validation.** As regras (`@NotBlank`, `@NotNull`) ficam no DTO de entrada e o `@Valid` no controlador barra dados inválidos antes de chegar ao banco.
- **Tratamento de erros centralizado.** Uma classe com `@RestControllerAdvice` converte falhas de validação em `400` com um corpo que indica cada campo inválido, em vez de repetir esse código em cada endpoint.
- **Códigos HTTP deliberados.** `POST` devolve `201` com o cabeçalho `Location`. `DELETE` devolve `204` quando remove e `404` quando o id não existe. `PUT` e `DELETE` são idempotentes.
- **`PUT` substitui o recurso inteiro.** Alterações parciais com `PATCH` não estão implementadas.
- **DTOs de entrada e saída.** A API recebe `PlantaoRequest` e devolve `PlantaoResponse` (records), e não a entidade JPA. Assim o cliente não controla o `id` e o formato da API não fica amarrado à tabela. A validação mora no DTO de entrada.
- **Autenticação delegada ao Keycloak.** A API valida JWTs em vez de guardar usuários e senhas. Um conversor transforma `realm_access.roles` do token em autoridades do Spring (`ROLE_COORDENADOR`, `ROLE_TECNICO`), e as regras de acesso continuam no `SegurancaConfig`.
- **API sem sessão e sem CSRF.** A API é stateless: o token vai em cada pedido, então não há sessão de navegador para proteger com CSRF. Por isso o CSRF foi desligado de propósito.
- **Autorização por papéis (RBAC).** Leitura para `COORDENADOR` e `TECNICO`; escrita só para `COORDENADOR`. Tudo que não foi listado exige autenticação.
- **Chaves pela rede interna, emissor pelo endereço público.** No Compose, a API busca as chaves de assinatura em `keycloak:8080` (`jwk-set-uri`), mas continua exigindo que o `iss` do token seja `http://localhost:8180/realms/plantao`, o endereço pelo qual os clientes pedem o token. Dentro do container, `localhost` não alcança o Keycloak.
- **Realm do Keycloak versionado.** O arquivo `keycloak/plantao-realm.json` descreve papéis, client e usuários de teste, e o Compose o importa ao subir. Quem clona o repositório tem o mesmo ambiente, sem configurar nada no painel. O secret do client vem de uma variável de ambiente.
- **Testes sem Keycloak.** Os testes de integração trocam o `JwtDecoder` por um de teste, que aceita tokens fictícios com os papéis. Eles exercitam os filtros de segurança de verdade (401, 403) sem depender de um servidor de identidade rodando.
- **Credenciais de desenvolvimento versionadas (limitação atual).** Senhas dos usuários de teste, do admin e do secret do client estão no repositório, só para uso local. Em produção, viriam de um cofre ou de variáveis de ambiente.
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
- [x] API protegida por OAuth2 com Keycloak (client credentials) e realm versionado
- [ ] Login de usuário por authorization code com PKCE
- [ ] Front-end em TypeScript consumindo a API

## Autor

Eduardo Muniz, [GitHub](https://github.com/EduardoMuniz-Dev)
