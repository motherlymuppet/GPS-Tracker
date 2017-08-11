SELECT
  data.time,
  data.van_imei,
  CASE WHEN vans.name IS NULL
    THEN CAST(vans.imei AS VARCHAR(50))
  ELSE vans.name END AS van_name,
  data.latitude,
  data.longitude,
  data.speed
FROM data
  INNER JOIN vans
    ON data.van_imei = vans.imei