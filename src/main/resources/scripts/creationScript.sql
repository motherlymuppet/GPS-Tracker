/* 1.0.0.A6 */

CREATE TABLE constants (
  key   VARCHAR(40) NOT NULL,
  value VARCHAR(40) NOT NULL,

  PRIMARY KEY (key)
);

CREATE TABLE vans (
  imei BIGINT      NOT NULL,
  name VARCHAR(20) NULL,
  PRIMARY KEY (imei),
  UNIQUE (name)
);

CREATE TABLE data (
  id        IDENTITY      NOT NULL,
  time      TIMESTAMP     NOT NULL,
  van_imei  VARCHAR(20) REFERENCES vans (imei) ON DELETE CASCADE ON UPDATE CASCADE,
  latitude  DECIMAL(9, 6) NOT NULL,
  longitude DECIMAL(9, 6) NOT NULL,
  speed     DECIMAL(4, 2) NOT NULL,

  PRIMARY KEY (id),
  UNIQUE (time, van_imei)
);

INSERT INTO constants (key, value) VALUES ('version', '1.0.0.A6');