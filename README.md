# KWormhole

A tool to help synchronize files between machines.

```
    ┌──────────────────────────────┐
    │       KWormhole Server       │
    │                              │
    │  Fat KFR            Fat KFR  │
    └──────────────────────────────┘
       Thin KFRs        Thin KFRs
         ▲
          │                  │
──────────┼──Transfer-Layer──┼──────────
          │                  │
                             ▼
       Thin KFRs        Thin KFRs
    ┌──────────────────────────────┐
    │  Fat KFR            Fat KFR  │
    │                              │
    │       KWormhole Client       │
    └──────────────────────────────┘
```
