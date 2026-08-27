# Food Catalog Asset Design

## Decision

The nutrition catalog uses human-maintained JSON as its authoritative source and
build-generated sharded JSON as the Android runtime asset. Protobuf, FlatBuffers,
SQLite, online updates, and external CSV/Excel import are out of scope for now.

The catalog is expected to remain below 500 records. The design therefore favors
debuggability and reliable round trips over a custom binary format.

## Source and Generated Assets

The maintained source is split by kind under
`tools/food_catalog/source/{ingredients,foods,dishes}.json`. The original
`app/src/main/assets/food_nutrition.json` is retained as a migration/comparison
snapshot and is accepted as a one-time input by `split`.
`tools/food_catalog/catalog_tool.py compile` generates:

```text
app/src/main/assets/food_catalog/
├── manifest.json
├── records/
│   ├── ingredients/<id-with-colons-replaced-by-underscores>.json
│   ├── foods/<id-with-colons-replaced-by-underscores>.json
│   └── dishes/<id-with-colons-replaced-by-underscores>.json
└── indexes/
    ├── by_id.json
    ├── search.json
    ├── categories.json
    └── related_dishes.json
```

Every generated filename replaces `:` with `_`. The compiler rejects collisions
after this conversion. Generated JSON uses the same readable two-space
indentation convention as the source; the source remains formatted and suitable
for review. The generated records preserve the source semantics and can be
exported back to one JSON document with `export`.

The generated image manifest is keyed by the food ID. An explicit `FoodImage`
key is used first for custom/user files; when a built-in record has the default
image key, the runtime uses that record ID to find its generated image variants.

## Nutrient Contract

`app/src/main/assets/DRIs/nutrients_meta.json` is the only allowed nutrient-key
registry. The compiler rejects every nutrient code that is not present there.
Missing values mean unknown, not zero. Ingredient values use the per-100g edible
portion convention. Food and dish nutrition remains runtime-derived through
`NutritionResolver`.

The compiler keeps the existing amount object shape, including
`unitCategory`/`unitId`, for compatibility with the unit conversion system.
These fields are intentionally not deduplicated: micronutrients may use `mg`
while macronutrients use `g`, and every amount must remain explicit at the DTO
boundary. Semantic round-trip equality includes the unit fields.

## Runtime Repository

`FoodNutrientRepository.fromContext` reads the generated manifest. Record files
are loaded on demand by ID. The existing `foods()` API still materializes the
complete list for current UI callers; `find` can resolve a single generated
record without first materializing the catalog. Future screens should prefer
`find`, index-backed search, and category queries so they do not require a full
catalog load.

The legacy `fromAsset` entry point continues to read a monolithic JSON file for
JVM tests and migration verification.

## Images

Only developer-provided source images are inputs. The image pipeline will rename
them using the same safe ID convention and generate WebP thumbnail/detail
variants. Original files are never runtime cache entries and are never removed by
application cache cleanup, food editing, or food deletion. Food image identity
must remain distinct per food record; sharing an image is not implicit.

Original image binaries are developer-local inputs and may be excluded from the
repository. The mapping file, image build script, and generated Android image
assets remain versioned so the build contract is documented and reproducible
when the source images are available.

## Custom Foods

Custom food data remains a per-user JSON archive because each user has at most a
small number of records and needs migration/export support. Custom records are
not compiled into the APK catalog. They may reference built-in ingredients and
foods, but not other custom records in the current release. Historical diet
entries retain nutrition snapshots. User originals remain separate from
regenerable image caches.

## Tooling

Run all catalog commands through the repository virtual environment:

```text
uv run python tools/food_catalog/catalog_tool.py validate
uv run python tools/food_catalog/catalog_tool.py split
uv run python tools/food_catalog/catalog_tool.py compile
uv run python tools/food_catalog/catalog_tool.py export
uv run python tools/food_catalog/catalog_tool.py inspect
uv run python tools/food_catalog/catalog_tool.py verify-roundtrip
```

The compiler validates IDs, kinds, references, dish cycles, nutrient keys,
generated filename collisions, and search/category/related-dish indexes.

`categoryTags` stores leaf tags only. For example, an item with
`food.oil.animal_fat` must not also store `food.oil`; ancestor filtering remains
supported by the dotted-tag `isWithin` rule. Run `normalize` once when importing
older source data; later redundant parent tags are rejected by `validate`.

The generated catalog is checked in as an Android asset because the project is
currently in local testing. Re-run `compile` after changing the source JSON;
the command only replaces generated records, indexes, and manifest metadata and
preserves existing generated image variants.
