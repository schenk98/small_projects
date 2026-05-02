/**
 * One-shot Mongo cleanup for deprecated legacy cosmetics from older schema versions.
 *
 * Run:
 *   mongosh "mongodb://localhost:27017/poe_pet" cleanup-legacy-pet-cosmetics.js
 */
const dbname = db.getName();
print(`Running cleanup in database: ${dbname}`);

const delOwned = db.owned_cosmetics.deleteMany({});
print(`owned_cosmetics removed: ${delOwned.deletedCount}`);

const delCosmeticsInShop = db.shop_items.deleteMany({ type: "COSMETIC" });
print(`shop_items cosmetics removed: ${delCosmeticsInShop.deletedCount}`);

const delPetColors = db.pet_colors.deleteMany({});
print(`pet_colors removed: ${delPetColors.deletedCount}`);

const delPetShapes = db.pet_shapes.deleteMany({});
print(`pet_shapes removed: ${delPetShapes.deletedCount}`);

const unsetVisualFields = db.pets.updateMany(
  {},
  { $unset: { shapeCode: "", colorCode: "", eyeCode: "", mouthCode: "", accessoryCode: "" } }
);
print(`pets visual fields unset: ${unsetVisualFields.modifiedCount}`);

print("Done.");
