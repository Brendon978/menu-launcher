// Visitas entre Glitches. Guarda só nome, cor, acessório, bichinho e quem é amigo de quem.
// Visita é recado: quem manda deixa aqui, o launcher do amigo busca e ela some.

const ALFABETO = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // sem 0, O, 1 e I, que se confundem
const CODIGO = /^GLITCH-[2-9A-HJ-NP-Z]{6}$/;
const MAX_AMIGOS = 50;
const VISITA_VALE_MS = 7 * 24 * 3600 * 1000;
const INTERVALO_VISITA_MS = 10 * 60 * 1000;

const json = (body, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json; charset=utf-8" } });
const erro = (status, codigo) => json({ erro: codigo }, status);

function novoCodigo() {
  const bytes = crypto.getRandomValues(new Uint8Array(6));
  return "GLITCH-" + [...bytes].map((b) => ALFABETO[b % ALFABETO.length]).join("");
}

function novoSegredo() {
  return [...crypto.getRandomValues(new Uint8Array(32))].map((b) => b.toString(16).padStart(2, "0")).join("");
}

async function hash(texto) {
  const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(texto));
  return [...new Uint8Array(d)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** Devolve o perfil limpo, ou null se algum campo não servir. */
function perfil(b) {
  if (!b || typeof b !== "object") return null;
  const name = typeof b.name === "string" ? b.name.trim() : "";
  const { color, accessory, buddy } = b;
  if (name.length < 1 || [...name].length > 16 || /[\u0000-\u001f\u007f]/.test(name)) return null;
  if (typeof color !== "string" || !/^#[0-9A-Fa-f]{6}$/.test(color)) return null;
  for (const v of [accessory, buddy]) if (typeof v !== "string" || !/^[A-Z_]{1,24}$/.test(v)) return null;
  return { name, color: color.toUpperCase(), accessory, buddy };
}

async function corpo(req) {
  try {
    const txt = await req.text();
    return txt.length > 2000 ? null : JSON.parse(txt);
  } catch {
    return null;
  }
}

/** "Authorization: Bearer GLITCH-XXXXXX:segredo". Devolve o código, ou null. */
async function autenticado(req, env) {
  const m = /^Bearer (GLITCH-[2-9A-HJ-NP-Z]{6}):([0-9a-f]{64})$/.exec(req.headers.get("authorization") ?? "");
  if (!m) return null;
  const linha = await env.DB.prepare("SELECT secret_hash FROM glitches WHERE code = ?").bind(m[1]).first();
  return linha && linha.secret_hash === (await hash(m[2])) ? m[1] : null;
}

async function criar(req, env) {
  const p = perfil(await corpo(req));
  if (!p) return erro(400, "perfil_invalido");
  const secret = novoSegredo();
  const secretHash = await hash(secret);
  for (let i = 0; i < 5; i++) {
    const code = novoCodigo();
    const r = await env.DB.prepare(
      "INSERT OR IGNORE INTO glitches (code, secret_hash, name, color, accessory, buddy, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
    ).bind(code, secretHash, p.name, p.color, p.accessory, p.buddy, Date.now()).run();
    if (r.meta.changes === 1) return json({ code, secret }, 201);
  }
  return erro(503, "tente_de_novo");
}

async function atualizar(req, env, eu) {
  const p = perfil(await corpo(req));
  if (!p) return erro(400, "perfil_invalido");
  await env.DB.prepare("UPDATE glitches SET name = ?, color = ?, accessory = ?, buddy = ? WHERE code = ?")
    .bind(p.name, p.color, p.accessory, p.buddy, eu).run();
  return json({ ok: true });
}

/** O perfil do amigo só aparece quando os dois se adicionaram. */
async function amigos(env, eu) {
  const { results } = await env.DB.prepare(
    `SELECT f.friend AS code, EXISTS(SELECT 1 FROM friends v WHERE v.owner = f.friend AND v.friend = f.owner) AS mutual,
            g.name, g.color, g.accessory, g.buddy
     FROM friends f JOIN glitches g ON g.code = f.friend WHERE f.owner = ? ORDER BY f.friend`,
  ).bind(eu).all();
  return results.map((r) =>
    r.mutual ? { code: r.code, mutual: true, name: r.name, color: r.color, accessory: r.accessory, buddy: r.buddy }
      : { code: r.code, mutual: false });
}

async function adicionar(req, env, eu) {
  const b = await corpo(req);
  const code = typeof b?.code === "string" ? b.code.trim().toUpperCase() : "";
  if (!CODIGO.test(code)) return erro(400, "codigo_invalido");
  if (code === eu) return erro(400, "voce_mesmo");
  const existe = await env.DB.prepare("SELECT 1 AS x FROM glitches WHERE code = ?").bind(code).first();
  if (!existe) return erro(404, "nao_existe");
  const conta = await env.DB.prepare("SELECT COUNT(*) AS n FROM friends WHERE owner = ?").bind(eu).first();
  if (conta.n >= MAX_AMIGOS) return erro(409, "amigos_demais");
  await env.DB.prepare("INSERT OR IGNORE INTO friends (owner, friend) VALUES (?, ?)").bind(eu, code).run();
  return json({ amigos: await amigos(env, eu) });
}

async function remover(env, eu, code) {
  await env.DB.prepare("DELETE FROM friends WHERE owner = ? AND friend = ?").bind(eu, code).run();
  await env.DB.prepare("DELETE FROM visits WHERE (to_code = ? AND from_code = ?) OR (to_code = ? AND from_code = ?)")
    .bind(eu, code, code, eu).run();
  return json({ amigos: await amigos(env, eu) });
}

async function visitar(req, env, eu) {
  const b = await corpo(req);
  const to = typeof b?.to === "string" ? b.to.trim().toUpperCase() : "";
  if (!CODIGO.test(to)) return erro(400, "codigo_invalido");
  const mutuo = await env.DB.prepare(
    `SELECT 1 AS x FROM friends a JOIN friends b ON b.owner = a.friend AND b.friend = a.owner
     WHERE a.owner = ? AND a.friend = ?`,
  ).bind(eu, to).first();
  if (!mutuo) return erro(403, "nao_sao_amigos");
  const agora = Date.now();
  // Uma visita pendente por dupla: mandar de novo só renova, e o intervalo segura a insistência.
  const r = await env.DB.prepare(
    `INSERT INTO visits (to_code, from_code, created_at) VALUES (?1, ?2, ?3)
     ON CONFLICT (to_code, from_code) DO UPDATE SET created_at = ?3 WHERE visits.created_at <= ?4`,
  ).bind(to, eu, agora, agora - INTERVALO_VISITA_MS).run();
  return r.meta.changes === 1 ? json({ ok: true }, 201) : erro(429, "visita_recente");
}

/** Entrega e apaga de uma vez: duas buscas ao mesmo tempo não repetem a visita. */
async function buscar(env, eu) {
  await env.DB.prepare("DELETE FROM visits WHERE created_at < ?").bind(Date.now() - VISITA_VALE_MS).run();
  const { results } = await env.DB.prepare("DELETE FROM visits WHERE to_code = ? RETURNING from_code").bind(eu).all();
  if (results.length === 0) return json({ visitas: [] });
  const codes = results.map((r) => r.from_code);
  const marcas = codes.map(() => "?").join(",");
  const perfis = await env.DB.prepare(
    `SELECT g.code, g.name, g.color, g.accessory, g.buddy FROM glitches g
     JOIN friends a ON a.owner = ? AND a.friend = g.code
     WHERE g.code IN (${marcas})`,
  ).bind(eu, ...codes).all();
  return json({ visitas: perfis.results });
}

async function apagarConta(env, eu) {
  await env.DB.batch([
    env.DB.prepare("DELETE FROM visits WHERE to_code = ?1 OR from_code = ?1").bind(eu),
    env.DB.prepare("DELETE FROM friends WHERE owner = ?1 OR friend = ?1").bind(eu),
    env.DB.prepare("DELETE FROM glitches WHERE code = ?").bind(eu),
  ]);
  return json({ ok: true });
}

async function rota(req, env) {
  const { pathname } = new URL(req.url);
  const ip = req.headers.get("cf-connecting-ip") ?? "local";
  const geral = await env.LIMITE_GERAL?.limit({ key: ip });
  if (geral && !geral.success) return erro(429, "devagar");

  if (req.method === "POST" && pathname === "/v1/glitches") {
    const r = await env.LIMITE_CRIAR?.limit({ key: ip });
    if (r && !r.success) return erro(429, "devagar");
    return criar(req, env);
  }

  const eu = await autenticado(req, env);
  if (!eu) return erro(401, "nao_autorizado");

  if (pathname === "/v1/me" && req.method === "PUT") return atualizar(req, env, eu);
  if (pathname === "/v1/me" && req.method === "DELETE") return apagarConta(env, eu);
  if (pathname === "/v1/friends" && req.method === "GET") return json({ amigos: await amigos(env, eu) });
  if (pathname === "/v1/friends" && req.method === "POST") return adicionar(req, env, eu);
  const f = /^\/v1\/friends\/(GLITCH-[2-9A-HJ-NP-Z]{6})$/.exec(pathname);
  if (f && req.method === "DELETE") return remover(env, eu, f[1]);
  if (pathname === "/v1/visits" && req.method === "POST") return visitar(req, env, eu);
  if (pathname === "/v1/visits/claim" && req.method === "POST") return buscar(env, eu);
  return erro(404, "rota_inexistente");
}

export default {
  async fetch(req, env) {
    try {
      return await rota(req, env);
    } catch (e) {
      console.error(e);
      return erro(500, "falha");
    }
  },
};
