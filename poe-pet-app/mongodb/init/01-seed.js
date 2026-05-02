db = db.getSiblingDB('poe_pet');

db.shop_items.deleteMany({});
db.shop_items.insertMany([
  { code: "food_small_50", type: "CONSUMABLE", shopSection: "CONSUMABLES", name: "Meal Pack +50", description: "Feeds pet for 50 hunger points.", priceCoins: 10, effects: [{ kind: "HUNGER_ADD", value: 50 }], stackable: true, oneTimePurchase: false, active: true, playerVisible: true, effectKey: null },
  { code: "food_boost_20_regen_10pct_10h", type: "CONSUMABLE", shopSection: "CONSUMABLES", name: "Boost Snack +20", description: "Feeds +20 hunger and boosts energy regeneration by 10% for 10 hours.", priceCoins: 10, effects: [{ kind: "HUNGER_ADD", value: 20 }, { kind: "ENERGY_REGEN_MULTIPLIER", value: 0.1, durationHours: 10, effectName: "fast_regen" }], stackable: true, oneTimePurchase: false, active: true, playerVisible: true, effectKey: "fast_regen" },
  { code: "food_combo_10_energy_25pct", type: "CONSUMABLE", shopSection: "CONSUMABLES", name: "Quick Bite +10", description: "Feeds +10 hunger and restores 25% energy instantly.", priceCoins: 10, effects: [{ kind: "HUNGER_ADD", value: 10 }, { kind: "ENERGY_PERCENT_ADD", value: 0.25 }], stackable: true, oneTimePurchase: false, active: true, playerVisible: true, effectKey: null },
  { code: "food_cozy_bites", type: "CONSUMABLE", shopSection: "CONSUMABLES", name: "Cozy Bites", description: "Small treat: +12 hunger and +15 happiness.", priceCoins: 12, effects: [{ kind: "HUNGER_ADD", value: 12 }, { kind: "HAPPINESS_ADD", value: 15 }], stackable: true, oneTimePurchase: false, active: true, playerVisible: true, effectKey: null },
  { code: "snack_coin_boost", type: "CONSUMABLE", shopSection: "CONSUMABLES", name: "Lucky Syrup (+20% coins)", description: "Feeds +5 hunger and multiplies coin rewards from minigames by 1.2 for 4 hours.", priceCoins: 15, effects: [{ kind: "HUNGER_ADD", value: 5 }, { kind: "COIN_MULTIPLIER", value: 1.2, durationHours: 4 }], stackable: true, oneTimePurchase: false, active: true, playerVisible: true, effectKey: "gold_rush" },
  { code: "resurrection_basic", type: "CONSUMABLE", shopSection: "CONSUMABLES", name: "Revive Snack", description: "Sets hunger, happiness and energy to 10%.", priceCoins: 200, effects: [{ kind: "SET_HUNGER_PERCENT", value: 0.1 }, { kind: "SET_HAPPINESS_PERCENT", value: 0.1 }, { kind: "SET_ENERGY_PERCENT", value: 0.1 }], stackable: true, oneTimePurchase: false, active: true, playerVisible: true, effectKey: null },
  { code: "cosmetic_bg_aurora", type: "COSMETIC", shopSection: "COSMETICS", name: "Scene: Aurora Hills", description: "Unlocks this background for your pet stage (equip in Customize).", priceCoins: 40, effects: [{ kind: "GRANT_VISUAL", visualAssetCode: "bg_aurora_default" }], stackable: false, oneTimePurchase: true, active: true, playerVisible: true, effectKey: null },
  { code: "cosmetic_bg_candy", type: "COSMETIC", shopSection: "COSMETICS", name: "Scene: Candy Dream", description: "Unlocks this background for your pet stage.", priceCoins: 40, effects: [{ kind: "GRANT_VISUAL", visualAssetCode: "bg_candy_default" }], stackable: false, oneTimePurchase: true, active: true, playerVisible: true, effectKey: null },
  { code: "cosmetic_bg_dusk", type: "COSMETIC", shopSection: "COSMETICS", name: "Scene: Dusk Glow", description: "Unlocks this background for your pet stage.", priceCoins: 45, effects: [{ kind: "GRANT_VISUAL", visualAssetCode: "bg_dusk_default" }], stackable: false, oneTimePurchase: true, active: true, playerVisible: true, effectKey: null },
  { code: "cosmetic_bg_underwater", type: "COSMETIC", shopSection: "COSMETICS", name: "Scene: Underwater", description: "Unlocks this background for your pet stage.", priceCoins: 45, effects: [{ kind: "GRANT_VISUAL", visualAssetCode: "bg_underwater_default" }], stackable: false, oneTimePurchase: true, active: true, playerVisible: true, effectKey: null },
  { code: "cosmetic_fg_sparkle", type: "COSMETIC", shopSection: "COSMETICS", name: "Overlay: Sparkle Frame", description: "Unlocks a soft sparkle foreground overlay.", priceCoins: 35, effects: [{ kind: "GRANT_VISUAL", visualAssetCode: "fg_sparkle_default" }], stackable: false, oneTimePurchase: true, active: true, playerVisible: true, effectKey: null }
]);

db.pet_visual_assets.deleteMany({});
db.pet_visual_assets.insertMany([
  { code: "dog_happy_default", assetType: "PET_MOOD", speciesCode: "dog", moodCode: "happy", label: "Dog Happy (Default)", imagePath: "/pet-assets/dog/happy-default.png", starter: true, active: true },
  { code: "dog_sad_default", assetType: "PET_MOOD", speciesCode: "dog", moodCode: "sad", label: "Dog Sad (Default)", imagePath: "/pet-assets/dog/sad-default.png", starter: true, active: true },
  { code: "dog_hungry_default", assetType: "PET_MOOD", speciesCode: "dog", moodCode: "hungry", label: "Dog Hungry (Default)", imagePath: "/pet-assets/dog/hungry-default.png", starter: true, active: true },
  { code: "dog_tired_default", assetType: "PET_MOOD", speciesCode: "dog", moodCode: "tired", label: "Dog Tired (Default)", imagePath: "/pet-assets/dog/tired-default.png", starter: true, active: true },
  { code: "dog_playing_dead_default", assetType: "PET_MOOD", speciesCode: "dog", moodCode: "playing_dead", label: "Dog Playing Dead (Default)", imagePath: "/pet-assets/dog/playing-dead-default.png", starter: true, active: true },
  { code: "cat_happy_default", assetType: "PET_MOOD", speciesCode: "cat", moodCode: "happy", label: "Cat Happy (Default)", imagePath: "/pet-assets/cat/happy-default.png", starter: true, active: true },
  { code: "cat_sad_default", assetType: "PET_MOOD", speciesCode: "cat", moodCode: "sad", label: "Cat Sad (Default)", imagePath: "/pet-assets/cat/sad-default.png", starter: true, active: true },
  { code: "cat_hungry_default", assetType: "PET_MOOD", speciesCode: "cat", moodCode: "hungry", label: "Cat Hungry (Default)", imagePath: "/pet-assets/cat/hungry-default.png", starter: true, active: true },
  { code: "cat_tired_default", assetType: "PET_MOOD", speciesCode: "cat", moodCode: "tired", label: "Cat Tired (Default)", imagePath: "/pet-assets/cat/tired-default.png", starter: true, active: true },
  { code: "cat_playing_dead_default", assetType: "PET_MOOD", speciesCode: "cat", moodCode: "playing_dead", label: "Cat Playing Dead (Default)", imagePath: "/pet-assets/cat/playing-dead-default.png", starter: true, active: true },
  { code: "bg_aurora_default", assetType: "BACKGROUND", speciesCode: "all", moodCode: "", label: "Aurora Hills", imagePath: "/cosmetic-staging/backgrounds/theme-aurora.svg", starter: false, active: true },
  { code: "bg_candy_default", assetType: "BACKGROUND", speciesCode: "all", moodCode: "", label: "Candy Dream", imagePath: "/cosmetic-staging/backgrounds/theme-candy.svg", starter: false, active: true },
  { code: "bg_dusk_default", assetType: "BACKGROUND", speciesCode: "all", moodCode: "", label: "Dusk Glow", imagePath: "/cosmetic-staging/backgrounds/theme-dusk.svg", starter: false, active: true },
  { code: "bg_underwater_default", assetType: "BACKGROUND", speciesCode: "all", moodCode: "", label: "Underwater", imagePath: "/cosmetic-staging/backgrounds/theme-underwater.svg", starter: false, active: true },
  { code: "fg_soft_vignette_default", assetType: "FOREGROUND", speciesCode: "all", moodCode: "", label: "Soft vignette (free)", imagePath: "/cosmetic-staging/foregrounds/soft-vignette.svg", starter: true, active: true },
  { code: "fg_sparkle_default", assetType: "FOREGROUND", speciesCode: "all", moodCode: "", label: "Sparkle frame", imagePath: "/cosmetic-staging/foregrounds/sparkle-frame.svg", starter: false, active: true }
]);

db.minigames.deleteMany({});
db.minigames.insertMany([
  {
    code: "higher_lower",
    name: "Higher or Lower",
    description: "Guess if the next random number from 1 to 100 will be higher or lower.",
    energyCost: 3,
    active: true,
    rewardStrategy: { type: "SHIFTED_FIBONACCI", sequenceStart: [1, 2], maxReward: 48 },
    happinessImpactStrategy: {
      type: "SCORE_THRESHOLDS",
      thresholds: [
        { minScore: 0, maxScore: 0, happinessDeltaPercent: -0.1 },
        { minScore: 1, maxScore: 1, happinessDeltaPercent: 0.0 },
        { minScore: 2, maxScore: 999, happinessDeltaPercentPerPoint: 0.125, maxPositivePercent: 0.625 }
      ]
    }
  },
  {
    code: "puzzle_swap",
    name: "Puzzle Swap",
    description: "Rebuild shuffled image by swapping two tiles.",
    energyCost: 4,
    active: true,
    rewardStrategy: { type: "SCORE_LINEAR", coinsPerPoint: 2, maxReward: 96 },
    happinessImpactStrategy: { type: "SCORE_THRESHOLDS" }
  },
  {
    code: "connect4_ai",
    name: "Connect 4 AI",
    description: "Play Connect 4 against AI with multiple difficulty levels.",
    energyCost: 5,
    active: true,
    rewardStrategy: { type: "CONNECT4_OUTCOME", rewards: { win: 9, draw: 4, loss: 1 } },
    happinessImpactStrategy: { type: "SCORE_THRESHOLDS" }
  },
  {
    code: "minesweep_ai",
    name: "Minesweeper",
    description: "Clear the field without hitting a mine. Mines are placed after your first reveal (safe first click).",
    energyCost: 4,
    active: true,
    rewardStrategy: { type: "CONNECT4_OUTCOME", rewards: { win: 10, draw: 0, loss: 1 } },
    happinessImpactStrategy: { type: "SCORE_THRESHOLDS" }
  },
  {
    code: "checkers_ai",
    name: "Checkers vs AI",
    description: "American checkers on an 8×8 board. Capture when available. Three AI strengths.",
    energyCost: 5,
    active: true,
    rewardStrategy: { type: "CONNECT4_OUTCOME", rewards: { win: 8, draw: 3, loss: 1 } },
    happinessImpactStrategy: { type: "SCORE_THRESHOLDS" }
  }
]);
db.owned_cosmetics.deleteMany({});
db.pet_colors.deleteMany({});
db.pet_shapes.deleteMany({});
