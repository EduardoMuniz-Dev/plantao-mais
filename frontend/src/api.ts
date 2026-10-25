import { userManager } from "./auth";

const API = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export type Plantao = {
  id: number;
  setor: string;
  profissional: string;
  inicio: string;
  fim: string;
};

export async function listarPlantoes(): Promise<Plantao[]> {
  const user = await userManager.getUser();
  const resposta = await fetch(`${API}/plantoes`, {
    headers: { Authorization: `Bearer ${user?.access_token}` },
  });
  if (!resposta.ok) throw new Error(`A API respondeu ${resposta.status}`);
  return resposta.json();
}
