/* 1.0.0.A1 */

CREATE TABLE constants (
  key   VARCHAR(40) NOT NULL,
  value VARCHAR(40) NOT NULL,

  PRIMARY KEY (key)
);

CREATE TABLE vans (
  id   IDENTITY    NOT NULL,
  name VARCHAR(20) NOT NULL,

  PRIMARY KEY (id),

  UNIQUE (name)
);

CREATE TABLE data (
  id IDENTITY NOT NULL,
  time TIMESTAMP NOT NULL,
  van_id BIGINT REFERENCES vans(id) ON DELETE CASCADE ON UPDATE CASCADE,
  latitude DECIMAL(9, 6) NOT NULL,
  longitude DECIMAL(9, 6) NOT NULL,

  PRIMARY KEY (id),
  UNIQUE (id, van_id)
);

INSERT INTO constants (key, value) VALUES ('version', '1.0.0.A1');
INSERT INTO vans (name) VALUES ('steven')