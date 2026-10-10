# Plantão+

API REST para gestão de escalas de plantão de uma unidade de saúde, desenvolvida com Java e Spring Boot.

> **Status:** em desenvolvimento. Projeto de estudo, construído por fases.

## Sobre o projeto

O Plantão+ nasceu da minha experiência como técnico em Enfermagem: coordenadores montam a escala da unidade e os profissionais consultam seus plantões. A ideia é cobrir, na prática, o que se espera de uma API de back-end profissional: REST, validação, segurança com papéis, autenticação com OAuth2, banco relacional e containers.

## Tecnologias

- Java 21
- Spring Boot 4 (Spring Web, Spring Data JPA, Bean Validation, Spring Security)
- Maven (via `mvnw`)
- PostgreSQL 17
- Docker e Docker Compose

## Como rodar

Pré-requisitos: JDK 21 e Docker.

```bash
git clone https://github.com/EduardoMuniz-Dev/plantao-mais.git
cd plantao-mais
docker compose up -d
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. O banco PostgreSQL roda em um container, e os dados ficam em um volume do Docker, então sobrevivem a reinícios da aplicação. A senha padrão do banco (`plantao_dev`) é apenas para desenvolvimento local; o banco só aceita conexões do próprio computador.

## Endpoints

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
- **`ddl-auto=update` (limitação atual).** O Hibernate cria e ajusta as tabelas sozinho. É prático em desenvolvimento, mas em produção o esquema deve ser versionado com uma ferramenta de migração, como o Flyway.
- **Validação na entrada com Bean Validation.** As regras (`@NotBlank`, `@NotNull`) ficam na entidade e o `@Valid` no controlador barra dados inválidos antes de chegar ao banco.
- **Tratamento de erros centralizado.** Uma classe com `@RestControllerAdvice` converte falhas de validação em `400` com um corpo que indica cada campo inválido, em vez de repetir esse código em cada endpoint.
- **Códigos HTTP deliberados.** `POST` devolve `201` com o cabeçalho `Location`. `DELETE` devolve `204` quando remove e `404` quando o id não existe. `PUT` e `DELETE` são idempotentes.
- **`PUT` substitui o recurso inteiro.** Alterações parciais com `PATCH` não estão implementadas.
- **DTOs de entrada e saída.** A API recebe `PlantaoRequest` e devolve `PlantaoResponse` (records), e não a entidade JPA. Assim o cliente não controla o `id` e o formato da API não fica amarrado à tabela. A validação mora no DTO de entrada.
- **API sem sessão e sem CSRF.** A API é stateless: as credenciais vão em cada pedido, então não há sessão de navegador para proteger com CSRF. Por isso o CSRF foi desligado de propósito.
- **Autorização por papéis (RBAC).** Leitura para `COORDENADOR` e `TECNICO`; escrita só para `COORDENADOR`. Tudo que não foi listado exige autenticação.
- **Senhas de desenvolvimento no código (limitação atual).** São apenas para uso local e ficam em memória com hash BCrypt. A fase do Keycloak remove esses usuários.
- **Código organizado por funcionalidade.** Os pacotes `plantao` e `erro` agrupam o que muda junto.

## Roteiro

- [x] API REST com CRUD de plantões e códigos HTTP corretos
- [x] Validação de entrada com mensagens por campo
- [x] Spring Security com papéis (`COORDENADOR` e `TECNICO`)
- [x] DTOs de entrada e saída no lugar da entidade exposta
- [ ] Autenticação com OAuth2 e Keycloak (authorization code e client credentials)
- [x] PostgreSQL com Docker Compose
- [ ] Dockerfile da API, para subir tudo com um comando
- [ ] Front-end em TypeScript consumindo a API
- [ ] Documentação das decisões técnicas

## Autor

Eduardo Muniz, [GitHub](https://github.com/EduardoMuniz-Dev)
