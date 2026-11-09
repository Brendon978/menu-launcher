CREATE TABLE glitches (
  code TEXT PRIMARY KEY,
  secret_hash TEXT NOT NULL,
  name TEXT NOT NULL,
  color TEXT NOT NULL,
  accessory TEXT NOT NULL,
  buddy TEXT NOT NULL,
  created_at INTEGER NOT NULL
);
CREATE TABLE friends (
  owner TEXT NOT NULL REFERENCES glitches(code) ON DELETE CASCADE,
  friend TEXT NOT NULL REFERENCES glitches(code) ON DELETE CASCADE,
  PRIMARY KEY (owner, friend)
);
CREATE INDEX friends_por_amigo ON friends(friend);
CREATE TABLE visits (
  to_code TEXT NOT NULL REFERENCES glitches(code) ON DELETE CASCADE,
  from_code TEXT NOT NULL REFERENCES glitches(code) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  PRIMARY KEY (to_code, from_code)
);
