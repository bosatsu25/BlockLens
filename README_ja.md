# BlockLens

[English](README.md) | **日本語**

BlockLensは、Minecraft Java Edition向けの**クライアント専用ビジュアル検査MOD**です。

AMATERASリソースパックで得られていた視認性・状態把握の価値を、何千個ものstate別JSON/PNGをそのまま抱える方式ではなく、**共通の状態解釈 + 共有レンダリング + 最小限の固有アセット**として再構築します。

目的は「リソースパックをJARに詰めること」ではありません。**37機能のユーザー価値を維持しながら、保守性・設定・Minecraftバージョン差分・テスト可能性・性能評価・配布物サイズを改善すること**が目的です。

> **現在の状態:** M0・M1・M2は完了済みです。M3は共通Render Descriptor、M0準拠の254 target、procedural visual grammar、Minecraft 26.1.2 / 26.2両方のzero-scan baked-model pipelineまで実装しています。ただし、代表的な実描画のvisual parityと13機能同時ONのゲーム内検証が完了するまでは**M3完了とは扱いません**。現時点では37機能すべての表示パリティ完成を主張しません。

## BlockLensが解決したいこと

元のリソースパックは有用ですが、状態の組み合わせを大量の小さなJSON・モデル・PNGで表現しています。BlockLensでは、同じ意味をsemantic stateとして1回だけ表し、共通render policyへ渡します。

```mermaid
flowchart LR
    A[AMATERAS baseline\n4,535 ZIP entries] --> B[振る舞いを固定\n37 capability contract]
    B --> C[Semantic State Engine\nfacing / axis / half / shape / connections]
    C --> D[共有Visual Semantics\noverlay / outline / marker]
    D --> E[薄いMinecraft Adapter\n26.1.2 + 26.2]
    E --> F[BlockLens\nsmall client-only runtime]
```

```text
大量のstate別 JSON / PNG
          ↓
共通状態解釈 + 共通描画 + 最小限の固有アセット
```

## 対応環境

| 項目 | 現在仕様 |
| --- | --- |
| Minecraft | **26.1.2** / **26.2** |
| Loader | Fabric |
| Java | **25** |
| 動作側 | **Client only** |
| Server側MOD | 不要 |
| 製品仕様 | 両Minecraft版で共通 |
| version固有コード | Minecraft/Fabric adapterの薄い層だけ |

現在仕様に明示的な例外がない限り、片方のMinecraft版でしか動かない機能は完了扱いにしません。

## 37機能の構成

凍結したsource baselineでは、**37個の独立設定可能な機能**があります。

```mermaid
flowchart TB
    BL[BlockLens · 37 capabilities]
    BL --> D[Decoration / Orientation · 13]
    BL --> R[Resource Highlighting · 18]
    BL --> O[Outline / Visibility · 4]
    BL --> X[Other Visual Tweaks · 2]

    D --> D1[Anvil · Beehive · Campfire · Glazed Terracotta]
    D --> D2[Grindstone · Fence Gate · Froglight · Slabs]
    D --> D3[Stained Glass · Stairs · Trapdoor · Wood · Log]

    R --> R1[Obsidian · Ancient Debris]
    R --> R2[Diamond · Gold · Emerald · Coal]
    R --> R3[Iron · Copper · Lapis · Redstone]
    R2 --> R4[通常鉱石 + Deepslate版]
    R3 --> R5[通常鉱石 + Deepslate版]

    O --> O1[Blue Ice · Dead Coral · Powder Snow · Sculk Catalyst]
    X --> X1[Nether Tweaks · String Tweaks]
```

提供されたRPO presetではBlue Ice / Dead Coral / Powder Snow / Sculk Catalyst / String Tweaksの5個だけがONですが、これはあくまでpresetです。残り32機能もBlockLensの機能契約から外しません。

## Runtime Architecture

Minecraftのmapped API差分は端に閉じ込め、製品ロジックはcommonへ集約します。

```mermaid
flowchart TD
    MC[Minecraft BlockState\n26.1.2 / 26.2] --> VA[Version State Adapter]
    VA --> SS[Common SemanticState]
    SS --> FP[Feature Policy / Config]
    SS --> RD[Render Descriptor]
    FP --> RD
    RD --> VR[Version Render Adapter]
    VR --> SR[Shared Renderer Families]
    SR --> BASE[Vanilla / active resource-pack texture]
    SR --> EXTRA[BlockLens visual information]
```

### M2 Semantic State

M2では、BlockLensが必要とする情報だけをMinecraft非依存の形へ圧縮しています。

- horizontal facing
- axis
- top / bottom half
- stair shape
- mount face
- slab type
- north / east / south / west connections
- honey level
- open / lit / in-wall / powered / attached

Mapped `BlockState`はMinecraft version境界でこの共通表現へ変換し、製品仕様をversion別に複製しません。

## M3 Procedural Decoration Pipeline

M3ではAMATERASのtextureをBlockLensへコピーしません。固定したsource packはbehavior / visual evidenceとして扱い、Minecraft本体またはユーザーのactive resource packがbakeしたmodelに、BlockLens独自のprocedural cueを適用します。

```mermaid
flowchart LR
    A[M0 exact target catalog\n254 bindings] --> B[Raw-ID target index\nmodel-load時]
    B --> C[BlockState semantic adapter\nmodel-bake時]
    C --> D[Common Render Descriptor]
    D --> E[WrapperBlockStateModel]
    E --> F{対象機能がON?}
    F -- no --> G[元baked modelをそのままemit]
    F -- yes --> H[共通quad cueを適用]
    H --> I[base model + BlockLens追加情報]
```

重要な設計:

- M0で凍結した254 targetだけをexactに扱い、suffixで勝手に対象を増やさない
- 13機能のtoggleはすべて独立
- X / Y / Z軸はsource intentに基づく赤 / 緑 / 青の共通visual grammar
- Stained Glassはsourceの意味どおり**opaque / solid-layer behavior**として扱う
- semantic state解釈はmodel bake時に行い、毎frameは行わない
- whole-world scanなし、毎frame registry scanなし
- render hot pathのON/OFF判定はprimitive capability bitmask
- wrapperが担当するM3機能がすべてOFFなら元baked modelを直接emit
- resource reload時は新しくbakeされたactive-resource-pack modelを再度wrapし、AMATERASのbase textureを所有しない

M3の現在仕様: [`knowledge/current/m3-visual-semantics.md`](knowledge/current/m3-visual-semantics.md)

## 開発状況

| Milestone | 状態 | 内容 |
| --- | --- | --- |
| M0 — Source baseline freeze | ✅ 完了 | 37/37機能の根拠・machine-readable contract |
| M1 — Dual-version quality scaffold | ✅ 完了 | Java 25 / config / CI / GameTest / artifact audit / size・load baseline |
| M2 — Shared state engine | ✅ 完了 | semantic model / 両version adapter / real-client oracle / composable bounded lookup |
| M3 — 13 Decoration / Orientation | 🔧 実装中 | exact target / render policy / procedural quad grammar / 両version model wrapperまで実装、実描画パリティ検証が残る |
| M4–M7 | 予定 | outline / resource highlight / Nether Tweaks / 全機能相互作用 |
| M8 | 予定 | performance / load / size 最終hardening |
| M9 | 予定 | release readiness / public artifact audit |

正本の実装順序は [`knowledge/current/roadmap.md`](knowledge/current/roadmap.md) です。

## M1実測baseline

M1は描画実装を増やす前の意図的に小さい基盤です。

| Minecraft | Runtime JAR | BlockLens init | Client GameTest | Reproducible artifact |
| --- | ---: | ---: | --- | --- |
| 26.1.2 | **14,481 B** | 約 **7.513 ms** | PASS | PASS |
| 26.2 | **14,476 B** | 約 **2.684 ms** | PASS | PASS |

これは**M1時点のbaseline**であり、全機能実装後も14 KiBのままになるという意味ではありません。

```text
Source resource-pack ZIP   2,366,865 B  ████████████████████████████████████████
M1 26.1.2 runtime JAR         14,481 B  ▏
M1 26.2 runtime JAR           14,476 B  ▏
Hard release maximum       1,183,432 B  ████████████████████
Stretch target               716,800 B  ████████████
```

各release artifactの目標:

- 必須: **< 1,183,433 bytes**
- stretch: **<= 716,800 bytes (700 KiB)**

ただし、容量より**機能パリティ・correctness・診断性**を優先します。

## Quality Graph

Minecraft連携変更は両versionの分岐を通過する必要があります。

```mermaid
flowchart TD
    C[Common JUnit / contracts] --> Q[JaCoCo + PIT selected policy gates]
    Q --> B1[26.1.2 build]
    Q --> B2[26.2 build]
    B1 --> G1[26.1.2 Client GameTest]
    B2 --> G2[26.2 Client GameTest]
    G1 --> A1[JAR audit + size + reproducibility]
    G2 --> A2[JAR audit + size + reproducibility]
    A1 --> P[Cross-version parity decision]
    A2 --> P
    P --> GREEN([CI GREEN])
```

現在はJUnit 5、37機能exact contract、cross-version semantic/config parity、JaCoCo、PIT、両Minecraft版Client GameTest、M3 exact-target/model-pipeline oracle、config persistence、runtime JAR privacy/residue audit、clean rebuild SHA-256 reproducibility、JAR byte budget、startup/load structural contractを自動化しています。

## Engineering Graph Loop

BlockLensでは、実装しただけではDONEになりません。

```mermaid
flowchart LR
    D[DISCOVER] --> P[PLAN]
    P --> I[IMPLEMENT]
    I --> V[VERIFY]
    V --> R[SELF REVIEW]
    R --> K[DOCUMENT]
    K --> PR[PR]
    PR --> CI[CI]
    CI --> U[ISSUE UPDATE]
    U --> DONE([DONE])

    V -- fail --> X[DIAGNOSE]
    R -- defect --> X
    CI -- fail --> X
    X --> F[FIX]
    F --> V
```

失敗時は必ず **DIAGNOSE → FIX → VERIFY** に戻します。「修正したはず」でCIを飛ばしてDONEにはしません。

正式ルール・各nodeのexit condition・`DONE / BLOCKED / PARTIAL`は [`knowledge/current/engineering-loop.md`](knowledge/current/engineering-loop.md) を正本とします。

## Repository構成

```text
BlockLens/
├─ common/                  # 共通product/config/state/render policy + contracts
├─ versions/
│  ├─ mc26_1_2/            # 26.1.2用の薄いMinecraft/Fabric adapter
│  └─ mc26_2/              # 26.2用の薄いMinecraft/Fabric adapter
├─ gametest/                # 両version共通の実Client integration oracle
├─ gradle/                  # version module共通build rule
├─ knowledge/
│  ├─ index.md
│  └─ current/              # 現在仕様の正本
├─ AGENTS.md                # 開発規約
└─ README.md                # English README
```

## Build / Verify

Java 25が必要です。

```bash
./gradlew qualityGate
./gradlew ciGate
```

Minecraft/Fabric連携を変更した場合は、GitHub Actions上で**現在head SHAの26.1.2 / 26.2両方がGREEN**になるまで完了扱いにしません。

## Performance Rule

startupやrender hot pathで不要な処理を行わないことを設計ルールにしています。

- runtime classpath/reflection feature discoveryなし
- startup update/network checkなし
- telemetry初期化なし
- 通常起動時のlegacy RPO parseなし
- OFF機能のgeometry eager生成なし
- 毎frameのwhole-world / unbounded chunk scanなし
- M3 wrapperで毎frame registry scan / BlockState reinterpretationなし
- 変化していないgeometryの毎frame rebuildなし
- target lookupはraw-IDベースでbounded
- render enable判定はprimitive config mask
- reload / world transitionではBlockLensが所有するstateを明確に管理する

性能改善は推測で主張せず、startup/load・resource reload・runtime/frame・allocation/memory・JAR bytesを別々に測定します。

## Resource Pack / Shader Compatibility

可能な限りMinecraft本体またはユーザーが有効化しているresource packのmodel/textureをbaseとして保ち、BlockLensが必要な追加情報だけを適用します。

Shader/backend互換性は「Vanillaで動いた」だけでは対応扱いにしません。26.2 OpenGLは必須検証track、Vulkanは別のexperimental trackとして管理します。

## Non-goals

- 元リソースパックをそのままJARへ内包すること
- 容量目標のために37機能を削ること
- Minecraft versionごとに製品ロジックを丸ごと複製すること
- visual機能のためにserver-side BlockLensを必須にすること
- 無関係なgameplay automationを追加すること
- 現在ONの5 RPO項目だけを製品全体として扱うこと
- 未検証shader/backendを「対応」と表記すること
- model wrapperがcompileできたことだけでM3 visual parity完了と主張すること

## Source / Licenseについて

AMATERASリソースパックは**behavior / visual reference baseline**として扱います。内部構造やbinary assetをそのままBlockLensへコピーすることは目標ではありません。

baselineには他作者へのattributionが含まれており、redistribution permissionがあるとは仮定しません。公開前に、runtime JARへ残る第三者texture / model / textなどのライセンス・再配布可否を必ず確認します。M3では現状、source binaryをコピーせずオリジナルのprocedural描画処理を優先しています。

## ドキュメント

- Engineering rules: [`AGENTS.md`](AGENTS.md)
- Knowledge index: [`knowledge/index.md`](knowledge/index.md)
- Product contract: [`knowledge/current/product-spec.md`](knowledge/current/product-spec.md)
- Architecture: [`knowledge/current/architecture.md`](knowledge/current/architecture.md)
- Quality strategy: [`knowledge/current/quality-strategy.md`](knowledge/current/quality-strategy.md)
- Performance strategy: [`knowledge/current/performance-strategy.md`](knowledge/current/performance-strategy.md)
- Roadmap: [`knowledge/current/roadmap.md`](knowledge/current/roadmap.md)
- M3 visual semantics: [`knowledge/current/m3-visual-semantics.md`](knowledge/current/m3-visual-semantics.md)
- Engineering Graph Loop: [`knowledge/current/engineering-loop.md`](knowledge/current/engineering-loop.md)

`knowledge/current/` が現在仕様の正本です。READMEはユーザー向けの要約であり、current specificationと食い違った場合はREADME側を修正します。
