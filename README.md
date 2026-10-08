# Plantão+

API REST para gestão de escalas de plantão de uma unidade de saúde, desenvolvida com Java e Spring Boot.

> **Status:** em desenvolvimento. Projeto de estudo, construído por fases.

## Sobre o projeto

O Plantão+ nasceu da minha experiência como técnico em Enfermagem: coordenadores montam a escala da unidade e os profissionais consultam seus plantões. A ideia é cobrir, na prática, o que se espera de uma API de back-end profissional: REST, validação, segurança com papéis, autenticação com OAuth2, banco relacional e containers.

## Tecnologias

- Java 21
- Spring Boot 4 (Spring Web, Spring Data JPA, Bean Validation)
- Maven (via `mvnw`)
- H2 (banco em memória, apenas nesta fase)

## Como rodar

Pré-requisito: JDK 21.

```bash
git clone https://github.com/EduardoMuniz-Dev/plantao-mais.git
cd plantao-mais
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Os dados ficam em memória e são apagados quando a aplicação para.

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
curl -i -X POST localhost:8080/plantoes \
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

## Decisões técnicas

- **H2 em memória, por enquanto.** Permite rodar o projeto com um comando e sem instalar banco. Os dados se perdem ao parar a aplicação. Na fase do Docker, a troca é para PostgreSQL.
- **Validação na entrada com Bean Validation.** As regras (`@NotBlank`, `@NotNull`) ficam na entidade e o `@Valid` no controlador barra dados inválidos antes de chegar ao banco.
- **Tratamento de erros centralizado.** Uma classe com `@RestControllerAdvice` converte falhas de validação em `400` com um corpo que indica cada campo inválido, em vez de repetir esse código em cada endpoint.
- **Códigos HTTP deliberados.** `POST` devolve `201` com o cabeçalho `Location`. `DELETE` devolve `204` quando remove e `404` quando o id não existe. `PUT` e `DELETE` são idempotentes.
- **`PUT` substitui o recurso inteiro.** Alterações parciais com `PATCH` não estão implementadas.
- **Entidade exposta diretamente na API (limitação atual).** É simples, mas deixa o cliente enviar campos como o `id`. Na fase de segurança, será trocada por DTOs de entrada e saída.
- **Código organizado por funcionalidade.** Os pacotes `plantao` e `erro` agrupam o que muda junto.

## Roteiro

- [x] API REST com CRUD de plantões e códigos HTTP corretos
- [x] Validação de entrada com mensagens por campo
- [ ] Spring Security com papéis (`COORDENADOR` e `TECNICO`) e DTOs
- [ ] Autenticação com OAuth2 e Keycloak (authorization code e client credentials)
- [ ] PostgreSQL e Docker (`docker-compose`)
- [ ] Front-end em TypeScript consumindo a API
- [ ] Documentação das decisões técnicas

## Autor

Eduardo Muniz, [GitHub](https://github.com/EduardoMuniz-Dev)
