import { useEffect, useState } from "react";
import type { User } from "oidc-client-ts";
import { papeisDo, userManager } from "./auth";
import { listarPlantoes, type Plantao } from "./api";
import "./App.css";

// Em desenvolvimento o React executa os efeitos duas vezes, mas o retorno do
// Keycloak (?code=...) só pode ser processado uma vez. Por isso a promessa é guardada.
let carregamento: Promise<User | null> | null = null;

function carregarUsuario(): Promise<User | null> {
  if (!carregamento) {
    const voltouDoLogin = new URLSearchParams(window.location.search).has("code");
    const busca = voltouDoLogin ? userManager.signinRedirectCallback() : userManager.getUser();
    carregamento = busca
      .catch(() => null)
      .then((u) => {
        window.history.replaceState({}, "", "/");
        return u && !u.expired ? u : null;
      });
  }
  return carregamento;
}

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [plantoes, setPlantoes] = useState<Plantao[]>([]);
  const [erro, setErro] = useState("");

  useEffect(() => {
    carregarUsuario()
      .then(setUser)
      .finally(() => setCarregando(false));
  }, []);

  useEffect(() => {
    if (!user) return;
    listarPlantoes()
      .then(setPlantoes)
      .catch((e: Error) => setErro(e.message));
  }, [user]);

  if (carregando) return <main><p>Carregando…</p></main>;

  if (!user) {
    return (
      <main>
        <h1>Plantão+</h1>
        <p>Entre para ver as escalas de plantão.</p>
        <button onClick={() => userManager.signinRedirect()}>Entrar</button>
      </main>
    );
  }

  const papeis = papeisDo(user);

  return (
    <main>
      <header>
        <div>
          <h1>Plantão+</h1>
          <p>
            {user.profile.preferred_username} ({papeis.join(", ") || "sem papel"})
          </p>
        </div>
        <button onClick={() => userManager.signoutRedirect()}>Sair</button>
      </header>

      {erro && <p className="erro">{erro}</p>}

      <table>
        <thead>
          <tr>
            <th>Setor</th>
            <th>Profissional</th>
            <th>Início</th>
            <th>Fim</th>
          </tr>
        </thead>
        <tbody>
          {plantoes.map((p) => (
            <tr key={p.id}>
              <td>{p.setor}</td>
              <td>{p.profissional}</td>
              <td>{new Date(p.inicio).toLocaleString("pt-BR")}</td>
              <td>{new Date(p.fim).toLocaleString("pt-BR")}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {plantoes.length === 0 && !erro && <p>Nenhum plantão cadastrado.</p>}
    </main>
  );
}
