// Roda contra `wrangler dev`: BASE=http://127.0.0.1:8799 node --test test/
import assert from "node:assert/strict";
import { test } from "node:test";

const BASE = process.env.BASE ?? "http://127.0.0.1:8799";
const P = { name: "Bit", color: "#7fb7a4", accessory: "CAP", buddy: "CAT" };

async function api(metodo, rota, { conta, body } = {}) {
  const r = await fetch(BASE + rota, {
    method: metodo,
    headers: conta ? { authorization: `Bearer ${conta.code}:${conta.secret}` } : {},
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return { status: r.status, json: await r.json() };
}
const nova = async (name = "Bit") => (await api("POST", "/v1/glitches", { body: { ...P, name } })).json;
async function dupla() {
  const a = await nova("Ana"), b = await nova("Beto");
  await api("POST", "/v1/friends", { conta: a, body: { code: b.code } });
  await api("POST", "/v1/friends", { conta: b, body: { code: a.code } });
  return [a, b];
}

test("cria com código legível e recusa perfil ruim", async () => {
  const r = await api("POST", "/v1/glitches", { body: P });
  assert.equal(r.status, 201);
  assert.match(r.json.code, /^GLITCH-[2-9A-HJ-NP-Z]{6}$/);
  assert.equal((await api("POST", "/v1/glitches", { body: { ...P, name: "" } })).status, 400);
  assert.equal((await api("POST", "/v1/glitches", { body: { ...P, name: "x".repeat(17) } })).status, 400);
  assert.equal((await api("POST", "/v1/glitches", { body: { ...P, color: "red" } })).status, 400);
});

test("sem segredo certo não entra", async () => {
  const a = await nova();
  assert.equal((await api("GET", "/v1/friends")).status, 401);
  assert.equal((await api("GET", "/v1/friends", { conta: { ...a, secret: "0".repeat(64) } })).status, 401);
});

test("perfil do amigo só aparece com amizade dos dois lados", async () => {
  const a = await nova("Ana"), b = await nova("Beto");
  const uma = await api("POST", "/v1/friends", { conta: a, body: { code: b.code.toLowerCase() } });
  assert.deepEqual(uma.json.amigos, [{ code: b.code, mutual: false }]);
  await api("POST", "/v1/friends", { conta: b, body: { code: a.code } });
  const ambos = await api("GET", "/v1/friends", { conta: a });
  assert.equal(ambos.json.amigos[0].name, "Beto");
});

test("não adiciona a si nem código inexistente", async () => {
  const a = await nova();
  assert.equal((await api("POST", "/v1/friends", { conta: a, body: { code: a.code } })).status, 400);
  assert.equal((await api("POST", "/v1/friends", { conta: a, body: { code: "GLITCH-222222" } })).status, 404);
});

test("só amigo mútuo manda visita", async () => {
  const a = await nova(), b = await nova();
  await api("POST", "/v1/friends", { conta: a, body: { code: b.code } });
  assert.equal((await api("POST", "/v1/visits", { conta: a, body: { to: b.code } })).status, 403);
});

test("visita chega uma vez, com o perfil de quem mandou", async () => {
  const [a, b] = await dupla();
  assert.equal((await api("POST", "/v1/visits", { conta: a, body: { to: b.code } })).status, 201);
  const r = await api("POST", "/v1/visits/claim", { conta: b });
  assert.deepEqual(r.json.visitas, [{ code: a.code, name: "Ana", color: "#7FB7A4", accessory: "CAP", buddy: "CAT" }]);
  assert.deepEqual((await api("POST", "/v1/visits/claim", { conta: b })).json.visitas, []);
});

test("insistir na visita é recusado", async () => {
  const [a, b] = await dupla();
  await api("POST", "/v1/visits", { conta: a, body: { to: b.code } });
  assert.equal((await api("POST", "/v1/visits", { conta: a, body: { to: b.code } })).status, 429);
});

test("desfazer amizade cancela a visita pendente", async () => {
  const [a, b] = await dupla();
  await api("POST", "/v1/visits", { conta: a, body: { to: b.code } });
  await api("DELETE", `/v1/friends/${a.code}`, { conta: b });
  assert.deepEqual((await api("POST", "/v1/visits/claim", { conta: b })).json.visitas, []);
  assert.equal((await api("POST", "/v1/visits", { conta: a, body: { to: b.code } })).status, 403);
});

test("apagar a conta some com tudo", async () => {
  const [a, b] = await dupla();
  assert.equal((await api("DELETE", "/v1/me", { conta: a })).status, 200);
  assert.equal((await api("GET", "/v1/friends", { conta: a })).status, 401);
  assert.deepEqual((await api("GET", "/v1/friends", { conta: b })).json.amigos, []);
});
