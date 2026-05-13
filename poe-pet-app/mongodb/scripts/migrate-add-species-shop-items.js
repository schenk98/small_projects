db = db.getSiblingDB('poe_pet');

/**
 * Upsert missing SPECIES shop items into an existing MongoDB.
 *
 * Usage (container): docker exec -it <mongodb-container> mongosh -u admin -p admin123 /docker-entrypoint-initdb.d/../scripts/migrate-add-species-shop-items.js
 * Usage (local): mongosh "mongodb://admin:admin123@localhost:27017/poe_pet?authSource=admin" ./mongodb/scripts/migrate-add-species-shop-items.js
 */

const items = [
  { code: "species_penguin", name: "Pet: Chubby Penguin", speciesCode: "penguin", priceCoins: 1000 },
  { code: "species_fox", name: "Pet: Little Cheeky Fox", speciesCode: "fox", priceCoins: 1000 },
  { code: "species_hamster", name: "Pet: Stuffed-Cheek Hamster", speciesCode: "hamster", priceCoins: 1000 },
  { code: "species_tiger", name: "Pet: Little Tiger Cub", speciesCode: "tiger", priceCoins: 1000 },
  { code: "species_lion", name: "Pet: Lion Cub", speciesCode: "lion", priceCoins: 1000 },
  { code: "species_horse", name: "Pet: Horse", speciesCode: "horse", priceCoins: 1000 },
  { code: "species_parrot", name: "Pet: Parrot", speciesCode: "parrot", priceCoins: 1000 },
  { code: "species_panda", name: "Pet: Chubby Clueless Panda", speciesCode: "panda", priceCoins: 1000 },
  { code: "species_goldfish", name: "Pet: Goldfish Aquarium", speciesCode: "goldfish", priceCoins: 1000 },
  { code: "species_lizard", name: "Pet: Little Lizard", speciesCode: "lizard", priceCoins: 1000 },
  { code: "species_unicorn", name: "Legendary Pet: Unicorn", speciesCode: "unicorn", priceCoins: 3000 },
  { code: "species_midnight_cat", name: "Legendary Pet: Midnight Cat", speciesCode: "midnight_cat", priceCoins: 3000 },
];

for (const it of items) {
  db.shop_items.updateOne(
    { code: it.code },
    {
      $setOnInsert: {
        type: "SPECIES",
        shopSection: "SPECIES",
        stackable: false,
        oneTimePurchase: true,
        effectKey: null,
      },
      $set: {
        name: it.name,
        description: `Unlocks ${it.speciesCode.replace("_", " ")} as a selectable pet.`,
        priceCoins: it.priceCoins,
        effects: [{ kind: "GRANT_SPECIES", speciesCode: it.speciesCode }],
        active: true,
        playerVisible: true,
      },
    },
    { upsert: true },
  );
}

print(`Upserted species shop items: ${items.length}`);

