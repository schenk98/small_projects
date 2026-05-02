/**
 * Run in mongosh against poe_pet (pre-release migration).
 * Renames mood slot key dead -> playing_dead on pets.
 * Then re-seed shop + pet_visual_assets (e.g. run mongodb/init/01-seed.js) so catalog matches the app.
 */
db = db.getSiblingDB('poe_pet');

db.pets.find({ 'moodAssetCodes.dead': { $exists: true } }).forEach(function (doc) {
  var m = doc.moodAssetCodes || {};
  if (Object.prototype.hasOwnProperty.call(m, 'dead')) {
    m.playing_dead = m.dead;
    delete m.dead;
    db.pets.updateOne({ _id: doc._id }, { $set: { moodAssetCodes: m } });
  }
});
