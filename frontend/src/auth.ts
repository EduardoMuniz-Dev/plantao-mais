import { UserManager, type User } from "oidc-client-ts";

export const userManager = new UserManager({
  authority: import.meta.env.VITE_KEYCLOAK_URL ?? "http://localhost:8180/realms/plantao",
  client_id: "plantao-web",
  redirect_uri: window.location.origin + "/",
  post_logout_redirect_uri: window.location.origin + "/",
  scope: "openid profile",
});

/** Lê os papéis do realm no access token. Serve só para esconder botões: quem decide é a API. */
export function papeisDo(user: User | null): string[] {
  if (!user) return [];
  try {
    const parte = user.access_token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(parte)).realm_access?.roles ?? [];
  } catch {
    return [];
  }
}
