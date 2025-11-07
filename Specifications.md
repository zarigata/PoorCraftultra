
# **RetroForge** — Smooth‑Voxel Survival RPG with LLM Companions (Godot 4) — **Spec v0.2**

**Pitch:** A low‑poly, smooth‑voxel sandbox that blends **Minecraft‑style mining/building**, **Rust‑style base pieces (square & triangle)**, **RPG‑style factions & diplomacy**, and **one LLM‑powered companion per player**. Classical AI governs wild creatures, factions, and nations; your LLM companion interprets voice/text, understands the world, plans tasks, and helps you mine, craft, build, trade, and negotiate.

**Engine:** Godot 4.x (GDScript + optional GDExtensions for Steam/Discord/LLM/voxel meshing)  
**Platforms:** Windows / Linux / macOS (console/mobile later)  
**Networking:** Co‑op multiplayer (host + clients), **one companion per player** (LLM instance per player)  
**Style:** Flat‑shaded low‑poly, minimal textures (AI‑assisted only for icons/decals/props)  
**Save Model:** World seed + edit‑ops + placed entities + AI memories (summaries)  
**Integrations:** Steam (Achieves/Stats/Cloud/Lobbies/Workshop), Discord (Rich Presence), optional local/cloud LLM backends

---

## 0) What’s new in v0.2
- **LLM Companion System:** local **or** cloud backends; tool‑use functions to mine/craft/build/navigate/negotiate; memory & retrieval; voice I/O.  
- **Multiplayer LLM:** **one companion per player** with per‑player settings, privacy, and rate‑limits; companions can coordinate tasks.  
- **RPG Layer:** **Factions/Nations** tied to biomes; **classical AI mobs** with agendas; **reputation/respect** and **diplomacy** (trade, warn, escort, raid).  
- **Procedural “Generative Mobs” (non‑LLM):** param‑driven species templates (tiers, roles, behaviors) influenced by biome and faction.  
- Expanded backlogs, schemas, and guardrails.

---

## 1) Game Pillars (updated)
1. **Smooth Voxel World:** Soft terrain you can carve/fill anywhere with performant chunked LOD.  
2. **Rust‑like Building:** **Square**/**Triangle** snap pieces; simple stability; material tiers.  
3. **RPG Liveness:** Biomes support **factions/nations** that hunt, patrol, trade, and negotiate.  
4. **LLM Companions (one per player):** Conversational planning + tool use; voice or text; memory.  
5. **Classical AI Mobs:** Utility/GOAP/state‑machine actors; scalable and predictable.  
6. **Performance First:** Flat shading, pooled meshes, async meshing, capped edits.

---

## 2) Core Loops (expanded)

### Explore → Mine
- Carve caves, expose ore veins; mark POIs for your companion or team.  
- Companion can **scout**, **ping hazards**, **suggest routes** (“Tunnel to the copper vein 30m north‑east”).

### Craft → Build
- Collect → **smelt** (furnace) → **craft** tools/furniture → **build** with square/triangle pieces.  
- Companion can **queue smelts**, **craft batches**, and **place blueprints** (Builder role).

### Diplomacy → Trade/Quest → Defend/Raid
- Approach caravans/outposts; negotiate **prices**, **passage**, **bounties** based on reputation and current biome risks.  
- Factions respond to your actions (poaching, deforestation, banditry) and your **companion’s personality**.

### Co‑op Flow
- Each player has a personal companion. Players can **assign joint objectives** (mine/build/escort). Companions coordinate via a **Task Board** (see §10.5).

---

## 3) World, Biomes, and Nations (RPG layer)

### Biomes (MVP)
- **Verdant Woods** (forest), **Storm Peaks** (mountain), **Amber Dunes** (desert), **Mirelow** (swamp), **Shatter Coast** (coastal cliffs), **Ashen Reach** (volcanic), **Glacier Steppe** (tundra), **Crystal Depths** (subterranean caverns).  

### Nations / Factions (examples)
- **Verdant Court** (forest‑folk, defensive, values ecology)  
- **Storm Spire** (mountain clans, industrious, values metalwork)  
- **Amber Concord** (desert traders, values wealth and contracts)  
- **Mire Covenant** (swamp mystics, values rituals and rare herbs)  
- **Coastal Compact** (sailors & fishers, values navigation goods)  
- **Ember Dominion** (volcanic forges, values fuel and ore)  
- **Frostheim Pact** (tundra hunters, values furs and survival kits)  
- **Underkin Syndicate** (cave dwellers, values crystals and secrecy)

Each nation defines:
- **Alignment vector:** Order–Chaos, Nature–Industry, Mercy–Severity.  
- **Economic tags:** Wants/Exports (e.g., Verdant wants *planks < stone < metal*; penalizes deforestation).  
- **Diplomacy posture:** Guarded/Neutral/Trading/Zealous; initial stance per biome proximity.  
- **Unit roster (non‑LLM mobs):** see §7.  
- **Territorial rules:** Trespass warnings, taxes, hunting limits.

### Reputation & Respect
- **Reputation** (per faction): actions & quests; changes **prices**, **access**, **security response**.  
- **Respect** (personal): gear tier, feats (boss kills, build marvels), and **companion archetype**. Certain factions revere certain companions.  
- **Fear** (derived): affects enemy **morale** & **engagement distance**.  
- **Diplomacy thresholds:** Hostile < Wary < Neutral < Friendly < Allied; numeric bands (e.g., −100..+100).

---

## 4) Resources & Items (unchanged + hooks)
**Mineable:** Stone, Dirt, Sand, Clay, **Iron**, **Copper**, **Coal**, **Sulfur**, **Quartz**, **Rare Crystal**.  
**Crafting materials:** Wood Plank, Fiber Rope, Stone Brick, ingots, Glass, Ceramic, Nails/Screws, Simple Circuits.  
**Furniture/Machines:** Furnace, Workbench, Machining Bench, Fabricator, Chests, Beds, Lamps, Doors/Windows.  
**Hooks for RPG:** faction contracts (letters, seals), trade permits, bounty marks.

---

## 5) Building (Rust‑style)
- **Grid:** 1×1 square & ½‑square triangle; floors every 3m.  
- **Pieces:** Foundation, Floor, Wall (variants), Doorframe/Door, Windows, Stairs, Ramps, Roof (square/triangle), Pillars/Beams.  
- **Stability:** reach limits; adjacency support; tiers (Wood → Stone → Metal).  
- **Blueprints:** players draft blueprints; **companion builder** can place from a queue if materials exist.

---

## 6) Companions (LLM‑powered; one per player)

### 6.1 Roles & Skills
- **Miner** (prioritizes ore), **Hauler**, **Builder**, **Scout**, **Quartermaster** (craft/stock), **Diplomat** (advises in negotiations).  
- Players can toggled **Personality Packs** (steady, cheeky, cautious, ambitious), which bias plans & voice.

### 6.2 Input/Output
- **Voice**: local VAD (voice activity detection) → STT → LLM; TTS response with lip‑sync emote.  
- **Text**: chat wheel + free text; subtitles for all LLM replies.

### 6.3 Knowledge & Memory
- **Short‑term**: rolling window from recent chat and events.  
- **Long‑term**: daily **summaries** (world seed, base location, allies, enemies, trade routes, POIs).  
- **Retrieval**: a small vector index of “World Facts” (biome, factions nearby, stock levels). Stored per‑save.  
- **Privacy**: per‑player memory (not shared unless opted‑in to the **Task Board**).

### 6.4 Tool‑Use API (function calling)
The companion never acts *directly*; it proposes tool calls:

```jsonc
{
  "tool": "plan_tasks",
  "args": {
    "objective": "Prepare for desert crossing",
    "constraints": ["30 real-time minutes", "no metal tools"],
    "deliverables": ["10 waterskins", "sand-proof boots", "2 shelters"]
  }
}
```
Available tools (non‑exhaustive):
- `query_world({bbox|radius})` → POIs, ore, mobs, factions, biome stats  
- `set_waypoint({position, label})`  
- `queue_edit({center, radius, delta})`  // dig/fill sphere (delegates to VoxelWorld)  
- `craft({recipe_id, qty})`  
- `place_blueprint({id, origin, rotation})`  
- `assign_companion_role({role, params})`  
- `negotiate({faction_id, proposal})`  // returns expected outcomes & constraints  
- `explain({topic})`  // generate user‑facing summary from World Facts

**Guardrails:** The game validates permissions, stock, and safety. If a tool call is invalid, return a structured error the LLM must resolve (“Need iron ingots x3”).

### 6.5 Backend Choices
- **Local:** `llama.cpp` / `Ollama` / GPU‑accel models; low latency; offline; limited context.  
- **Cloud:** provider‑agnostic via HTTP; higher accuracy; costs & network dependency.  
- **Selection:** per‑player toggle; **hybrid** (local intent classification, cloud for heavy planning).  
- **Rate limits:** tokens/minute caps; **backpressure** UI when saturated.  
- **Timeouts:** non‑blocking UX; fall back to rule‑based actions if LLM stalls.

### 6.6 Safety & Moderation
- System prompts enforce **polite, non‑toxic**, **no spoilers**, **no disallowed content**, and **non‑authoritative** tone.  
- **No hard authority**: LLM **advises and plans**; all world‑changing actions go through tool validators.

---

## 7) Generative Mobs (Classical AI; HOMM‑inspired, not LLM)

### 7.1 Design Goals
- Distinct **faction rosters** per biome/nation; scalable and deterministic.  
- Behaviors: patrol, hunt, herd, guard, flee, **negotiate** (scripted offers), **retaliate**, **escort**, **caravan**.

### 7.2 Creature Template (data-driven)
```yaml
id: "frost_wolf"
tier: 2              # 1–7 scale (HOMM‑like tiers; higher = rarer, deadlier)
biomes: ["tundra", "mountain"]
factions: ["Frostheim Pact"]
roles: ["hunter", "pack"]
stats: { hp: 60, atk: 12, def: 6, speed: 6 }
abilities: ["howl_fear", "ice_resist"]
morale: { base: 0, pack_bonus: +2, fire_penalty: -2 }
ai:
  archetype: "pack_predator"
  behaviors:
    - { when: "see_weak_target", do: "ambush" }
    - { when: "outnumbered", do: "flee" }
  diplomacy: { negotiate_threshold: -30 }  # unlikely to parley
loot: ["fur", "meat"]
```
**Generation:** spawn tables per biome draw from tier‑weighted lists; variants (elite/juvenile) via stat modifiers & palette swaps.

### 7.3 AI Approach
- **Utility AI** for continuous choices (hunt vs. rest vs. regroup).  
- **GOAP** for patrol/escort/raid sequences.  
- **State machines** for combat micro (attack, retreat, kite).  
- **Morale** influences flee/charge (fear & reputation interact).

### 7.4 Diplomacy (with classical AI)
- Most mobs **don’t speak**; faction **agents** (guards, traders, envoys) do.  
- Negotiation model: **issue → stance → offer/counter → outcome**. Offers: tolls, bounties, safe passage, protection, resource rights.  
- Companion (LLM) **advises** you during talks; the NPC logic is **not LLM**.

---

## 8) Diplomacy, Reputation, and Consequences

- **Standing (−100..+100)** per faction; new players start near 0.  
- **Respect** scales with your build feats, gear tier, boss kills, and chosen **companion role**.  
- **Visibility:** actions propagate along **trade routes** and **hearsay radius** with decay over time.  
- **Prices & Access:** discounts at ≥+30; rare goods at ≥+60; bounties lifted at ≥+50.  
- **Law & Response:** trespass → warning → fines → confiscation → raid.  
- **Quest hooks:** escort caravan, hunt poachers, deliver ore quota, build a bridge blueprint, cleanse lair.

---

## 9) Voice & UX

- **Push‑to‑talk** or hotword; VAD trims silence.  
- **LLM subtitles** + **dialog wheel** with suggested intents (“Ask for discount”, “Request escort”).  
- **Evidence tags**: the companion quotes sources: *“We have 6 iron ingots in chest A and copper 80m west.”* (links to chest/POI).  
- **Status pulses** when LLM is planning; UI remains interactive; cancelable.

---

## 10) Technical Design Additions

### 10.1 LLM Bridge (GDExtension + HTTP)
```
/llm
  LLMProvider.gd        # interface
  LLM_Local.gd          # llama.cpp/ollama bindings
  LLM_Cloud.gd          # HTTP provider
  ToolBus.gd            # validates & executes tool calls
  Memory.gd             # summaries, vector store (small)
  Speech.gd             # STT/TTS adapters
```
- **ToolBus** is the only path to world mutation.  
- **Memory** builds daily/world summaries + embeds POIs/inventory facts for retrieval.  
- **Providers** are swappable at runtime per player.

### 10.2 Tool Call Schema (selected)
```ts
type Tool =
 | { kind: "query_world"; args: { radius?: float; bbox?: AABB; filters?: string[] } }
 | { kind: "plan_tasks";  args: { objective: string; constraints?: string[]; deliverables?: string[] } }
 | { kind: "craft";       args: { recipe_id: string; qty: int } }
 | { kind: "place_blueprint"; args: { id: string; origin: Vec3; rot_y_deg: float } }
 | { kind: "queue_edit";  args: { center: Vec3; radius: float; delta: float } }
 | { kind: "negotiate";   args: { faction_id: string; proposal: NegotiationOffer } };
```
Return values always include `{ ok: bool, reason?: string, evidence?: any }`.

### 10.3 Multiplayer Topology
- **Server authoritative** for voxel edits, placements, diplomacy outcomes.  
- **Per‑player LLM** runs **client‑side** by default (privacy & cost) but can be **proxied** via server microservice (opt‑in).  
- **Task Board:** replicated entity where companions post/claim subtasks so they coordinate without chatting directly.  
- **Voice:** player microphones are local only; no voice relay unless using Discord/Steam voice (future).

### 10.4 Data & Persistence
- **Companion memory** stored per player as compact JSON summaries + small embeddings.  
- **Diplomacy ledger**: time‑stamped entries change reputation; decay tick daily.  
- **Spawn seeds** pinned to chunk coordinates for deterministic mobs.

### 10.5 Companion Coordination
- **Blackboard (Task Board):** `projects/{id}/tasks/{id}` with status (`open/claimed/done/blocked`).  
- Tools: `post_task`, `claim_task`, `update_task`.  
- Example: Player A plans “Fortify Outpost Alpha”; Builder posts pieces; Miner posts ore requirements; Hauler claims deliveries.

### 10.6 Performance
- LLM calls are **non‑blocking**. Actions proceed once ToolBus validates.  
- Throttle planning frequency; coalesce repeated intents; cache explanations.  
- Mobs use classical AI only → stable perf regardless of LLM load.

### 10.7 Security & Abuse Prevention
- **Sandbox tools** (no arbitrary file/network).  
- **Rate limits** per player; server rejects oversized prompts.  
- **Cloud redaction**: strip player handles/PII from prompt; optional offline‑only mode.

---

## 11) Steam / Discord / Workshop (updates)

**Steam**
- Keep achievements for LLM milestones (first negotiated truce, first blueprint auto‑build).  
- Cloud saves now include **companion memory** + diplomacy ledger.  
- Workshop adds **Prompt/Personality Packs** (JSON), **Blueprint packs**, **Faction paint jobs** (cosmetics).

**Discord**
- Rich Presence includes **biome**, **faction stance**, **companion role**; future “Ask‑to‑Join” aligns with co‑op.

---

## 12) Balancing Notes
- LLM must **not trivialize** gameplay: tool calls consume real resources; planning costs time.  
- Gains from diplomacy should **compete** with combat gains; both viable.  
- Respect increases **aggro radius reductions** and **price breaks**; fear reduces trash mob persistence.

---

## 13) Backlog (added/updated for Copilot/Agent)

**EPIC: LLM Companion**
- [ ] Implement `LLMProvider` interface; `LLM_Local` (llama.cpp) and `LLM_Cloud` (HTTP).  
- [ ] `ToolBus` with validators for `query_world`, `queue_edit`, `craft`, `place_blueprint`, `negotiate`.  
- [ ] Memory: rolling window + daily summaries + vector search (tiny).  
- [ ] Voice: VAD + STT + TTS adapters; subtitle UI.  
- [ ] Per‑player settings UI; token/rate meter; offline fallback mode.  
- [ ] Guardrails & system prompts; error taxonomy.

**EPIC: RPG Factions**
- [ ] Faction data (alignments, wants/exports, unit rosters).  
- [ ] Reputation/Respect/Fear systems + UI.  
- [ ] Outposts/caravans; basic negotiation loop (offers, tolls, quests).  
- [ ] Diplomacy propagation (routes, decay).

**EPIC: Generative Mobs**
- [ ] Biome spawn tables with tiered species templates.  
- [ ] Utility AI + GOAP behaviors; morale system; elite variants.  
- [ ] Encounter scripts (ambush/escort/raid/trade).

**EPIC: Building & Crafting**
- [ ] Blueprint queue & Builder companion tool calls.  
- [ ] Batch crafting via Quartermaster role.  
- [ ] Furniture tiers affect respect (aesthetics bonus).

**EPIC: Multiplayer**
- [ ] Server‑auth edits & diplomacy; Task Board replication.  
- [ ] Client‑side LLM with optional server proxy.  
- [ ] Cross‑companion coordination via task claims.

**EPIC: QA & Perf**
- [ ] LLM latency harness; fake provider for deterministic tests.  
- [ ] Load tests for 4 players + 4 companions + ~80 mobs.  
- [ ] Autosave including memory/diplomacy; cloud sync.

---

## 14) Example Flows

### 14.1 Voice Command → Plan → Action
1. Player: “We need a shelter before night in the desert.”  
2. Companion (local LLM): `plan_tasks(objective="build desert shelter", constraints=["15m"])`  
3. ToolBus validates plan → produces tasks: gather **Wood Plank x24**, **Nails x16**, place **Foundations/Floors/Walls/Roof**.  
4. Companion posts tasks to **Task Board**; claims **place_blueprint**; asks player to craft nails if short.  
5. Result: shelter built; reputation with **Amber Concord** +2 for lawful encampment.

### 14.2 Diplomacy
1. Player meets a **Storm Spire** patrol at mountain pass.  
2. Patrol AI (classical) offers toll for safe passage based on current **reputation** and **respect**.  
3. Player asks companion to advise; companion estimates acceptance odds on counter‑offer.  
4. Outcome saved to **diplomacy ledger** and propagates along route.

---

## 15) Data Tables (delta)

### 15.1 New Items
- **Waterskin**, **Trade Seal**, **Faction Banner**, **Permit (Hunting/Logging/Mining)**.

### 15.2 Negotiation Offers (schema)
```json
{
  "type": "toll",
  "ask": {"currency": "ingot_iron", "qty": 6},
  "duration": "24h",
  "benefit": ["safe_passage:mountain_pass_3"]
}
```

---

## 16) Definition of Done (v0.2 MVP)
- 4 biomes playable; 4 nations seeded; tiered spawn tables working.  
- LLM companion online (local provider) with `query_world`, `plan_tasks`, `craft`, `place_blueprint`.  
- Voice I/O operational; subtitles + cancel.  
- Diplomacy loop with reputation effects; at least one caravan + one patrol type.  
- Co‑op 2–4 players; **one companion per player** coordinating via Task Board.  
- Steam Cloud Save includes companion memory & diplomacy ledger; Discord presence updated.

---

## 17) Legal & Ethics
- Player chooses local/cloud LLM; cloud calls are **opt‑in** with clear disclosure.  
- Strip PII from prompts; store only hashed identifiers in memory.  
- Non‑LLM NPCs avoid harmful/lurid content; tone controls on companion packs.

---

## 18) Appendix — Prompts & System Messages (sketch)

**System Prompt (Companion):** _You are a field engineer companion in a survival sandbox. You must only change the world by proposing tool calls… Prefer concise plans, cite evidence from World Facts, and wait for validation. If a tool call fails, repair the plan. Avoid unsafe actions._

**Negotiation Advisor Prompt:** _Given faction stance and ledger entries, outline 2 offers and 1 fallback that maximize expected gain under current constraints. Never commit; always return structured proposals._

---

**End of Spec v0.2**
