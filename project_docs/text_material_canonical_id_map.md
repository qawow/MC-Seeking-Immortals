- `recipe_condensation` maps to `seeking_immortals:alchemy_formula_essence_condensing_pill_jade`; it is surfaced through the Cangming Isle contribution hall as the active Chaotic Sea contribution equivalent.
# Text-material canonical id map

This document records the first cross-category id decisions for importing `文本材料/data` into the mod.
The shipped source of truth is `src/main/resources/data/seeking_immortals/reference/text_material_id_map.json`.

## Scope

- The map is reference-only. It is not a runtime registry loader and does not create items, blocks, shops, techniques, or packets.
- Implemented item entries must point to registered `seeking_immortals:*` ids with shipped item models.
- Implemented realm entries must point to current `Realm` enum names such as `FOUNDATION_ESTABLISHMENT`, not loose design aliases such as `FOUNDATION`.
- Deferred entries are kept so future imports can avoid accidentally inventing duplicate ids.

## First decisions

Currency:
- Generic low currency ids `low_spirit_stone`, `spirit_stone`, and `spirit_stone_low` currently map to `seeking_immortals:metal_spirit_stone`.
- `mid_spirit_stone` and `spirit_stone_mid` map to `seeking_immortals:metal_spirit_stone_mid`.
- `high_spirit_stone` maps to `seeking_immortals:metal_spirit_stone_high`.
- `top_spirit_stone` maps to `seeking_immortals:metal_spirit_stone_superior`.
- `spirit_stone_shard` maps to `seeking_immortals:spirit_stone_shard` and is stocked by `outer_sea_public_stall`; reverse exchange and fragment-specific UI remain deferred.
- `alliance_merit_token` and Tianyuan `border_merit_token` map to `seeking_immortals:alliance_merit_token` as the current physical item carrier; server-authoritative merit accounts, reward sources, and exchange services remain deferred.
- `yin_stone` and war contribution are deferred until their item/account systems exist.

Access and permit items:
- `diyuan_permit` and `diyuan_access_token` map to `seeking_immortals:diyuan_permit` as `implemented`.
- `wind_feather_raft_ticket` maps to `seeking_immortals:wind_feather_raft_ticket` as `implemented`.
- `pressure_resist_charm` maps to `seeking_immortals:pressure_resist_charm` as an implemented item carrier.
- The 0.1.236 item slice registers the physical permit, creative-tab entry, zh_cn/en_us names, paper-texture model, and reference-map aliases.
- The 0.1.237 guide sync updates Tianyuan City Patchouli text so the guide no longer says the physical permit item is missing.
- The 0.1.238/0.1.239 ticket work registers the physical Wind Feather Raft ticket and updates Tianyuan Patchouli text so the guide no longer says the travel ticket item is missing.
- The 0.1.241-0.1.243 Wind Feather route work consumes that ticket server-side for non-portal worldpack travel from `great_jin_central` or `dajin` to `tianyuan`; other Spirit-Realm non-portal travel still requires the 3x3 Spirit Gathering Array portal path.
- The 0.1.245 Diyuan entry work makes `seeking_immortals:diyuan_permit` the consumed `ticket_item` for worldpack secret realm `diyuan` through the existing secret-realm ticket path.
- The 0.1.246 Diyuan no-fly runtime work makes active secret realms tagged `no_fly`, including `diyuan`, suppress managed flying artifact, Qi flying, and Foundation flying sources through existing `FlyingAuthority` state.
- The 0.1.248 current-tree verification confirms the no-fly runtime on the latest shared tree without adding packets, loaders, items, or new worldpack data.
- Tianyuan merit exchange, permit/ticket purchase sources, Diyuan quest checks, pressure debuff damage/slowdown, pressure-resist pill/charm mitigation, broader route policies, NPCs, quests, dedicated route presentation, and dedicated art remain deferred.

## 0.1.230 Star Palace tax receipt shop-stock decision

Quest/economy items:
- `star_palace_tax_receipt` maps to `seeking_immortals:star_palace_tax_receipt` as `implemented`.
- The registered tax receipt is now surfaced through `chaotic_sea_island_general` for 5 low-grade metal spirit stones with stock 20.
- Right-click use now records QuestProgress flag `star_palace_island_trade_tax_paid` in the existing PlayerCultivation NBT path.
- `chaotic_sea_island_general` and `outer_sea_public_stall` now read that flag through the existing market cost modifier: unpaid players pay the 10% Star Palace island-market tax on taxable goods, while players who used the receipt see base prices. The receipt item itself is not taxed.
- Ferry/tax quests, NPC placement, reputation consequences, visible UI/status feedback, and exact pearl-economy rewards remain deferred.

Pills:
- `bigu_pill` maps to `seeking_immortals:fasting_pill_low`.
- `foundation_pill` maps to `seeking_immortals:foundation_building_pill_low`.
- `condensation_pill` maps to `seeking_immortals:essence_condensing_pill`.
- `spirit_condense_pill` maps to `seeking_immortals:spirit_gathering_pill`.
- `calm_spirit_pill` maps to `seeking_immortals:clear_void_pill`; the current contribution-shop import surfaces it in Yanyue data.
- `pressure_resist_pill` maps to `seeking_immortals:pressure_resist_pill` and now has a shipped alchemy recipe from 2x `seeking_immortals:diyuan_pressure_moss`; pressure mitigation, formula acquisition, and realm-gated use behavior remain deferred.
- `spirit_realm_condense_pill` maps to `seeking_immortals:spirit_realm_condense_pill`, has a shipped alchemy recipe from 1x `seeking_immortals:fengyuan_clan_ginseng`, has first realm-gated right-click behavior through `CatalogPillItem`, and now has first sect-secret formula acquisition through `seeking_immortals:alchemy_formula_spirit_realm_condense_pill_sect` in `danxia_valley_contribution_hall`; Spirit Fengyuan-specific acquisition and gathering placement remain deferred.
- High-system pills such as `bu_tian_pill`, `nascent_soul_pill`, and `thunder_soothing_pill` remain deferred.

Realms:
- `FOUNDATION` maps to `FOUNDATION_ESTABLISHMENT`.
- `DEITY_TRANSFORMATION`, `SPIRIT_SEVERANCE`, and `SPIRIT_SEVERING` map to `SOUL_TRANSFORMATION`.
- `BODY_INTEGRATION` maps to `UNITY`.
- `GREAT_VEHICLE` maps to `MAHAYANA`.
- `TRIBULATION_LAND` maps to `TRIBULATION`.

## Import rule

Before any larger shop, talisman, spirit-beast, artifact, puppet, formation, or technique import:

1. Add every source id to the canonical map or an approved category-specific map.
2. Mark the entry `implemented` only if the target registry/resource exists and is covered by tests.
3. Keep ambiguous, missing, or future-system ids as `deferred` or `blocked`.
4. Do not bulk-import `文本材料/data/techniques/*.json` until technique ids have explicit `SkillEffectRegistry` coverage.

Related existing map: `project_docs/alchemy_pill_material_mapping.md`.

## 0.1.186 Huangfeng/Danxia and Luoyun herb-garden seed-pack decision

Contribution materials:
- `sect_herb_garden_seed_pack` maps to `seeking_immortals:spirit_grass` as `implemented_partial`.
- The current Luoyun contribution hall surfaces it as 8 generic Spirit Grass for 60 sect contribution.
- The Huangfeng/Danxia data surface the same source id through Qinglan and Danxia contribution halls as 8 generic Spirit Grass for 50 sect contribution.
- This represents usable low-tier herb stock without introducing a new seed registry item. Exact seed items, planting and growth rules, garden plots, source-accurate herb composition, rank gates, monthly limits, and per-player contribution limits remain deferred.

## 0.1.114 talisman decisions

Talismans:
- `fire_burst_talisman` maps to `seeking_immortals:fire_talisman` as `implemented_partial`; the current item covers the low-tier fire attack role, while area-burst tuning remains future work.
- `body_guard_talisman` maps to `seeking_immortals:armor_talisman` as `implemented_partial`; the current item covers the low-tier protection role, while exact shield-value tuning remains future work.
- Talisman paper, inks, crafting grades, mid/high talismans, teleport talismans, beast-contract talismans, yin/ghost talismans, and tribulation talismans remain deferred until matching systems or registered resources exist.

## 0.1.115 herb decisions

Materials:
- `yellow_essence` maps to `seeking_immortals:spirit_grass`; the current material system keeps it as a low-tier generic spirit herb without age-tier identity.
- `ginseng_spirit` maps to `seeking_immortals:immortal_ginseng`; the current material system keeps one registered ginseng item without 100-year tiering.
- `fire_sparrow_fruit` remains mapped to `seeking_immortals:phoenix_feather_flower` from the first canonical map pass and is now surfaced through the low-tier market.

## 0.1.116 low-pill market decisions

Pills:
- `spirit_recovery_pill` already maps to `seeking_immortals:qi_recovery_pill` and is now surfaced through the low-tier market.
- `cultivate_speed_pill` already maps to `seeking_immortals:cultivation_pill` and is now surfaced through the low-tier market.
- Mid/high recovery pills, controlled Foundation/Core breakthrough pills, and future body-tempering pills remain deferred from the market until their source, gate, and balance rules are explicit.

## 0.1.117 spirit-mushroom market decisions

Materials:
- `spirit_mushroom` already maps to `seeking_immortals:cloud_mushroom` and is now surfaced through the low-tier market.
- The current material system keeps one generic mushroom material; exact mushroom species, age tiers, regional scarcity, and herb-gathering sources remain deferred.

## 0.1.118 beast-core fragment market decisions

Materials:
- `demon_core_fragment` maps to `seeking_immortals:beast_core` as `implemented_partial`; the current material set has one generic beast-core item and does not distinguish fragments, mid-tier cores, or beast source tiers yet.
- The mapped entry is now surfaced through the low-tier market. Exact fragment/core tiering, beast drops, and recipe-specific consumption remain deferred.

## 0.1.118 Qinglan contribution hall decisions

Recipe unlocks:
- `recipe_spirit_condense` maps to `seeking_immortals:alchemy_formula_spirit_gathering_pill_paper`.
- `recipe_bigu` maps to `seeking_immortals:alchemy_formula_fasting_pill_paper`.
- `recipe_calm_spirit` maps to `seeking_immortals:alchemy_formula_clear_void_pill_paper`.

Contribution materials and low supplies:
- `spirit_herb_bundle` is surfaced as 8x `seeking_immortals:spirit_grass` in the current Qinglan contribution hall.
- `spirit_recovery_pill` and `body_guard_talisman` reuse their existing canonical targets, `seeking_immortals:qi_recovery_pill` and `seeking_immortals:armor_talisman`.
- Rank, monthly limit, realm gate, reputation gate, and exact herb-pack composition from the text-material source remain deferred until the shop schema and sect progression can express those constraints per player.

## 0.1.120 Qinglan alchemy equipment decision

Equipment:
- `alchemy_furnace_g2` maps to `seeking_immortals:alchemy_furnace_tier_2`.
- The registered tier-2 alchemy furnace is now surfaced through `qinglan_contribution_hall` for 350 sect contribution.
- Higher furnace grades, realm/rank gates, monthly limits, and sect-specific alchemy privileges remain deferred until the shop schema can express those constraints per player.

## 0.1.123 Chaotic Sea material market decisions

Materials:
- `jiao_scale` maps to `seeking_immortals:dragon_scale` as `implemented_partial`; the current material covers the beast-scale armor/refinement role, while sea-jiao source drops and water-artifact tuning remain deferred.
- `deep_sea_cold_iron` maps to `seeking_immortals:spirit_iron` as `implemented_partial`; the current material covers the forging-metal role, while exact deep-sea cold/water metal tiering remains deferred.
- Both mapped entries are now surfaced through `chaotic_sea_island_general` using the existing low-grade metal-spirit-stone item currency.

## 0.1.124 Tiannan refinement forge decisions

Materials:
- `low_spirit_iron` maps to `seeking_immortals:spirit_iron` as `implemented_partial`; the current material covers the low-grade forging metal role, while exact ingot tier identity remains deferred.
- `spirit_iron_ingot_mid` maps to `seeking_immortals:spirit_iron` as `implemented_partial`; the Tiannan refinement forge currently sells it as two Spirit Iron items to represent a denser mid-grade bundle without adding a new registry item.
- Both mapped entries are now surfaced through `tiannan_refinement_forge` using the existing low-grade metal-spirit-stone item currency.

## 0.1.125 Tiannan refinement forge version reconciliation

- The 0.1.124 Tiannan refinement forge decisions above are finalized under `mod_version=0.1.125` because the local ai-preflight successful-build fingerprint had already recorded `0.1.124`.
- Canonical mappings and shipped shop content are unchanged from the 0.1.124 draft.

## 0.1.126 Tiannan refinement forge final verification

- The Tiannan refinement forge mappings and shop content were build-verified under `mod_version=0.1.126`.
- This supersedes the temporary 0.1.124/0.1.125 version notes for final reporting; canonical mappings are unchanged.

Shop:
- `tiannan_refinement_forge` maps to the shipped `tiannan_refinement_forge` market shop as `implemented_partial`; unregistered refinement manuals, quench oil, forge hammers, and injection tools remain deferred.

## 0.1.124 Tiannan refinement forge decisions

Materials:
- `low_spirit_iron` maps to `seeking_immortals:spirit_iron` as `implemented_partial`; the current material covers the low-tier forging-metal role, while exact low-grade ingot itemization remains deferred.
- `spirit_iron_ingot_mid` maps to `seeking_immortals:spirit_iron` as `implemented_partial`; the current material is granted in a count of 2 to approximate a mid-grade ingot bundle until distinct tiered ingots exist.
- Both mapped entries are now surfaced through `tiannan_refinement_forge` using the existing low-grade metal-spirit-stone item currency.

## 0.1.127 Huanglong pill market decision

Pills:
- huanglong_pill maps to seeking_immortals:cultivation_pill as implemented_partial; the current item covers the low-tier Qi Refining cultivation-gain role, while exact Huanglong Pill formula, independent effect tuning, and source-specific balance remain deferred.
- The mapped entry is now surfaced through market_herbal_stall using the existing low-grade metal-spirit-stone item currency.

## 0.1.146 Heqi pill market decision

Pills:
- `heqi_pill` maps to `seeking_immortals:cultivation_pill` as implemented_partial; the current item covers the low-tier Qi Refining cultivation-gain role, while exact Heqi Pill dual-cultivation compatibility, formula identity, and independent effect tuning remain deferred.
- The mapped entry is now surfaced through `market_herbal_stall` using the existing low-grade metal-spirit-stone item currency.

## 0.1.136 Luoyun alchemy equipment decision

Equipment:
- `alchemy_furnace_g3` maps to `seeking_immortals:alchemy_furnace_tier_3` as `implemented_partial`; the current registered block item covers the tier-3 furnace role, while Luoyun sect membership, rank gates, monthly limits, and alchemy privilege rules remain deferred.
- The mapped entry is now surfaced through the shipped `luoyun_contribution_hall` data shop for 800 sect contribution.

## 0.1.140 Chaotic Sea Ningshen Pill market decision

Pills:
- `ningshen_pill` maps to `seeking_immortals:calming_pill_low` as `implemented_partial`; the text-material effect is stabilizing the mind against inner demons, while the current registered Calming Pill lowers qi-deviation risk.
- The mapped entry is now surfaced through `chaotic_sea_island_general` using the existing low-grade metal-spirit-stone item currency.
- Exact Ningshen Pill itemization, recipe unlock, Foundation realm gating, and inner-demon-specific tuning remain deferred.

## 0.1.141 Yanyue calm-spirit contribution decision

Pills:
- `calm_spirit_pill` is surfaced through `yanyue_contribution_hall` as `seeking_immortals:clear_void_pill`.
- Illusion talisman scrolls, foundation-pill stock, heqi/ningshen pill lines, Yanyue access routing, rank gates, and monthly limits remain deferred.

## 0.1.142 Yanyue implemented pill-stock decision

Pills:
- `foundation_pill` is now surfaced through `yanyue_contribution_hall` as `seeking_immortals:foundation_building_pill_low` for 1400 sect contribution.
- `ningshen_pill` is now surfaced through `yanyue_contribution_hall` as `seeking_immortals:calming_pill_low` for 70 sect contribution, matching the existing partial Ningshen-to-Calming mapping.
- `calm_spirit_pill` remains surfaced as `seeking_immortals:clear_void_pill` for 60 sect contribution.
- `illusion_talisman_scroll` remains deferred because no exact registered scroll target is currently available.

## 0.1.143 Cangming Yanghun formula decision

Recipe unlocks:
- `recipe_yanghun` maps to `seeking_immortals:alchemy_formula_soul_gathering_pill_jade` as `implemented_partial`; the registered Soul Gathering Pill jade formula is the closest current Yanghun/Nourish Soul formula equivalent.
- The mapped entry is now surfaced through `cangming_isle_contribution_hall` for 600 sect contribution.
- Exact Yanghun Pill formula identity, Star Palace reputation gates, monthly limits, and per-player contribution-shop restrictions remain deferred.

## 0.1.145 Chaotic Sea Yanghun Pill market decision

Pills:
- `yanghun_pill` maps to `seeking_immortals:soul_gathering_pill` as `implemented_partial`; the text-material role is minor soul-injury recovery, while the registered Soul Gathering Pill is the closest current soul-recovery equivalent.
- The mapped entry is now surfaced through `chaotic_sea_island_general` using the existing low-grade metal-spirit-stone item currency.
- Exact Yanghun Pill itemization, soul-injury rules, recipe identity, market gates, and stock-limit semantics remain deferred.

## 0.1.144 build-hygiene reconciliation

- Canonical mapping decisions are unchanged from 0.1.143.
- The final verified workspace version is `0.1.144` because `build.gradle` changed to add an explicit `compileTestJava` main-output classpath fallback before the final build.

## 0.1.147 Danxia/Huangfeng formula decision

Recipe unlocks:
- `recipe_huanglong` maps to `seeking_immortals:alchemy_formula_cultivation_pill_paper` as `implemented_partial`; this follows the existing `huanglong_pill -> seeking_immortals:cultivation_pill` compression and surfaces the text-material Huanglong formula through Danxia Valley contribution data.
- `recipe_ningshen` maps to `seeking_immortals:alchemy_formula_calming_pill_jade` as `implemented_partial`; this follows the existing `ningshen_pill -> seeking_immortals:calming_pill_low` compression and surfaces the Ningshen formula through Danxia Valley contribution data.
- Exact Huanglong/Ningshen recipe identities, rank gates, per-player monthly limits, and source-accurate pill effects remain deferred.

## 0.1.149 Yanyue Heqi formula decision

Recipe unlocks:
- `recipe_heqi` maps to `seeking_immortals:alchemy_formula_cultivation_pill_paper` as `implemented_partial`; this follows the existing `heqi_pill -> seeking_immortals:cultivation_pill` compression and surfaces the text-material Heqi formula through Yanyue contribution data.
- Exact Heqi formula identity, dual-cultivation compatibility behavior, Yanyue rank gates, monthly/per-player limits, and source-accurate pill effects remain deferred.

## 0.1.151 Danxia Jiangchen formula decision

Recipe unlocks:
- `recipe_jiangchen` maps to `seeking_immortals:alchemy_formula_foundation_building_pill_paper` as `implemented_partial`; the text-material Jiangchen Pill is a weaker Foundation breakthrough aid, while the registered Foundation Building Pill paper formula is the closest current breakthrough-formula carrier.
- The mapped entry is now surfaced through `danxia_valley_contribution_hall` for 25 sect contribution.
- Exact Jiangchen Pill itemization, weaker breakthrough tuning, Tiannan market stock, rank gates, monthly/per-player limits, and source-accurate recipe behavior remain deferred.

## 0.1.152 Luoyun Spirit formula decision

Recipe unlocks:
- `recipe_luoyun_spirit` maps to `seeking_immortals:alchemy_formula_spirit_gathering_pill_paper` as `implemented_partial`; this follows the current `luoyun_spirit_pill -> seeking_immortals:spirit_gathering_pill` compression, while the exact Core Formation jade-slip formula remains deferred.
- The mapped entry is now surfaced through `luoyun_contribution_hall` for 400 sect contribution.
- Exact Luoyun Spirit Pill formula identity, jade-slip carrier, Core Formation/furnace/fire gates, source ingredients, monthly/per-player limits, and alchemy privilege rules remain deferred.

## 0.1.154 Jiangchen market and Danxia flying sword decisions

Pills:
- `jiangchen_pill` maps to `seeking_immortals:foundation_building_pill_low` as `implemented_partial`; the text-material pill is a weaker Foundation breakthrough aid, while the registered low-quality Foundation Building Pill is the closest current breakthrough-aid carrier.
- The mapped entry is now surfaced through `market_herbal_stall` for 120 low-grade metal spirit stones with stock 3.

Artifacts:
- `flying_sword_low` maps to `seeking_immortals:flying_sword_low` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, activation/flight support, and Qinglan/Danxia contribution stock, while the older `seeking_immortals:flying_sword` remains a compatibility carrier.
- `cloud_boots` maps to `seeking_immortals:cloud_boots` as `implemented_partial`; the exact P0 carrier exists with catalog tooltip metadata, while movement/equipment behavior and dedicated art remain deferred.
- `spirit_gathering_bead` maps to `seeking_immortals:spirit_gathering_bead` as `implemented_partial`; the exact P0 carrier exists with catalog tooltip metadata, while anti-illusion/equipment behavior and dedicated art remain deferred.
- `yellow_umbrella` maps to `seeking_immortals:yellow_umbrella` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while defensive activation behavior, refinement output, placement, and dedicated art remain deferred.
- `qingye_leaf_fan` maps to `seeking_immortals:qingye_leaf_fan` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while offense/equipment behavior, refinement output, placement, and dedicated art remain deferred.
- `storage_bracelet_low` maps to `seeking_immortals:storage_bracelet_low` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while storage inventory behavior, placement, and dedicated art remain deferred.
- `snake_pearl` maps to `seeking_immortals:snake_pearl` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while poison behavior, refinement output, placement, and dedicated art remain deferred.
- `flying_needle_set` maps to `seeking_immortals:flying_needle_set` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while needle attack behavior, refinement output, placement, and dedicated art remain deferred.
- `black_gold_shield` maps to `seeking_immortals:black_gold_shield` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while shield activation behavior, refinement output, placement, and dedicated art remain deferred.
- `bedrock_shield` maps to `seeking_immortals:bedrock_shield` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while shield activation behavior, refinement output, placement, and dedicated art remain deferred.
- `artifact_repair_kit` maps to `seeking_immortals:artifact_repair_kit` as `implemented_partial`; the exact P0 carrier exists as a stackable item, participates in the current artifact integrity repair runtime, and is now stocked in Qinglan/Danxia contribution halls, while repair recipes/workstation outputs and dedicated art remain deferred.
- `silver_giant_sword` maps to `seeking_immortals:silver_giant_sword` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while flying-sword/equipment behavior, refinement output, placement, and dedicated art remain deferred.
- `gold_demon_chain` maps to `seeking_immortals:gold_demon_chain` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while offense behavior, auction/refinement placement, and dedicated art remain deferred.
- `evil_illusion_mirror` maps to `seeking_immortals:evil_illusion_mirror` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while illusion behavior, refinement output, placement, and dedicated art remain deferred.
- `qingning_mirror` maps to `seeking_immortals:qingning_mirror` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while defensive mirror behavior, refinement output, placement, and dedicated art remain deferred.
- `gold_light_brick` maps to `seeking_immortals:gold_light_brick` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while talisman-treasure uses/durability, refinement output, placement, and dedicated art remain deferred.
- `beast_taming_whip` maps to `seeking_immortals:beast_taming_whip` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while beast-control behavior, refinement output, placement, and dedicated art remain deferred.
- `spirit_beast_bridle` maps to `seeking_immortals:spirit_beast_bridle` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while beast-control/equipment behavior, refinement output, placement, and dedicated art remain deferred.
- `wind_escape_sail` maps to `seeking_immortals:wind_escape_sail` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while the older `seeking_immortals:flying_artifact` remains the behavior/compatibility carrier until exact movement behavior is split.
- `moon_shadow_disk` maps to `seeking_immortals:moon_shadow_disk` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while offense behavior, refinement output, placement, and dedicated art remain deferred.
- `talisman_treasure_soul_charm` maps to `seeking_immortals:talisman_treasure_soul_charm` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while talisman-treasure uses/durability, placement, and dedicated art remain deferred.
- `void_palace_cold_jade_pendant` maps to `seeking_immortals:void_palace_cold_jade_pendant` as `implemented_partial`; the exact priority carrier exists with catalog tooltip metadata, while defensive/equipment behavior, realm-drop placement, and dedicated art remain deferred.
- The mapped entry is now surfaced through `danxia_valley_contribution_hall` for 180 sect contribution.
- Exact Jiangchen Pill itemization, weaker breakthrough tuning, Qi Refining-only purchase/use gates, recipe output identity, source-accurate market balance, exact low-tier flying-sword itemization, rank gates, and refining progression remain deferred.
